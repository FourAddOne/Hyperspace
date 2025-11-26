package com.lihuahua.hyperspace.controller.user;

import com.lihuahua.hyperspace.constant.MessageConstant;
import com.lihuahua.hyperspace.models.dto.MessageDTO;
import com.lihuahua.hyperspace.models.vo.ResVO;
import com.lihuahua.hyperspace.server.MessageServer;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/messages")
public class MessageController {
    
    @Resource
    private MessageServer messageServer;
    
    // 发送消息
    @PostMapping("/send")
    public ResVO<?> sendMessage(@RequestBody MessageDTO messageDTO) {
        //私聊
        if ("user".equals(messageDTO.getToTargetType())){
            try {
                messageServer.sendMessage(messageDTO);
                return ResVO.success("消息发送成功");
            } catch (Exception e) {
                return ResVO.fail(e.getMessage());
            }
        }
        //群聊
        else if ("group".equals(messageDTO.getToTargetType())){
            try {
                messageServer.sendGroupMessage(messageDTO);
                return ResVO.success("消息发送成功");
            } catch (Exception e) {
                return ResVO.fail(e.getMessage());
            }
        }  else {
            return ResVO.fail("消息发送失败");
        }
    }
    
    // 获取聊天历史记录
    @GetMapping("/history")
    public ResVO<List<MessageDTO>> getChatHistory(@RequestParam String friendId,
                                                 @RequestAttribute("userId") String userId) {
        try {
            List<MessageDTO> messages = messageServer.getChatHistory(userId, friendId);
            return ResVO.success(messages);
        } catch (Exception e) {
            return ResVO.fail(e.getMessage());
        }
    }
    
    // 获取未读消息数量
    @GetMapping("/unread-counts")
    public ResVO<Map<String, Integer>> getUnreadMessageCounts(@RequestParam List<String> friendIds,
                                                              @RequestAttribute("userId") String userId) {
        try {
            Map<String, Integer> unreadCounts = messageServer.getUnreadMessageCounts(userId, friendIds);
            return ResVO.success(unreadCounts);
        } catch (Exception e) {
            return ResVO.fail(e.getMessage());
        }
    }

    // 标记消息为已读
    @PostMapping("/mark-as-read")
    public ResVO<?> markMessagesAsRead(@RequestParam String friendId,
                                   @RequestAttribute("userId") String userId) {
        try {
            messageServer.markMessagesAsRead(userId, friendId);
            return ResVO.success("消息已标记为已读");
        } catch (Exception e) {
            return ResVO.fail(e.getMessage());
        }
    }
}