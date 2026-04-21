package com.yupi.yupicturebackend.common;

import lombok.Data;

/**
 * 分页请求包装类
 */
@Data
public class PageRequest {
    /**
     * 当前页码，默认为1
     */
    private int current = 1;

    /**
     * 页面大小，默认为10
     */
    private int pageSize = 10;

    /**
     * 排序字段
     */
    private String sortField;

    /**
     * 排序方式，默认为降序
     */
    private String sortOrder = "descend";
}
