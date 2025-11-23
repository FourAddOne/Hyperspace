package com.lihuahua.hyperspace.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lihuahua.hyperspace.models.entity.Friend;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface FriendMapper extends BaseMapper<Friend> {
    
    @Select("SELECT * FROM friend WHERE (user_id = #{userId} AND friend_id = #{friendId}) OR (user_id = #{friendId} AND friend_id = #{userId})")
    Friend findByUserAndFriend(@Param("userId") String userId, @Param("friendId") String friendId);
    
    @Select("SELECT * FROM friend WHERE (user_id = #{userId} AND friend_id = #{friendId}) OR (user_id = #{friendId} AND friend_id = #{userId})")
    List<Friend> findMutualFriends(@Param("userId") String userId, @Param("friendId") String friendId);
    
    @Select("SELECT COUNT(*) > 0 FROM friend WHERE ((user_id = #{userId} AND friend_id = #{friendId}) OR (user_id = #{friendId} AND friend_id = #{userId})) AND status = 'ACCEPTED'")
    boolean isFriend(@Param("userId") String userId, @Param("friendId") String friendId);
    
    @Select("SELECT COUNT(*) > 0 FROM friend WHERE ((user_id = #{userId} AND friend_id = #{friendId}) OR (user_id = #{friendId} AND friend_id = #{userId})) AND status = 'PENDING'")
    boolean hasPendingRequest(@Param("userId") String userId, @Param("friendId") String friendId);
}