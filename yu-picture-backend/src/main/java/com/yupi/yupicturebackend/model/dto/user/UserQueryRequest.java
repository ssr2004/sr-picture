package com.yupi.yupicturebackend.model.dto.user;

import com.yupi.yupicturebackend.common.PageRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;

/**
 * 用户查询请求体
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class UserQueryRequest extends PageRequest implements Serializable {

    private static final long serialVersionUID = 5802219420603578535L;
    /**
     * 用户id
     */
    private Long id;
    /**
     * 用户昵称
     */
    private String userName;

    /**
     * 用户账号
     */
    private String userAccount;

    /**
     * 用户简介
     */
    private String userProfile;

     /**
     * 用户角色权限
     */
    private String userRole;

}
