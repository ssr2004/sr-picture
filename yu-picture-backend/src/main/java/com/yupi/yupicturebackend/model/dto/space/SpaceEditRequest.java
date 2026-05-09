package com.yupi.yupicturebackend.model.dto.space;

import lombok.Data;

import java.io.Serializable;

/**
 * 空间编辑请求
 */
@Data
public class SpaceEditRequest implements Serializable {

    private static final long serialVersionUID = 3213699318834190005L;

    /**
     *  id
     */
    private Long id;

    /**
     * 空间名
     */
    private String spaceName;
}
