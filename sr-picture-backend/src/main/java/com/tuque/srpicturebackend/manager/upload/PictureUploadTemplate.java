package com.tuque.srpicturebackend.manager.upload;


import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.NumberUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpResponse;
import cn.hutool.http.HttpStatus;
import cn.hutool.http.HttpUtil;
import cn.hutool.http.Method;
import com.qcloud.cos.model.PutObjectResult;
import com.qcloud.cos.model.ciModel.persistence.CIObject;
import com.qcloud.cos.model.ciModel.persistence.ImageInfo;
import com.qcloud.cos.model.ciModel.persistence.ProcessResults;
import com.tuque.srpicturebackend.config.CosClientConfig;
import com.tuque.srpicturebackend.exception.BusinessException;
import com.tuque.srpicturebackend.exception.ErrorCode;
import com.tuque.srpicturebackend.exception.ThrowUtils;
import com.tuque.srpicturebackend.manager.CosManager;
import com.tuque.srpicturebackend.model.dto.file.UploadPictureResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

/**
 * 图片上传模板
 */
@Slf4j
public abstract class PictureUploadTemplate {
    @Resource
    private CosClientConfig cosClientConfig;

    @Resource
    private CosManager cosManager;

    /**
     * 图片上传
     * @param inputSource
     * @param uploadPathPrefiex
     * @return
     */
    public UploadPictureResult uploadPicture(Object inputSource, String uploadPathPrefiex){
        //校验图片
        validPicture(inputSource);
        //图片上传地址
        String uuid = RandomUtil.randomString(16);
        String originFilename = getOriginFilename(inputSource);
        String uploadFilename = String.format("%s_%s.%s", DateUtil.formatDate(new Date()),uuid,
                FileUtil.getSuffix(originFilename));
        String uploadPath = String.format("/%s/%s", uploadPathPrefiex, uploadFilename);
        File file = null;

        try {
            //创建临时文件
            file = File.createTempFile(uploadPath, null);
            //处理文件来源（本地或URL）
            processFile(inputSource, file);
            //上传图片
            PutObjectResult putObjectResult = cosManager.putPictureObject(uploadPath, file);
            ImageInfo imageInfo = putObjectResult.getCiUploadResult().getOriginalInfo().getImageInfo();
            ProcessResults processResults = putObjectResult.getCiUploadResult().getProcessResults();
            List<CIObject> objectList = processResults.getObjectList();
            if(CollUtil.isNotEmpty(objectList)){
                CIObject compressCiObject = objectList.get(0);
                CIObject thumbnailCiObject = compressCiObject;
                if(objectList.size() > 1){
                    thumbnailCiObject = objectList.get(1);
                }
                //封装压缩图返回结果
                return buildResult(originFilename, compressCiObject, thumbnailCiObject, imageInfo);
            }
            //封装返回结果
            return buildResult(originFilename, file, uploadPath, imageInfo);

        } catch (Exception e) {
            log.error("图片上传到对象存储失败, inputSource={}, uploadPath={}", inputSource, uploadPath, e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "上传失败");
        }finally {
            this.deleteTempFile(file);
        }
    }

    /**
     * 构建图片压缩后的返回结果
     * @param originFilename
     * @param compressCiObject
     * @return
     */
    private UploadPictureResult buildResult(String originFilename, CIObject compressCiObject, CIObject thumbnailCiObject, ImageInfo imageInfo) {
        UploadPictureResult uploadPictureResult = new UploadPictureResult();
        //计算宽高比
        int picWidth = compressCiObject.getWidth();
        int picHeight = compressCiObject.getHeight();
        double picScale = NumberUtil.round(picWidth *1.0 / picHeight,2).doubleValue();
        uploadPictureResult.setPicName(FileUtil.mainName(originFilename));
        uploadPictureResult.setPicWidth(picWidth);
        uploadPictureResult.setPicHeight(picHeight);
        uploadPictureResult.setPicScale(picScale);
        uploadPictureResult.setPicFormat(compressCiObject.getFormat());
        uploadPictureResult.setPicColor(imageInfo.getAve());
        uploadPictureResult.setPicSize(compressCiObject.getSize().longValue());
        String thumbnailUrl = cosClientConfig.getHost() + "/" + thumbnailCiObject.getKey();
        String url = cosClientConfig.getHost() + "/" + compressCiObject.getKey();
        String format = compressCiObject.getFormat();
        uploadPictureResult.setThumbnailUrl(ensureUrlHasExtension(thumbnailUrl, format));
        uploadPictureResult.setUrl(ensureUrlHasExtension(url, format));
        return uploadPictureResult;
    }

    /**
     * 校验输入源（本地文件或URL）
     * @param inputSource
     */
    protected abstract void validPicture(Object inputSource);

    /**
     * 获取输入源的原始文件名
     * @param inputSource
     * @return
     */
    protected abstract String getOriginFilename(Object inputSource);

    /**
     * 处理输入源并生成本地临时文件
     * @param inputSource
     * @param file
     */
    protected abstract void processFile(Object inputSource, File file) throws Exception;

    /**
     * 构建返回结果
     * @param originFilename
     * @param file
     * @param uploadPath
     * @param imageInfo
     * @return
     */
    private UploadPictureResult buildResult(String originFilename, File file, String uploadPath, ImageInfo imageInfo){
        UploadPictureResult uploadPictureResult = new UploadPictureResult();
        //计算宽高比
        int picWidth = imageInfo.getWidth();
        int picHeight = imageInfo.getHeight();
        double picScale = NumberUtil.round(picWidth *1.0 / picHeight,2).doubleValue();
        uploadPictureResult.setPicName(FileUtil.mainName(originFilename));
        uploadPictureResult.setPicWidth(picWidth);
        uploadPictureResult.setPicHeight(picHeight);
        uploadPictureResult.setPicScale(picScale);
        uploadPictureResult.setPicFormat(imageInfo.getFormat());
        uploadPictureResult.setPicColor(imageInfo.getAve());
        uploadPictureResult.setPicSize(FileUtil.size(file));
        String url = cosClientConfig.getHost() + "/" + uploadPath;
        uploadPictureResult.setUrl(ensureUrlHasExtension(url, imageInfo.getFormat()));
        return uploadPictureResult;
    }
    /**
     * 确保 URL 有正确的文件扩展名
     */
    private String ensureUrlHasExtension(String url, String format) {
        if (StrUtil.isBlank(url) || StrUtil.isBlank(format)) {
            return url;
        }
        String suffix = FileUtil.getSuffix(url);
        if (StrUtil.isNotBlank(suffix)) {
            return url;
        }
        // URL 没有扩展名，去掉末尾可能多余的点，补上格式后缀
        if (url.endsWith(".")) {
            url = url.substring(0, url.length() - 1);
        }
        return url + "." + format.toLowerCase();
    }

    /**
     * 删除临时文件
     * @param file
     */
    private void deleteTempFile(File file) {
        if(file == null){
            return;
        }
        //删除临时文件
        boolean deleteResult = file.delete();
        if(!deleteResult){
            log.warn("file delete error,filepath = {}", file.getAbsolutePath());
        }
    }
}
















