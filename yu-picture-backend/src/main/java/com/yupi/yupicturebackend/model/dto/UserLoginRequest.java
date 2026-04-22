package com.yupi.yupicturebackend.model.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * 用户登录请求封装类
 */
@Data
public class UserLoginRequest implements Serializable {


    private static final long serialVersionUID = -1066641875584759783L;
    /**
     * 账号
     */
    private String userAccount;

    /**
     * 密码
     */
    private String userPassword;
}
