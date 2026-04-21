package com.yupi.yupicturebackend.service;

import com.yupi.yupicturebackend.model.entity.User;
import com.baomidou.mybatisplus.extension.service.IService;

/**
* @author songran
* @description 针对表【user(用户)】的数据库操作Service
* @createDate 2026-04-21 15:24:52
*/
public interface UserService extends IService<User> {

    public Long userRegister(String userAccount, String userPassword, String checkPassword);

    String getEncryptPassword(String userPassword);
}
