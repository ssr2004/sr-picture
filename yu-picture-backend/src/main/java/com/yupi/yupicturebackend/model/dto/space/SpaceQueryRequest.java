package com.yupi.yupicturebackend.model.dto.space;

import com.yupi.yupicturebackend.common.PageRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;

/**
 * 空间查询请求
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class SpaceQueryRequest extends PageRequest implements Serializable {

    private static final long serialVersionUID = -8161789033511316766L;

    /**
     *  id
     */
    private Long id;

    /**
     * 创建用户id
     */
    private Long userId;

    /**
     * 空间名
     */
    private String spaceName;

    /**
     * 空间级别 0-普通版 1-专业版 2-旗舰版
     */
    private Integer spaceLevel;
}
