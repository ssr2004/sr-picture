package com.tuque.srpicturebackend.controller;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.tuque.srpicturebackend.annotation.RateLimit;
import com.tuque.srpicturebackend.common.BaseResponse;
import com.tuque.srpicturebackend.common.ResultUtils;
import com.tuque.srpicturebackend.exception.ErrorCode;
import com.tuque.srpicturebackend.exception.ThrowUtils;
import com.tuque.srpicturebackend.model.entity.Picture;
import com.tuque.srpicturebackend.model.vo.PictureVO;
import com.tuque.srpicturebackend.service.PictureSearchService;
import com.tuque.srpicturebackend.service.PictureService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 图片语义搜索控制器
 */
@RestController
@RequestMapping("/picture/search")
@Slf4j
@ConditionalOnProperty(prefix = "picture.search", name = "vector-enabled", havingValue = "true")
public class PictureSearchController {

    @Resource
    private PictureSearchService pictureSearchService;

    @Resource
    private PictureService pictureService;

    /**
     * AI 自然语言搜索图片
     */
    @PostMapping("/ai")
    @RateLimit(maxCount = 20, timeWindowSeconds = 60)
    public BaseResponse<List<PictureVO>> searchByAI(@RequestBody AISearchRequest request,
                                                        HttpServletRequest httpRequest) {
        ThrowUtils.throwIf(StrUtil.isBlank(request.getQuery()), ErrorCode.PARAMS_ERROR, "搜索内容不能为空");
        // 语义搜索，获取匹配的图片 ID 列表
        List<Long> pictureIds = pictureSearchService.searchPictureIds(
                request.getQuery(), request.getSpaceId());
        if (pictureIds.isEmpty()) {
            return ResultUtils.success(new ArrayList<>());
        }
        // 查询图片详情
        List<Picture> pictures = pictureService.listByIds(pictureIds);
        // 转为 VO（脱敏）
        List<PictureVO> pictureVOList = pictures.stream()
                .map(p -> pictureService.getPictureVO(p, httpRequest))
                .collect(Collectors.toList());
        return ResultUtils.success(pictureVOList);
    }

    /**
     * AI 搜索请求体
     */
    @lombok.Data
    public static class AISearchRequest {
        private String query;
        private Long spaceId;
    }
}
