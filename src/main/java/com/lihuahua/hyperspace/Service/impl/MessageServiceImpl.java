package com.lihuahua.hyperspace.Service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.lihuahua.hyperspace.Service.UserService;
import com.lihuahua.hyperspace.mapper.GroupMapper;
import com.lihuahua.hyperspace.mapper.GroupMemberMapper;
import com.lihuahua.hyperspace.mapper.MessageMapper;
import com.lihuahua.hyperspace.mapper.UserMapper;
import com.lihuahua.hyperspace.models.bo.MessageBO;
import com.lihuahua.hyperspace.Service.MessageService;
import com.lihuahua.hyperspace.models.entity.Group;
import com.lihuahua.hyperspace.models.entity.Message;
import com.lihuahua.hyperspace.models.enums.TargetType;
import com.lihuahua.hyperspace.models.po.GroupMemberPO;
import com.lihuahua.hyperspace.models.po.GroupPO;
import com.lihuahua.hyperspace.rabbitmq.manager.RabbitQueueManager;
import com.lihuahua.hyperspace.rabbitmq.producer.MessageProducer;
import com.lihuahua.hyperspace.rabbitmq.producer.dto.ChatMessageDTO;
import com.lihuahua.hyperspace.utils.MessageConvertUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.Objects;


@Service
public class MessageServiceImpl implements MessageService {


    @Autowired
    private MessageMapper messageMapper;

    @Autowired
    private UserService userService;

    @Autowired
    private MessageProducer messageProducer;
    
    @Autowired
    private RabbitQueueManager rabbitQueueManager;
    
    @Autowired
    private UserMapper userMapper;

    @Autowired
    private GroupMapper groupMapper;
    @Autowired
    private GroupMemberMapper groupMemberMapper;


    @Override
    public void sendPrivateMessage(MessageBO message) {
        // 检查消息发送者是否为空
        if (message.getFrom() == null || message.getFrom().getUserId() == null) {
            throw new IllegalArgumentException("消息发送者信息不能为空");
        }

        Message messageEntity = MessageConvertUtil.toEntity(message);
        ChatMessageDTO chatMessageDTO = MessageConvertUtil.toChatMessageDTO(messageEntity);
        String userId= chatMessageDTO.getToTargetId();
        
        // 确保目标用户的队列已创建
        rabbitQueueManager.createPrivateQueue(userId);
        
        messageProducer.sendPrivateMessage(userId, chatMessageDTO);
        userMapper.updateLastMessageTime(userId, new Date());
        messageMapper.insert(MessageConvertUtil.toPO(messageEntity));
    }

    @Override
    public void sendGroupMessage(MessageBO message) {

        //群状态判断 是否禁言,是否被封禁
    // TODO: 实现群组状态检查逻辑
    // 1. 检查群组是否被禁言
    // 2. 检查群组是否被封禁
    // 3. 检查发送者在群组中是否有发言权限
        if(message.getTo().getTargetType()!= TargetType.GROUP){
            throw new IllegalArgumentException("错误目标");
        }
        QueryWrapper<GroupPO> groupQueryWrapper = new QueryWrapper<>();
        groupQueryWrapper.eq("group_id", message.getTo().getTargetId());
        GroupPO groupPO = groupMapper.selectOne(groupQueryWrapper);
        if(Objects.equals(groupPO.getStatus(), "DISABLED")){
            throw new IllegalArgumentException("群组被封禁");
        }else if(Objects.equals(groupPO.getStatus(), "MUTE_ALL")){
            throw new IllegalArgumentException("群组被禁言");
        }
        QueryWrapper<GroupMemberPO> groupMemberQueryWrapper = new QueryWrapper<>();
        groupMemberQueryWrapper.eq("group_id", message.getTo().getTargetId());
        groupMemberQueryWrapper.eq("user_id", message.getFrom().getUserId());
        GroupMemberPO groupMemberPO = groupMemberMapper.selectOne(groupMemberQueryWrapper);
        if(Objects.equals(groupMemberPO.getStatus(), "MUTE")){
            throw new IllegalArgumentException("用户被禁言");
        }

        Message messageEntity = MessageConvertUtil.toEntity(message);
        ChatMessageDTO chatMessageDTO = MessageConvertUtil.toChatMessageDTO(messageEntity);
        String groupId= chatMessageDTO.getToTargetId();
        // 确保目标用户的队列已创建
        rabbitQueueManager.createGroupQueue(groupId);
        messageProducer.sendGroupMessage(groupId, chatMessageDTO);
        messageMapper.insert(MessageConvertUtil.toPO(messageEntity));



    }
}