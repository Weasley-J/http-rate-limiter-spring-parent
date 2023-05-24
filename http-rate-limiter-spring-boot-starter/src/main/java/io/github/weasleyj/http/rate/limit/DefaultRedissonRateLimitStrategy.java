package io.github.weasleyj.http.rate.limit;

import io.github.weasleyj.http.rate.limit.annotation.EnableHttpRateLimiter;
import io.github.weasleyj.http.rate.limit.annotation.RateLimit;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletRequest;
import java.util.Map;

/**
 * The rate limit algorithm strategy implementation of {@code RedissonRateLimiter}
 *
 * @author weasley
 * @version 1.0.0
 * @see org.redisson.RedissonRateLimiter
 */
@Slf4j
@Component
@ConditionalOnClass({EnableHttpRateLimiter.class})
public class DefaultRedissonRateLimitStrategy implements RateLimitStrategy {
    @Override
    public boolean tryLimit(RateLimit rateLimit, Map<String, Object> headers, HttpServletRequest request) throws InterruptedException {
        // TODO: 2023/5/20
        return false;
    }
}
