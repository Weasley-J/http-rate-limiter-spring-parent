package io.github.weasleyj.http.rate.limit.annotation;

import io.github.weasleyj.http.rate.limit.RateLimitStrategy;
import io.github.weasleyj.http.rate.limit.config.HttpRateLimitProperties;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.concurrent.TimeUnit;

/**
 * Rate Limit
 * <p>
 * 注解优先级：方法上 > 类
 *
 * @author weasley
 * @version 1.0.0
 */
@Inherited
@Documented
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface RateLimit {

    /**
     * 同一个用户提交请求的时间间隔
     * <p>
     *
     * @return 时间, 默认时间单位：TimeUnit.SECONDS
     * @see TimeUnit#SECONDS
     */
    long value() default 1;

    /**
     * 在 value timeUnit 内，最大只能请求 maxCount 次
     */
    int maxCount() default 1;

    /**
     * @return 时间单位
     */
    TimeUnit timeUnit() default TimeUnit.SECONDS;

    /**
     * The head name from HttpServletRequest
     *
     * @apiNote priority: headName > headerKeys
     * @see HttpRateLimitProperties#headerKeys
     */
    String headName() default "";

    /**
     * The cookie name from HttpServletRequest
     *
     * @apiNote priority: cookieName > headerKeys
     * @see HttpRateLimitProperties#headerKeys
     */
    String cookieName() default "";

    /**
     * @return The rate limit strategy of users
     * @apiNote It's recommended to use the YAML to configure the throttling policy to improve performance
     * @see HttpRateLimitProperties#strategyClass
     */
    Class<? extends RateLimitStrategy> strategy() default RateLimitStrategy.class;
}
