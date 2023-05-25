package io.github.weasleyj.http.rate.limit.config;

import io.github.weasleyj.http.rate.limit.HttpRateLimitHandler;
import io.github.weasleyj.http.rate.limit.annotation.EnableHttpRateLimiter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Http Rate Limit Web Mvc Configure
 *
 * @author weasley
 * @version 1.0.0
 */
@Configuration
@ConditionalOnClass({EnableHttpRateLimiter.class})
public class HttpRateLimitWebMvcConfig implements WebMvcConfigurer {

    private final HttpRateLimitHandler httpRateLimitHandler;

    public HttpRateLimitWebMvcConfig(HttpRateLimitHandler httpRateLimitHandler) {
        this.httpRateLimitHandler = httpRateLimitHandler;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        if (null != httpRateLimitHandler) {
            registry.addInterceptor(httpRateLimitHandler).addPathPatterns("/**");
        }
    }
}
