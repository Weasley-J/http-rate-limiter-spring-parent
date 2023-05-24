package io.github.weasleyj.http.rate.limit.annotation;

import io.github.weasleyj.http.rate.limit.DefaultCounterRateLimitStrategy;
import io.github.weasleyj.http.rate.limit.DefaultRedissonRateLimitStrategy;
import io.github.weasleyj.http.rate.limit.config.HttpRateLimitProperties;
import io.github.weasleyj.http.rate.limit.config.HttpRateLimitRedissonConfig;
import io.github.weasleyj.http.rate.limit.config.HttpRateLimitWebMvcConfig;
import io.github.weasleyj.http.rate.limit.config.RateLimitStrategyConfig;
import io.github.weasleyj.http.rate.limit.interceptor.DefaultHttpRateLimitInterceptor;
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
@Import({HttpRateLimitWebMvcConfig.class, HttpRateLimitProperties.class,
        DefaultHttpRateLimitInterceptor.class, HttpRateLimitRedissonConfig.class,
        DefaultCounterRateLimitStrategy.class, DefaultRedissonRateLimitStrategy.class,
        RateLimitStrategyConfig.class,
})
public @interface EnableHttpRateLimiter {
}
