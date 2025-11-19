package com.lihuahua.hyperspace.rabbitmq.producer;


import com.lihuahua.hyperspace.config.RabbitConfig;
import com.lihuahua.hyperspace.constant.RabbitConstant;
import com.lihuahua.hyperspace.mapper.GroupMapper;
import com.lihuahua.hyperspace.mapper.GroupMemberMapper;
import com.lihuahua.hyperspace.models.entity.GroupMember;
import com.lihuahua.hyperspace.models.entity.Message;
import com.lihuahua.hyperspace.models.po.GroupMemberPO;
import com.lihuahua.hyperspace.rabbitmq.producer.dto.ChatMessageDTO;
import com.lihuahua.hyperspace.utils.MessageConvertUtil;
import org.slf4j.Logger;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

@Service
public class MessageProducer {

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired
    private GroupMemberMapper groupMemberMapper;
    @Autowired
    private GroupMapper groupMapper;

    private final Logger logger = org.slf4j.LoggerFactory.getLogger(MessageProducer.class);

    /**
     * 发送私聊消息的方法
     * @param routingKey 路由键，用于确定消息的路由规则，这里应该是目标用户的userId
     * @param message 要发送的消息内容，可以是任意类型的对象
     */
    public void sendPrivateMessage(String routingKey, ChatMessageDTO message) {
        // 确保使用正确的队列名称
        String queueName = RabbitConfig.getPrivateQueueName(routingKey);
        rabbitTemplate.convertAndSend(
                RabbitConstant.DIRECT_EXCHANGE,  // Direct交换机
                routingKey,                      // 路由键（目标用户ID）
                message                          // 消息内容
        );
        logger.info("发送私聊消息到队列 {}: {}", queueName, message);
    }
    
    /**
     * 发送群聊消息的方法
     * @param groupId 群组ID
     * @param message 要发送的消息内容
     */
    public void sendGroupMessage(String groupId, ChatMessageDTO message) {
        rabbitTemplate.convertAndSend(
                RabbitConstant.TOPIC_EXCHANGE,                    // Topic交换机
                RabbitConstant.GROUP_ROUTING_KEY_PREFIX + groupId, // 路由键（group.{groupId}格式）
                message                                           // 消息内容
        );
    }
    
    /**
     * 发送私聊消息的方法（直接传入Message对象）
     * @param routingKey 路由键，用于确定消息的路由规则，这里应该是目标用户的userId
     * @param message 要发送的消息实体
     */
    public void sendPrivateMessage(String routingKey, Message message) {
        ChatMessageDTO chatMessageDTO = MessageConvertUtil.toChatMessageDTO(message);
        sendPrivateMessage(routingKey, chatMessageDTO);
        logger.info("发送私聊消息：{}", chatMessageDTO);

    }
    
    /**
     * 发送群聊消息的方法（直接传入Message对象）
     * @param groupId 群组ID
     * @param message 要发送的消息实体
     */
    public void sendGroupMessage(String groupId, Message message) {
        // 先获取群聊用户列表
        java.util.List<String> userList = getGroupUserList(groupId);
        
        if (userList != null && !userList.isEmpty()) {
            // 只转换一次消息对象，避免重复转换
            ChatMessageDTO chatMessageDTO = MessageConvertUtil.toChatMessageDTO(message);
            
            // 使用并行处理提高性能
            userList.parallelStream().forEach(userId -> {
                // 不向发送者自己发送消息
                if (!userId.equals(message.getFrom().getUserId())) {
                    try {
                        String userQueueName = RabbitConfig.getPrivateQueueName(userId);
                        rabbitTemplate.convertAndSend(
                                RabbitConstant.DIRECT_EXCHANGE,
                                userId,
                                chatMessageDTO
                        );
                        logger.debug("已发送群聊消息到用户 {} 的私信队列: {}", userId, userQueueName);
                    } catch (Exception e) {
                        logger.error("发送群聊消息到用户 {} 失败", userId, e);
                    }
                }
            });
            
            logger.info("群聊消息已发送给 {} 个用户", userList.size() - 1);
        }
    }
    
    /**
     * 获取群组用户列表（先查Redis，没有再查数据库）
     * @param groupId 群组ID
     * @return 用户ID列表
     */
    private java.util.List<String> getGroupUserList(String groupId) {
        String groupUserListKey = "group_users:" + groupId;
        Boolean hasKey = redisTemplate.hasKey(groupUserListKey);
        java.util.List<String> userList = null;
        
        if (hasKey != null && hasKey) {
            // 如果redis中有群聊用户名单，则直接使用
            Object userListObj = redisTemplate.opsForValue().get(groupUserListKey);
            if (userListObj instanceof java.util.List) {
                userList = (java.util.List<String>) userListObj;
            }
        } else {
            // 如果Redis中没有用户名单，则查询数据库获取群成员列表
            logger.info("Redis中未找到群聊 {} 的用户名单，尝试从数据库查询", groupId);
            
            // 从数据库查询群成员
            com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<GroupMemberPO> queryWrapper =
                new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<>();
            queryWrapper.eq("group_id", groupId);
            java.util.List<GroupMemberPO> groupMembers = groupMemberMapper.selectList(queryWrapper);
            
            if (groupMembers != null && !groupMembers.isEmpty()) {
                // 提取用户ID列表
                userList = groupMembers.stream()
                        .map(GroupMemberPO::getUserId)
                        .collect(java.util.stream.Collectors.toList());
                
                // 将用户列表存入Redis，设置过期时间（例如30分钟）
                redisTemplate.opsForValue().set(groupUserListKey, userList, java.time.Duration.ofMinutes(30));
            } else {
                // 数据库中也没有找到群成员
                logger.warn("数据库中未找到群聊 {} 的成员列表", groupId);
                userList = null;
            }
        }
        
        return userList;
    }

    /**
     * 异步发送群聊消息的方法（直接传入Message对象）
     * @param groupId 群组ID
     * @param message 要发送的消息实体
     */
    public CompletableFuture<Void> sendGroupMessageAsync(String groupId, Message message) {
        return CompletableFuture.runAsync(() -> sendGroupMessage(groupId, message));
    }
}