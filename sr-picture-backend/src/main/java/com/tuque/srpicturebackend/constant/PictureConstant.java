package com.tuque.srpicturebackend.constant;

import java.util.Arrays;
import java.util.List;

public interface PictureConstant {

    /**
     * 图片标签列表
     */
    List<String> TAG_LIST = Arrays.asList(
            "热门", "搞笑", "生活", "高清", "艺术", "校园", "背景", "简历", "创意",
            "自然", "风景", "动物", "宠物", "植物", "花卉", "美食", "建筑", "人物",
            "运动", "科技", "交通", "海洋", "天空", "夜景", "城市", "乡村", "节日",
            "卡通", "插画", "图标", "壁纸", "头像", "表情", "穿搭", "家居", "汽车"
    );

    /**
     * 图片分类列表
     */
    List<String> CATEGORY_LIST = Arrays.asList(
            "模板", "电商", "表情包", "素材", "海报",
            "风景", "动物", "人物", "美食", "建筑",
            "科技", "艺术", "自然", "运动", "交通"
    );
}
