package com.lihuahua.hyperspace.controller.user;


import com.lihuahua.hyperspace.mapper.MessageMapper;
import com.lihuahua.hyperspace.models.dto.MessageDTO;
import com.lihuahua.hyperspace.models.vo.ResVO;
import com.lihuahua.hyperspace.server.MessageServer;
import jakarta.annotation.Resource;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.stereotype.Controller;

@Controller
public class GroupMessageController {
    //群聊消息
    @Resource
    private MessageServer messageServer;


    @Resource
    private MessageMapper messageMapper;
    @MessageMapping("/group")
    @SendTo("/topic/group/{groupId}")
    public ResVO<MessageDTO> sendGroupMessage(MessageDTO messageDTO){
        messageServer.sendGroupMessage(messageDTO);
        return ResVO.success(messageDTO);

    }
}
