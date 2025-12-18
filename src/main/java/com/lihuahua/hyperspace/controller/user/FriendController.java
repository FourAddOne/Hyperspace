package com.lihuahua.hyperspace.controller.user;

import com.lihuahua.hyperspace.models.vo.ResVO;
import com.lihuahua.hyperspace.models.vo.FriendVO;
import com.lihuahua.hyperspace.models.vo.UserBasicVO;
import com.lihuahua.hyperspace.server.FriendServer;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/friends")
public class FriendController {
    
    @Resource
    private FriendServer friendServer;
    
    // 发送好友请求
    @PostMapping("/request")
    public ResVO<?> sendFriendRequest(@RequestParam String friendId, 
                                     @RequestAttribute("userId") String userId) {
        try {
            friendServer.sendFriendRequest(userId, friendId);
            return ResVO.success("好友请求已发送");
        } catch (Exception e) {
            return ResVO.fail(e.getMessage());
        }
    }
    
    // 接受好友请求
    @PostMapping("/accept")
    public ResVO<?> acceptFriendRequest(@RequestParam String requesterId,
                                       @RequestAttribute("userId") String userId) {
        try {
            friendServer.acceptFriendRequest(userId, requesterId);
            return ResVO.success("好友请求已接受");
        } catch (Exception e) {
            return ResVO.fail(e.getMessage());
        }
    }
    
    // 拒绝好友请求
    @PostMapping("/reject")
    public ResVO<?> rejectFriendRequest(@RequestParam String requesterId,
                                       @RequestAttribute("userId") String userId) {
        try {
            friendServer.rejectFriendRequest(userId, requesterId);
            return ResVO.success("好友请求已拒绝");
        } catch (Exception e) {
            return ResVO.fail(e.getMessage());
        }
    }
    
    // 屏蔽用户（拒绝并阻止其再次发送请求）
    @PostMapping("/block")
    public ResVO<?> blockFriendRequest(@RequestParam String requesterId,
                                      @RequestAttribute("userId") String userId) {
        try {
            friendServer.blockFriendRequest(userId, requesterId);
            return ResVO.success("已屏蔽该用户");
        } catch (Exception e) {
            return ResVO.fail(e.getMessage());
        }
    }
    
    // 获取好友列表
    @GetMapping("/list")
    public ResVO<List<FriendVO>> getFriendList(@RequestAttribute("userId") String userId) {
        try {
            List<FriendVO> friends = friendServer.getFriendList(userId);
            return ResVO.success(friends);
        } catch (Exception e) {
            return ResVO.fail(e.getMessage());
        }
    }
    
    // 获取好友申请列表
    @GetMapping("/requests")
    public ResVO<List<UserBasicVO>> getFriendRequests(@RequestAttribute("userId") String userId) {
        try {
            List<UserBasicVO> requests = friendServer.getFriendRequests(userId);
            return ResVO.success(requests);
        } catch (Exception e) {
            return ResVO.fail(e.getMessage());
        }
    }
    
    // 删除好友
    @DeleteMapping("/delete")
    public ResVO<?> deleteFriend(@RequestParam String friendId,
                                @RequestAttribute("userId") String userId) {
        try {
            friendServer.deleteFriend(userId, friendId);
            return ResVO.success("好友已删除");
        } catch (Exception e) {
            return ResVO.fail(e.getMessage());
        }
    }
    
    // 更新好友备注
    @PostMapping("/remark")
    public ResVO<?> updateFriendRemark(@RequestParam String friendId,
                                      @RequestParam String remark,
                                      @RequestAttribute("userId") String userId) {
        try {
            friendServer.updateFriendRemark(userId, friendId, remark);
            return ResVO.success("备注已更新");
        } catch (Exception e) {
            return ResVO.fail(e.getMessage());
        }
    }
    
    // 获取用户发出的好友申请列表
    @GetMapping("/sent-requests")
    public ResVO<List<UserBasicVO>> getSentFriendRequests(@RequestAttribute("userId") String userId) {
        try {
            List<UserBasicVO> requests = friendServer.getSentFriendRequests(userId);
            return ResVO.success(requests);
        } catch (Exception e) {
            return ResVO.fail(e.getMessage());
        }
    }
}