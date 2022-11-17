package com.example.request.controller;


import cn.dev33.satoken.stp.SaTokenInfo;
import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.json.JSONUtil;
import com.example.request.entity.AuthenticationRequest;
import com.example.request.entity.AuthenticationResponse;
import io.github.weasleyj.request.restrict.annotation.ApiRestrict;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;

/**
 * 登录凭据
 *
 * @author weasley
 * @version 1.0.0
 */
@Slf4j
@RestController
@RequestMapping("/api/public/auth")
public class AuthenticationController {

    /**
     * 获取登录凭证
     */
    @PostMapping("/login/token")
    public AuthenticationResponse getAuthToken(@RequestBody @Validated AuthenticationRequest request) {
        StpUtil.login(request.getOpenId());
        SaTokenInfo tokenInfo = StpUtil.getTokenInfo();
        AuthenticationResponse response = AuthenticationResponse.builder()
                .tokenName(tokenInfo.getTokenName())
                .tokenValue(tokenInfo.getTokenValue())
                .isLogin(tokenInfo.getIsLogin())
                .loginId(tokenInfo.getLoginId())
                .build();
        log.info("获取登录凭证 {} {}", JSONUtil.toJsonStr(request), JSONUtil.toJsonStr(response));
        return response;
    }

    /**
     * 查询登录状态
     */
    @ApiRestrict
    @GetMapping("/login/status")
    public Boolean getLoginStatus(HttpServletRequest request) {
        log.info("查询登录状态 {}", StpUtil.getLoginId());
        return StpUtil.isLogin();
    }

    /**
     * 退出登录
     *
     * @apiNote 会清楚会话信息，需要重新登陆
     */
    @PostMapping("/login/out")
    public void loginOut() {
        log.info("退出登录 {}", StpUtil.getLoginId());
        StpUtil.logout();
    }
}
