package com.lihuahua.hyperspace.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lihuahua.hyperspace.models.entity.Message;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Options;

import java.util.Date;
import java.util.List;

@Mapper
public interface MessageMapper extends BaseMapper<Message> {
    
    @Select("SELECT * FROM message WHERE (from_user_id = #{userId} AND to_target_id = #{friendId} AND to_target_type = 'user') OR (from_user_id = #{friendId} AND to_target_id = #{userId} AND to_target_type = 'user') ORDER BY server_timestamp ASC")
    List<Message> getChatHistory(@Param("userId") String userId, @Param("friendId") String friendId);
    
    @Select("SELECT * FROM message WHERE message_id = #{messageId}")
    Message getMessageById(@Param("messageId") String messageId);
    
    /**
     * 删除指定时间之前的消息
     * @param timestamp 时间戳
     * @return 删除的消息数量
     */
    @Delete("DELETE FROM message WHERE server_timestamp < #{timestamp}")
    int deleteMessagesOlderThan(@Param("timestamp") Date timestamp);
    
    /**
     * 插入消息
     * 明确指定字段以避免列不匹配问题
     */
    @Insert("INSERT INTO message (message_id, type, from_user_id, from_username, to_target_id, to_target_type, to_target_name, text_content, image_urls, file_urls, file_name, file_size, client_timestamp, server_timestamp, status, quote_message_id, quote_message_sender_name, device_type, device_name, create_time, update_time, rich_content) VALUES (#{messageId}, #{type}, #{fromUserId}, #{fromUsername}, #{toTargetId}, #{toTargetType}, #{toTargetName}, #{textContent}, #{imageUrls}, #{fileUrls}, #{fileName}, #{fileSize}, #{clientTimestamp}, #{serverTimestamp}, #{status}, #{quoteMessageId}, #{quoteMessageSenderName}, #{deviceType}, #{deviceName}, #{createTime}, #{updateTime}, #{richContent})")
    @Options(useGeneratedKeys = false)
    int insertMessage(Message message);
    
    /**
     * 获取用户未读消息数量
     * @param userId 用户ID
     * @return 未读消息数量
     */
    @Select("SELECT COUNT(*) FROM message WHERE to_target_id = #{userId} AND status != 'read'")
    int getUnreadMessageCount(@Param("userId") String userId);
    
    /**
     * 获取与特定好友的未读消息数量
     * @param userId 用户ID
     * @param friendId 好友ID
     * @return 未读消息数量
     */
    @Select("SELECT COUNT(*) FROM message WHERE from_user_id = #{friendId} AND to_target_id = #{userId} AND status != 'read'")
    int getUnreadMessageCountFromFriend(@Param("userId") String userId, @Param("friendId") String friendId);
}