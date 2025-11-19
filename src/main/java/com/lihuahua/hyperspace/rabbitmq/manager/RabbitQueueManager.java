package com.lihuahua.hyperspace.rabbitmq.manager;

import com.lihuahua.hyperspace.config.RabbitConfig;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import static com.lihuahua.hyperspace.constant.RabbitConstant.*;

/**
 * RabbitMQ队列管理工具类
 * 用于动态创建和管理用户私聊队列和群聊队列
 */
@Service
public class RabbitQueueManager {
    
    @Autowired
    private RabbitAdmin amqpAdmin;
    
    @Autowired
    private DirectExchange directExchange;
    
    @Autowired
    private TopicExchange topicExchange;

    /**
     * 创建用户私聊队列并绑定到Direct交换机
     * @param userId 用户ID
     */
    public void createPrivateQueue(String userId) {
        // 创建队列
        Queue queue = QueueBuilder.durable(RabbitConfig.getPrivateQueueName(userId))
                .withArgument("x-dead-letter-exchange", DLQ_EXCHANGE)
                .build();
        amqpAdmin.declareQueue(queue);
        
        // 创建绑定关系
        Binding binding = BindingBuilder.bind(queue).to(directExchange).with(userId);
        amqpAdmin.declareBinding(binding);
    }

    /**
     * 创建群聊队列并绑定到Topic交换机
     * @param groupId 群组ID
     */
    public void createGroupQueue(String groupId) {
        // 创建队列
        Queue queue = QueueBuilder.durable(GROUP_QUEUE_PREFIX + groupId)
                .withArgument("x-dead-letter-exchange", DLQ_EXCHANGE)
                .build();
        amqpAdmin.declareQueue(queue);
        
        // 创建绑定关系
        Binding binding = BindingBuilder.bind(queue).to(topicExchange).with(GROUP_ROUTING_KEY_PREFIX + groupId);
        amqpAdmin.declareBinding(binding);
    }
    
    /**
     * 删除用户私聊队列及其绑定关系
     * @param userId 用户ID
     */
    public void removePrivateQueue(String userId) {
        String queueName = RabbitConfig.getPrivateQueueName(userId);
        amqpAdmin.deleteQueue(queueName);
    }
    
    /**
     * 删除群聊队列及其绑定关系
     * @param groupId 群组ID
     */
    public void removeGroupQueue(String groupId) {
        String queueName = GROUP_QUEUE_PREFIX + groupId;
        amqpAdmin.deleteQueue(queueName);
    }
}