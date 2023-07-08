package io.github.weasleyj.http.rate.limit.config;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.Expiry;
import io.github.weasleyj.http.rate.limit.annotation.EnableHttpRateLimiter;
import lombok.extern.slf4j.Slf4j;
import org.checkerframework.checker.index.qual.NonNegative;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Memcached Client Configuration
 *
 * @author weasley
 * @version 1.0.0
 */
@Slf4j
@Configuration
@ConditionalOnClass({EnableHttpRateLimiter.class})
public class CaffeineCacheConfig {
    public static final String CACHE_BEAN_NAME = "caffeineCache4RateLimiter";

    /**
     * @param <T> The type for value
     * @return A singleton Cache
     */
    @Bean(name = {CACHE_BEAN_NAME})
    @ConditionalOnMissingBean(Cache.class)
    public <T> Cache<String, T> caffeineCache4RateLimiter() {
        Cache<String, T> cache = Caffeine.newBuilder()
                .expireAfter(new Expiry<String, T>() {
                    @Override
                    public long expireAfterCreate(String key, T value, long currentTime) {
                        return currentTime;
                    }

                    @Override
                    public long expireAfterUpdate(String key, T value, long currentTime, @NonNegative long currentDuration) {
                        return currentDuration;
                    }

                    @Override
                    public long expireAfterRead(String key, T value, long currentTime, @NonNegative long currentDuration) {
                        return currentDuration;
                    }
                })
                .build();

        log.info("Initializing Caffeine cache success.");
        return cache;
    }

}
