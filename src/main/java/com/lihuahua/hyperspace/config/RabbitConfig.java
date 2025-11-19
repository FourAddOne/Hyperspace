package com.lihuahua.hyperspace.config;


import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static com.lihuahua.hyperspace.constant.RabbitConstant.*;

/**
 * RabbitMQ配置类
 * 用于配置RabbitMQ连接、模板、交换机和队列等组件
 */
@Configuration
public class RabbitConfig {


    // 从配置文件中注入RabbitMQ主机地址
    @Value("${spring.rabbitmq.host}")
    private String rabbitmqHost;
    // 从配置文件中注入RabbitMQ端口号
    @Value("${spring.rabbitmq.port}")
    private int rabbitmqPort;
    // 从配置文件中注入RabbitMQ用户名
    @Value("${spring.rabbitmq.username}")
    private String rabbitmqUsername;
    // 从配置文件中注入RabbitMQ密码
    @Value("${spring.rabbitmq.password}")
    private String rabbitmqPassword;

    /**
     * 创建RabbitMQ连接工厂
     * @return 配置好的连接工厂实例
     */
    @Bean
    public ConnectionFactory rabbitConnectionFactory() {
        CachingConnectionFactory connectionFactory = new CachingConnectionFactory();
        connectionFactory.setHost(rabbitmqHost);
        connectionFactory.setPort(rabbitmqPort);
        connectionFactory.setUsername(rabbitmqUsername);
        connectionFactory.setPassword(rabbitmqPassword);
        return connectionFactory;
    }

    /**
     * 创建RabbitAdmin用于管理RabbitMQ组件
     * @param connectionFactory 连接工厂
     * @return 配置好的RabbitAdmin实例
     */
    @Bean
    public RabbitAdmin rabbitAdmin(ConnectionFactory connectionFactory) {
        return new RabbitAdmin(connectionFactory);
    }

    /**
     * 创建RabbitMQ模板
     * @param connectionFactory 连接工厂
     * @return 配置好的RabbitTemplate实例
     */
    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
        rabbitTemplate.setMessageConverter(new Jackson2JsonMessageConverter());
        return rabbitTemplate;
    }

    /**
     * 创建Direct类型的交换机，用于点对点消息传递
     * @return 配置好的DirectExchange实例
     */
    @Bean
    public DirectExchange directExchange() {
        return new DirectExchange(DIRECT_EXCHANGE, true, false);
    }
    
    /**
     * 创建Topic类型的交换机，用于群聊消息广播
     * @return 配置好的TopicExchange实例
     */
    @Bean
    public TopicExchange topicExchange() {
        return new TopicExchange(TOPIC_EXCHANGE, true, false);
    }
    
    /**
     * 创建死信交换机
     * @return 配置好的DirectExchange实例
     */
    @Bean
    public DirectExchange dlqExchange() {
        return new DirectExchange(DLQ_EXCHANGE, true, false);
    }

    /**
     * 创建死信队列
     * @return 配置好的Queue实例
     */
    @Bean
    public Queue dlqQueue() {
        return QueueBuilder.durable(DLQ_QUEUE).build();
    }

    /**
     * 创建死信队列与死信交换机的绑定关系
     * @param dlqExchange 死信交换机
     * @param dlqQueue 死信队列
     * @return 配置好的Binding实例
     */
    @Bean
    public Binding dlqBinding(DirectExchange dlqExchange, Queue dlqQueue) {
        return BindingBuilder.bind(dlqQueue).to(dlqExchange).with("#");
    }

    /**
     * 创建用户私聊队列的声明方法
     * @param userId 用户ID
     * @return 配置好的Queue实例
     */
    public Queue privateQueue(String userId) {
        return QueueBuilder.durable(PRIVATE_QUEUE_PREFIX + userId)
                .withArgument("x-dead-letter-exchange", DLQ_EXCHANGE)
                .build();
    }
    
    /**
     * 创建群聊队列的声明方法
     * @param groupId 群组ID
     * @return 配置好的Queue实例
     */
    public Queue groupQueue(String groupId) {
        return QueueBuilder.durable(GROUP_QUEUE_PREFIX + groupId)
                .withArgument("x-dead-letter-exchange", DLQ_EXCHANGE)
                .build();
    }
    
    /**
     * 创建私聊队列与交换机的绑定关系
     * @param userId 用户ID
     * @param directExchange Direct交换机
     * @return 配置好的Binding实例
     */
    public Binding privateBinding(String userId, DirectExchange directExchange) {
        return BindingBuilder.bind(privateQueue(userId)).to(directExchange).with(userId);
    }
    
    /**
     * 创建群聊队列与交换机的绑定关系
     * @param groupId 群组ID
     * @param topicExchange Topic交换机
     * @return 配置好的Binding实例
     */
    public Binding groupBinding(String groupId, TopicExchange topicExchange) {
        return BindingBuilder.bind(groupQueue(groupId)).to(topicExchange).with(GROUP_ROUTING_KEY_PREFIX + groupId);
    }
    
    /**
     * 获取私聊队列名称
     * @param userId 用户ID
     * @return 队列名称
     */
    public static String getPrivateQueueName(String userId) {
        return PRIVATE_QUEUE_PREFIX + userId;
    }
}