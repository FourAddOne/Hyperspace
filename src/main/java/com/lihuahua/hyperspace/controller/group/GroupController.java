package com.lihuahua.hyperspace.controller.group;

import cn.hutool.db.sql.Wrapper;
import com.lihuahua.hyperspace.Service.GroupService;
import com.lihuahua.hyperspace.mapper.GroupMapper;
import com.lihuahua.hyperspace.mapper.GroupMemberMapper;
import com.lihuahua.hyperspace.models.dto.CreateGroupDTO;
import com.lihuahua.hyperspace.models.entity.GroupMember;
import com.lihuahua.hyperspace.models.po.GroupPO;
import com.lihuahua.hyperspace.models.vo.GroupVO;
import com.lihuahua.hyperspace.models.vo.ResVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@RestController
@RequestMapping("/group")
@Slf4j
public class GroupController {

    @Autowired
    private GroupService groupService;

    @RequestMapping("/create")
    public ResVO createGroup(CreateGroupDTO createGroupDTO){


        GroupVO groupVO = groupService.createGroup(createGroupDTO);


        return ResVO.success(groupVO);

    }
}
