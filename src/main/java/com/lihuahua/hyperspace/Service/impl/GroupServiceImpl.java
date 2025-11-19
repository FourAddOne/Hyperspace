package com.lihuahua.hyperspace.Service.impl;

import com.lihuahua.hyperspace.Service.GroupService;
import com.lihuahua.hyperspace.mapper.GroupMapper;
import com.lihuahua.hyperspace.mapper.GroupMemberMapper;
import com.lihuahua.hyperspace.models.dto.CreateGroupDTO;
import com.lihuahua.hyperspace.models.po.GroupMemberPO;
import com.lihuahua.hyperspace.models.po.GroupPO;
import com.lihuahua.hyperspace.models.vo.GroupVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Service
public class GroupServiceImpl implements GroupService {

    @Autowired
    private GroupMapper groupMapper;
    @Autowired
    private GroupMemberMapper groupMemberMapper;


    @Override
    public GroupVO createGroup(CreateGroupDTO createGroupDTO) {
        GroupPO groupPO=new GroupPO();

        groupPO.setGroupName(createGroupDTO.getName());
        groupPO.setCreatorId(createGroupDTO.getOwner());
        groupPO.setCreatedAt(new Date().getTime());
        groupMapper.insert(groupPO);

        String[] membersId=createGroupDTO.getMembers();
        List<GroupMemberPO> groupMembers=new ArrayList<>();
        for(String id:membersId){
            GroupMemberPO groupMember=new GroupMemberPO();
            groupMember.setGroupId(String.valueOf(groupPO.getId()));
            groupMember.setUserId(id);
            groupMember.setJoinedAt(new Date().getTime());
            groupMember.setRole("NORMAL");
            groupMembers.add(groupMember);
        }
        groupMemberMapper.insert(groupMembers);

        GroupVO groupVO=new GroupVO();
        groupVO.setGroupId(String.valueOf(groupPO.getId()));
        groupVO.setGroupName(createGroupDTO.getName());
        groupVO.setCreatorId(createGroupDTO.getOwner());
        groupVO.setMemberIds(List.of(membersId));
        groupVO.setCreatedAt(groupPO.getCreatedAt());
        return groupVO;
    }
}
