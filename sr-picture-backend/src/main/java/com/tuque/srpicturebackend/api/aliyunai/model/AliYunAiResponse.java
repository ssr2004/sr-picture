package com.tuque.srpicturebackend.api.aliyunai.model;

import lombok.Data;

import java.util.List;

/**
 * 阿里云 DashScope 多模态响应
 */
@Data
public class AliYunAiResponse {

    private List<Choice> choices;

    private String code;

    private String message;

    @Data
    public static class Choice {
        private Message message;
    }

    @Data
    public static class Message {
        private String content;
    }
}
