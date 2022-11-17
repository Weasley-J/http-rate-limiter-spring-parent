package io.github.weasleyj.request.restrict.interceptor;

import cn.hutool.core.date.TemporalUtil;
import cn.hutool.json.JSONUtil;
import io.github.weasleyj.request.restrict.RequestRestrictHandler;
import io.github.weasleyj.request.restrict.annotation.ApiRestrict;
import io.github.weasleyj.request.restrict.annotation.EnableApiRestrict;
import io.github.weasleyj.request.restrict.config.RedisVersion;
import io.github.weasleyj.request.restrict.config.RequestRestrictProperties;
import io.github.weasleyj.request.restrict.exception.FrequentRequestException;
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
import java.util.Collections;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
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
@ConditionalOnClass({EnableApiRestrict.class})
public class DefaultRequestRestrictInterceptor implements HandlerInterceptor {

    private final StringCodec stringCodec;
    private final RedisVersion redisVersion;
    private final RedissonClient redissonClient;
    private final RequestRestrictProperties restrictHeaderProperties;

    public DefaultRequestRestrictInterceptor(StringCodec stringCodec,
                                             RedisVersion redisVersion,
                                             RedissonClient redissonClient,
                                             RequestRestrictProperties restrictHeaderProperties) {
        this.stringCodec = stringCodec;
        this.redisVersion = redisVersion;
        this.redissonClient = redissonClient;
        this.restrictHeaderProperties = restrictHeaderProperties;
    }


    /**
     * Get Api Restrict Annotation
     */
    public static ApiRestrict getApiRestrictAnnotation(Object handler) {
        HandlerMethod handlerMethod;
        if (!handler.getClass().isAssignableFrom(HandlerMethod.class)) return null;
        else handlerMethod = (HandlerMethod) handler;
        ApiRestrict methodAnnotation = handlerMethod.getMethodAnnotation(ApiRestrict.class);
        ApiRestrict classAnnotation = handlerMethod.getMethod().getDeclaringClass().getAnnotation(ApiRestrict.class);
        return methodAnnotation != null ? methodAnnotation : classAnnotation;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if (restrictHeaderProperties.getEnable().equals(false)) return true;
        if (RequestRestrictHandler.shouldCancelRestrict()) return true;

        ApiRestrict restrict = getApiRestrictAnnotation(handler);
        if (restrict == null) return true;

        String realTokenValue = null;
        Map<String, Object> headerMap = new ConcurrentHashMap<>();
        if (StringUtils.isNotBlank(restrict.headName()) && StringUtils.isNotBlank(request.getHeader(restrict.headName()))) {
            realTokenValue = request.getHeader(restrict.headName());
            headerMap.put(restrict.headName(), realTokenValue);
        } else if (StringUtils.isNotBlank(restrict.cookieName()) && null != request.getCookies() && request.getCookies().length > 0) {
            for (Cookie cookie : request.getCookies()) {
                if (Objects.equals(restrict.cookieName(), cookie.getName()) && StringUtils.isNotBlank(cookie.getValue())) {
                    realTokenValue = cookie.getValue();
                    headerMap.put(restrict.cookieName(), realTokenValue);
                    break;
                }
            }
        }

        if (StringUtils.isBlank(realTokenValue)) {
            headerMap.putAll(handleHeaderValueFromHttpHeader(request));
            handleHeaderValueFromCookie(request, headerMap);
            if (CollectionUtils.isEmpty(headerMap)) {
                if (log.isWarnEnabled()) {
                    log.warn("DefaultRequestRestrictInterceptor请求头缺失，不触发限流；{}", JSONUtil.toJsonStr(restrictHeaderProperties.getHeaderKeys()));
                }
                return true;
            }
        }

        boolean shouldRestrict = shouldRestrict(restrict, headerMap, request);
        if (Objects.equals(shouldRestrict, true)) {
            log.warn("触发防刷，接口URI：{}, header_map: {}", request.getRequestURI(), JSONUtil.toJsonStr(headerMap));
            String formatMsg = MessageFormat.format("接口：{0}, {1} {2}内仅能请求{3}次。", request.getRequestURI(), restrict.value(), restrict.timeUnit().toString().toLowerCase(), restrict.maxCount());
            throw new FrequentRequestException("操作太过频繁，请稍后再试；" + formatMsg + "。");
        }

        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        boolean shouldCancelRestrict = RequestRestrictHandler.shouldCancelRestrict();
        if (shouldCancelRestrict) {
            Map<String, Object> headerMap = handleHeaderValueFromHttpHeader(request);
            handleHeaderValueFromCookie(request, headerMap);
            if (CollectionUtils.isEmpty(headerMap)) return;

            ApiRestrict restrict = getApiRestrictAnnotation(handler);
            if (restrict == null) return;

            String redisKeyName = getRedisKeyName(headerMap, restrict, request);
            if (null == redisKeyName) return;

            RBucket<Object> bucket = redissonClient.getBucket(redisKeyName);
            if (bucket.isExists()) bucket.delete();

            RequestRestrictHandler.remove();
        }
    }

    /**
     * 判断请求接口是否需要防重复提交
     *
     * @return false: 不需要防
     */
    public boolean shouldRestrict(ApiRestrict restrict, Map<String, Object> headerMap, HttpServletRequest request) throws InterruptedException {
        if (CollectionUtils.isEmpty(headerMap)) return false;

        String redisKeyName = getRedisKeyName(headerMap, restrict, request);
        if (null == redisKeyName) return false;
        if (log.isDebugEnabled()) {
            log.debug("判断请求接口是否需要防重复提交, redis_key_name: {}, header_map: {}", redisKeyName, JSONUtil.toJsonStr(headerMap));
        }
        RBucket<Object> bucket = redissonClient.getBucket(redisKeyName, stringCodec);
        if (!bucket.isExists()) {
            RSemaphore semaphore = redissonClient.getSemaphore(redisKeyName);
            semaphore.trySetPermits(restrict.maxCount());
            if (redisVersion.getIntVersion() >= 7) {
                semaphore.expireIfNotSet(Duration.of(restrict.value(), TemporalUtil.toChronoUnit(restrict.timeUnit())));
            } else bucket.setIfExists(restrict.maxCount(), restrict.value(), restrict.timeUnit());
            return false;
        }

        RSemaphore semaphore = redissonClient.getSemaphore(redisKeyName);
        semaphore.tryAcquire(1, 100, TimeUnit.MILLISECONDS);
        int availablePermits = semaphore.availablePermits();
        return availablePermits == 0;
    }

    /**
     * handle header value from http header
     */
    public Map<String, Object> handleHeaderValueFromHttpHeader(HttpServletRequest request) {
        if (CollectionUtils.isEmpty(restrictHeaderProperties.getHeaderKeys())) return Collections.emptyMap();

        Map<String, Object> headerMap = new LinkedHashMap<>();
        Enumeration<String> headNames = request.getHeaderNames();
        while (headNames.hasMoreElements()) {
            String headName = headNames.nextElement();
            for (String headerKey : restrictHeaderProperties.getHeaderKeys()) {
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
        for (String headerKey : restrictHeaderProperties.getHeaderKeys()) {
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
                log.error("解析headerKe {}发生异常 {}", headerKey, JSONUtil.toJsonStr(headerMap), e);
            }
        }
    }

    /**
     * Get the Redis Key Name
     */
    public String getRedisKeyName(Map<String, Object> headerMap, ApiRestrict restrict, HttpServletRequest request) {
        if (CollectionUtils.isEmpty(headerMap)) return null;
        RequestRestrictProperties.RedisProperties redis = restrictHeaderProperties.getRedis();
        if (StringUtils.isNotBlank(restrict.headName())) {
            return redis.getKeyPrefix() + DigestUtils.md5DigestAsHex((headerMap.get(restrict.headName()) + ":" + request.getRequestURI()).getBytes(StandardCharsets.UTF_8));
        }
        if (StringUtils.isNotBlank(restrict.cookieName())) {
            return redis.getKeyPrefix() + DigestUtils.md5DigestAsHex((headerMap.get(restrict.cookieName()) + ":" + request.getRequestURI()).getBytes(StandardCharsets.UTF_8));
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
