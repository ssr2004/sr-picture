package com.tuque.srpicturebackend.model.dto.picture;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Data
public class PictureEditByBatchRequest implements Serializable {

    private static final long serialVersionUID = -431881005284330707L;

    /**
     * 图片id列表
     */
    private List<Long> pictureIdList;

    /**
     * 空间id
     */
    private Long spaceId;

    /**
     *  分类
     */
    private String category;

    /**
     * 标签
     */
    private List<String> tags;

    /**
     * 重命名规则
     */
    private String nameRule;
}
