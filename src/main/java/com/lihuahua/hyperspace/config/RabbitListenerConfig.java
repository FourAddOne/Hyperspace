package com.lihuahua.hyperspace.config;

import com.lihuahua.hyperspace.constant.RabbitConstant;
import org.springframework.amqp.core.Queue;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static com.lihuahua.hyperspace.constant.RabbitConstant.PRIVATE_QUEUE_PREFIX;

/**
 * RabbitMQ监听器配置类
 * 用于动态创建和管理队列监听器
 */
@Configuration
public class RabbitListenerConfig {
    
    /**
     * 创建示例私聊队列（用于测试）
     * @return 配置好的Queue实例
     */
    @Bean
    public Queue examplePrivateQueue() {
        // 创建一个示例队列，实际应用中应该根据用户ID动态创建
        return new Queue(PRIVATE_QUEUE_PREFIX + "example", true);
    }
    
    /**
     * 获取私聊队列名称
     * 注意：在实际应用中，应该根据当前用户ID动态返回队列名称
     * @return 队列名称
     */
    @Bean
    public String privateQueueName() {
        // 返回示例队列名称
        return PRIVATE_QUEUE_PREFIX + "example";
    }
}