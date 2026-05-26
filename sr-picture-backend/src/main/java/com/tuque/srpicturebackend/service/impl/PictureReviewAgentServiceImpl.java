package com.tuque.srpicturebackend.service.impl;

import com.tuque.srpicturebackend.api.aliyunai.AliYunAiApi;
import com.tuque.srpicturebackend.model.entity.Picture;
import com.tuque.srpicturebackend.model.enums.PictureReviewStatusEnum;
import com.tuque.srpicturebackend.service.PictureReviewAgentService;
import com.tuque.srpicturebackend.service.PictureService;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

/**
 * AI 图片审核 Agent 实现
 */
@Service
@Slf4j
@ConditionalOnProperty(prefix = "picture.search", name = "vector-enabled", havingValue = "true")
public class PictureReviewAgentServiceImpl implements PictureReviewAgentService {

    @Resource
    private ChatLanguageModel chatLanguageModel;

    @Resource
    private AliYunAiApi aliYunAiApi;

    @Resource
    @Lazy
    private PictureService pictureService;

    /**
     * 审核 Agent 接口定义
     */
    interface ReviewAgent {
        @SystemMessage("""
            你是一个图片内容审核 Agent。你的职责是：
            1. 使用 analyzeImage 工具分析图片内容
            2. 使用 checkSafety 工具检测图片是否包含敏感内容
            3. 根据分析结果做出审核决策：
               - 内容正常：调用 approvePicture 通过审核
               - 内容违规：调用 rejectPicture 拒绝审核
            请严格执行工具调用，不要跳过任何步骤。
        """)
        String review(@UserMessage("请审核这张图片：{{imageUrl}}") @P("图片URL") String imageUrl);
    }

    /**
     * 审核工具类
     */
    class ReviewTools {
        private final Long pictureId;

        ReviewTools(Long pictureId) {
            this.pictureId = pictureId;
        }

        @Tool("分析图片内容，返回场景描述和物体列表")
        public String analyzeImage(@P("图片URL") String imageUrl) {
            try {
                String result = aliYunAiApi.recognizePictureTag(imageUrl);
                return "图片分析结果：" + result;
            } catch (Exception e) {
                log.error("图片分析失败", e);
                return "图片分析失败：" + e.getMessage();
            }
        }

        @Tool("检测图片是否包含敏感内容（暴力、色情、政治敏感等），返回风险评估")
        public String checkSafety(@P("图片URL") String imageUrl) {
            try {
                String prompt = "请检测这张图片是否包含敏感内容（暴力、色情、政治敏感、侵权等）。" +
                        "请返回 JSON 格式：{\"safe\": true/false, \"risk_level\": \"none/low/medium/high\", " +
                        "\"risk_categories\": [\"类别\"], \"reason\": \"原因\"}";
                String result = aliYunAiApi.recognizePictureTag(imageUrl);
                return "安全检测结果：" + result;
            } catch (Exception e) {
                log.error("安全检测失败", e);
                return "安全检测失败：" + e.getMessage();
            }
        }

        @Tool("通过图片审核")
        public String approvePicture() {
            log.info("Agent 调用 approvePicture，pictureId={}", pictureId);
            boolean ok = pictureService.lambdaUpdate()
                    .eq(Picture::getId, pictureId)
                    .set(Picture::getReviewStatus, PictureReviewStatusEnum.PASS.getValue())
                    .set(Picture::getReviewMessage, "AI 审核通过")
                    .update();
            log.info("approvePicture 更新结果：{}", ok);
            return "审核已通过，pictureId=" + pictureId;
        }

        @Tool("拒绝图片审核")
        public String rejectPicture(@P("拒绝原因") String reason) {
            log.info("Agent 调用 rejectPicture，pictureId={}, reason={}", pictureId, reason);
            boolean ok = pictureService.lambdaUpdate()
                    .eq(Picture::getId, pictureId)
                    .set(Picture::getReviewStatus, PictureReviewStatusEnum.REJECT.getValue())
                    .set(Picture::getReviewMessage, "AI 审核拒绝：" + reason)
                    .update();
            log.info("rejectPicture 更新结果：{}", ok);
            return "审核已拒绝，pictureId=" + pictureId + "，原因：" + reason;
        }
    }

    @Async
    @Override
    public void reviewPicture(Long pictureId, String imageUrl) {
        try {
            // 创建 Agent，绑定工具
            ReviewAgent agent = AiServices.builder(ReviewAgent.class)
                    .chatLanguageModel(chatLanguageModel)
                    .chatMemory(MessageWindowChatMemory.withMaxMessages(10))
                    .tools(new ReviewTools(pictureId))
                    .build();

            // 执行审核
            String result = agent.review(imageUrl);
            log.info("AI 审核完成，pictureId={}, result={}", pictureId, result);
        } catch (Exception e) {
            log.error("AI 审核失败，pictureId={}", pictureId, e);
        }
    }
}
