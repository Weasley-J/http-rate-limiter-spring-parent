package com.example.request.controller;

import io.github.weasleyj.http.rate.limit.CancelLimitStrategy;
import io.github.weasleyj.http.rate.limit.RequestLimitHandler;
import io.github.weasleyj.http.rate.limit.annotation.RateLimit;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.concurrent.TimeUnit;

/**
 * rate limit demo controller
 *
 * @author weasley
 * @version 1.0.0
 */
@Slf4j
@RestController
@RequestMapping("/api/public/demo")
public class RateLimitDemoController {

    @PostMapping("/clickOnce5Seconds")
    @RateLimit(value = 5, maxCount = 1, timeUnit = TimeUnit.SECONDS)
    public String clickOnce5Seconds() {
        log.info("5秒内点仅能点击1次");
        return "ok";
    }

    @PostMapping("/click2Times10Seconds")
    @RateLimit(value = 10, maxCount = 2, timeUnit = TimeUnit.SECONDS)
    public String click2Times10Seconds() {
        log.info("10秒内仅能点击2次");
        return "ok";
    }

    @PostMapping("/click5Times5Minutes")
    @RateLimit(value = 5, maxCount = 5, timeUnit = TimeUnit.MINUTES)
    public String click5Times5Minutes() {
        log.info("5分钟只能内只能点5次");
        return "ok";
    }

    @PostMapping("/click10Times5Minutes/{id}")
    @RateLimit(value = 5, maxCount = 10, timeUnit = TimeUnit.MINUTES)
    public String click5Times5Minutes(@PathVariable("id") String id) {
        log.info("5分钟只能内只能点10次 {}", id);
        return "ok";
    }

    @PostMapping("/click2Times10SecondsByHeaderName")
    @RateLimit(value = 5, maxCount = 1, timeUnit = TimeUnit.SECONDS, headName = "x-auth-token")
    public String click2Times10SecondsByHeaderName() {
        log.info("指定headerName: 5秒内仅能点击1次");
        return "ok";
    }

    @PostMapping("/click2Times10SecondsByCookieName")
    @RateLimit(value = 10, maxCount = 2, timeUnit = TimeUnit.SECONDS, cookieName = "x-auth-token")
    public String click2Times10SecondsByCookieName() {
        log.info("指定cookieName: 10秒内仅能点击2次");
        return "ok";
    }

    /**
     * 当满足“特定条件”时退出限流模式
     *
     * @see CancelLimitStrategy
     */
    @PostMapping("/click5Times10MinutesBySeniorTechnical")
    @RateLimit(value = 10, maxCount = 5, timeUnit = TimeUnit.MINUTES)
    public String click5Times10MinutesBySeniorTechnical(@RequestParam String name) {
        log.info("指定cookieName: 10分钟内仅能点击5次，使用高级玩法，当满足“特定条件”时退出限流模式: name = {}", name);
        //name为李四时第一次访问后，会取消限流，name != lisi 会触发限流
        RequestLimitHandler.set(() -> "lisi".equals(name));
        return "ok";
    }
}
