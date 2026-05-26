package com.tuque.srpicturebackend.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 接口限流注解
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RateLimit {

    /**
     * 时间窗口内最大请求数
     */
    int maxCount() default 10;

    /**
     * 时间窗口（秒）
     */
    int timeWindowSeconds() default 60;

    /**
     * 限流 key 前缀，默认使用方法名
     */
    String key() default "";

    /**
     * 被限流时的提示信息
     */
    String message() default "请求过于频繁，请稍后再试";
}
