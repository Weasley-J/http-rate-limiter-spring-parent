package io.github.weasleyj.http.rate.limit;

/**
 * Rate limit strategy
 *
 * @author weasley
 */
public enum Strategy {
    /**
     * Counter strategy
     */
    COUNTER,
    /**
     * Redisson rate limiter strategy
     */
    REDISSON_RATE_LIMITER,
    /**
     * The customize strategy of users
     */
    CUSTOMIZE,
    ;
}
