package com.yupi.yupicturebackend.model.dto.picture;

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


}
