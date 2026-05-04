package com.yupi.yupicturebackend.model.dto.picture;

import lombok.Data;

import java.io.Serializable;

/**
 * 批量上传图片请求
 */
@Data
public class PictureUploadByBatchRequest implements Serializable {

    private static final long serialVersionUID = 8844165578342474901L;

    /**
     * 搜索关键词
     */
    private String searchText;

    /**
     * 搜索数量
     */
    private Integer count = 10;

    /**
     * 图片名称前缀
     */
    private String namePrefix;
}
