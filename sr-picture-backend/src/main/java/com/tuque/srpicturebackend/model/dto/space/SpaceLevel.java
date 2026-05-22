package com.tuque.srpicturebackend.model.dto.space;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class SpaceLevel {
    private int value;
    private String text;
    private Long maxCount;
    private Long maxSize;
}
