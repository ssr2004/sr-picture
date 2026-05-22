package com.tuque.srpicturebackend.model.dto.space.analyze;

import lombok.Data;

import java.io.Serializable;

@Data
public class SpaceRankAnalyzeRequest implements Serializable {

    private static final long serialVersionUID = -105380605231046458L;
    /**
     * 排名前 N 的空间
     */
    private Integer topN = 10;

}