package io.github.weasleyj.http.rate.limit;

/**
 * The rate limit algorithm strategy implementation of {@code RedissonRateLimiter}
 *
 * @author weasley
 * @version 1.0.0
 * @see org.redisson.RedissonRateLimiter
 */
public class DefaultRedissonRateLimitStrategy implements RateLimitStrategy {
    @Override
    public boolean tryLimit() {
        // TODO: 2023/5/20
        return false;
    }
}
