package com.tuque.srpicturebackend.api.imagesearch.sub;

import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.http.HttpUtil;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.tuque.srpicturebackend.api.imagesearch.model.ImageSearchResult;
import com.tuque.srpicturebackend.exception.BusinessException;
import com.tuque.srpicturebackend.exception.ErrorCode;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.function.ToDoubleBiFunction;

//todo 以图搜图功能实现

@Deprecated
@Slf4j
public class GetImageListApi {
    public static List<ImageSearchResult> getImageList(String url){
        try {
            //发送GET请求
            HttpResponse response = HttpUtil.createGet(url).execute();
            //获取响应内容
            int statusCode = response.getStatus();
            String body = response.body();

            //处理响应
            if(statusCode == 200){
                //解析JOSN数据并处理
                return processResponse(body);
            } else {
                throw new BusinessException(ErrorCode.OPERATION_ERROR,"接口调用失败");
            }
        } catch (Exception e) {
            log.error("获取图片列表失败", e);
            throw new BusinessException(ErrorCode.OPERATION_ERROR,"获取图片列表失败");
        }
    }

    private static List<ImageSearchResult> processResponse(String responseBody) {
        //解析响应对象
        JSONObject jsonObject = new JSONObject(responseBody);
        if(!jsonObject.containsKey("data")){
            throw new BusinessException(ErrorCode.OPERATION_ERROR,"未读取到图片列表");
        }
        JSONObject data = jsonObject.getJSONObject("data");
        if(!data.containsKey("list")){
            throw new BusinessException(ErrorCode.OPERATION_ERROR,"未读取到图片列表");
        }
        JSONArray list = data.getJSONArray("list");
        return JSONUtil.toList(list, ImageSearchResult.class);
    }
}
