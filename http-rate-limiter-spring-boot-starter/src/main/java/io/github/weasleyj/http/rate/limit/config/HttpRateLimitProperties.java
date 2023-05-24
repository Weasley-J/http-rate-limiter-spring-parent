package io.github.weasleyj.http.rate.limit.config;

import io.github.weasleyj.http.rate.limit.RateLimitStrategy;
import io.github.weasleyj.http.rate.limit.Strategy;
import io.github.weasleyj.http.rate.limit.annotation.EnableHttpRateLimiter;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.NestedConfigurationProperty;
import org.springframework.context.annotation.Configuration;

import java.util.List;

import static io.github.weasleyj.http.rate.limit.config.HttpRateLimitProperties.PREFIX;

/**
 * Http rate limit properties
 *
 * @author weasley
 * @version 1.0.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Configuration
@ConditionalOnClass({EnableHttpRateLimiter.class})
@ConfigurationProperties(prefix = PREFIX)
public class HttpRateLimitProperties {
    public static final String PREFIX = "spring.http.rate.limiter";
    /**
     * 是否启用
     */
    private Boolean enable = false;
    /**
     * Rate limit strategy
     */
    private Strategy strategy = Strategy.COUNTER;
    /**
     * The strategy Class of users defined
     *
     * @apiNote Prioritize the use of user-defined throttling algorithms if specified
     */
    private Class<? extends RateLimitStrategy> strategyClass;
    /**
     * API防刷请求头, 客户端传过来
     */
    private List<String> headerKeys;
    /**
     * redis配置属性
     */
    @NestedConfigurationProperty
    private RedisProperties redis;


    /**
     * redis配置属性
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RedisProperties {
        /**
         * Redis的缓存key的前缀
         */
        private String keyPrefix = "http:rate:limit:";
        /**
         * use "rediss://" for SSL connection
         */
        private Boolean enableSsl = false;
        /**
         * Redis host
         */
        private String host = "localhost";
        /**
         * Redis port
         */
        private int port = 6379;
        /**
         * Redis username
         */
        private String username;
        /**
         * Redis password
         */
        private String password;
        /**
         * Redis database
         */
        private Integer database;
    }
}
