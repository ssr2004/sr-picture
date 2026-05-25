package com.tuque.srpicturebackend.api.aliyunai;

import cn.hutool.core.util.StrUtil;
import cn.hutool.http.ContentType;
import cn.hutool.http.Header;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.json.JSONUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tuque.srpicturebackend.api.aliyunai.model.*;
import com.tuque.srpicturebackend.constant.PictureConstant;
import com.tuque.srpicturebackend.exception.BusinessException;
import com.tuque.srpicturebackend.exception.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * 阿里云 AI 接口
 */
@Slf4j
@Component
public class AliYunAiApi {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    // 读取配置文件
    @Value("${aliYunAi.apiKey}")
    private String apiKey;

    // 创建任务地址
    public static final String CREATE_OUT_PAINTING_TASK_URL = "https://dashscope.aliyuncs.com/api/v1/services/aigc/image2image/out-painting";

    // 查询任务状态
    public static final String GET_OUT_PAINTING_TASK_URL = "https://dashscope.aliyuncs.com/api/v1/tasks/%s";

    // 多模态对话地址
    public static final String CHAT_URL = "https://dashscope.aliyuncs.com/compatible-mode/v1/chat/completions";

    /**
     * 创建任务
     *
     * @param createOutPaintingTaskRequest
     * @return
     */
    public CreateOutPaintingTaskResponse createOutPaintingTask(CreateOutPaintingTaskRequest createOutPaintingTaskRequest) {
        if (createOutPaintingTaskRequest == null) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "扩图参数为空");
        }

        // 发送请求
        String requestJson;
        try {
            requestJson = objectMapper.writeValueAsString(createOutPaintingTaskRequest);
        } catch (JsonProcessingException e) {
            log.error("请求参数序列化失败", e);
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "AI 扩图参数错误");
        }
        log.info("AI 扩图请求参数：{}", requestJson);
        HttpRequest httpRequest = HttpRequest.post(CREATE_OUT_PAINTING_TASK_URL)
                .header(Header.AUTHORIZATION, "Bearer " + apiKey)
                // 必须开启异步处理，设置为enable。
                .header("X-DashScope-Async", "enable")
                .header(Header.CONTENT_TYPE, ContentType.JSON.getValue())
                .body(requestJson);

        try (HttpResponse httpResponse = httpRequest.execute()) {
            if (!httpResponse.isOk()) {
                log.error("请求异常，状态码：{}，响应体：{}", httpResponse.getStatus(), httpResponse.body());
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "AI 扩图失败");
            }

            CreateOutPaintingTaskResponse response = JSONUtil.toBean(httpResponse.body(), CreateOutPaintingTaskResponse.class);
            log.info("AI 扩图响应：{}", httpResponse.body());
            String errorCode = response.getCode();
            if (StrUtil.isNotBlank(errorCode)) {
                String errorMessage = response.getMessage();
                log.error("AI 扩图失败, errorCode:{}, errorMessage:{}", errorCode, errorMessage);
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "AI 扩图接口响应异常");
            }
            return response;
        }
    }

    /**
     * 查询创建的任务
     *
     * @param taskId
     * @return
     */
    public GetOutPaintingTaskResponse getOutPaintingTask(String taskId) {
        if (StrUtil.isBlank(taskId)) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "任务 id 不能为空");
        }

        try (HttpResponse httpResponse = HttpRequest.get(String.format(GET_OUT_PAINTING_TASK_URL, taskId))
                .header(Header.AUTHORIZATION, "Bearer " + apiKey)
                .execute()) {

            if (!httpResponse.isOk()) {
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "获取任务失败");
            }
            return JSONUtil.toBean(httpResponse.body(), GetOutPaintingTaskResponse.class);
        }
    }

    /**
     * AI 识别图片标签和分类
     *
     * @param imageUrl 图片 URL
     * @return 识别结果 JSON 字符串，格式：{"tags": ["标签1","标签2"], "category": "分类"}
     */
    public String recognizePictureTag(String imageUrl) {
        if (StrUtil.isBlank(imageUrl)) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "图片地址不能为空");
        }

        String tagList = String.join("、", PictureConstant.TAG_LIST);
        String categoryList = String.join("、", PictureConstant.CATEGORY_LIST);

        String prompt = String.format(
                "请分析这张图片的内容，从以下预定义选项中选择最匹配的结果。" +
                "标签列表（可多选）：%s。" +
                "分类列表（单选）：%s。" +
                "请严格按照以下 JSON 格式返回，不要包含其他内容：{\"tags\": [\"选中的标签\"], \"category\": \"选中的分类\"}",
                tagList, categoryList
        );

        // 构造请求体
        AliYunAiRequest.Content imageContent = new AliYunAiRequest.Content();
        imageContent.setType("image_url");
        AliYunAiRequest.ImageUrl imageUrlObj = new AliYunAiRequest.ImageUrl();
        imageUrlObj.setUrl(imageUrl);
        imageContent.setImageUrl(imageUrlObj);

        AliYunAiRequest.Content textContent = new AliYunAiRequest.Content();
        textContent.setType("text");
        textContent.setText(prompt);

        AliYunAiRequest.Message message = new AliYunAiRequest.Message();
        message.setRole("user");
        message.setContent(java.util.Arrays.asList(imageContent, textContent));

        AliYunAiRequest request = new AliYunAiRequest();
        request.setModel("qwen-vl-plus");
        request.setMessages(java.util.Collections.singletonList(message));

        String requestJson;
        try {
            requestJson = objectMapper.writeValueAsString(request);
        } catch (JsonProcessingException e) {
            log.error("请求参数序列化失败", e);
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "AI 图片标签识别参数错误");
        }
        log.info("AI 图片标签识别请求：{}", requestJson);

        try (HttpResponse httpResponse = HttpRequest.post(CHAT_URL)
                .header(Header.AUTHORIZATION, "Bearer " + apiKey)
                .header(Header.CONTENT_TYPE, ContentType.JSON.getValue())
                .body(requestJson)
                .execute()) {

            if (!httpResponse.isOk()) {
                log.error("AI 图片标签识别请求异常，状态码：{}，响应体：{}", httpResponse.getStatus(), httpResponse.body());
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "AI 图片标签识别失败");
            }

            AliYunAiResponse response = JSONUtil.toBean(httpResponse.body(), AliYunAiResponse.class);
            if (response.getCode() != null) {
                log.error("AI 图片标签识别失败, code:{}, message:{}", response.getCode(), response.getMessage());
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "AI 图片标签识别接口响应异常");
            }

            String content = response.getChoices().get(0).getMessage().getContent();
            log.info("AI 图片标签识别结果：{}", content);
            return content;
        }
    }
}