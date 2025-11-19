package com.lihuahua.hyperspace.models.dto;

import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;

@Data
@Tag(name = "创建群组")
public class CreateGroupDTO {

    private String name;
    private String description;
    private String avatar;
    private String owner;
    private String[] members;
}
