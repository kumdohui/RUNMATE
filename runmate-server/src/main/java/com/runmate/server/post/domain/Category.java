package com.runmate.server.post.domain;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Category {
    private Long categoryId;

    private String name;

    private Integer displayOrder;

    private Boolean active;
}
