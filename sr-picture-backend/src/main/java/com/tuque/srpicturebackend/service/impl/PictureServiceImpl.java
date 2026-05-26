package com.tuque.srpicturebackend.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import cn.hutool.json.JSONObject;
import com.tuque.srpicturebackend.api.aliyunai.AliYunAiApi;
import com.tuque.srpicturebackend.constant.PictureConstant;
import com.tuque.srpicturebackend.api.aliyunai.model.CreateOutPaintingTaskRequest;
import com.tuque.srpicturebackend.api.aliyunai.model.CreateOutPaintingTaskResponse;
import com.tuque.srpicturebackend.config.CosClientConfig;
import com.tuque.srpicturebackend.exception.BusinessException;
import com.tuque.srpicturebackend.exception.ErrorCode;
import com.tuque.srpicturebackend.exception.ThrowUtils;
import com.tuque.srpicturebackend.manager.CosManager;
import com.tuque.srpicturebackend.manager.FileManager;
import com.tuque.srpicturebackend.manager.upload.FilePictureUpload;
import com.tuque.srpicturebackend.manager.upload.PictureUploadTemplate;
import com.tuque.srpicturebackend.manager.upload.UrlPictureUpload;
import com.tuque.srpicturebackend.model.dto.picture.*;
import com.tuque.srpicturebackend.model.dto.file.UploadPictureResult;
import com.tuque.srpicturebackend.model.entity.Picture;
import com.tuque.srpicturebackend.model.entity.Space;
import com.tuque.srpicturebackend.model.entity.User;
import com.tuque.srpicturebackend.model.enums.PictureReviewStatusEnum;
import com.tuque.srpicturebackend.model.vo.PictureVO;
import com.tuque.srpicturebackend.model.vo.UserVO;
import com.tuque.srpicturebackend.service.PictureService;
import com.tuque.srpicturebackend.service.PictureSearchService;
import org.springframework.beans.factory.annotation.Autowired;
import com.tuque.srpicturebackend.service.PictureReviewAgentService;
import com.tuque.srpicturebackend.mapper.PictureMapper;
import com.tuque.srpicturebackend.service.SpaceService;
import com.tuque.srpicturebackend.service.UserService;
import com.tuque.srpicturebackend.utils.ColorSimilarUtils;
import com.tuque.srpicturebackend.utils.ColorTransformUtils;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.beans.BeanUtils;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.awt.*;
import java.io.IOException;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

/**
* @author songran
* @description 针对表【picture(图片)】的数据库操作Service实现
* @createDate 2026-04-27 15:14:57
*/
@Service
@Slf4j
public class PictureServiceImpl extends ServiceImpl<PictureMapper, Picture>
    implements PictureService{

    @Resource
    private FileManager fileManager;

    @Resource
    private UserService userService;

    @Resource
    private SpaceService spaceService;

    @Resource
    private FilePictureUpload filepictureUpload;

    @Resource
    private UrlPictureUpload urlPictureUpload;
    @Resource
    private CosManager cosManager;
    @Resource
    private CosClientConfig cosClientConfig;
    @Resource
    private TransactionTemplate transactionTemplate;
    @Resource
    private AliYunAiApi aliYunAiApi;
    @Autowired(required = false)
    private PictureSearchService pictureSearchService;
    @Autowired(required = false)
    private PictureReviewAgentService pictureReviewAgentService;
    @Resource
    @Lazy
    private PictureServiceImpl selfProxy;
    /**
     * 图片上传
     * @param inputSource
     * @param pictureUploadRequest
     * @param loginUser
     * @return
     */
    @Override
    public PictureVO uploadPicture(Object inputSource, PictureUploadRequest pictureUploadRequest, User loginUser) {
        ThrowUtils.throwIf(loginUser == null, ErrorCode.NO_AUTH_ERROR);
        // 头像上传：只上传 COS，不入库
        if (pictureUploadRequest != null && "avatar".equals(pictureUploadRequest.getBizType())) {
            String uploadPathPrefix = String.format("avatar/%s", loginUser.getId());
            PictureUploadTemplate template = inputSource instanceof String ? urlPictureUpload : filepictureUpload;
            UploadPictureResult result = template.uploadPicture(inputSource, uploadPathPrefix);
            PictureVO vo = new PictureVO();
            vo.setUrl(result.getUrl());
            return vo;
        }
        //校验空间是否存在
        Long spaceId = pictureUploadRequest.getSpaceId();
        if(spaceId != null){
            Space space = spaceService.getById(spaceId);
            ThrowUtils.throwIf(space == null, ErrorCode.NOT_FOUND_ERROR,"空间不存在");
//            //必须空间创建人才能上传
//            if(!loginUser.getId().equals(space.getUserId())){
//                throw new BusinessException(ErrorCode.NO_AUTH_ERROR,"没有空间权限");
//            }
            //校验额度
            if(space.getTotalCount() >= space.getMaxCount()){
                throw new BusinessException(ErrorCode.OPERATION_ERROR,"空间条数不足");
            }
            if(space.getTotalSize() >= space.getMaxSize()){
                throw new BusinessException(ErrorCode.OPERATION_ERROR,"空间大小不足");
            }
        }
        //TODO 更新图片时要清理老图片以及删除图片时存储桶内的原图没有删除
        //用于判断是新增还是更新图片
        Long pictureId = null;
//        //获取旧图片信息，以供后续清理
//        Picture oldPicture = new Picture();
        if(pictureUploadRequest != null){
            pictureId = pictureUploadRequest.getId();
        }
        //如果是更新图片，需要校验图片是否存在
        if(pictureId != null){
            Picture oldPicture = this.getById(pictureId);
            ThrowUtils.throwIf(oldPicture == null, ErrorCode.NOT_FOUND_ERROR,"图片不存在");
//            //仅本人和管理员可编辑
//            if(!oldPicture.getUserId().equals(loginUser.getId()) && !userService.isAdmin(loginUser)){
//                throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
//            }
//            boolean exists = this.lambdaQuery().eq(Picture::getId,pictureId).exists();
//            ThrowUtils.throwIf(!exists, ErrorCode.PARAMS_ERROR,"图片不存在");
            //校验空间是否一致
            //没传 spaceId，则复用旧图片的 spaceId
            if(spaceId == null){
                if(oldPicture.getSpaceId() != null){
                    spaceId = oldPicture.getSpaceId();
                }
            }else {
                //传了 spaceId，必须和原有图片一致
                if(ObjUtil.notEqual(spaceId, oldPicture.getSpaceId())){
                    throw new BusinessException(ErrorCode.PARAMS_ERROR,"空间 id 不一致");
                }
            }
        }
        //上传图片，得到信息
        //按照用户id划分目录
        String uploadPathPrefix;
        if(spaceId == null){
            uploadPathPrefix = String.format("public/%s", loginUser.getId());
        }else {
            uploadPathPrefix = String.format("space/%s", spaceId);
        }
        PictureUploadTemplate pictureUploadTemplate = filepictureUpload;
        if(inputSource instanceof String){
            pictureUploadTemplate = urlPictureUpload;
        }
        UploadPictureResult uploadPictureResult = pictureUploadTemplate.uploadPicture(inputSource, uploadPathPrefix);
        //构造要入库的图片信息
        Picture picture = new Picture();
        //补充设置 spaceId
        picture.setSpaceId(spaceId);
        picture.setUrl(uploadPictureResult.getUrl());
        picture.setThumbnailUrl(uploadPictureResult.getThumbnailUrl());
        //构造要入库的图片信息
        String picName = uploadPictureResult.getPicName();
        if(pictureUploadRequest != null && StrUtil.isNotBlank(pictureUploadRequest.getPicName())){
            picName = pictureUploadRequest.getPicName();
        }
        picture.setName(picName);
        picture.setPicSize(uploadPictureResult.getPicSize());
        picture.setPicWidth(uploadPictureResult.getPicWidth());
        picture.setPicHeight(uploadPictureResult.getPicHeight());
        picture.setPicScale(uploadPictureResult.getPicScale());
        picture.setPicFormat(uploadPictureResult.getPicFormat());
        picture.setPicColor(ColorTransformUtils.getStandardColor(uploadPictureResult.getPicColor()));
        picture.setUserId(loginUser.getId());
        //补充审核参数
        fillReviewParams(picture, loginUser);
        //如果 pictureId 不为空，说明是更新图片，否则是新增图片
        if(pictureId != null){
            //如果是更新，需要补充 id 和编辑时间
            picture.setId(pictureId);
            picture.setEditTime(new Date());

        }
        //开启事务
        Long finalSpaceId = spaceId;
        transactionTemplate.execute(status -> {
            boolean result = this.saveOrUpdate(picture);
            ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR,"图片上传失败");
            if(finalSpaceId != null){
                boolean update = spaceService.lambdaUpdate()
                        .eq(Space::getId, finalSpaceId)
                        .setSql("totalSize = totalSize +" + picture.getPicSize())
                        .setSql("totalCount = totalCount + 1")
                        .update();
                ThrowUtils.throwIf(!update, ErrorCode.OPERATION_ERROR,"额度更新失败");
            }
            return true;
        });

//        //清理旧图片
//        if(oldPicture != null){
//            this.clearPictureFile(oldPicture);
//        }

        // 异步 AI 识别标签（仅新增时触发，更新时不覆盖用户手动设置）
        if (pictureId == null) {
            selfProxy.autoRecognizeTags(picture.getId(), picture.getUrl());
        }

        return PictureVO.objToVo(picture);
    }


    /**
     * 构造图片查询条件
     * @param pictureQueryRequest
     * @return
     */
    @Override
    public QueryWrapper<Picture> getQueryWrapper(PictureQueryRequest pictureQueryRequest) {
        QueryWrapper<Picture> queryWrapper = new QueryWrapper<>();
        if(pictureQueryRequest == null) {
            return queryWrapper;
        }
        //从对象中取值
        Long id = pictureQueryRequest.getId();
        String name = pictureQueryRequest.getName();
        String introduction = pictureQueryRequest.getIntroduction();
        String category = pictureQueryRequest.getCategory();
        List<String> tags = pictureQueryRequest.getTags();
        Long picSize = pictureQueryRequest.getPicSize();
        Integer picWidth = pictureQueryRequest.getPicWidth();
        Integer picHeight = pictureQueryRequest.getPicHeight();
        Double picScale = pictureQueryRequest.getPicScale();
        String picFormat = pictureQueryRequest.getPicFormat();
        String searchText = pictureQueryRequest.getSearchText();
        Long userId = pictureQueryRequest.getUserId();
        Long spaceId = pictureQueryRequest.getSpaceId();
        boolean nullSpaceId = pictureQueryRequest.isNullSpaceId();
        Integer reviewStatus = pictureQueryRequest.getReviewStatus();
        Long reviewId = pictureQueryRequest.getReviewId();
        String reviewMessage = pictureQueryRequest.getReviewMessage();
        String sortField = pictureQueryRequest.getSortField();
        String sortOrder = pictureQueryRequest.getSortOrder();
        //从多字段中搜索
        if(StrUtil.isNotBlank(searchText)){
            queryWrapper.and(qw -> qw.like("name", searchText)
                    .or()
                    .like("introduction", searchText)
                    .or()
                    .like("category", searchText)
                    .or()
                    .like("tags", searchText));
        }
        queryWrapper.eq(ObjUtil.isNotEmpty(id), "id", id);
        queryWrapper.eq(ObjUtil.isNotEmpty(userId), "userId", userId);
        queryWrapper.eq(ObjUtil.isNotEmpty(spaceId), "spaceId", spaceId);
        queryWrapper.isNull(nullSpaceId, "spaceId");
        queryWrapper.like(StrUtil.isNotBlank(name), "name", name);
        queryWrapper.like(StrUtil.isNotBlank(introduction), "introduction", introduction);
        queryWrapper.like(StrUtil.isNotBlank(picFormat), "picFormat", picFormat);
        queryWrapper.eq(StrUtil.isNotBlank(category), "category", category);
        queryWrapper.eq(ObjUtil.isNotEmpty(picSize), "picSize", picSize);
        queryWrapper.eq(ObjUtil.isNotEmpty(picWidth), "picWidth", picWidth);
        queryWrapper.eq(ObjUtil.isNotEmpty(picHeight), "picHeight", picHeight);
        queryWrapper.eq(ObjUtil.isNotEmpty(picScale), "picScale", picScale);
        queryWrapper.eq(ObjUtil.isNotEmpty(reviewStatus), "reviewStatus", reviewStatus);
        queryWrapper.like(StrUtil.isNotBlank(reviewMessage), "reviewMessage", reviewMessage);
        queryWrapper.eq(ObjUtil.isNotEmpty(reviewId), "reviewId", reviewId);
        Date startEditTime = pictureQueryRequest.getStartEditTime();
        Date endEditTime = pictureQueryRequest.getEndEditTime();
        queryWrapper.ge(ObjUtil.isNotEmpty(startEditTime), "editTime", startEditTime);
        queryWrapper.lt(ObjUtil.isNotEmpty(endEditTime), "editTime", endEditTime);
        //JSON数组查询
        if(CollUtil.isNotEmpty(tags)){
            for(String tag : tags){
                queryWrapper.like("tags", "\"" + tag + "\"");
            }
        }
        //排序
        queryWrapper.orderBy(StrUtil.isNotBlank(sortField),
                sortOrder.equals("ascend"), sortField);
        return queryWrapper;
    }

    /**
     * 对象转封装类，并关联查询用户信息
     * @param picture
     * @param request
     * @return
     */
    @Override
    public PictureVO getPictureVO(Picture picture, HttpServletRequest request) {
        //对象转封装类
        PictureVO pictureVO = PictureVO.objToVo(picture);
        //关联查询用户信息
        Long userId = picture.getUserId();
        if(userId != null && userId > 0){
            User user = userService.getById(userId);
            UserVO userVO = userService.getUserVO(user);
            pictureVO.setUser(userVO);
        }
        return pictureVO;
    }

    /**
     * 分页查询视图封装类，并关联查询用户信息
     * @param picturePage
     * @param request
     * @return
     */
    @Override
    public Page<PictureVO> getPictureVOPage(Page<Picture> picturePage, HttpServletRequest request) {
        List<Picture> pictureList = picturePage.getRecords();
        Page<PictureVO> pictureVOPage = new Page<>(picturePage.getCurrent(), picturePage.getSize(), picturePage.getTotal());
        if(CollUtil.isEmpty(pictureList)){
            return pictureVOPage;
        }
        //对象列表 => 封装对象列表
        List<PictureVO> pictureVOList = pictureList.stream()
                .map(PictureVO::objToVo)
                .collect(Collectors.toList());
        //1.关联查询用户信息
        Set<Long> userIdSet = pictureList.stream()
                .map(Picture::getUserId)
                .collect(Collectors.toSet());
        Map<Long, List<User>> userIdUserListMap = userService.listByIds(userIdSet).stream()
                .collect(Collectors.groupingBy(User::getId));
        //2.填充信息
        pictureVOList.forEach(pictureVO -> {
            Long userId = pictureVO.getUserId();
            User user = null;
            if(userIdUserListMap.containsKey(userId)){
                user = userIdUserListMap.get(userId).get(0);
            }
            pictureVO.setUser(userService.getUserVO(user));
        });
        pictureVOPage.setRecords(pictureVOList);
        return pictureVOPage;
    }

    /**
     * 校验图片是否合法
     * @param picture
     */
    @Override
    public void validPicture(Picture picture) {
        ThrowUtils.throwIf(picture == null, ErrorCode.PARAMS_ERROR);
        //从对象中取值
        Long id = picture.getId();
        String url = picture.getUrl();
        String introduction = picture.getIntroduction();
        //修改数据是，id不能为空，有参数则校验
        ThrowUtils.throwIf(ObjUtil.isNull(id), ErrorCode.PARAMS_ERROR, "参数不能为空");
        if(StrUtil.isNotBlank(url)){
            ThrowUtils.throwIf(url.length() > 1024, ErrorCode.PARAMS_ERROR, "url过长");
        }
        if(StrUtil.isNotBlank(introduction)){
            ThrowUtils.throwIf(introduction.length() > 800, ErrorCode.PARAMS_ERROR, "简介过长");
        }
    }

    /**
     * 图片审核
     * @param pictureReviewRequest
     * @param loginUser
     */
    @Override
    public void doPictureReview(PictureReviewRequest pictureReviewRequest, User loginUser) {
        //1、校验参数
        ThrowUtils.throwIf(pictureReviewRequest == null, ErrorCode.PARAMS_ERROR);

        Long id = pictureReviewRequest.getId();
        Integer reviewStatus = pictureReviewRequest.getReviewStatus();
        PictureReviewStatusEnum reviewStatusEnum = PictureReviewStatusEnum.getEnumByValue(reviewStatus);
        if(id == null || reviewStatusEnum == null || PictureReviewStatusEnum.REVIEWING.equals(reviewStatus)){
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        //2、判断是否存在
        Picture oldPicture = this.getById(id);
        ThrowUtils.throwIf(oldPicture == null, ErrorCode.NOT_FOUND_ERROR);
        //3、已经是该状态
        if(oldPicture.getReviewStatus().equals(reviewStatus)){
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "请勿重复审核");
        }
        //4、修改数据库
        Picture updatePicture = new Picture();
        BeanUtils.copyProperties(pictureReviewRequest, updatePicture);
        updatePicture.setReviewId(loginUser.getId());
        updatePicture.setReviewTime(new Date());
        boolean result = this.updateById(updatePicture);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
    }

    /**
     * 填充审核参数
     * @param picture
     * @param loginUser
     */
    @Override
    public void fillReviewParams(Picture picture, User loginUser) {
        if(userService.isAdmin(loginUser)){
            //管理员自动过审
            picture.setReviewStatus(PictureReviewStatusEnum.PASS.getValue());
            picture.setReviewId(loginUser.getId());
            picture.setReviewMessage("管理员自动过审");
            picture.setReviewTime(new Date());
        }
        else {
            //非管理员，创建或编辑都要改为待审核
            picture.setReviewStatus(PictureReviewStatusEnum.REVIEWING.getValue());
        }
    }

    /**
     * 批量抓取和上传图片
     * @param pictureUploadByBatchRequest
     * @param loginUser
     * @return
     */
    @Override
    public Integer uploadPictureByBatch(PictureUploadByBatchRequest pictureUploadByBatchRequest, User loginUser) {
        String searchText = pictureUploadByBatchRequest.getSearchText();
        //格式化数量
        Integer count = pictureUploadByBatchRequest.getCount();
        ThrowUtils.throwIf(count > 30, ErrorCode.PARAMS_ERROR, "最多 30 条");
        String namePrefix = pictureUploadByBatchRequest.getNamePrefix();
        if(StrUtil.isBlank(namePrefix)){
            namePrefix = searchText;
        }
        //要抓取的地址
        String fetchUrl = String.format("https://cn.bing.com/images/async?q=%s&mmasync=1", searchText);
        Document document;
        try {
            document = Jsoup.connect(fetchUrl).get();
        } catch (IOException e){
            log.error("获取页面失败",e);
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "获取页面失败");
        }
        Element div = document.getElementsByClass("dgControl").first();
        if(ObjUtil.isNull(div)){
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "获取元素失败");
        }
        Elements imgElementList = div.select("img.mimg");
        int uploadCount = 0;
        for(Element imgElement : imgElementList){
            String fileUrl = imgElement.attr("src");
            if(StrUtil.isBlank(fileUrl)){
                log.info("当前链接为空，已跳过：{}", fileUrl);
                continue;
            }
            //处理图片地址，防止出现转义问题
            int questionMarkIndex = fileUrl.indexOf("?");
            if(questionMarkIndex > -1){
                fileUrl = fileUrl.substring(0, questionMarkIndex);
            }
            //上传图片
            PictureUploadRequest pictureUploadRequest = new PictureUploadRequest();
            if(StrUtil.isNotBlank(namePrefix)){
                pictureUploadRequest.setPicName(namePrefix + (uploadCount + 1));
            }
            try{
                PictureVO pictureVO = this.uploadPicture(fileUrl, pictureUploadRequest, loginUser);
                log.info("图片上传成功，id = {}",pictureVO.getId());
                uploadCount++;
            }catch (Exception e){
                log.error("图片上传失败", e);
                continue;
            }
            if(uploadCount >= count){
                break;
            }
        }
        return uploadCount;
    }

    /**
     * 清理图片文件
     * @param oldPicture
     */
    @Async
    @Override
    public void clearPictureFile(Picture oldPicture){
        //判断是否被多条记录使用
        String pictureUrl = oldPicture.getUrl();
        long count = this.lambdaQuery()
                .eq(Picture::getUrl, pictureUrl)
                .count();
        //有不止一条记录使用，不清理
        if(count > 1){
            return;
        }
        String thumbnailUrl = oldPicture.getThumbnailUrl();
        //
        String oldPictureUrl = oldPicture.getUrl();
        String oldPictureUrltoKey = getKeyFromUrl(oldPictureUrl);

        cosManager.deleteObject(oldPictureUrltoKey);
        //清理缩略图
        if(StrUtil.isNotBlank(thumbnailUrl)){
            cosManager.deleteObject(getKeyFromUrl(thumbnailUrl));
            //删除原图
            String originKey = FileUtil.mainName(oldPictureUrltoKey) + "." + FileUtil.getSuffix(getKeyFromUrl(thumbnailUrl));
            cosManager.deleteObject(originKey);
        }
    }

    /**
     * 获取图片的key
     * @param url
     * @return
     */
    @Override
    public String getKeyFromUrl(String url) {
        String host = cosClientConfig.getHost();
        if (StrUtil.isNotBlank(host) && url.startsWith(host)) {
            return url.substring(host.length() + 1); // +1 去掉开头的 "/"
        }
        return url;
    }

    /**
     * 异步 AI 识别图片标签和分类
     * @param pictureId 图片 id
     * @param imageUrl 图片 URL
     */
    @Async
    public void autoRecognizeTags(Long pictureId, String imageUrl) {
        try {
            String result = aliYunAiApi.recognizePictureTag(imageUrl);
            // 去除 AI 返回中的 markdown 代码块标记
            result = result.trim();
            if (result.startsWith("```")) {
                result = result.replaceAll("```json\\s*", "").replaceAll("```\\s*", "").trim();
            }
            // 解析 AI 返回的 JSON
            JSONObject jsonObject = JSONUtil.parseObj(result);
            // 提取 tags
            List<String> tags = jsonObject.getBeanList("tags", String.class);
            // 提取 category
            String category = jsonObject.getStr("category");
            // 校验标签是否在预定义列表中
            if (tags != null) {
                tags.retainAll(PictureConstant.TAG_LIST);
            }
            // 校验分类是否在预定义列表中
            if (category != null && !PictureConstant.CATEGORY_LIST.contains(category)) {
                category = null;
            }
            // 更新数据库
            this.lambdaUpdate()
                    .eq(Picture::getId, pictureId)
                    .set(tags != null && !tags.isEmpty(), Picture::getTags, JSONUtil.toJsonStr(tags))
                    .set(StrUtil.isNotBlank(category), Picture::getCategory, category)
                    .update();
            log.info("AI 图片标签识别完成，pictureId: {}, tags: {}, category: {}", pictureId, tags, category);
            // 用 AI 生成的自然语言描述做向量化存储（用于语义搜索）
            if (pictureSearchService != null) {
                String aiDescription = jsonObject.getStr("description");
                if (StrUtil.isBlank(aiDescription)) {
                    aiDescription = buildPictureDescription(null, tags, category);
                }
                pictureSearchService.indexPicture(pictureId, aiDescription);
            }
            // AI Agent 自动审核图片
            if (pictureReviewAgentService != null) {
                pictureReviewAgentService.reviewPicture(pictureId, imageUrl);
            }
        } catch (Exception e) {
            log.error("AI 图片标签识别失败，pictureId: {}", pictureId, e);
        }
    }

    /**
     * 构建图片描述文本（用于向量化）
     */
    private String buildPictureDescription(String picName, List<String> tags, String category) {
        StringBuilder sb = new StringBuilder();
        if (StrUtil.isNotBlank(picName)) {
            sb.append(picName).append("。");
        }
        if (StrUtil.isNotBlank(category)) {
            sb.append("分类：").append(category).append("。");
        }
        if (tags != null && !tags.isEmpty()) {
            sb.append("标签：").append(String.join("、", tags));
        }
        return sb.toString();
    }

    /**
     * AI 识别图片标签和分类（同步，用于前端表单填充）
     * @param imageUrl 图片 URL
     * @return 包含 tags 和 category 的 Map
     */
    @Override
    public Map<String, Object> recognizePictureTags(String imageUrl) {
        String result = aliYunAiApi.recognizePictureTag(imageUrl);
        // 去除 AI 返回中的 markdown 代码块标记
        result = result.trim();
        if (result.startsWith("```")) {
            result = result.replaceAll("```json\\s*", "").replaceAll("```\\s*", "").trim();
        }
        // 解析 AI 返回的 JSON
        JSONObject jsonObject = JSONUtil.parseObj(result);
        // 提取 tags
        List<String> tags = jsonObject.getBeanList("tags", String.class);
        // 提取 category
        String category = jsonObject.getStr("category");
        // 校验标签是否在预定义列表中
        if (tags != null) {
            tags.retainAll(PictureConstant.TAG_LIST);
        }
        // 校验分类是否在预定义列表中
        if (category != null && !PictureConstant.CATEGORY_LIST.contains(category)) {
            category = null;
        }
        Map<String, Object> map = new java.util.HashMap<>();
        map.put("tags", tags);
        map.put("category", category);
        return map;
    }

    /**
     * 删除图片
     * @param pictureId
     * @param loginUser
     */
    @Override
    public void deletePicture(long pictureId, User loginUser) {
        ThrowUtils.throwIf(pictureId <= 0, ErrorCode.PARAMS_ERROR);
        ThrowUtils.throwIf(loginUser == null, ErrorCode.NO_AUTH_ERROR);
        //判断是否存在
        Picture oldPicture = this.getById(pictureId);
        ThrowUtils.throwIf(oldPicture == null, ErrorCode.NOT_FOUND_ERROR);
        //权限校验
        //已改为注解鉴权
        //this.checkPictureAuth(loginUser, oldPicture);
        //开启事务
        transactionTemplate.execute(status -> {
            //操作数据库
            boolean result = this.removeById(pictureId);
            ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
            //释放额度
            Long finalSpaceId = oldPicture.getSpaceId();
            if(finalSpaceId != null){
                boolean update = spaceService.lambdaUpdate()
                        .eq(Space::getId, finalSpaceId)
                        .setSql("totalSize = totalSize -" + oldPicture.getPicSize())
                        .setSql("totalCount = totalCount - 1")
                        .update();
                ThrowUtils.throwIf(!update, ErrorCode.OPERATION_ERROR,"额度更新失败");
            }
            return true;
        });
        //异步清理图片
        this.clearPictureFile(oldPicture);
    }

    /**
     * 编辑图片
     * @param pictureEditRequest
     * @param loginUser
     */
    @Override
    public void editPicture(PictureEditRequest pictureEditRequest, User loginUser) {
        //将dto转为实体类
        Picture picture = new Picture();
        BeanUtils.copyProperties(pictureEditRequest, picture);
        //注意tags的转换
        picture.setTags(JSONUtil.toJsonStr(pictureEditRequest.getTags()));
        //设置编辑时间
        picture.setEditTime(new Date());
        //数据校验
        this.validPicture(picture);
        //判断是否存在
        Long id = pictureEditRequest.getId();
        Picture oldPicture = this.getById(id);
        ThrowUtils.throwIf(oldPicture == null, ErrorCode.NOT_FOUND_ERROR);
        //权限校验
        //已改为注解鉴权
        //this.checkPictureAuth(loginUser, oldPicture);
        //补充审核参数
        this.fillReviewParams(picture, loginUser);
        //操作数据库
        boolean result = this.updateById(picture);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        // 编辑后重新触发 AI 审核
        if (pictureReviewAgentService != null) {
            pictureReviewAgentService.reviewPicture(id, oldPicture.getUrl());
        }
    }


    /**
     * 检查图片权限
     * @param loginUser
     * @param picture
     */
    @Override
    public void checkPictureAuth(User loginUser, Picture picture) {
        Long spaceId = picture.getSpaceId();
        if(spaceId == null){
            //公共图库，仅本人和管理员可操作
            if(!userService.isAdmin(loginUser) && !picture.getUserId().equals(loginUser.getId())){
                throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
            }
        }else {
            //私有空间，仅空间管理员可操作
            if(!picture.getUserId().equals(loginUser.getId())){
                throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
            }
        }
    }

    /**
     * 根据图片主色调搜索图片
     * @param spaceId
     * @param picColor
     * @param loginUser
     * @return
     */
    @Override
    public List<PictureVO> searchPictureByColor(Long spaceId, String picColor, User loginUser) {
        //1、校验参数
        ThrowUtils.throwIf(spaceId == null || StrUtil.isBlank(picColor), ErrorCode.PARAMS_ERROR);
        ThrowUtils.throwIf(loginUser == null, ErrorCode.NO_AUTH_ERROR);
        //2、校验空间权限
        Space space = spaceService.getById(spaceId);
        ThrowUtils.throwIf(space == null, ErrorCode.NOT_FOUND_ERROR, "空间不存在");
        if(!loginUser.getId().equals(space.getUserId())) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "没有空间访问权限");
        }
        //3、查询该空间下所有图片（必须有主色调)
        List<Picture> pictureList = this.lambdaQuery()
                .eq(Picture::getSpaceId, spaceId)
                .isNotNull(Picture::getPicColor)
                .list();
        //如果没有图片，直接返回空列表
        if(CollUtil.isEmpty(pictureList)) {
            return Collections.emptyList();
        }
        //将目标颜色转为Color对象
        Color targetColor = Color.decode(picColor);
        //4、计算相似度并排序
        List<Picture> sortedPictures = pictureList.stream()
                .sorted(Comparator.comparingDouble(picture -> {
                    //提取图片主色调
                    String hexColor = picture.getPicColor();
                    //没有主色调的图片放在最后
                    if(StrUtil.isBlank(hexColor)) {
                        return Double.MAX_VALUE;
                    }
                    Color pictureColor = Color.decode(hexColor);
                    //越大越相似
                    return -ColorSimilarUtils.calculateSimilarity(targetColor, pictureColor);
                }))
                .limit(12)
                .collect(Collectors.toList());
        return sortedPictures.stream()
                .map(PictureVO::objToVo)
                .collect(Collectors.toList());
    }

    /**
     * 批量编辑图片
     * @param pictureEditByBatchRequest
     * @param loginUser
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void editPictureByBatch(PictureEditByBatchRequest pictureEditByBatchRequest, User loginUser) {
        List<Long> pictureIdList = pictureEditByBatchRequest.getPictureIdList();
        Long spaceId = pictureEditByBatchRequest.getSpaceId();
        String category = pictureEditByBatchRequest.getCategory();
        List<String> tags = pictureEditByBatchRequest.getTags();
        //1、校验参数
        ThrowUtils.throwIf(spaceId == null || CollUtil.isEmpty(pictureIdList), ErrorCode.PARAMS_ERROR);
        ThrowUtils.throwIf(loginUser == null, ErrorCode.NO_AUTH_ERROR);
        //2、校验空间权限
        Space space = spaceService.getById(spaceId);
        ThrowUtils.throwIf(space == null, ErrorCode.NOT_FOUND_ERROR, "空间不存在");
        if(!loginUser.getId().equals(space.getUserId())) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "没有空间访问权限");
        }
        //3、查询指定图片，仅选择需要的字段
        List<Picture> pictureList = this.lambdaQuery()
                .select(Picture::getId, Picture::getSpaceId)
                .in(Picture::getId, pictureIdList)
                .list();
        if(pictureList.isEmpty()){
            return;
        }
        //4、更新分类和标签
        pictureList.forEach(picture -> {
            if(StrUtil.isNotBlank( category)){
                picture.setCategory(category);
            }
            if(CollUtil.isNotEmpty(tags)){
                picture.setTags(JSONUtil.toJsonStr(tags));
            }
        });
        //批量重命名
        String nameRule = pictureEditByBatchRequest.getNameRule();
        fillPictureWithNameRule(pictureList, nameRule);
        //5、批量更新
        boolean result = this.updateBatchById(pictureList);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
    }

    /**
     * 根据重命名规则批量重命名图片
     * @param pictureList
     * @param nameRule
     */
    private void fillPictureWithNameRule(List<Picture> pictureList, String nameRule) {
        if(CollUtil.isEmpty(pictureList) || StrUtil.isBlank(nameRule)){
            return;
        }
        long count = 1;
        try {
            for (Picture picture : pictureList){
                String pictureName = nameRule.replaceAll("\\{序号}", String.valueOf(count++));
                picture.setName(pictureName);
            }
        } catch (Exception e) {
            log.error("名称解析错误", e);
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "名称解析错误");
        }
    }

    /**
     * 创建图片外绘任务
     * @param createPictureOutPaintingTaskRequest
     * @param loginUser
     * @return
     */
    @Override
    public CreateOutPaintingTaskResponse createPictureOutPaintingTask(CreatePictureOutPaintingTaskRequest createPictureOutPaintingTaskRequest, User loginUser) {
        // 获取图片信息
        Long pictureId = createPictureOutPaintingTaskRequest.getPictureId();
        Picture picture = Optional.ofNullable(this.getById(pictureId))
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND_ERROR));
        // 权限校验
        //已改为注解鉴权
        //checkPictureAuth(loginUser, picture);
        // 构造请求参数
        CreateOutPaintingTaskRequest taskRequest = new CreateOutPaintingTaskRequest();
        CreateOutPaintingTaskRequest.Input input = new CreateOutPaintingTaskRequest.Input();
        input.setImageUrl(picture.getUrl());
        taskRequest.setInput(input);
        BeanUtil.copyProperties(createPictureOutPaintingTaskRequest, taskRequest, "parameters");
        // 设置扩图参数默认值
        CreateOutPaintingTaskRequest.Parameters parameters = createPictureOutPaintingTaskRequest.getParameters();
        if (parameters == null) {
            parameters = new CreateOutPaintingTaskRequest.Parameters();
        }
        if (parameters.getXScale() == null) {
            parameters.setXScale(1.5f);
        }
        if (parameters.getYScale() == null) {
            parameters.setYScale(1.5f);
        }
        taskRequest.setParameters(parameters);
        // 创建任务
        return aliYunAiApi.createOutPaintingTask(taskRequest);
    }
}





























