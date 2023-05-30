package com.example.request.strategy;

import io.github.weasleyj.http.rate.limit.RateLimitStrategy;
import io.github.weasleyj.http.rate.limit.annotation.RateLimit;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletRequest;
import java.util.Map;

/**
 * 我的限流算法
 *
 * @author weasley
 * @version 1.0.0
 */
@Slf4j
@Component
public class MyReteLimitStrategy implements RateLimitStrategy {

    @Override
    public boolean tryLimit(RateLimit rateLimit, Map<String, Object> headers, HttpServletRequest request) throws InterruptedException {
        log.info("我不需要限流, 快来自定义你的限流策略 rateLimit {} headers {} ", rateLimit, headers);
        // TODO: 2023/5/30 我的限流策略
        return false;
    }
}
