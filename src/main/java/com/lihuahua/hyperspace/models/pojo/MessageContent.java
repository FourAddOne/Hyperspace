package com.lihuahua.hyperspace.models.pojo;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 消息内容类
 * 根据消息类型不同，包含不同的内容字段
 */
@Data
public class MessageContent implements Serializable {
    /**
     * 文本内容
     */
    private String text;
    
    /**
     * 图片URL列表
     */
    private List<String> imageUrls;
    
    /**
     * 音频URL
     */
    private String audioUrl;
    
    /**
     * 视频URL
     */
    private String videoUrl;
    
    /**
     * 文件URL
     */
    private String fileUrl;
    
    /**
     * 文件名称
     */
    private String fileName;
    
    /**
     * 富文本内容（包含图片位置信息的HTML格式文本）
     */
    private String richText;
    
    /**
     * 创建文本消息内容
     * @param text 文本内容
     * @return MessageContent对象
     */
    public static MessageContent textContent(String text) {
        MessageContent content = new MessageContent();
        content.text = text;
        return content;
    }
    
    /**
     * 创建图片消息内容
     * @param imageUrls 图片URL列表
     * @return MessageContent对象
     */
    public static MessageContent imageContent(List<String> imageUrls) {
        MessageContent content = new MessageContent();
        content.imageUrls = imageUrls;
        return content;
    }
    
    /**
     * 创建混合消息内容
     * @param text 文本内容
     * @param imageUrls 图片URL列表
     * @return MessageContent对象
     */
    public static MessageContent mixedContent(String text, List<String> imageUrls) {
        MessageContent content = new MessageContent();
        content.text = text;
        content.imageUrls = imageUrls;
        content.richText = buildRichText(text, imageUrls);
        return content;
    }
    
    /**
     * 创建富文本消息内容
     * @param richText 包含图片位置信息的HTML格式文本
     * @return MessageContent对象
     */
    public static MessageContent richContent(String richText) {
        MessageContent content = new MessageContent();
        content.richText = richText;
        return content;
    }
    
    /**
     * 构建富文本内容
     * @param text 文本内容
     * @param imageUrls 图片URL列表
     * @return HTML格式的富文本
     */
    private static String buildRichText(String text, List<String> imageUrls) {
        if (text == null || text.isEmpty()) {
            StringBuilder richText = new StringBuilder();
            if (imageUrls != null) {
                for (String imageUrl : imageUrls) {
                    richText.append("<img src='").append(imageUrl).append("'/>");
                }
            }
            return richText.toString();
        }
        
        // 简单实现：将图片插入到文本末尾
        // 实际应用中可以根据需要实现更复杂的插入逻辑
        StringBuilder richText = new StringBuilder(text);
        if (imageUrls != null) {
            for (String imageUrl : imageUrls) {
                richText.append("<img src='").append(imageUrl).append("'/>");
            }
        }
        return richText.toString();
    }
}