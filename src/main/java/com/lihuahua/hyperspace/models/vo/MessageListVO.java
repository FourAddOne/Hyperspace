package com.lihuahua.hyperspace.models.vo;

import lombok.Data;

import java.util.List;

/**
 * 消息列表视图对象
 * 用于向前端返回消息列表数据
 */
@Data
public class MessageListVO {
    
    /**
     * 消息列表
     */
    private List<MessageVO> messages;

    /**
     * 是否还有更多消息
     */
    private Boolean hasMore;

    /**
     * 下一页的游标/时间戳
     */
    private Long nextCursor;
}