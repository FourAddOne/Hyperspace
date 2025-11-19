package com.lihuahua.hyperspace.models.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * 发送消息传输对象
 * 用于发送消息时的数据传输
 */
@Data
@Schema(name = "发送消息DTO", description = "用于发送消息的数据传输对象")
public class SendMessageDTO {
    
    @Schema(description = "接收者ID(用户ID或群组ID)")
    private String toTargetId;

    @Schema(description = "接收者类型(USER, GROUP)")
    private String toTargetType;

    @Schema(description = "消息类型(TEXT, IMAGE, MIXED, SYSTEM等)")
    private String type;

    @Schema(description = "文本内容")
    private String textContent;

    @Schema(description = "图片URL列表")
    private List<String> imageUrls;

    @Schema(description = "富文本内容（包含图片位置信息的HTML格式文本）")
    private String richContent;

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