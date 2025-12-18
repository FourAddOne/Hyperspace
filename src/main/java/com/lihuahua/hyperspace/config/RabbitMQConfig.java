package com.lihuahua.hyperspace.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * RabbitMQ 配置类
 */
@Configuration
public class RabbitMQConfig {
    
    // 帖子点赞队列
    @Bean
    public Queue postLikeQueue() {
        return new Queue("post.like.queue", true); // durable=true 持久化队列
    }
    
    // 帖子点赞交换机
    @Bean
    public DirectExchange postLikeExchange() {
        return new DirectExchange("post.like.exchange", true, false); // durable=true, autoDelete=false
    }
    
    // 绑定帖子点赞队列和交换机
    @Bean
    public Binding bindingPostLikeQueue(Queue postLikeQueue, DirectExchange postLikeExchange) {
        return BindingBuilder.bind(postLikeQueue).to(postLikeExchange).with("post.like.routing.key");
    }
    
    // 评论点赞队列
    @Bean
    public Queue commentLikeQueue() {
        return new Queue("comment.like.queue", true); // durable=true 持久化队列
    }
    
    // 评论点赞交换机
    @Bean
    public DirectExchange commentLikeExchange() {
        return new DirectExchange("comment.like.exchange", true, false); // durable=true, autoDelete=false
    }
    
    // 绑定评论点赞队列和交换机
    @Bean
    public Binding bindingCommentLikeQueue(Queue commentLikeQueue, DirectExchange commentLikeExchange) {
        return BindingBuilder.bind(commentLikeQueue).to(commentLikeExchange).with("comment.like.routing.key");
    }
}