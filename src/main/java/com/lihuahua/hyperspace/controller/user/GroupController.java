package com.lihuahua.hyperspace.controller.user;


import com.github.xiaoymin.knife4j.annotations.ApiOperationSupport;
import com.lihuahua.hyperspace.models.dto.GroupDTO;
import com.lihuahua.hyperspace.models.vo.ResVO;
import com.lihuahua.hyperspace.server.GroupServer;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.ArrayList;


@Slf4j
@RestController
@Tag(name = "群组管理")
@RequestMapping("/groups")
public class GroupController {

    @Resource
    private GroupServer groupServer;

    @Operation(summary = "创建群组")
    @PostMapping("/create")
    public ResVO createGroup(@RequestBody GroupDTO groupDTO){
        try{
            groupServer.createGroup(groupDTO);
            return ResVO.success("创建群组成功");
        }catch (Exception e){
            return ResVO.error(201,e.getMessage());
        }
    }


    @Operation(summary = "加入群组")
    @PostMapping("/join")
    public ResVO joinGroup(@RequestBody GroupDTO groupDTO) {
        try{
            groupServer.joinGroup(groupDTO);
            return ResVO.success("加入群组成功");
        }catch (Exception e){
            return ResVO.error(201,e.getMessage());
        }
    }

    @Operation(summary = "退出群组")
    @PostMapping("/quit")
    public ResVO quitGroup(@RequestBody GroupDTO groupDTO) {
        try{
            groupServer.quitGroup(groupDTO);
            return ResVO.success("退出群组成功");
        }catch (Exception e){
            return ResVO.error(201,e.getMessage());
        }
    }
    
    @Operation(summary = "踢出成员")
    @PostMapping("/kick")
    public ResVO kickMember(@RequestBody GroupDTO groupDTO) {
        try{
            groupServer.kickMember(groupDTO);
            return ResVO.success("踢出成员成功");
        }catch (Exception e){
            return ResVO.error(201,e.getMessage());
        }
    }
    
    @Operation(summary = "解散群组")
    @PostMapping("/disband")
    public ResVO disbandGroup(@RequestBody GroupDTO groupDTO) {
        try{
            groupServer.disbandGroup(groupDTO);
            return ResVO.success("解散群组成功");
        }catch (Exception e){
            return ResVO.error(201,e.getMessage());
        }
    }



    @Operation(summary = "群组消息")
    @PostMapping("/message")
    public ResVO groupMessage(@RequestBody GroupDTO groupDTO) {
        try{
            groupServer.groupMessage(groupDTO);
            return ResVO.success();
        }catch (Exception e){
            return ResVO.error(201,e.getMessage());
        }
    }


    @Operation(summary = "群组列表")
    @GetMapping("/list")
    public ResVO<List<GroupDTO>> groupList(@RequestParam(required = false) String userId) {
        try{
            GroupDTO groupDTO = new GroupDTO();
            groupDTO.setUserId(userId);
            List<GroupDTO> groupList = groupServer.groupList(groupDTO);
            return ResVO.success(groupList);
        }catch (Exception e){
            return ResVO.error(201,e.getMessage());
        }
    }
    
    @Operation(summary = "搜索群组")
    @GetMapping("/search")
    public ResVO<List<GroupDTO>> searchGroups(@RequestParam String keyword, @RequestParam String userId) {
        try{
            List<GroupDTO> groups = groupServer.searchGroups(keyword, userId);
            return ResVO.success(groups);
        }catch (Exception e){
            return ResVO.error(201,e.getMessage());
        }
    }
    
    @Operation(summary = "获取群组详情")
    @GetMapping("/info/{groupId}")
    public ResVO<GroupDTO> getGroupInfo(@PathVariable String groupId) {
        try{
            GroupDTO group = groupServer.getGroupById(groupId);
            if (group != null) {
                return ResVO.success(group);
            } else {
                return ResVO.error(201, "群组不存在");
            }
        }catch (Exception e){
            return ResVO.error(201,e.getMessage());
        }
    }
    
    @Operation(summary = "获取群组消息")
    @GetMapping("/messages/{groupId}")
    public ResVO<List<GroupDTO>> getGroupMessages(@PathVariable String groupId) {
        try {
            List<GroupDTO> messages = groupServer.getGroupMessages(groupId);
            return ResVO.success(messages);
        } catch (Exception e) {
            return ResVO.error(201, e.getMessage());
        }
    }
    
    @Operation(summary = "获取群组成员列表")
    @GetMapping("/info/{groupId}/members")
    public ResVO<List<GroupDTO>> getGroupMembers(@PathVariable String groupId) {
        try {
            List<GroupDTO> members = groupServer.getGroupMembers(groupId);
            return ResVO.success(members);
        } catch (Exception e) {
            return ResVO.error(201, e.getMessage());
        }
    }


}