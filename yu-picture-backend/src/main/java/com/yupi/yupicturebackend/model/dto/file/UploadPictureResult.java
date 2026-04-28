package com.yupi.yupicturebackend.model.dto.file;

import lombok.Data;

/**
 * 图片解析信息请求封装类
 */
@Data
public class UploadPictureResult {

    /**
     * 图片url
     */
    private String url;

    /**
     * 图片名称
     */
    private String picName;

    /**
     * 图片体积
     */
    private Long picSize;

    /**
     * 图片宽度
     */
    private int picWidth;

    /**
     * 图片高度
     */
    private int picHeight;

    /**
     * 图片宽高比例
     */
    private Double picScale;

    /**
     * 图片格式
     */
    private String picFormat;


}
