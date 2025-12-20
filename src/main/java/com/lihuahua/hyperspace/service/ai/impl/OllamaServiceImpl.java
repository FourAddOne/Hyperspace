package com.lihuahua.hyperspace.service.ai.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lihuahua.hyperspace.service.ai.AiChatService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.HashMap;
import java.util.Map;

@Service("ollamaService")
public class OllamaServiceImpl implements AiChatService {
    private static final Logger logger = LoggerFactory.getLogger(OllamaServiceImpl.class);
    private static final String OLLAMA_API_URL = "http://localhost:11434/api/generate";
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public OllamaServiceImpl() {
        this.httpClient = HttpClient.newHttpClient();
        this.objectMapper = new ObjectMapper();
    }

    @Override
    public String sendMessage(String message, String model) {
        try {
            // 构建请求体
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", model);
            requestBody.put("prompt", message);
            requestBody.put("stream", false);

            // 发送请求
            String requestJson = objectMapper.writeValueAsString(requestBody);
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(OLLAMA_API_URL))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(requestJson))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            // 解析响应
            if (response.statusCode() == 200) {
                JsonNode responseJson = objectMapper.readTree(response.body());
                return responseJson.get("response").asText();
            } else {
                logger.error("Ollama API error: {}", response.body());
                return "AI助手暂时不可用，请稍后重试。";
            }
        } catch (Exception e) {
            logger.error("Failed to call Ollama API", e);
            return "AI助手暂时不可用，请稍后重试。";
        }
    }
}
