package com.example.request;

import io.github.weasleyj.http.rate.limit.annotation.EnableHttpRateLimiter;
import io.github.weasleyj.satoken.session.annotation.EnableSaIndependentRedisSession;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * DEMO启动入口
 */
@SpringBootApplication
@EnableHttpRateLimiter
@EnableSaIndependentRedisSession
public class HttpRateLimitApplication {

    public static void main(String[] args) {
        SpringApplication.run(HttpRateLimitApplication.class, args);
    }

}
