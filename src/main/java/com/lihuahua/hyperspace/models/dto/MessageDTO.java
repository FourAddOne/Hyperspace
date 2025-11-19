package com.lihuahua.hyperspace.models.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * 消息传输对象
 * 用于前后端数据传输
 */
@Data
@Schema(name = "消息DTO", description = "用于消息发送和接收的数据传输对象")
public class MessageDTO {
    
    @Schema(description = "消息唯一标识")
    private String messageId;

    @Schema(description = "消息类型(TEXT, IMAGE, MIXED, SYSTEM等)")
    private String type;

    @Schema(description = "发送者用户ID")
    private String fromUserId;

    @Schema(description = "发送者用户名")
    private String fromUsername;

    @Schema(description = "接收者ID(用户ID或群组ID)")
    private String toTargetId;

    @Schema(description = "接收者类型(USER, GROUP)")
    private String toTargetType;

    @Schema(description = "接收者名称")
    private String toTargetName;

    @Schema(description = "文本内容")
    private String textContent;

    @Schema(description = "图片URL列表")
    private List<String> imageUrls;

    @Schema(description = "富文本内容（包含图片位置信息的HTML格式文本）")
    private String richContent;

    @Schema(description = "客户端时间戳")
    private Long clientTimestamp;

    @Schema(description = "消息状态(SENDING, DELIVERED, READ, FAILED)")
    private String status;

    @Schema(description = "引用消息ID")
    private String quoteMessageId;

    @Schema(description = "设备类型")
    private String deviceType;

    @Schema(description = "设备名称")
    private String deviceName;
    
    @Schema(description = "IP地址")
    @JsonProperty("Ip")
    private String ip;
}