package io.github.weasleyj.request.restrict.config;

import io.github.weasleyj.request.restrict.Version;
import io.github.weasleyj.request.restrict.annotation.EnableApiRestrict;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.client.codec.StringCodec;
import org.redisson.config.Config;
import org.redisson.config.SingleServerConfig;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Request Restrict Redisson Config
 *
 * @author weasley
 * @version 1.0.0
 */
@Slf4j
@Configuration(proxyBeanMethods = false)
@ConditionalOnClass({EnableApiRestrict.class})
public class RequestRestrictRedissonConfig {
    private final RequestRestrictHeaderProperties headerProperties;

    public RequestRestrictRedissonConfig(RequestRestrictHeaderProperties headerProperties) {
        this.headerProperties = headerProperties;
    }

    /**
     * @return RedissonClient
     */
    @Bean
    @ConditionalOnMissingBean({RedissonClient.class})
    public RedissonClient redissonClient() {
        RequestRestrictHeaderProperties.RedisProperties redis = headerProperties.getRedis();
        Config config = new Config();
        SingleServerConfig singleServer = config.useSingleServer()
                .setPassword(redis.getPassword())
                .setDatabase(redis.getDatabase());
        if (redis.getEnableSsl().equals(true)) {
            singleServer.setAddress("rediss://" + redis.getHost() + ":" + redis.getPort());
        } else {
            singleServer.setAddress("redis://" + redis.getHost() + ":" + redis.getPort());
        }
        String version = Version.getVersion();
        if (StringUtils.isNotBlank(version)) {
            log.info("Http request restrict version: " + version);
        }
        return Redisson.create(config);
    }

    @Bean
    @ConditionalOnMissingBean({StringCodec.class})
    public StringCodec stringCodec() {
        return new StringCodec();
    }
}
