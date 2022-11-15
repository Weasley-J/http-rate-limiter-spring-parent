package com.example.request;

import io.github.weasleyj.request.restrict.annotation.EnableApiRestrict;
import io.github.weasleyj.satoken.session.annotation.EnableSaIndependentRedisSession;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * DEMO启动入口
 */
@SpringBootApplication
@EnableApiRestrict
@EnableSaIndependentRedisSession
public class HttpRequestRestrictDemoApp {

    public static void main(String[] args) {
        SpringApplication.run(HttpRequestRestrictDemoApp.class, args);
    }

}
