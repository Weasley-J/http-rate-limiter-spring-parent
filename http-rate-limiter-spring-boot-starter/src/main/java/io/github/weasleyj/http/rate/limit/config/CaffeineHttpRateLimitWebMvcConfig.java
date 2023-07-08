package io.github.weasleyj.http.rate.limit.config;

import com.github.benmanes.caffeine.cache.Cache;
import io.github.weasleyj.http.rate.limit.HttpRateLimitHandler;
import io.github.weasleyj.http.rate.limit.annotation.EnableHttpRateLimiter;
import io.github.weasleyj.http.rate.limit.annotation.RateLimit;
import io.github.weasleyj.http.rate.limit.entity.CacheValue;
import io.github.weasleyj.http.rate.limit.entity.RedisKeyRequest;
import io.github.weasleyj.http.rate.limit.exception.FrequentRequestException;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import static io.github.weasleyj.http.rate.limit.HttpRateLimitHandler.getRateLimitKey;
import static io.github.weasleyj.http.rate.limit.config.HttpRateLimitProperties.PREFIX;

/**
 * Caffeine Http Rate Limit Web Mvc Configure
 *
 * @author weasley
 * @version 1.0.0
 */
@Slf4j
@Configuration
@AllArgsConstructor
@ConditionalOnClass({EnableHttpRateLimiter.class})
@ConditionalOnProperty(prefix = PREFIX, name = {"cache-type"}, havingValue = "memory")
public class CaffeineHttpRateLimitWebMvcConfig implements WebMvcConfigurer {
    private final HttpRateLimitProperties httpRateLimitProperties;
    private final Cache<String, CacheValue<String, Integer>> caffeineCache4RateLimiter;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        CaffeineHttpRateLimitInterceptor interceptor = new CaffeineHttpRateLimitInterceptor(httpRateLimitProperties, caffeineCache4RateLimiter);
        registry.addInterceptor(interceptor).addPathPatterns("/**");
    }

    @Data
    @Slf4j
    @AllArgsConstructor
    public static class CaffeineHttpRateLimitInterceptor implements HandlerInterceptor {
        private final HttpRateLimitProperties httpRateLimitProperties;
        private final Cache<String, CacheValue<String, Integer>> cache;
        private final Lock lock = new ReentrantLock();

        private RateLimit getRateLimit(Object handler) {
            if (!handler.getClass().isAssignableFrom(HandlerMethod.class)) {
                return null;
            } else {
                HandlerMethod handlerMethod = (HandlerMethod) handler;
                RateLimit methodAnnotation = handlerMethod.getMethodAnnotation(RateLimit.class);
                RateLimit classAnnotation = handlerMethod.getMethod().getDeclaringClass().getAnnotation(RateLimit.class);
                return ObjectUtils.defaultIfNull(methodAnnotation, classAnnotation);
            }
        }

        public void invalidIfExpired(String key) {
            if (null != cache.getIfPresent(key) && Objects.requireNonNull(cache.getIfPresent(key)).isExpired()) {
                cache.invalidate(key);
            }
        }

        @Override
        public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
            lock.lock();
            try {
                RateLimit rateLimit = getRateLimit(handler);
                if (null == rateLimit) return true;
                HttpRateLimitHandler rateLimitHandler = new HttpRateLimitHandler(null, httpRateLimitProperties, null);
                Map<String, Object> headers = new LinkedHashMap<>();
                headers.putAll(rateLimitHandler.handleHeaderValueFromHttpHeader(request));
                headers.putAll(rateLimitHandler.handleHeaderValueFromCookie(request, headers));
                String key = getRateLimitKey(new RedisKeyRequest()
                        .setHeaders(headers)
                        .setRateLimit(rateLimit)
                        .setHttpServletRequest(request)
                        .setHttpRateLimitProperties(this.httpRateLimitProperties));
                if (null == key) return true;
                boolean limit = false;
                if (null == cache.getIfPresent(key)) {
                    int maxCount = rateLimit.maxCount(); // value: clicks allowed
                    cache.put(key, new CacheValue<>(key, maxCount - 1, rateLimit.value(), TimeUnit.SECONDS));
                } else {
                    CacheValue<String, Integer> cacheValue = Objects.requireNonNull(cache.getIfPresent(key));
                    int maxCount;
                    if (cacheValue.isExpired() || cacheValue.getValue() == 0) {
                        limit = true;
                    } else {
                        maxCount = cacheValue.getValue() - 1;
                        cacheValue.setValue(maxCount);
                        cache.put(key, cacheValue);
                    }
                }
                invalidIfExpired(key);
                if (limit) {
                    throw new FrequentRequestException("操作太过频繁，请稍后再试: " + request.getRequestURI());
                }
                return true;
            } finally {
                lock.unlock();
            }
        }
    }
}
