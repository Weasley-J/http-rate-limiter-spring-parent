package com.example.request.controller;

import com.example.request.entity.Human;
import com.example.request.entity.HumanDTO;
import com.example.request.util.RedisCastErrorUtil;
import com.example.request.util.RedisCastErrorUtil.RedisCastWrapper;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RBucket;
import org.redisson.api.RedissonClient;
import org.redisson.codec.TypedJsonJacksonCodec;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Enter the description of this class here
 *
 * @author weasley
 * @version 1.0.0
 */
@Slf4j
@RestController
@RequestMapping("/api/public/cache")
public class CacheDemoController {
    protected static final String PREFIX = "redisson_client:test:human";
    @Autowired
    private RedissonClient httpRateLimitRedissonClient;

    /**
     * 缓存用对象
     */
    @PostMapping("/user/add/{id}")
    @CachePut(value = PREFIX, key = "#id") // key: redisson_client:test:human:id
    public Human add(@PathVariable("id") Long id) {
        log.warn("add {}", id);
        Human human = new Human();
        human.setId(id);
        human.setName("李四_" + id);
        human.setAge(28);
        human.setSex(1);
        human.setAddress("上海市长宁区泉口路109弄116号-" + id);
        return human;
    }

    /**
     * 从缓存中获取用户
     */
    @GetMapping("/user/info/{id}")
    @Cacheable(value = PREFIX, key = "#root.args[0]")
    public Human getHuman(@PathVariable("id") Long id) {
        log.warn("getHuman id {}", id);
        return new Human();
    }

    /**
     * 从缓存中获取用户
     */
    @GetMapping("/user/info/dto/{id}")
    public HumanDTO getHumanDTO(@PathVariable("id") Long id) {
        log.warn("getHumanDTO id {}", id);
        String name = PREFIX + ":" + id;
        RBucket<HumanDTO> bucket = httpRateLimitRedissonClient.getBucket(name, new TypedJsonJacksonCodec(HumanDTO.class));
        try {
            if (bucket.isExists()) {
                return bucket.get();
            }
        } catch (Exception e) {
            bucket = RedisCastErrorUtil.handleRedisCastError(new RedisCastWrapper<HumanDTO>()
                    .setRedissonClient(httpRateLimitRedissonClient)
                    .setTargetBucket(bucket)
                    .setRedisKey(name)
                    .setPayload(null)
                    .setException(e));
        }
        return bucket.get();
    }

    /**
     * 从删除缓存用户
     */
    @DeleteMapping("/user/delete/{id}")
    @CacheEvict(value = PREFIX, key = "#root.args[0]")
    public Long delete(@PathVariable("id") Long id) {
        log.warn("delete id {}", id);
        return id;
    }

}
