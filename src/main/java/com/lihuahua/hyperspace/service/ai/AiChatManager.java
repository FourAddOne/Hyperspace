package com.lihuahua.hyperspace.service.ai;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
public class AiChatManager {

    private final Map<String, AiChatService> serviceMap;
    private final AiChatService ollamaService;

    @Autowired
    public AiChatManager(@Qualifier("ollamaService") AiChatService ollamaService) {
        this.ollamaService = ollamaService;
        this.serviceMap = new HashMap<>();
        
        // 初始化服务映射
        // 将所有模型映射到Ollama服务，因为Ollama可以管理多种模型
        this.serviceMap.put("deepseek-r1:8b", ollamaService);
        this.serviceMap.put("llama3.2", ollamaService);
        this.serviceMap.put("gemma2:9b", ollamaService);
        this.serviceMap.put("mistral:7b", ollamaService);
    }

    /**
     * 根据模型名称获取对应的AI聊天服务
     */
    public AiChatService getServiceByModel(String model) {
        // 如果找不到对应的服务，返回默认的Ollama服务
        return serviceMap.getOrDefault(model, ollamaService);
    }

    /**
     * 发送消息到指定模型
     */
    public String sendMessage(String message, String model) {
        AiChatService service = getServiceByModel(model);
        return service.sendMessage(message, model);
    }
}
