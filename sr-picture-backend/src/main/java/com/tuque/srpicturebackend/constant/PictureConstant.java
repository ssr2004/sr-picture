package com.tuque.srpicturebackend.constant;

import java.util.Arrays;
import java.util.List;

public interface PictureConstant {

    /**
     * 图片标签列表
     */
    List<String> TAG_LIST = Arrays.asList("热门", "搞笑", "生活", "高清", "艺术", "校园", "背景", "简历", "创意");

    /**
     * 图片分类列表
     */
    List<String> CATEGORY_LIST = Arrays.asList("模板", "电商", "表情包", "素材", "海报");
}
