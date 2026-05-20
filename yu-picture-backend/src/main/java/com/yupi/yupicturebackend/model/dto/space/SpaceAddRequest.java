package com.yupi.yupicturebackend.model.dto.space;


import lombok.Data;

import java.io.Serializable;

/**
 * 空间创建请求
 */
@Data
public class SpaceAddRequest implements Serializable {
    private static final long serialVersionUID = 8296395758497341043L;

    /**
     * 空间名
     */
    private String spaceName;

    /**
     * 空间级别 0-普通版 1-专业版 2-旗舰版
     */
    private Integer spaceLevel;

    /**
     * 空间类型：0-私有 1-团队
     */
    private Integer spaceType;

}
