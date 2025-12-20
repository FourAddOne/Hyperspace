package com.lihuahua.hyperspace.config;

import org.springframework.amqp.core.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQChatConfig {
    
    // 群聊消息交换机
    public static final String CHAT_GROUP_EXCHANGE = "chat.group.exchange";
    
    // 群聊消息队列
    public static final String CHAT_GROUP_QUEUE = "chat.group.queue";
    
    // 群聊消息路由键
    public static final String CHAT_GROUP_ROUTING_KEY = "chat.group.routing.key";
    
    // 声明交换机
    @Bean
    public DirectExchange chatGroupExchange() {
        return new DirectExchange(CHAT_GROUP_EXCHANGE);
    }
    
    // 声明队列
    @Bean
    public Queue chatGroupQueue() {
        return new Queue(CHAT_GROUP_QUEUE, true); // durable=true 持久化队列
    }
    
    // 绑定队列到交换机
    @Bean
    public Binding bindingChatGroup(Queue chatGroupQueue, DirectExchange chatGroupExchange) {
        return BindingBuilder.bind(chatGroupQueue).to(chatGroupExchange).with(CHAT_GROUP_ROUTING_KEY);
    }
}