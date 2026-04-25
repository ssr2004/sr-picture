package com.yupi.yupicturebackend.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.yupi.yupicturebackend.model.dto.user.UserQueryRequest;
import com.yupi.yupicturebackend.model.dto.user.UserUpdateSelfRequest;
import com.yupi.yupicturebackend.model.entity.User;
import com.baomidou.mybatisplus.extension.service.IService;
import com.yupi.yupicturebackend.model.vo.LoginUserVO;
import com.yupi.yupicturebackend.model.vo.UserVO;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

/**
* @author songran
* @description 针对表【user(用户)】的数据库操作Service
* @createDate 2026-04-21 15:24:52
*/
public interface UserService extends IService<User> {

    /**
     * 用户注册
     * @param userAccount
     * @param userPassword
     * @param checkPassword
     * @return
     */
    public Long userRegister(String userAccount, String userPassword, String checkPassword);

    /**
     * 用户登录
     * @param userAccount
     * @param userPassword
     * @param request
     * @return
     */
    LoginUserVO userLogin(String userAccount, String userPassword, HttpServletRequest request);

    /**
     * 实体转换为脱敏后的用户视图对象
     * @param user
     * @return
     */
    LoginUserVO getLoginUserVO(User user);

    /**
     * 获取当前登录用户
     * @param request
     * @return
     */
    User getLoginUser(HttpServletRequest request);

    /**
     * 实体转换为脱敏后的用户视图对象
     * @param user
     * @return
     */
    UserVO getUserVO(User user);

    /**
     * 实体列表转换为脱敏后的用户视图对象列表
     * @param userList
     * @return
     */
    List<UserVO> getUserVOList(List<User> userList);
    /**
     * 获取加密后的密码
     * @param userPassword
     * @return
     */
    String getEncryptPassword(String userPassword);

    /**
     * 用户注销--退出登录
     * @param request
     * @return
     */
    boolean userLogout(HttpServletRequest request);

    /**
     * 用户更新个人信息（本人）
     * @param userUpdateSelfRequest
     * @param loginUser
     * @return
     */
    boolean updateUserSelf(UserUpdateSelfRequest userUpdateSelfRequest, User loginUser);

    /**
     * 根据查询请求构建查询条件
     * @param userQueryRequest
     * @return
     */
    QueryWrapper<User> getQueryWrapper(UserQueryRequest userQueryRequest);
}
