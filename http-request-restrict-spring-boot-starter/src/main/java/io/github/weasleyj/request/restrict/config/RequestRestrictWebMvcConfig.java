package io.github.weasleyj.request.restrict.config;

import io.github.weasleyj.request.restrict.annotation.EnableApiRestrict;
import io.github.weasleyj.request.restrict.interceptor.DefaultRequestRestrictInterceptor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Api Restrict Web Mvc Configure
 *
 * @author weasley
 * @version 1.0.0
 */
@Configuration
@ConditionalOnClass({EnableApiRestrict.class})
public class RequestRestrictWebMvcConfig implements WebMvcConfigurer {

    private final DefaultRequestRestrictInterceptor defaultRequestRestrictInterceptor;

    public RequestRestrictWebMvcConfig(DefaultRequestRestrictInterceptor defaultRequestRestrictInterceptor) {
        this.defaultRequestRestrictInterceptor = defaultRequestRestrictInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        if (null != defaultRequestRestrictInterceptor) {
            registry.addInterceptor(defaultRequestRestrictInterceptor).addPathPatterns("/**");
        }
    }
}
