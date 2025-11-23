package com.lihuahua.hyperspace.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lihuahua.hyperspace.models.entity.Friend;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface FriendMapper extends BaseMapper<Friend> {
    
    @Select("SELECT * FROM friend WHERE (user_one = #{userId} AND user_sec = #{friendId}) OR (user_one = #{friendId} AND user_sec = #{userId})")
    Friend findByUserAndFriend(@Param("userId") String userId, @Param("friendId") String friendId);
    
    @Select("SELECT * FROM friend WHERE (user_one = #{userId} AND user_sec = #{friendId}) OR (user_one = #{friendId} AND user_sec = #{userId})")
    List<Friend> findMutualFriends(@Param("userId") String userId, @Param("friendId") String friendId);
    
    @Select("SELECT COUNT(*) > 0 FROM friend WHERE ((user_one = #{userId} AND user_sec = #{friendId}) OR (user_one = #{friendId} AND user_sec = #{userId})) AND status = 'ACCEPTED'")
    boolean isFriend(@Param("userId") String userId, @Param("friendId") String friendId);
    
    @Select("SELECT COUNT(*) > 0 FROM friend WHERE ((user_one = #{userId} AND user_sec = #{friendId}) OR (user_one = #{friendId} AND user_sec = #{userId})) AND status = 'PENDING'")
    boolean hasPendingRequest(@Param("userId") String userId, @Param("friendId") String friendId);
}