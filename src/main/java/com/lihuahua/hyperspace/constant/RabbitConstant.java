package com.lihuahua.hyperspace.constant;

/**
 * RabbitMQ常量类
 * 用于统一管理交换机、队列等名称常量
 */
public class RabbitConstant {
    // 交换机名称
    public static final String DIRECT_EXCHANGE = "hyperspace.lihuahua.direct.exchange";
    public static final String TOPIC_EXCHANGE = "hyperspace.lihuahua.topic.exchange";
    public static final String DLQ_EXCHANGE = "hyperspace.lihuahua.dlq.exchange";
    
    // 队列名称前缀
    public static final String PRIVATE_QUEUE_PREFIX = "hyperspace.lihuahua.queue.private.";
    public static final String GROUP_QUEUE_PREFIX = "hyperspace.lihuahua.queue.group.";
    public static final String DLQ_QUEUE = "hyperspace.lihuahua.queue.dlq";
    
    // 群聊路由键前缀
    public static final String GROUP_ROUTING_KEY_PREFIX = "group.";
}