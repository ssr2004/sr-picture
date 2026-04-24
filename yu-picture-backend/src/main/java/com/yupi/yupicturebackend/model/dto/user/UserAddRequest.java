package com.yupi.yupicturebackend.model.dto.user;

import lombok.Data;

import java.io.Serializable;

/**
 * 用户添加请求体
 */
@Data
public class UserAddRequest implements Serializable {


    private static final long serialVersionUID = 5802219420603578535L;
    /**
     * 用户昵称
     */
    private String userName;

    /**
     * 用户账号
     */
    private String userAccount;

    /**
     * 用户头像
     */
    private String userAvatar;

    /**
     * 用户简介
     */
    private String userProfile;

    /**
     * 用户角色权限
     */
    private String userRole;
}
