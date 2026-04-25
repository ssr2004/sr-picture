package com.yupi.yupicturebackend.model.dto.user;

import lombok.Data;

import java.io.Serializable;

@Data
public class UserUpdateSelfRequest implements Serializable {

    private static final long serialVersionUID = 5802219420603578535L;

    /**
     * 用户昵称
     */
    private String userName;

    /**
     * 用户头像
     */
    private String userAvatar;

    /**
     * 用户简介
     */
    private String userProfile;
}
