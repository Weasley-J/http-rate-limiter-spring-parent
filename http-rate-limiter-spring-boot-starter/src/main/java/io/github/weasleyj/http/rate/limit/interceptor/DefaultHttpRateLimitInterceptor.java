package io.github.weasleyj.http.rate.limit.interceptor;

import io.github.weasleyj.http.rate.limit.RedisVersion;
import io.github.weasleyj.http.rate.limit.RequestLimitHandler;
import io.github.weasleyj.http.rate.limit.annotation.EnableHttpRateLimiter;
import io.github.weasleyj.http.rate.limit.annotation.RateLimit;
import io.github.weasleyj.http.rate.limit.config.HttpRateLimitProperties;
import io.github.weasleyj.http.rate.limit.exception.FrequentRequestException;
import io.github.weasleyj.http.rate.limit.util.TemporalUnitUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.redisson.api.RBucket;
import org.redisson.api.RSemaphore;
import org.redisson.api.RedissonClient;
import org.redisson.client.codec.StringCodec;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.DigestUtils;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.nio.charset.StandardCharsets;
import java.text.MessageFormat;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

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

    private final StringCodec stringCodec;
    private final RedisVersion redisVersion;
    private final RedissonClient httpRateLimitRedissonClient;
    private final HttpRateLimitProperties httpRateLimitProperties;

    public DefaultHttpRateLimitInterceptor(StringCodec stringCodec,
                                           RedisVersion redisVersion,
                                           RedissonClient httpRateLimitRedissonClient,
                                           HttpRateLimitProperties httpRateLimitProperties) {
        this.stringCodec = stringCodec;
        this.redisVersion = redisVersion;
        this.httpRateLimitRedissonClient = httpRateLimitRedissonClient;
        this.httpRateLimitProperties = httpRateLimitProperties;
    }


    /**
     * Get Api Restrict Annotation
     */
    public static RateLimit getRateLimitAnnotation(Object handler) {
        HandlerMethod handlerMethod;
        if (!handler.getClass().isAssignableFrom(HandlerMethod.class)) return null;
        else handlerMethod = (HandlerMethod) handler;
        RateLimit methodAnnotation = handlerMethod.getMethodAnnotation(RateLimit.class);
        RateLimit classAnnotation = handlerMethod.getMethod().getDeclaringClass().getAnnotation(RateLimit.class);
        return methodAnnotation != null ? methodAnnotation : classAnnotation;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if (httpRateLimitProperties.getEnable().equals(false)) return true;
        if (RequestLimitHandler.shouldCancelLimit()) return true;

        RateLimit rateLimit = getRateLimitAnnotation(handler);
        if (rateLimit == null) return true;

        String realTokenValue = null;
        Map<String, Object> headerMap = new ConcurrentHashMap<>();
        if (StringUtils.isNotBlank(rateLimit.headName()) && StringUtils.isNotBlank(request.getHeader(rateLimit.headName()))) {
            realTokenValue = request.getHeader(rateLimit.headName());
            headerMap.put(rateLimit.headName(), realTokenValue);
        } else if (StringUtils.isNotBlank(rateLimit.cookieName()) && null != request.getCookies() && request.getCookies().length > 0) {
            for (Cookie cookie : request.getCookies()) {
                if (Objects.equals(rateLimit.cookieName(), cookie.getName()) && StringUtils.isNotBlank(cookie.getValue())) {
                    realTokenValue = cookie.getValue();
                    headerMap.put(rateLimit.cookieName(), realTokenValue);
                    break;
                }
            }
        }

        if (StringUtils.isBlank(realTokenValue)) {
            headerMap.putAll(handleHeaderValueFromHttpHeader(request));
            handleHeaderValueFromCookie(request, headerMap);
            if (CollectionUtils.isEmpty(headerMap)) {
                if (log.isWarnEnabled()) {
                    log.warn("DefaultRequestRestrictInterceptor请求头缺失，不触发限流；{}", httpRateLimitProperties.getHeaderKeys());
                }
                return true;
            }
        }

        boolean shouldLimit = shouldLimit(rateLimit, headerMap, request);
        if (shouldLimit) {
            log.warn("触发防刷，接口URI：{}, header_map: {}", request.getRequestURI(), headerMap);
            String formatMsg = MessageFormat.format("接口：{0}, {1} {2} 内仅能请求 {3} 次", request.getRequestURI(), rateLimit.value(), rateLimit.timeUnit().toString().toLowerCase(), rateLimit.maxCount());
            throw new FrequentRequestException("操作太过频繁，请稍后再试；" + formatMsg);
        }

        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        boolean shouldCancelRestrict = RequestLimitHandler.shouldCancelLimit();
        if (shouldCancelRestrict) {
            Map<String, Object> headerMap = handleHeaderValueFromHttpHeader(request);
            handleHeaderValueFromCookie(request, headerMap);
            if (CollectionUtils.isEmpty(headerMap)) return;

            RateLimit rateLimit = getRateLimitAnnotation(handler);
            if (rateLimit == null) return;

            String redisKeyName = getRedisKeyName(headerMap, rateLimit, request);
            if (null == redisKeyName) return;

            RBucket<Object> bucket = httpRateLimitRedissonClient.getBucket(redisKeyName);
            if (bucket.isExists()) bucket.delete();

            RequestLimitHandler.remove();
        }
    }

    /**
     * 判断请求接口是否需要防重复提交
     *
     * @return false: 不需要防
     */
    public boolean shouldLimit(RateLimit rateLimit, Map<String, Object> headerMap, HttpServletRequest request) throws InterruptedException {
        if (CollectionUtils.isEmpty(headerMap)) return false;

        String redisKeyName = getRedisKeyName(headerMap, rateLimit, request);
        if (null == redisKeyName) return false;
        if (log.isDebugEnabled()) {
            log.debug("判断请求接口是否需要防重复提交, redis_key_name: {}, header_map: {}", redisKeyName, headerMap);
        }
        RBucket<Object> bucket = httpRateLimitRedissonClient.getBucket(redisKeyName, stringCodec);
        if (!bucket.isExists()) {
            RSemaphore semaphore = httpRateLimitRedissonClient.getSemaphore(redisKeyName);
            semaphore.trySetPermits(rateLimit.maxCount());
            if (redisVersion.getIntVersion() >= 7) {
                semaphore.expireIfNotSet(Duration.of(rateLimit.value(), TemporalUnitUtils.toChronoUnit(rateLimit.timeUnit())));
            } else bucket.setIfExists(rateLimit.maxCount(), rateLimit.value(), rateLimit.timeUnit());
            return false;
        }

        RSemaphore semaphore = httpRateLimitRedissonClient.getSemaphore(redisKeyName);
        semaphore.tryAcquire(1, 100, TimeUnit.MILLISECONDS);
        int availablePermits = semaphore.availablePermits();
        return availablePermits == 0;
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
                if (StringUtils.isBlank(xAuthToken) && null != request.getCookies() && request.getCookies().length > 0) {
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
     * Get the Redis Key Name
     */
    public String getRedisKeyName(Map<String, Object> headerMap, RateLimit rateLimit, HttpServletRequest request) {
        if (CollectionUtils.isEmpty(headerMap)) return null;
        HttpRateLimitProperties.RedisProperties redis = httpRateLimitProperties.getRedis();
        if (StringUtils.isNotBlank(rateLimit.headName())) {
            return redis.getKeyPrefix() + DigestUtils.md5DigestAsHex((headerMap.get(rateLimit.headName()) + ":" + request.getRequestURI()).getBytes(StandardCharsets.UTF_8));
        }
        if (StringUtils.isNotBlank(rateLimit.cookieName())) {
            return redis.getKeyPrefix() + DigestUtils.md5DigestAsHex((headerMap.get(rateLimit.cookieName()) + ":" + request.getRequestURI()).getBytes(StandardCharsets.UTF_8));
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
        if (null == realTokenValue) {
            return null;
        }
        String md5DigestAsHex = DigestUtils.md5DigestAsHex((redisKeyName + ":" + request.getRequestURI()).getBytes(StandardCharsets.UTF_8));
        return redis.getKeyPrefix() + md5DigestAsHex;
    }
}
