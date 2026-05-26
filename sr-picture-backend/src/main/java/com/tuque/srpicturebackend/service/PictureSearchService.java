package com.tuque.srpicturebackend.service;

import com.tuque.srpicturebackend.model.vo.PictureVO;

import java.util.List;

/**
 * 图片语义搜索服务
 */
public interface PictureSearchService {

    /**
     * 将图片描述向量化并存入向量数据库
     * @param pictureId 图片 ID
     * @param description 图片描述
     */
    void indexPicture(Long pictureId, String description);

    /**
     * 自然语言搜索图片
     * @param query 自然语言查询
     * @param spaceId 空间 ID（可选，为空则搜索公共图片）
     * @return 匹配的图片 ID 列表
     */
    List<Long> searchPictureIds(String query, Long spaceId);
}
