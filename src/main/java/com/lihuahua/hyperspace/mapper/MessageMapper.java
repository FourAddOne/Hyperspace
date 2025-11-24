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
    
    @Select("SELECT * FROM messages WHERE (sender_id = #{userId} AND receiver_id = #{friendId}) OR (sender_id = #{friendId} AND receiver_id = #{userId}) ORDER BY timestamp ASC")
    List<Message> getChatHistory(@Param("userId") String userId, @Param("friendId") String friendId);
    
    /**
     * 删除指定时间之前的消息
     * @param timestamp 时间戳
     * @return 删除的消息数量
     */
    @Delete("DELETE FROM messages WHERE timestamp < #{timestamp}")
    int deleteMessagesOlderThan(@Param("timestamp") Date timestamp);
    
    /**
     * 插入消息
     * 明确指定字段以避免列不匹配问题
     */
    @Insert("INSERT INTO messages (message_id, conversation_id, sender_id, receiver_id, content_type, content, file_url, status, timestamp) VALUES (#{messageId}, #{conversationId}, #{senderId}, #{receiverId}, #{contentType}, #{content}, #{fileUrl}, #{status}, #{timestamp})")
    @Options(useGeneratedKeys = false)
    int insertMessage(Message message);
    
    /**
     * 获取用户未读消息数量
     * @param userId 用户ID
     * @return 未读消息数量
     */
    @Select("SELECT COUNT(*) FROM messages m " +
           "LEFT JOIN message_status ms ON m.message_id = ms.message_id AND ms.user_id = #{userId} " +
           "WHERE m.receiver_id = #{userId} AND (ms.status IS NULL OR ms.status != 'read')")
    int getUnreadMessageCount(@Param("userId") String userId);
    
    /**
     * 获取与特定好友的未读消息数量
     * @param userId 用户ID
     * @param friendId 好友ID
     * @return 未读消息数量
     */
    @Select("SELECT COUNT(*) FROM messages m " +
           "LEFT JOIN message_status ms ON m.message_id = ms.message_id AND ms.user_id = #{userId} " +
           "WHERE m.sender_id = #{friendId} AND m.receiver_id = #{userId} AND (ms.status IS NULL OR ms.status != 'read')")
    int getUnreadMessageCountFromFriend(@Param("userId") String userId, @Param("friendId") String friendId);
}