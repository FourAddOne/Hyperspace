package com.lihuahua.hyperspace.controller.user;


import com.github.xiaoymin.knife4j.annotations.ApiOperationSupport;
import com.lihuahua.hyperspace.models.dto.GroupDTO;
import com.lihuahua.hyperspace.models.vo.ResVO;
import com.lihuahua.hyperspace.server.GroupServer;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.tags.Tags;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


@Slf4j
@RestController
@Tag( name = "群组管理")
@RequestMapping("/api/groups")
public class GroupController {

    @Resource
    private GroupServer groupServer;

    @Operation(summary = "创建群组")
    @PostMapping("/create")
    public ResVO createGroup(@RequestBody GroupDTO groupDTO){
        groupServer.createGroup(groupDTO);
        return ResVO.success();
    };


    @Operation(summary = "加入群组")
    @PostMapping("/join")
    public ResVO joinGroup(@RequestBody GroupDTO groupDTO) {
        groupServer.joinGroup(groupDTO);
        return ResVO.success();
    };

    @Operation(summary = "退出群组")
    @PostMapping("/quit")
    public ResVO quitGroup(@RequestBody GroupDTO groupDTO) {
        groupServer.quitGroup(groupDTO);
        return ResVO.success();
    };



    @Operation(summary = "群组消息")
    @PostMapping("/message")
    public ResVO groupMessage(@RequestBody GroupDTO groupDTO) {
        groupServer.groupMessage(groupDTO);
        return ResVO.success();
    }


    @Operation(summary = "群组列表")
    @PostMapping("/list")
    public ResVO groupList(@RequestBody GroupDTO groupDTO) {
        groupServer.groupList(groupDTO);
        return ResVO.success();
    }


};
