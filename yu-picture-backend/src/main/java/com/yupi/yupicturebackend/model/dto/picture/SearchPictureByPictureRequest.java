package com.yupi.yupicturebackend.model.dto.picture;

import lombok.Data;

import java.io.Serializable;

@Data
public class SearchPictureByPictureRequest implements Serializable {

    private static final long serialVersionUID = -130366864275430208L;
    /**
     * 图片id
     */
    private Long pictureId;
}
