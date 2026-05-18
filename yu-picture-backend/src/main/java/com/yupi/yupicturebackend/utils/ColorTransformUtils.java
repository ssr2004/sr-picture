package com.yupi.yupicturebackend.utils;

/**
 * 颜色转换工具类
 */
public class ColorTransformUtils {
    private ColorTransformUtils() {
        // 禁止实例化
    }

    /**
     * 获取标准颜色
     * @param color
     * @return
     */
    public static String getStandardColor(String color){
        if(color.length() == 7){
            color = color.substring(0, 4) + "0" + color.substring(4, 7);
        }
        return color;
    }
}
