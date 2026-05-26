package com.tuque.srpicturebackend.service;

/**
 * AI 图片审核 Agent 服务
 */
public interface PictureReviewAgentService {

    /**
     * AI 审核图片
     * @param pictureId 图片 ID
     * @param imageUrl 图片 URL
     */
    void reviewPicture(Long pictureId, String imageUrl);
}
