package com.lihuahua.hyperspace.models.entity;

import lombok.Data;

import java.util.List;


@Data
public class Group {
    private String groupId;
    private String groupName;
    private String ownerId;
    private List<User> members;
    private int Status;
}
