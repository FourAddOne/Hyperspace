package com.lihuahua.hyperspace.models.dto;

import lombok.Data;

@Data
public class ChatBotRequestDTO {
    private String userId;
    private String message;
    private String model = "deepseek-r1:7b";
}