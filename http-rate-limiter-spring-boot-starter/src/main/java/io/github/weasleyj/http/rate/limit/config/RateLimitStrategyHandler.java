package io.github.weasleyj.http.rate.limit.config;

import io.github.weasleyj.http.rate.limit.DefaultCounterRateLimitStrategy;
import io.github.weasleyj.http.rate.limit.DefaultRedissonRateLimitStrategy;
import io.github.weasleyj.http.rate.limit.RateLimitStrategy;
import io.github.weasleyj.http.rate.limit.Strategy;
import io.github.weasleyj.http.rate.limit.annotation.EnableHttpRateLimiter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.objenesis.instantiator.util.ClassUtils;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Rate Limit Strategy Configuration
 *
 * @author weasley
 * @version 1.0.0
 */
@Configuration
@ConditionalOnClass({EnableHttpRateLimiter.class})
@EnableConfigurationProperties({HttpRateLimitProperties.class})
public class RateLimitStrategyHandler {

    /**
     * @return Rate limit strategy map
     */
    @Bean
    public Map<Strategy, RateLimitStrategy> rateLimitStrategyMap(ApplicationContext applicationContext, HttpRateLimitProperties httpRateLimitProperties) {
        Map<Strategy, RateLimitStrategy> strategies = new ConcurrentHashMap<>(16);
        if (null != httpRateLimitProperties.getStrategyClass()) {
            strategies.put(Strategy.CUSTOMIZATION, ClassUtils.newInstance(httpRateLimitProperties.getStrategyClass()));
        }
        Map<String, RateLimitStrategy> beans = applicationContext.getBeansOfType(RateLimitStrategy.class);
        beans.forEach((k, v) -> {
            if (k.equals(DefaultCounterRateLimitStrategy.class.getName())) {
                strategies.put(Strategy.COUNTER, v);
            }
            if (k.equals(DefaultRedissonRateLimitStrategy.class.getName())) {
                strategies.put(Strategy.REDISSON_RATE_LIMITER, v);
            }
        });
        return strategies;
    }

}
