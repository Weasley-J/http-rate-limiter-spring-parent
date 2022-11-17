package com.example.request.controller;

import io.github.weasleyj.request.restrict.RequestRestrictHandler;
import io.github.weasleyj.request.restrict.RestrictCancelStrategy;
import io.github.weasleyj.request.restrict.annotation.ApiRestrict;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.concurrent.TimeUnit;

/**
 * Api Restrict Common Controller
 *
 * @author weasley
 * @version 1.0.0
 */
@Slf4j
@RestController
@RequestMapping("/api/public/demo")
public class ApiRestrictDemoController {

    @PostMapping("/clickOnce5Seconds")
    @ApiRestrict(value = 5, maxCount = 1, timeUnit = TimeUnit.SECONDS)
    public void clickOnce5Seconds() {
        log.info("5秒内点仅能点击1次");
    }

    @PostMapping("/click2Times10Seconds")
    @ApiRestrict(value = 10, maxCount = 2, timeUnit = TimeUnit.SECONDS)
    public void click2Times10Seconds() {
        log.info("10秒内仅能点击2次");
    }

    @PostMapping("/click5Times5Minutes")
    @ApiRestrict(value = 5, maxCount = 5, timeUnit = TimeUnit.MINUTES)
    public void click5Times5Minutes() {
        log.info("5分钟只能内只能点5次");
    }

    @PostMapping("/click5Times5Minutes/{id}")
    @ApiRestrict(value = 5, maxCount = 5, timeUnit = TimeUnit.MINUTES)
    public void click5Times5Minutes(@PathVariable("id") String id) {
        log.info("5分钟只能内只能点5次 {}", id);
    }

    @PostMapping("/click2Times10SecondsByHeaderName")
    @ApiRestrict(value = 5, maxCount = 1, timeUnit = TimeUnit.SECONDS, headName = "x-auth-token")
    public void click2Times10SecondsByHeaderName() {
        log.info("指定headerName: 5秒内仅能点击1次");
    }

    @PostMapping("/click2Times10SecondsByCookieName")
    @ApiRestrict(value = 10, maxCount = 2, timeUnit = TimeUnit.SECONDS, cookieName = "x-auth-token")
    public void click2Times10SecondsByCookieName() {
        log.info("指定cookieName: 10秒内仅能点击2次");
    }

    /**
     * 当满足“特定条件”时退出限流模式
     *
     * @see RestrictCancelStrategy
     */
    @PostMapping("/click5Times10MinutesBySeniorTechnical")
    @ApiRestrict(value = 10, maxCount = 5, timeUnit = TimeUnit.MINUTES)
    public void click5Times10MinutesBySeniorTechnical(@RequestParam String name) {
        log.info("指定cookieName: 10分钟内仅能点击5次，使用高级玩法，当满足“特定条件”时退出限流模式: name = {}", name);
        //name为李四时第一次访问后，会取消限流，name != lisi 会触发限流
        RequestRestrictHandler.set(() -> "lisi".equals(name));
    }
}
