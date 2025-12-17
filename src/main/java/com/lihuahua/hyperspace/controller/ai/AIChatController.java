package com.lihuahua.hyperspace.controller.ai;

import com.lihuahua.hyperspace.Result.Result;
import com.lihuahua.hyperspace.models.dto.ChatBotRequestDTO;
import com.lihuahua.hyperspace.service.ai.AiChatManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping({"/api/chatbot", "/chatbot"})
public class AIChatController {

    private final AiChatManager aiChatManager;

    @Autowired
    public AIChatController(AiChatManager aiChatManager) {
        this.aiChatManager = aiChatManager;
    }

    @PostMapping("/send")
    public Result<String> sendMessage(@RequestBody ChatBotRequestDTO request) {
        try {
            // 调用AI聊天管理器处理消息
            String response = aiChatManager.sendMessage(request.getMessage(), request.getModel());
            return Result.success(response);
        } catch (Exception e) {
            return Result.error("AI服务错误：" + e.getMessage());
        }
    }
}
