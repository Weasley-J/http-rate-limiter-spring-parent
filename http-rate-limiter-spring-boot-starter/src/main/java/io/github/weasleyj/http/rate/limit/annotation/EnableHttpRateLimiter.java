package io.github.weasleyj.http.rate.limit.annotation;

import io.github.weasleyj.http.rate.limit.DefaultCounterRateLimitStrategy;
import io.github.weasleyj.http.rate.limit.DefaultRedissonRateLimitStrategy;
import io.github.weasleyj.http.rate.limit.HttpRateLimitHandler;
import io.github.weasleyj.http.rate.limit.config.CaffeineCacheConfig;
import io.github.weasleyj.http.rate.limit.config.CaffeineHttpRateLimitWebMvcConfig;
import io.github.weasleyj.http.rate.limit.config.HttpRateLimitRedissonConfig;
import io.github.weasleyj.http.rate.limit.config.RateLimitStrategyConfig;
import io.github.weasleyj.http.rate.limit.config.RedisHttpRateLimitWebMvcConfig;
import org.springframework.context.annotation.Import;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * The annotation to enable http rate limiter
 *
 * @author weasley
 * @version 1.0.0
 */
@Inherited
@Documented
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Import({RedisHttpRateLimitWebMvcConfig.class, RateLimitStrategyConfig.class,
        HttpRateLimitHandler.class, HttpRateLimitRedissonConfig.class,
        DefaultCounterRateLimitStrategy.class, DefaultRedissonRateLimitStrategy.class,
        CaffeineCacheConfig.class, CaffeineHttpRateLimitWebMvcConfig.class
})
public @interface EnableHttpRateLimiter {
}
