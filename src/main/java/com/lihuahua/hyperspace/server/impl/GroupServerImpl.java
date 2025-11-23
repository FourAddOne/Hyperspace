package com.lihuahua.hyperspace.server.impl;

import com.lihuahua.hyperspace.mapper.GroupMapper;
import com.lihuahua.hyperspace.models.dto.GroupDTO;
import com.lihuahua.hyperspace.server.GroupServer;
import com.lihuahua.hyperspace.utils.JacksonUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;


@Slf4j
@Service
public class GroupServerImpl implements GroupServer {
    @Autowired
    private GroupMapper groupMapper;




    @Override
    public void createGroup(GroupDTO groupDTO) {

        groupDTO.addMember(groupDTO.getUserId());

        groupMapper.createGroup(groupDTO.getGroupName(), groupDTO.getGroupId(), groupDTO.getUserId(),JacksonUtils.toJson(groupDTO.getMembers()));
        groupMapper.joinGroup(groupDTO.getGroupId(), groupDTO.getUserId(),"OWNER");


    }

    @Override
    public void joinGroup(GroupDTO groupDTO) {

        groupDTO.setRole("MEMBER");
        groupMapper.joinGroup(groupDTO.getGroupId(), groupDTO.getUserId(),groupDTO.getRole());

    }

    @Override
    public void quitGroup(GroupDTO groupDTO) {
        groupMapper.quitGroup(groupDTO.getGroupId(), groupDTO.getUserId());

    }

    @Override
    public void groupMessage(GroupDTO groupDTO) {

    }

    @Override
    public List<GroupDTO> groupList(GroupDTO groupDTO) {
        return groupMapper.groupListbyUserId(groupDTO.getUserId());

    }

    @Override
    public void groupUserList(GroupDTO groupDTO) {

    }
}
