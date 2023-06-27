package com.example.request.retelimit;

import cn.alphahub.dtt.plus.util.JacksonUtil;
import com.example.request.entity.Human;
import com.example.request.entity.HumanDTO;
import com.example.request.util.RedisCastErrorUtil;
import com.example.request.util.RedisCastErrorUtil.RedisCastWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.redisson.api.RBucket;
import org.redisson.api.RedissonClient;
import org.redisson.codec.TypedJsonJacksonCodec;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.Serializable;
import java.util.concurrent.TimeUnit;

/**
 * http rate limit redisson client tests
 *
 * @author weasley
 * @version 1.0.0
 */
@Slf4j
@SpringBootTest
class HttpRateLimitRedissonClientTests {
    protected final String prefix = "redisson_client:test:user:info";

    @Autowired
    private RedissonClient httpRateLimitRedissonClient;


    @Test
    void testSaveOther() {
        RBucket<Integer> bucket = httpRateLimitRedissonClient.getBucket("redisson_client:test:user:add_age", new TypedJsonJacksonCodec(Integer.class));
        Integer andSet = bucket.getAndSet(666, 30, TimeUnit.DAYS);
        System.err.println(JacksonUtil.toJson(andSet));
    }

    @Test
    @DisplayName("旧方式存对象hex")
    void testSaveUserDefault() {
        User user = new User();
        user.setName("张三");
        user.setAge(18);
        user.setSex(1);
        RBucket<User> bucket = httpRateLimitRedissonClient.getBucket(prefix);
        bucket.set(user, 30, TimeUnit.DAYS); // 1\x00com.example.request.retelimit.Use\xf2\x01$\x83\xe5\xbc\xa0\xe4\xb8\x89\x01\x02
        User _user = bucket.get();
        System.err.println(JacksonUtil.toJson(_user));
    }

    @Test
    @DisplayName("新方式获取")
    void testGetUserDefault() {
        // 1\x00com.example.request.retelimit.Use\xf2\x01$\x83\xe5\xbc\xa0\xe4\xb8\x89\x01\x02
        RBucket<Person> targetBucket = httpRateLimitRedissonClient.getBucket(prefix, new TypedJsonJacksonCodec(new TypeReference<Person>() {
        }));
        try {
            if (targetBucket.isExists()) {
                Person person = targetBucket.get();
                // TODO: 2023/6/27 如果存在直接返回Bucket中的数据
            }
        } catch (Exception e) {
            targetBucket = RedisCastErrorUtil.handleCastError(new RedisCastWrapper<Person>()
                    .setRedissonClient(httpRateLimitRedissonClient)
                    .setTargetBucket(targetBucket)
                    .setRedisKey(prefix)
                    .setTimeToLive(1)
                    .setTimeUnit(TimeUnit.DAYS)
                    .setPayload(null)
                    .setException(e));
        }
        System.err.println(JacksonUtil.toJson(targetBucket.get())); //{"name":"张三","age":18,"sex":1,"ext":null}
    }

    @Test
    void testSaveUserBucket() {

        User user = new User();
        user.setName("张三");
        user.setAge(18);
        user.setSex(1);
        RBucket<User> bucket = httpRateLimitRedissonClient.getBucket(prefix, new TypedJsonJacksonCodec(new TypeReference<User>() {
        }));
        bucket.set(user, 30, TimeUnit.DAYS);
        System.err.println(JacksonUtil.toJson(user));
    }

    @Test
    void testGetUser() {
        RBucket<Person> bucket$person2 = httpRateLimitRedissonClient.getBucket(prefix, new TypedJsonJacksonCodec(Person.class));
        System.err.println(JacksonUtil.toJson(bucket$person2.get()) + "\n");
        RBucket<Person> bucket$person = httpRateLimitRedissonClient.getBucket(prefix, new TypedJsonJacksonCodec(new TypeReference<Person>() {
        }));
        Person person = bucket$person.get();
        System.err.println(JacksonUtil.toJson(person));
    }

    @Test
    void testGetHuman() {
        RBucket<Human> bucket$person2 = httpRateLimitRedissonClient.getBucket(prefix, new TypedJsonJacksonCodec(Human.class));
        System.err.println(JacksonUtil.toJson(bucket$person2.get()) + "\n"); //{"id":null,"name":"张三","age":18,"sex":1,"address":null}
        RBucket<Human> bucket$person = httpRateLimitRedissonClient.getBucket(prefix, new TypedJsonJacksonCodec(new TypeReference<Human>() {
        }));
        Human human = bucket$person.get();
        human.setAddress("上海市长宁区泉口路109弄116号");
        System.err.println(JacksonUtil.toJson(human) + "\n"); //{"id":null,"name":"张三","age":18,"sex":1,"address":"上海市长宁区泉口路109弄116号"}
        RBucket<HumanDTO> bucket$HumanDTO = httpRateLimitRedissonClient.getBucket(prefix, new TypedJsonJacksonCodec(new TypeReference<HumanDTO>() {
        }));
        System.err.println(JacksonUtil.toJson(bucket$HumanDTO.get()) + "\n"); //{"id":null,"name":"张三","age":18,"sex":1,"address":null,"email":null}
    }


}


@Data
class User implements Serializable {
    private String name;
    private Integer age;
    private Integer sex;
}

@Data
class Person implements Serializable {
    private String name;
    private Integer age;
    private Integer sex;
    private String ext;
}
