package com.lihuahua.hyperspace.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lihuahua.hyperspace.models.entity.Friend;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface FriendMapper extends BaseMapper<Friend> {
    
    @Select("SELECT * FROM friends WHERE user_id = #{userId} AND friend_id = #{friendId}")
    Friend findByUserAndFriend(@Param("userId") String userId, @Param("friendId") String friendId);
    
    @Select("SELECT * FROM friends WHERE (user_id = #{userId} AND friend_id = #{friendId}) OR (user_id = #{friendId} AND friend_id = #{userId})")
    List<Friend> findMutualFriends(@Param("userId") String userId, @Param("friendId") String friendId);
    
    @Select("SELECT COUNT(*) > 0 FROM friends WHERE user_id = #{userId} AND friend_id = #{friendId} AND status = 'accepted'")
    boolean isFriend(@Param("userId") String userId, @Param("friendId") String friendId);
    
    @Select("SELECT COUNT(*) > 0 FROM friends WHERE user_id = #{userId} AND friend_id = #{friendId} AND status = 'pending'")
    boolean hasPendingRequest(@Param("userId") String userId, @Param("friendId") String friendId);
}