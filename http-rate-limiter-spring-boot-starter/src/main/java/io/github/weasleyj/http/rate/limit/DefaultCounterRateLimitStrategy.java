package io.github.weasleyj.http.rate.limit;

import io.github.weasleyj.http.rate.limit.annotation.EnableHttpRateLimiter;
import io.github.weasleyj.http.rate.limit.annotation.RateLimit;
import io.github.weasleyj.http.rate.limit.config.HttpRateLimitProperties;
import io.github.weasleyj.http.rate.limit.entity.RedisKeyRequest;
import io.github.weasleyj.http.rate.limit.entity.RedisVersion;
import io.github.weasleyj.http.rate.limit.util.TemporalUnitUtils;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RBucket;
import org.redisson.api.RSemaphore;
import org.redisson.api.RedissonClient;
import org.redisson.client.codec.StringCodec;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import javax.servlet.http.HttpServletRequest;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * The counter rate limit algorithm strategy
 *
 * @author weasley
 * @version 1.0.0
 */
@Slf4j
@Component
@ConditionalOnClass({EnableHttpRateLimiter.class})
public class DefaultCounterRateLimitStrategy implements RateLimitStrategy {

    private final StringCodec stringCodec;
    private final RedisVersion redisVersion;
    private final RedissonClient httpRateLimitRedissonClient;
    private final HttpRateLimitProperties httpRateLimitProperties;

    public DefaultCounterRateLimitStrategy(StringCodec stringCodec, RedisVersion redisVersion, RedissonClient httpRateLimitRedissonClient, HttpRateLimitProperties httpRateLimitProperties) {
        this.stringCodec = stringCodec;
        this.redisVersion = redisVersion;
        this.httpRateLimitRedissonClient = httpRateLimitRedissonClient;
        this.httpRateLimitProperties = httpRateLimitProperties;
    }

    @Override
    public boolean tryLimit(RateLimit rateLimit, Map<String, Object> headers, HttpServletRequest request) throws InterruptedException {
        if (CollectionUtils.isEmpty(headers)) return false;
        String rateLimitKey = HttpRateLimitHandler.getRateLimitKey(new RedisKeyRequest()
                .setHeaders(headers)
                .setRateLimit(rateLimit)
                .setHttpServletRequest(request)
                .setHttpRateLimitProperties(this.httpRateLimitProperties));
        if (null == rateLimitKey) return false;
        if (log.isDebugEnabled()) {
            log.debug("判断请求接口是否需要防重复提交,  redis_key_name: {}, headers: {}", rateLimitKey, headers);
        }
        RBucket<Object> bucket = httpRateLimitRedissonClient.getBucket(rateLimitKey, stringCodec);
        if (!bucket.isExists()) {
            RSemaphore semaphore = httpRateLimitRedissonClient.getSemaphore(rateLimitKey);
            semaphore.trySetPermits(rateLimit.maxCount());
            if (redisVersion.getIntVersion() >= 7) {
                semaphore.expireIfNotSet(Duration.of(rateLimit.value(), TemporalUnitUtils.toChronoUnit(rateLimit.timeUnit())));
            } else bucket.setIfExists(rateLimit.maxCount(), rateLimit.value(), rateLimit.timeUnit());
            return false;
        }

        RSemaphore semaphore = httpRateLimitRedissonClient.getSemaphore(rateLimitKey);
        semaphore.tryAcquire(1, 100, TimeUnit.MILLISECONDS);
        int availablePermits = semaphore.availablePermits();
        return availablePermits == 0;
    }

}
