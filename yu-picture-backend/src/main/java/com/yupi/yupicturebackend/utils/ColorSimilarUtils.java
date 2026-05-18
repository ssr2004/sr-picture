package com.yupi.yupicturebackend.utils;

import java.awt.*;

public class ColorSimilarUtils {

    private ColorSimilarUtils() {
        //工具类不需要实例化
    }

    /**
     * 计算颜色相似度
     * @param color1
     * @param color2
     * @return
     */
    public static double calculateSimilarity(Color color1, Color color2) {
        int r1 = color1.getRed();
        int g1 = color1.getGreen();
        int b1 = color1.getBlue();

        int r2 = color2.getRed();
        int g2 = color2.getGreen();
        int b2 = color2.getBlue();
        //计算欧氏距离
        double distance = Math.sqrt(Math.pow(r1 - r2, 2) + Math.pow(g1 - g2, 2) + Math.pow(b1 - b2, 2));

        //计算相似度
        return 1 - distance / Math.sqrt(3 * Math.pow(255, 2));
    }

    /**
     * 计算十六进制颜色相似度
     * @param hexColor1
     * @param hexColor2
     * @return
     */
    public static double calculateSimilarity(String hexColor1, String hexColor2) {
        Color color1 = Color.decode(hexColor1);
        Color color2 = Color.decode(hexColor2);
        return calculateSimilarity(color1, color2);
    }

    /**
     * 测试程序
     * @param args
     */
    public static void main(String[] args) {
        String hexColor1 = "#FF0000";
        String hexColor2 = "#00FF00";
        double similarity = calculateSimilarity(hexColor1, hexColor2);
        System.out.println("相似度：" + similarity);
    }
}
