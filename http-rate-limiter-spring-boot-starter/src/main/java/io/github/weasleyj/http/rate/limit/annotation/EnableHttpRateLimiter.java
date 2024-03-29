package io.github.weasleyj.http.rate.limit.annotation;

import io.github.weasleyj.http.rate.limit.DefaultCounterRateLimitStrategy;
import io.github.weasleyj.http.rate.limit.DefaultRedissonRateLimitStrategy;
import io.github.weasleyj.http.rate.limit.HttpRateLimitHandler;
import io.github.weasleyj.http.rate.limit.config.HttpRateLimitRedissonConfig;
import io.github.weasleyj.http.rate.limit.config.HttpRateLimitWebMvcConfig;
import io.github.weasleyj.http.rate.limit.config.RateLimitStrategyConfig;
import org.springframework.context.annotation.Import;

import java.lang.annotation.*;

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
@Import({HttpRateLimitWebMvcConfig.class, RateLimitStrategyConfig.class,
        HttpRateLimitHandler.class, HttpRateLimitRedissonConfig.class,
        DefaultCounterRateLimitStrategy.class, DefaultRedissonRateLimitStrategy.class,
})
public @interface EnableHttpRateLimiter {
}
