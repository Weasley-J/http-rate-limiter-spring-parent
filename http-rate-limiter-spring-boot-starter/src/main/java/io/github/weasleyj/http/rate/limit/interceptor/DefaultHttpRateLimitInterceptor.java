package io.github.weasleyj.http.rate.limit.interceptor;

import io.github.weasleyj.http.rate.limit.RateLimitStrategy;
import io.github.weasleyj.http.rate.limit.RequestLimitHandler;
import io.github.weasleyj.http.rate.limit.Strategy;
import io.github.weasleyj.http.rate.limit.annotation.EnableHttpRateLimiter;
import io.github.weasleyj.http.rate.limit.annotation.RateLimit;
import io.github.weasleyj.http.rate.limit.config.HttpRateLimitProperties;
import io.github.weasleyj.http.rate.limit.entity.RedisKeyRequest;
import io.github.weasleyj.http.rate.limit.exception.FrequentRequestException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.redisson.api.RBucket;
import org.redisson.api.RedissonClient;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.DigestUtils;
import org.springframework.util.ObjectUtils;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.nio.charset.StandardCharsets;
import java.text.MessageFormat;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Default Request Restrict Interceptor
 *
 * @author weasley
 * @version 1.0.0
 */
@Slf4j
@Component
@ConditionalOnClass({EnableHttpRateLimiter.class})
public class DefaultHttpRateLimitInterceptor implements HandlerInterceptor {
    /**
     * The throttling policy is dynamically changed at runtime
     */
    public static final ThreadLocal<RateLimitStrategy> RUNTIME_STRATEGY = new ThreadLocal<>();
    private final RedissonClient httpRateLimitRedissonClient;
    private final HttpRateLimitProperties httpRateLimitProperties;
    private final Map<Strategy, RateLimitStrategy> rateLimitStrategyMap;

    public DefaultHttpRateLimitInterceptor(RedissonClient httpRateLimitRedissonClient, HttpRateLimitProperties httpRateLimitProperties, Map<Strategy, RateLimitStrategy> rateLimitStrategyMap) {
        this.httpRateLimitRedissonClient = httpRateLimitRedissonClient;
        this.httpRateLimitProperties = httpRateLimitProperties;
        this.rateLimitStrategyMap = rateLimitStrategyMap;
    }

    /**
     * Get the key name of limit-key in redis
     */
    public static String getRateLimitKey(RedisKeyRequest keyRequest) {
        if (CollectionUtils.isEmpty(keyRequest.getHeaders())) return null;
        HttpServletRequest httpServletRequest = keyRequest.getHttpServletRequest();
        HttpRateLimitProperties.RedisProperties redis = keyRequest.getHttpRateLimitProperties().getRedis();
        Map<String, Object> headerMap = keyRequest.getHeaders();
        RateLimit rateLimit = keyRequest.getRateLimit();
        if (StringUtils.isNotBlank(rateLimit.headName())) {
            return redis.getKeyPrefix() + DigestUtils.md5DigestAsHex((headerMap.get(rateLimit.headName()) + ":" + httpServletRequest.getRequestURI()).getBytes(StandardCharsets.UTF_8));
        }
        if (StringUtils.isNotBlank(rateLimit.cookieName())) {
            return redis.getKeyPrefix() + DigestUtils.md5DigestAsHex((headerMap.get(rateLimit.cookieName()) + ":" + httpServletRequest.getRequestURI()).getBytes(StandardCharsets.UTF_8));
        }
        String redisKeyName = null;
        String realTokenValue = null;
        Set<Map.Entry<String, Object>> entries = headerMap.entrySet();
        for (Map.Entry<String, Object> entry : entries) {
            realTokenValue = entry.getValue().toString();
            if (StringUtils.isNotBlank(realTokenValue)) {
                redisKeyName = realTokenValue;
                break;
            }
        }
        if (null == realTokenValue) return null;
        String md5DigestAsHex = DigestUtils.md5DigestAsHex((redisKeyName + ":" + httpServletRequest.getRequestURI()).getBytes(StandardCharsets.UTF_8));
        return redis.getKeyPrefix() + md5DigestAsHex;
    }


    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if (httpRateLimitProperties.getEnable().equals(false)) return true;
        if (RequestLimitHandler.shouldCancelLimit()) return true;

        RateLimit rateLimit = getRateLimitAnnotation(handler);
        if (rateLimit == null) return true;

        String realTokenValue = null;
        Map<String, Object> headers = new ConcurrentHashMap<>();
        if (StringUtils.isNotBlank(rateLimit.headName()) && StringUtils.isNotBlank(request.getHeader(rateLimit.headName()))) {
            realTokenValue = request.getHeader(rateLimit.headName());
            headers.put(rateLimit.headName(), realTokenValue);
        } else if (StringUtils.isNotBlank(rateLimit.cookieName()) && !ObjectUtils.isEmpty(request.getCookies())) {
            for (Cookie cookie : request.getCookies()) {
                if (Objects.equals(rateLimit.cookieName(), cookie.getName()) && StringUtils.isNotBlank(cookie.getValue())) {
                    realTokenValue = cookie.getValue();
                    headers.put(rateLimit.cookieName(), realTokenValue);
                    break;
                }
            }
        }

        if (StringUtils.isBlank(realTokenValue)) {
            headers.putAll(handleHeaderValueFromHttpHeader(request));
            handleHeaderValueFromCookie(request, headers);
            if (CollectionUtils.isEmpty(headers)) {
                if (log.isWarnEnabled()) {
                    log.warn("DefaultRequestRestrictInterceptor请求头缺失，不触发限流；{}", httpRateLimitProperties.getHeaderKeys());
                }
                return true;
            }
        }
        boolean shouldLimit = this.deduceRateLimitStrategy().tryLimit(rateLimit, headers, request);
        if (shouldLimit) {
            log.warn("触发防刷，接口URI：{}, header_map: {}", request.getRequestURI(), headers);
            String formatMsg = MessageFormat.format("接口：{0}, {1} {2} 内仅能请求 {3} 次", request.getRequestURI(), rateLimit.value(), rateLimit.timeUnit().toString().toLowerCase(), rateLimit.maxCount());
            throw new FrequentRequestException("操作太过频繁，请稍后再试；" + formatMsg);
        }

        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        RUNTIME_STRATEGY.remove();
        if (RequestLimitHandler.shouldCancelLimit()) {
            Map<String, Object> headers = handleHeaderValueFromHttpHeader(request);
            handleHeaderValueFromCookie(request, headers);
            if (CollectionUtils.isEmpty(headers)) return;

            RateLimit rateLimit = getRateLimitAnnotation(handler);
            if (rateLimit == null) return;

            String redisKeyName = getRateLimitKey(new RedisKeyRequest()
                    .setHeaders(headers)
                    .setRateLimit(rateLimit)
                    .setHttpServletRequest(request)
                    .setHttpRateLimitProperties(this.httpRateLimitProperties));
            if (null == redisKeyName) return;

            RBucket<Object> bucket = httpRateLimitRedissonClient.getBucket(redisKeyName);
            if (bucket.isExists()) bucket.delete();

            RequestLimitHandler.remove();
        }
    }

    /**
     * handle header value from http header
     */
    public Map<String, Object> handleHeaderValueFromHttpHeader(HttpServletRequest request) {
        if (CollectionUtils.isEmpty(httpRateLimitProperties.getHeaderKeys())) return Collections.emptyMap();

        Map<String, Object> headerMap = new LinkedHashMap<>();
        Enumeration<String> headNames = request.getHeaderNames();
        while (headNames.hasMoreElements()) {
            String headName = headNames.nextElement();
            for (String headerKey : httpRateLimitProperties.getHeaderKeys()) {
                if (headerKey.toLowerCase().equals(headName)) {
                    headerMap.put(headName, request.getHeader(headName));
                }
            }
        }
        return headerMap;
    }

    /**
     * handle header value from cookie
     */
    public void handleHeaderValueFromCookie(HttpServletRequest request, Map<String, Object> headerMap) {
        String xAuthToken;
        for (String headerKey : httpRateLimitProperties.getHeaderKeys()) {
            try {
                xAuthToken = request.getHeader(headerKey);
                if (StringUtils.isBlank(xAuthToken) && !ObjectUtils.isEmpty(request.getCookies())) {
                    for (Cookie cookie : request.getCookies()) {
                        if (headerKey.equalsIgnoreCase(cookie.getName())) {
                            if (log.isDebugEnabled()) {
                                log.debug("Cookie解析到token {}: {}", cookie.getName(), cookie.getValue());
                            }
                            xAuthToken = cookie.getValue();
                            headerMap.put(headerKey, cookie.getValue());
                            break;
                        }
                    }
                }
            } catch (Exception e) {
                log.error("解析headerKe {}发生异常 {}", headerKey, headerMap, e);
            }
        }
    }

    /**
     * Get rate limit annotation
     */
    public RateLimit getRateLimitAnnotation(Object handler) {
        HandlerMethod handlerMethod;
        if (!handler.getClass().isAssignableFrom(HandlerMethod.class)) return null;
        else handlerMethod = (HandlerMethod) handler;
        RateLimit methodAnnotation = handlerMethod.getMethodAnnotation(RateLimit.class);
        RateLimit classAnnotation = handlerMethod.getMethod().getDeclaringClass().getAnnotation(RateLimit.class);
        return methodAnnotation != null ? methodAnnotation : classAnnotation;
    }

    /**
     * @return The RateLimitStrategy
     */
    public RateLimitStrategy deduceRateLimitStrategy() {
        if (null != RUNTIME_STRATEGY.get()) {
            return RUNTIME_STRATEGY.get();
        }
        return this.rateLimitStrategyMap.get(this.httpRateLimitProperties.getStrategy());
    }
}
