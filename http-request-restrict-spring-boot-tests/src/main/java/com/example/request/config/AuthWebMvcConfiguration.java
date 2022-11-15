package com.example.request.config;

import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.json.JSONUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.PrintWriter;
import java.util.Enumeration;
import java.util.LinkedHashMap;

/**
 * Authentication Web Mvc Configuration
 */
@Configuration
@ConditionalOnProperty(prefix = "sa-token", name = {"enable"}, havingValue = "true")
public class AuthWebMvcConfiguration implements WebMvcConfigurer {

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new AuthenticationInterceptor()).addPathPatterns("/**");
    }

    /**
     * Authentication Interceptor
     *
     * @author weasley
     * @version 1.0.0
     */
    @Slf4j
    public static class AuthenticationInterceptor implements HandlerInterceptor {

        protected static final AntPathMatcher ANT_PATH_MATCHER = new AntPathMatcher();
        private static final String[] URI_WHITELIST = new String[]{"/api/public/**"};

        @Override
        public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {

            String requestUri = request.getRequestURI();
            for (String uriPattern : URI_WHITELIST) {
                if (ANT_PATH_MATCHER.match(uriPattern, requestUri)) return true;
            }

            Enumeration<String> headerNames = request.getHeaderNames();
            String tokenValue = null;
            while (headerNames.hasMoreElements()) {
                String headerName = headerNames.nextElement();
                String headerValue = request.getHeader(headerName);
                log.info("获取到请求头 {}: {}", headerName, headerValue);
            }

            boolean login = StpUtil.isLogin();
            if (!login) {
                LinkedHashMap<String, Object> map = new LinkedHashMap<>();
                map.put("msg", "invalid token");
                map.put("code", 10000);
                map.put("success", false);
                response.setContentType("application/json;charset=utf-8");
                PrintWriter writer = response.getWriter();
                writer.println(JSONUtil.toJsonStr(map));
                writer.flush();
                writer.close();
                return false;
            }

            return true;
        }
    }
}
