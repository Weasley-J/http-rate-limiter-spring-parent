package com.example.request.util;

import cn.alphahub.dtt.plus.util.JacksonUtil;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.type.TypeReference;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RBucket;
import org.redisson.api.RedissonClient;
import org.springframework.lang.Nullable;

import java.io.Serializable;
import java.util.concurrent.TimeUnit;

/**
 * Redis Cast Error Util
 *
 * @author weasley
 * @version 1.0.0
 */
@Slf4j
public class RedisCastErrorUtil {
    /**
     * 处理redis类型转换异常
     *
     * @param wrapper Redis类型转换包装类
     * @return RedissonClient用来存取json数据的目标Bucket
     */
    public static <T> RBucket<T> handleCastError(RedisCastWrapper<T> wrapper) {
        if (wrapper.getException().getCause() instanceof JsonParseException
                || wrapper.getException().getCause() instanceof ClassCastException
                || wrapper.getException() instanceof JsonParseException
                || wrapper.getException() instanceof ClassCastException) {
            log.warn("处理Redis类型转换异常处理：{}", wrapper.getException().getMessage());
            RBucket<Object> originBucket = wrapper.getRedissonClient().getBucket(wrapper.getRedisKey());
            // 将本次需要存放与targetBucket中的数据进行缓存
            if (null != wrapper.getPayload()) {
                originBucket.delete();
                wrapper.getTargetBucket().set(wrapper.getPayload(), wrapper.getTimeToLive(), wrapper.getTimeUnit());
                return wrapper.getTargetBucket();
            }
            // 将旧数据转为json存储于targetBucket中
            if (originBucket.isExists() && null != originBucket.get()) {
                String jsonValue = JacksonUtil.toJson(originBucket.get());
                T originData = JacksonUtil.readValue(jsonValue, new TypeReference<T>() {
                });
                wrapper.getTargetBucket().set(originData, wrapper.getTimeToLive(), wrapper.getTimeUnit());
            }
        }
        return wrapper.getTargetBucket();
    }

    /**
     * Redis类型转换包装类
     *
     * @param <T> 目标Bucket中的Java类型
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Accessors(chain = true)
    public static class RedisCastWrapper<T> implements Serializable {
        /**
         * Redisson Client
         */
        private RedissonClient redissonClient;
        /**
         * 用来存取json数据的目标Bucket
         */
        private RBucket<T> targetBucket;
        /**
         * 源Redis的key
         */
        private String redisKey;
        /**
         * 过期时间
         */
        private long timeToLive = 30;
        /**
         * 过期时间单位
         */
        private TimeUnit timeUnit = TimeUnit.DAYS;
        /**
         * 数据载荷（本次需要存放与targetBucket中的数据）
         */
        @Nullable
        private T payload;
        /**
         * Redis类转相关异常
         */
        private Exception exception;
    }
}
