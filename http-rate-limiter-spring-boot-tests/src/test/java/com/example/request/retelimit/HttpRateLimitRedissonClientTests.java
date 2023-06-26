package com.example.request.retelimit;

import cn.alphahub.dtt.plus.util.JacksonUtil;
import com.fasterxml.jackson.core.type.TypeReference;
import lombok.Data;
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
@SpringBootTest
class HttpRateLimitRedissonClientTests {
    protected final String prefix = "redisson_client:test:user:info";

    @Autowired
    private RedissonClient httpRateLimitRedissonClient;


    @Test
    void testSaveUserBucket() {

        User user = new User();
        user.setName("张三");
        user.setAge(12);
        user.setSex(12);

        RBucket<User> bucket = httpRateLimitRedissonClient.getBucket(prefix, new TypedJsonJacksonCodec(User.class));
        RBucket<User> bucket2 = httpRateLimitRedissonClient.getBucket(prefix, new TypedJsonJacksonCodec(new TypeReference<User>() {
        }));
        User andSet = bucket.getAndSet(user, 30, TimeUnit.DAYS);
        System.err.println(JacksonUtil.toJson(andSet));
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
}
