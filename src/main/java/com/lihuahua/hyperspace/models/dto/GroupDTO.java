package com.lihuahua.hyperspace.models.dto;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class GroupDTO {
    private String groupId;
    private String groupName;
    private String userId;
    private String role;
    List<String> members= new ArrayList<>();
    public void addMember(String memberId) {
        members.add(memberId);
    }


}
