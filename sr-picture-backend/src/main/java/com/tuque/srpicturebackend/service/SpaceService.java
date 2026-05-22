package com.tuque.srpicturebackend.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tuque.srpicturebackend.model.dto.space.SpaceAddRequest;
import com.tuque.srpicturebackend.model.dto.space.SpaceQueryRequest;
import com.tuque.srpicturebackend.model.entity.Space;
import com.baomidou.mybatisplus.extension.service.IService;
import com.tuque.srpicturebackend.model.entity.User;
import com.tuque.srpicturebackend.model.vo.SpaceVO;

import javax.servlet.http.HttpServletRequest;

/**
* @author songran
* @description 针对表【space(空间)】的数据库操作Service
* @createDate 2026-05-08 16:43:20
*/
public interface SpaceService extends IService<Space> {

    /**
     * 校验空间
     * @param space
     */
    void validSpace(Space space, boolean add);

    /**
     * 获取空间视图对象
     * @param space
     * @param request
     * @return
     */
    SpaceVO getSpaceVO(Space space, HttpServletRequest request);

    /**
     * 分页查询空间视图对象
     * @param spacePage
     * @param request
     * @return
     */
   Page<SpaceVO> getSpaceVOPage(Page<Space> spacePage, HttpServletRequest request);

    /**
     * 构造空间查询条件
     * @param spaceQueryRequest
     * @return
     */
    QueryWrapper<Space> getQueryWrapper(SpaceQueryRequest spaceQueryRequest);

    /**
     * 根据空间级别填充空间信息
     * @param space
     */
    void fillSpaceBySpaceLevel(Space space);

    /**
     * 用户添加空间（一人只能添加一个空间）
     * @param spaceAddRequest
     * @param loginUser
     * @return
     */
    long addSpace(SpaceAddRequest spaceAddRequest, User loginUser);

    /**
     * 校验空间权限
     * @param loginUser
     * @param space
     */
    void checkSpaceAuth(User loginUser, Space space);
}
