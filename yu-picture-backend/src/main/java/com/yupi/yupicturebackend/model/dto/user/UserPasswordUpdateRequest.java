package com.yupi.yupicturebackend.model.dto.user;

import lombok.Data;

import java.io.Serializable;

/**
 * 用户密码更新请求体
 */
@Data
public class UserPasswordUpdateRequest implements Serializable {

    private static final long serialVersionUID = 547668403357321334L;

    /**
     * 旧密码
     */
    private String oldPassword;

    /**
     * 新密码
     */
    private String newPassword;

    /**
     * 确认新密码
     */
    private String checkPassword;
}
