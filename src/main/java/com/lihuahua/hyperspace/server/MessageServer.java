package com.lihuahua.hyperspace.server;

import com.lihuahua.hyperspace.models.dto.MessageDTO;

import java.util.List;
import java.util.Map;

public interface MessageServer {

    boolean sendGroupMessage(MessageDTO messageDTO);
    
    // 发送消息
    boolean sendMessage(MessageDTO messageDTO);
    
    // 获取聊天历史记录
    List<MessageDTO> getChatHistory(String userId, String friendId);
    
    // 获取未读消息数量
    Map<String, Integer> getUnreadMessageCounts(String userId, List<String> friendIds);
    
    // 标记消息为已读
    void markMessagesAsRead(String userId, String friendId);
}