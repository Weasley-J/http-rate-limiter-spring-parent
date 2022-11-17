package io.github.weasleyj.request.restrict.config;

import io.github.weasleyj.request.restrict.annotation.EnableApiRestrict;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.NestedConfigurationProperty;
import org.springframework.context.annotation.Configuration;

import java.util.List;

import static io.github.weasleyj.request.restrict.config.RequestRestrictProperties.PREFIX;

/**
 * Api Restrict Properties
 *
 * @author weasley
 * @version 1.0.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Configuration
@ConditionalOnClass({EnableApiRestrict.class})
@ConfigurationProperties(prefix = PREFIX)
public class RequestRestrictProperties {
    public static final String PREFIX = "spring.request.restrict";
    /**
     * 是否启用
     */
    private Boolean enable = false;
    /**
     * API防刷请求头, 客户端传过来
     */
    private List<String> headerKeys;
    /**
     * redis配置属性
     */
    @NestedConfigurationProperty
    private RedisProperties redis;


    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RedisProperties {
        /**
         * Redis的缓存key的前缀
         */
        private String keyPrefix = "http:request.restrict:";
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
         * Redis user
         */
        private String user;
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
