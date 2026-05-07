package com.yupi.yupicturebackend.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.yupi.yupicturebackend.model.dto.picture.PictureQueryRequest;
import com.yupi.yupicturebackend.model.dto.picture.PictureReviewRequest;
import com.yupi.yupicturebackend.model.dto.picture.PictureUploadByBatchRequest;
import com.yupi.yupicturebackend.model.dto.picture.PictureUploadRequest;
import com.yupi.yupicturebackend.model.entity.Picture;
import com.baomidou.mybatisplus.extension.service.IService;
import com.yupi.yupicturebackend.model.entity.User;
import com.yupi.yupicturebackend.model.vo.PictureVO;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;

/**
* @author songran
* @description 针对表【picture(图片)】的数据库操作Service
* @createDate 2026-04-27 15:14:57
*/
public interface PictureService extends IService<Picture> {

    /**
     * 图片上传
     * @param inputSource
     * @param pictureUploadRequest
     * @param loginUser
     * @return
     */
    PictureVO uploadPicture(Object inputSource,
                            PictureUploadRequest pictureUploadRequest,
                            User loginUser);

    /**
     * 构造图片查询条件
     * @param pictureQueryRequest
     * @return
     */
    public QueryWrapper<Picture> getQueryWrapper(PictureQueryRequest pictureQueryRequest);

    /**
     * 实体转换为图片视图对象
     * @param picture
     * @return
     */
    public PictureVO getPictureVO(Picture picture, HttpServletRequest request);

    /**
     * 分页查询图片视图对象
     * @param picturePage
     * @param request
     * @return
     */
    public Page<PictureVO> getPictureVOPage(Page<Picture> picturePage, HttpServletRequest request);

    /**
     * 校验图片是否合法
     * @param picture
     */
    public void validPicture(Picture picture);

    /**
     * 图片审核
     * @param pictureReviewRequest
     * @param loginUser
     */
    public void doPictureReview(PictureReviewRequest pictureReviewRequest, User loginUser);

    /**
     * 填充审核参数
     * @param picture
     * @param loginUser
     */
    void fillReviewParams(Picture picture, User loginUser);

    /**
     * 批量抓取与上传图片
     * @param pictureUploadByBatchRequest
     * @param loginUser
     * @return
     */
    Integer uploadPictureByBatch(PictureUploadByBatchRequest pictureUploadByBatchRequest, User loginUser);

    /**
     * 清理图片文件
     * @param oldPicture
     */
    void clearPictureFile(Picture oldPicture);

    /**
     * 从url中获取key
     * @param url
     * @return
     */
    String getKeyFromUrl(String url);
}
