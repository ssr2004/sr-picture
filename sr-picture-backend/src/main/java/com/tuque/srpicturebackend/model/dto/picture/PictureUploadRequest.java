package com.tuque.srpicturebackend.model.dto.picture;

import lombok.Data;

import java.io.Serializable;

/**
 * 图片上传请求封装类
 */
@Data
public class PictureUploadRequest implements Serializable {

    private static final long serialVersionUID = 694800440605966914L;
    /**
     * id（用于修改）
     */
    private Long id;

    /**
     * 图片地址
     */
    private String fileUrl;

    /**
     * 图片名称
     */
    private String picName;

    /**
     * 拥有的空间id
     */
    private Long spaceId;

    /**
     * 业务类型：avatar=头像上传（不入库），默认为图片上传
     */
    private String bizType;

}
