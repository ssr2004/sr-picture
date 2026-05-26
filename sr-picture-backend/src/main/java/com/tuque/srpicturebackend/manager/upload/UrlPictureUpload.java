package com.tuque.srpicturebackend.manager.upload;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpResponse;
import cn.hutool.http.HttpStatus;
import cn.hutool.http.HttpUtil;
import cn.hutool.http.Method;
import com.tuque.srpicturebackend.exception.BusinessException;
import com.tuque.srpicturebackend.exception.ErrorCode;
import com.tuque.srpicturebackend.exception.ThrowUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.List;

/**
 * url图片上传
 */
@Service
@Slf4j
public class UrlPictureUpload extends PictureUploadTemplate {
    /**
     * 验证图片
     * @param inputSource
     */
    @Override
    protected void validPicture(Object inputSource) {
        String fileUrl = (String) inputSource;
        ThrowUtils.throwIf(StrUtil.isBlank(fileUrl), ErrorCode.PARAMS_ERROR, "文件地址不能为空");
        //1、验证url格式
        try{
            new URL(fileUrl);//验证是否是合法的url

        }catch (MalformedURLException e){
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "文件地址格式错误");
        }
        //2、校验url协议
        ThrowUtils.throwIf(!fileUrl.startsWith("http://") && !fileUrl.startsWith("https://"),
                ErrorCode.PARAMS_ERROR, "仅支持HTTP 或 HTTPS 协议的文件地址");
        //3、发送HEAD请求判断文件是否存在
        HttpResponse response = null;
        try {
            response = HttpUtil.createRequest(Method.HEAD, fileUrl)
                    .execute();
            //未正常返回，无需执行其他判断
            if (response.getStatus() != HttpStatus.HTTP_OK) {
                return;
            }
            //4、校验文件类型
            String contentType = response.header("Content-Type");
            if (StrUtil.isNotBlank(contentType)) {
                //允许的图片类型
                final List<String> ALLOW_CONTENT_TYPES = Arrays.asList("image/jpeg", "image/png", "image/jpg", "image/webp");
                ThrowUtils.throwIf(!ALLOW_CONTENT_TYPES.contains(contentType.toLowerCase()),
                        ErrorCode.PARAMS_ERROR, "文件类型错误");
            }
            //5、校验文件大小
            String contentLengthStr = response.header("Content-Length");
            if (StrUtil.isNotBlank(contentLengthStr)) {
                try {
                    long contentLength = Long.parseLong(contentLengthStr);
                    final long FIVE_MB = 5 * 1024 * 1024L;
                    ThrowUtils.throwIf(contentLength > FIVE_MB, ErrorCode.PARAMS_ERROR, "文件大小不能超过5MB");
                } catch (NumberFormatException e) {
                    throw new BusinessException(ErrorCode.PARAMS_ERROR, "文件大小格式错误");
                }
            }
        }finally {
            if(response != null){
                response.close();
            }
        }
    }

    /**
     * 获取原始文件名
     * @param inputSource
     * @return
     */
    @Override
    protected String getOriginFilename(Object inputSource) {
        String fileUrl = (String) inputSource;
        try {
            URL url = new URL(fileUrl);
            String name = FileUtil.getName(url.getPath());
            // 校验文件名：长度 > 2 且不是纯数字/布尔值
            if (StrUtil.isNotBlank(name) && name.length() > 2
                    && !name.equalsIgnoreCase("true") && !name.equalsIgnoreCase("false")) {
                return name;
            }
        } catch (MalformedURLException e) {
            // ignore
        }
        // fallback：用 URL 哈希值生成文件名
        return "img_" + Math.abs(fileUrl.hashCode()) + ".jpg";
    }

    /**
     * 处理文件
     * @param inputSource
     * @param file
     * @throws Exception
     */
    @Override
    protected void processFile(Object inputSource, File file) throws Exception {
        String fileUrl = (String) inputSource;
        log.info("开始下载文件：{}", fileUrl);
        try {
            HttpUtil.downloadFile(fileUrl, file);
        } catch (Exception e) {
            log.error("文件下载失败：{}", fileUrl, e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "文件下载失败：" + e.getMessage());
        }
        log.info("文件下载成功，大小：{} bytes", file.length());
    }
}
