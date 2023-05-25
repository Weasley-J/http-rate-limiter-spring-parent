package io.github.weasleyj.http.rate.limit;

import io.github.weasleyj.http.rate.limit.annotation.EnableHttpRateLimiter;
import io.github.weasleyj.http.rate.limit.annotation.RateLimit;
import io.github.weasleyj.http.rate.limit.config.HttpRateLimitProperties;
import io.github.weasleyj.http.rate.limit.entity.RedisKeyRequest;
import io.github.weasleyj.http.rate.limit.entity.RedisVersion;
import io.github.weasleyj.http.rate.limit.util.TemporalUnitUtils;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RRateLimiter;
import org.redisson.api.RateIntervalUnit;
import org.redisson.api.RateLimiterConfig;
import org.redisson.api.RedissonClient;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletRequest;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.redisson.api.RateType.PER_CLIENT;

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
    private final RedisVersion redisVersion;
    private final RedissonClient httpRateLimitRedissonClient;
    private final HttpRateLimitProperties httpRateLimitProperties;

    public DefaultRedissonRateLimitStrategy(RedisVersion redisVersion, RedissonClient httpRateLimitRedissonClient, HttpRateLimitProperties httpRateLimitProperties) {
        this.redisVersion = redisVersion;
        this.httpRateLimitRedissonClient = httpRateLimitRedissonClient;
        this.httpRateLimitProperties = httpRateLimitProperties;
    }

    @Override
    public boolean tryLimit(RateLimit rateLimit, Map<String, Object> headers, HttpServletRequest request) throws InterruptedException {
        String rateLimitKey = HttpRateLimitHandler.getRateLimitKey(new RedisKeyRequest()
                .setHeaders(headers)
                .setRateLimit(rateLimit)
                .setHttpServletRequest(request)
                .setHttpRateLimitProperties(this.httpRateLimitProperties));
        RRateLimiter rRateLimiter = httpRateLimitRedissonClient.getRateLimiter(rateLimitKey);

        if (!rRateLimiter.isExists()) {
            rRateLimiter.trySetRate(PER_CLIENT, rateLimit.maxCount(), rateLimit.value(), toRateIntervalUnit(rateLimit.timeUnit()));
            expireRateLimitKey(rateLimit, rRateLimiter);
        }
        // 获取限流的配置信息
        RateLimiterConfig rateLimiterConfig = rRateLimiter.getConfig();
        // 上次配置的限流时间毫秒值
        Long rateInterval = rateLimiterConfig.getRateInterval();
        // 上次配置的限流次数
        Long rate = rateLimiterConfig.getRate();
        // 将timeOut转换成毫秒之后再跟rateInterval进行比较
        if (TimeUnit.MILLISECONDS.convert(rateLimit.value(), rateLimit.timeUnit()) != rateInterval || rateLimit.maxCount() != rate) {
            // RateLimiterConfig的配置跟我们注解上面的值不一致, 删除原有配置, 重新设置
            rRateLimiter.delete();
            rRateLimiter.trySetRate(PER_CLIENT, rateLimit.maxCount(), rateLimit.value(), toRateIntervalUnit(rateLimit.timeUnit()));
            expireRateLimitKey(rateLimit, rRateLimiter);
        }
        // 是否触发限流
        boolean acquire = rRateLimiter.tryAcquire();
        return !acquire;
    }

    /**
     * Expire rate limit key
     *
     * @param rateLimit    RateLimit
     * @param rRateLimiter RRateLimiter
     */
    protected void expireRateLimitKey(RateLimit rateLimit, RRateLimiter rRateLimiter) {
        if (redisVersion.getIntVersion() >= 7)
            rRateLimiter.expireIfNotSet(Duration.of(rateLimit.value(), TemporalUnitUtils.toChronoUnit(rateLimit.timeUnit())));
        else
            rRateLimiter.expire(Duration.of(rateLimit.value(), TemporalUnitUtils.toChronoUnit(rateLimit.timeUnit())));
    }

    /**
     * Convert TimeUnit to RateIntervalUnit
     *
     * @param timeUnit {@code TimeUnit}
     * @return RateIntervalUnit
     */
    protected RateIntervalUnit toRateIntervalUnit(TimeUnit timeUnit) {
        switch (timeUnit) {
            case MILLISECONDS:
                return RateIntervalUnit.MILLISECONDS;
            case SECONDS:
                return RateIntervalUnit.SECONDS;
            case MINUTES:
                return RateIntervalUnit.MINUTES;
            case HOURS:
                return RateIntervalUnit.HOURS;
            case DAYS:
                return RateIntervalUnit.DAYS;
            default:
                throw new UnsupportedOperationException("Unsupported timeUnit when org.redisson.api.RRateLimiter used.");
        }
    }
}
