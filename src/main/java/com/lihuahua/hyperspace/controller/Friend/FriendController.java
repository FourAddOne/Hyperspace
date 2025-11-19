package com.lihuahua.hyperspace.controller.Friend;


import com.lihuahua.hyperspace.Service.FriendService;
import com.lihuahua.hyperspace.models.dto.FriendDTO;
import com.lihuahua.hyperspace.models.vo.ResVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/friend")
public class FriendController {

    @Autowired
    private FriendService friendService;

    @RequestMapping(value = "/add", method = RequestMethod.POST)
    public ResVO addFriend(@RequestBody FriendDTO friendDTO) {
        return ResVO.success(friendService.addFriend(friendDTO));
    }

    @RequestMapping(value = "/get", method = RequestMethod.GET)
    public ResVO getFriendList(String userId) {
        return ResVO.success(friendService.getFriendList(userId));
    }

    @RequestMapping(value = "/search", method = RequestMethod.GET)
    public ResVO searchFriends(String userId, String keyword) {
        return ResVO.success(friendService.searchFriends(userId, keyword));
    }

    @RequestMapping(value = "/delete", method = RequestMethod.POST)
    public ResVO deleteFriend(String userId, String friendId) {
        return ResVO.success(friendService.deleteFriend(userId, friendId));
    }
    
    @RequestMapping(value = "/updateRemark", method = RequestMethod.PUT)
    public ResVO updateFriendRemark(@RequestBody FriendDTO friendDTO) {
        return ResVO.success(friendService.updateFriendRemark(friendDTO));
    }
    
    @RequestMapping(value = "/block", method = RequestMethod.PUT)
    public ResVO blockFriend(String userId, String friendId) {
        return ResVO.success(friendService.blockFriend(userId, friendId));
    }
    
    @RequestMapping(value = "/unblock", method = RequestMethod.PUT)
    public ResVO unblockFriend(String userId, String friendId) {
        return ResVO.success(friendService.unblockFriend(userId, friendId));
    }
    
    @RequestMapping(value = "/isBlocked", method = RequestMethod.GET)
    public ResVO isBlocked(String userId, String friendId) {
        return ResVO.success(friendService.isBlocked(userId, friendId));
    }
    
    @RequestMapping(value = "/getFriendById", method = RequestMethod.GET)
    public ResVO getFriendById(String userOne, String userSec) {
        return ResVO.success(friendService.getFriendById(userOne, userSec));
    }
    
    @RequestMapping(value = "/updateStatus", method = RequestMethod.POST)
    public ResVO updateFriendStatus(@RequestBody FriendDTO friendDTO) {
        return ResVO.success(friendService.updateFriendStatus(friendDTO));
    }
    
    @RequestMapping(value = "/getRequests", method = RequestMethod.GET)
    public ResVO getFriendRequests(String userId) {
        return ResVO.success(friendService.getFriendRequests(userId));
    }
    
    @RequestMapping(value = "/handleRequest", method = RequestMethod.POST)
    public ResVO handleFriendRequest(@RequestBody FriendDTO friendDTO) {
        return ResVO.success(friendService.handleFriendRequest(friendDTO));
    }
}