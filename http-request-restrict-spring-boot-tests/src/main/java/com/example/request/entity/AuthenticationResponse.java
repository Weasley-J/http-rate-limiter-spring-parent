package com.example.request.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.io.Serializable;

/**
 * Auth Response
 *
 * @author weasley
 * @version 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
public class AuthenticationResponse implements Serializable {
    /**
     * token名称
     */
    private String tokenName;
    /**
     * token值
     */
    private String tokenValue;
    /**
     * 此token是否已经登录
     */
    private Boolean isLogin;
    /**
     * 此token对应的LoginId，未登录时为null
     */
    private Object loginId;
}
