package io.github.weasleyj.http.rate.limit.config;

import io.github.weasleyj.http.rate.limit.Version;
import io.github.weasleyj.http.rate.limit.annotation.EnableHttpRateLimiter;
import io.github.weasleyj.http.rate.limit.entity.RedisVersion;
import lombok.Data;
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
import redis.clients.jedis.HostAndPort;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisClientConfig;

/**
 * Request Restrict Redisson Config
 *
 * @author weasley
 * @version 1.0.0
 */
@Slf4j
@Configuration(proxyBeanMethods = false)
@ConditionalOnClass({EnableHttpRateLimiter.class})
public class HttpRateLimitRedissonConfig {
    private final HttpRateLimitProperties redisProperties;

    public HttpRateLimitRedissonConfig(HttpRateLimitProperties redisProperties) {
        this.redisProperties = redisProperties;
    }

    /**
     * @return RedissonClient
     */
    @Bean
    @ConditionalOnMissingBean(value = {RedissonClient.class}, name = {"httpRateLimitRedissonClient"})
    public RedissonClient httpRateLimitRedissonClient() {
        HttpRateLimitProperties.RedisProperties redis = redisProperties.getRedis();
        Config config = new Config();
        SingleServerConfig singleServer = config.useSingleServer()
                .setUsername(redis.getUsername())
                .setPassword(redis.getPassword())
                .setDatabase(redis.getDatabase());
        if (redis.getEnableSsl().equals(true)) {
            singleServer.setAddress("rediss://" + redis.getHost() + ":" + redis.getPort());
        } else {
            singleServer.setAddress("redis://" + redis.getHost() + ":" + redis.getPort());
        }
        String version = Version.getVersion();
        if (StringUtils.isNotBlank(version)) {
            log.info("Http rate limiter version: " + version);
        }
        return Redisson.create(config);
    }

    @Bean
    @ConditionalOnMissingBean({StringCodec.class})
    public StringCodec stringCodec() {
        return new StringCodec();
    }

    @Bean
    @ConditionalOnMissingBean({RedisVersion.class})
    public RedisVersion redisVersion() {
        RedisVersion version = new RedisVersion();
        try (Jedis jedis = new Jedis(new HostAndPort(redisProperties.getRedis().getHost(), redisProperties.getRedis().getPort()), new RequestRestrictJedisClientConfig(redisProperties.getRedis()))) {
            String server = jedis.info("server");
            if (StringUtils.isNotBlank(server)) {
                String[] serverInfos = server.split("\r\n");
                for (String serverInfo : serverInfos) {
                    if ("redis_version".equals(serverInfo.split(":")[0])) {
                        version.setVersion(serverInfo.split(":")[1]);
                        version.setIntVersion(Integer.parseInt(version.getVersion().split("\\.")[0]));
                        break;
                    }
                }
            }
        }
        log.info("Redis version {}", version.getVersion());
        return version;
    }

    /**
     * Request Restrict Jedis Client Config
     */
    @Data
    public static class RequestRestrictJedisClientConfig implements JedisClientConfig {
        /**
         * The properties of redis
         */
        private final HttpRateLimitProperties.RedisProperties redis;

        RequestRestrictJedisClientConfig(HttpRateLimitProperties.RedisProperties redis) {
            this.redis = redis;
        }

        @Override
        public String getUser() {
            return redis.getUsername();
        }

        @Override
        public String getPassword() {
            return redis.getPassword();
        }

        @Override
        public int getDatabase() {
            return redis.getDatabase();
        }

        @Override
        public boolean isSsl() {
            return redis.getEnableSsl();
        }
    }
}
