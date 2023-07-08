package com.example.request;

import cn.hutool.json.JSONUtil;
import com.github.benmanes.caffeine.cache.Cache;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;

/**
 * Enter the description of this class here
 *
 * @author weasley
 * @version 1.0.0
 */
@Slf4j
@SpringBootTest
class CaffeineCacheTest {

    @Autowired
    Cache<String, String> cache1;
    @Autowired
    Cache<String, LocalDateTime> cache2;

    /**
     * 存入对象到缓存，并设置特定键的过期时间
     */
    @Test
    void stringCacheTest() throws InterruptedException {
        // 存入对象到缓存，并设置特定键的过期时间

        String key = "my_key";
        String value = "存入对象到缓存，并设置特定键的过期时间";
        int expire = 3;

        cache1.policy().expireVariably().ifPresent(c -> {
            c.put(key, value, expire, TimeUnit.SECONDS);
        });
        log.info("Cached value: " + cache1.getIfPresent(key));
        // 等待过期时间后再次获取对象
        TimeUnit.SECONDS.sleep(4);
        log.info("Expired value: " + cache1.getIfPresent(key));

        String key2 = "my_key_2";
        LocalDateTime value2 = LocalDateTime.now();
        int expire2 = 2;

        cache2.policy().expireVariably().ifPresent(c -> {
            c.put(key2, value2, expire2, TimeUnit.SECONDS);
        });
        log.info("Cached value2: " + JSONUtil.toJsonStr(cache2.getIfPresent(key2)));
        TimeUnit.SECONDS.sleep(3);
        log.info("Expired value2: " + JSONUtil.toJsonStr(cache2.getIfPresent(key2)));
    }
}
