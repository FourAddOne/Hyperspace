package com.lihuahua.hyperspace.Service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.lihuahua.hyperspace.Service.FriendService;
import com.lihuahua.hyperspace.mapper.FriendMapper;
import com.lihuahua.hyperspace.mapper.convert.FriendConvertMapper;
import com.lihuahua.hyperspace.models.bo.FriendBO;
import com.lihuahua.hyperspace.models.dto.FriendDTO;
import com.lihuahua.hyperspace.models.entity.Friend;
import com.lihuahua.hyperspace.models.po.FriendPO;
import com.lihuahua.hyperspace.models.vo.FriendVO;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.web.socket.TextMessage;
import com.lihuahua.hyperspace.websocket.ChatWebSocketHandler;



/**
 * 好友服务实现类，实现好友相关的业务逻辑
 */
@Slf4j
@Service
public class FriendServiceImpl implements FriendService {
    
    /**
     * 好友数据访问层，用于数据库操作
     */
    @Autowired
    private FriendMapper friendMapper;
    
    /**
     * 好友转换器，用于不同对象之间的转换（DTO、BO、VO、PO）
     */
    @Autowired
    private FriendConvertMapper friendConvertMapper;

    private static final Logger logger = LoggerFactory.getLogger(FriendServiceImpl.class);

    /**
     * 添加好友方法
     * @param friendDTO 好友信息传输对象，包含添加好友所需的信息
     * @return 返回好友视图对象，用于前端展示
     */
    @Override
    public FriendVO addFriend(FriendDTO friendDTO) {
        logger.info("添加好友信息：{}", friendDTO);
        // 创建FriendBO对象
        FriendBO friendBO = new FriendBO();
        friendBO.setUserOne(friendDTO.getUserOne());
        friendBO.setUserSec(friendDTO.getUserSec());
        friendBO.setStatus(friendDTO.getStatus());
        friendBO.setBlockStatus(friendDTO.getBlockStatus());
        friendBO.setUserOneRemark(friendDTO.getUserOneRemark());
        friendBO.setUserSecRemark(friendDTO.getUserSecRemark());
        friendBO.setCreatedAt(System.currentTimeMillis());
        friendBO.setUpdatedAt(System.currentTimeMillis());
        logger.info("添加好友信息：{}", friendBO);
        // 转换为PO并保存
        FriendPO friendPO = friendConvertMapper.toPO(friendBO);

        int result = friendMapper.insert(friendPO);
        
        // 如果添加成功，通过WebSocket通知被添加的用户
        if (result > 0) {
            try {
                // 发送好友申请通知给被添加的用户
                TextMessage message = new TextMessage("{\"type\": \"friendRequest\", \"from\": \"" + friendDTO.getUserOne() + "\", \"remark\": \"" + (friendDTO.getUserOneRemark() != null ? friendDTO.getUserOneRemark() : "") + "\"}");
                ChatWebSocketHandler.sendMessageToUser(friendDTO.getUserSec(), message);
                logger.info("已通过WebSocket向用户 {} 发送好友申请通知", friendDTO.getUserSec());
            } catch (Exception e) {
                logger.error("通过WebSocket发送好友申请通知失败", e);
            }
        }
        
        // 转换为Friend实体
        Friend friend = new Friend();
        friend.setUserOne(friendBO.getUserOne());
        friend.setUserSec(friendBO.getUserSec());
        friend.setStatus(friendBO.getStatus());
        friend.setBlockStatus(friendBO.getBlockStatus());
        friend.setUserOneRemark(friendBO.getUserOneRemark());
        friend.setUserSecRemark(friendBO.getUserSecRemark());
        friend.setCreatedAt(friendBO.getCreatedAt());
        friend.setUpdatedAt(friendBO.getUpdatedAt());
        
        // 转换为VO返回
        return friendConvertMapper.toVO(friend);
    }

    @Override
    public Friend getFriendById(String userOne, String userSec) {
        QueryWrapper<FriendPO> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("user_one", userOne).eq("user_sec", userSec);
        FriendPO friendPO = friendMapper.selectOne(queryWrapper);
        
        if (friendPO == null) {
            return null;
        }
        
        FriendBO friendBO = friendConvertMapper.toBO(friendPO);
        Friend friend = new Friend();
        friend.setUserOne(friendBO.getUserOne());
        friend.setUserSec(friendBO.getUserSec());
        friend.setStatus(friendBO.getStatus());
        friend.setBlockStatus(friendBO.getBlockStatus());
        friend.setUserOneRemark(friendBO.getUserOneRemark());
        friend.setUserSecRemark(friendBO.getUserSecRemark());
        friend.setCreatedAt(friendBO.getCreatedAt());
        friend.setUpdatedAt(friendBO.getUpdatedAt());
        
        return friend;
    }

    @Override
    public Boolean updateFriendStatus(FriendDTO friendDTO) {
        UpdateWrapper<FriendPO> updateWrapper = new UpdateWrapper<>();
        updateWrapper.eq("user_one", friendDTO.getUserOne())
                .eq("user_sec", friendDTO.getUserSec())
                .set("status", friendDTO.getStatus())
                .set("updated_at", System.currentTimeMillis());
        
        int result = friendMapper.update(null, updateWrapper);
        return result > 0;
    }

    @Override
    public Boolean updateFriendRemark(FriendDTO friendDTO) {
        UpdateWrapper<FriendPO> updateWrapper = new UpdateWrapper<>();
        updateWrapper.eq("user_one", friendDTO.getUserOne())
                .eq("user_sec", friendDTO.getUserSec())
                .set("user_one_remark", friendDTO.getUserOneRemark())
                .set("user_sec_remark", friendDTO.getUserSecRemark())
                .set("updated_at", System.currentTimeMillis());
        
        int result = friendMapper.update(null, updateWrapper);
        return result > 0;
    }

    @Override
    public Boolean deleteFriend(String userId, String friendId) {
        QueryWrapper<FriendPO> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("user_one", userId).eq("user_sec", friendId);
        int result = friendMapper.delete(queryWrapper);
        return result > 0;
    }

    @Override
    public List<FriendVO> getFriendList(String userId) {
        // 分别查询两个条件的结果，用 UNION ALL 逻辑合并（避免重复）
        List<FriendPO> list1 = friendMapper.selectByUserOne(userId);
        List<FriendPO> list2 = friendMapper.selectByUserSec(userId);

        // 合并两个列表（因 user_one < user_sec 唯一约束，无重复，直接 addAll 即可）
        List<FriendPO> friendPOList = new ArrayList<>();
        friendPOList.addAll(list1);
        friendPOList.addAll(list2);

        return friendPOList.stream()
                .map(po -> {
                    FriendBO bo = friendConvertMapper.toBO(po);
                    Friend friend = new Friend();
                    friend.setUserOne(bo.getUserOne());
                    friend.setUserSec(bo.getUserSec());
                    friend.setStatus(bo.getStatus());
                    friend.setBlockStatus(bo.getBlockStatus());
                    friend.setUserOneRemark(bo.getUserOneRemark());
                    friend.setUserSecRemark(bo.getUserSecRemark());
                    friend.setCreatedAt(bo.getCreatedAt());
                    friend.setUpdatedAt(bo.getUpdatedAt());
                    return friendConvertMapper.toVO(friend);
                })
                .collect(Collectors.toList());
    }

    @Override
    public List<FriendVO> searchFriends(String userId, String keyword) {
        QueryWrapper<FriendPO> queryWrapper = new QueryWrapper<>();
        queryWrapper.and(wrapper -> wrapper.eq("user_one", userId).or().eq("user_sec", userId));
        queryWrapper.and(wrapper -> wrapper.like("user_one_remark", keyword).or().like("user_sec_remark", keyword));
        
        List<FriendPO> friendPOList = friendMapper.selectList(queryWrapper);
        
        return friendPOList.stream()
                .map(po -> {
                    FriendBO bo = friendConvertMapper.toBO(po);
                    Friend friend = new Friend();
                    friend.setUserOne(bo.getUserOne());
                    friend.setUserSec(bo.getUserSec());
                    friend.setStatus(bo.getStatus());
                    friend.setBlockStatus(bo.getBlockStatus());
                    friend.setUserOneRemark(bo.getUserOneRemark());
                    friend.setUserSecRemark(bo.getUserSecRemark());
                    friend.setCreatedAt(bo.getCreatedAt());
                    friend.setUpdatedAt(bo.getUpdatedAt());
                    return friendConvertMapper.toVO(friend);
                })
                .collect(Collectors.toList());
    }

    @Override
    public Boolean blockFriend(String userId, String friendId) {
        // 构造查询条件
        QueryWrapper<FriendPO> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("user_one", userId).eq("user_sec", friendId);
        
        FriendPO friendPO = friendMapper.selectOne(queryWrapper);
        if (friendPO != null) {
            // userId是userOne的情况
            UpdateWrapper<FriendPO> updateWrapper = new UpdateWrapper<>();
            updateWrapper.eq("user_one", userId)
                    .eq("user_sec", friendId)
                    .set("block_status", "USER_ONE_BLOCKED")
                    .set("updated_at", System.currentTimeMillis());
            friendMapper.update(null, updateWrapper);
            return true;
        }
        
        // 检查是否userId是userSec的情况
        queryWrapper.clear();
        queryWrapper.eq("user_one", friendId).eq("user_sec", userId);
        friendPO = friendMapper.selectOne(queryWrapper);
        if (friendPO != null) {
            UpdateWrapper<FriendPO> updateWrapper = new UpdateWrapper<>();
            updateWrapper.eq("user_one", friendId)
                    .eq("user_sec", userId)
                    .set("block_status", "USER_SEC_BLOCKED")
                    .set("updated_at", System.currentTimeMillis());
            friendMapper.update(null, updateWrapper);
            return true;
        }
        
        return false;
    }

    @Override
    public Boolean unblockFriend(String userId, String friendId) {
        // 构造查询条件
        QueryWrapper<FriendPO> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("user_one", userId).eq("user_sec", friendId);
        
        FriendPO friendPO = friendMapper.selectOne(queryWrapper);
        if (friendPO != null) {
            // userId是userOne的情况
            UpdateWrapper<FriendPO> updateWrapper = new UpdateWrapper<>();
            updateWrapper.eq("user_one", userId)
                    .eq("user_sec", friendId)
                    .set("block_status", "NONE")
                    .set("updated_at", System.currentTimeMillis());
            friendMapper.update(null, updateWrapper);
            return true;
        }
        
        // 检查是否userId是userSec的情况
        queryWrapper.clear();
        queryWrapper.eq("user_one", friendId).eq("user_sec", userId);
        friendPO = friendMapper.selectOne(queryWrapper);
        if (friendPO != null) {
            UpdateWrapper<FriendPO> updateWrapper = new UpdateWrapper<>();
            updateWrapper.eq("user_one", friendId)
                    .eq("user_sec", userId)
                    .set("block_status", "NONE")
                    .set("updated_at", System.currentTimeMillis());
            friendMapper.update(null, updateWrapper);
            return true;
        }
        
        return false;
    }

    @Override
    public Boolean isBlocked(String userId, String friendId) {
        // 构造查询条件
        QueryWrapper<FriendPO> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("user_one", userId).eq("user_sec", friendId);
        
        FriendPO friendPO = friendMapper.selectOne(queryWrapper);
        if (friendPO != null) {
            // userId是userOne的情况
            return "USER_ONE_BLOCKED".equals(friendPO.getBlockStatus()) || "BOTH_BLOCKED".equals(friendPO.getBlockStatus());
        }
        
        // 检查是否userId是userSec的情况
        queryWrapper.clear();
        queryWrapper.eq("user_one", friendId).eq("user_sec", userId);
        friendPO = friendMapper.selectOne(queryWrapper);
        if (friendPO != null) {
            return "USER_SEC_BLOCKED".equals(friendPO.getBlockStatus()) || "BOTH_BLOCKED".equals(friendPO.getBlockStatus());
        }
        
        return false;
    }

    @Override
    public List<FriendVO> getFriendRequests(String userId) {
        QueryWrapper<FriendPO> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("user_sec", userId).eq("status", "PENDING");
        List<FriendPO> friendPOList = friendMapper.selectList(queryWrapper);
        
        return friendPOList.stream()
                .map(po -> {
                    FriendBO bo = friendConvertMapper.toBO(po);
                    Friend friend = new Friend();
                    friend.setUserOne(bo.getUserOne());
                    friend.setUserSec(bo.getUserSec());
                    friend.setStatus(bo.getStatus());
                    friend.setBlockStatus(bo.getBlockStatus());
                    friend.setUserOneRemark(bo.getUserOneRemark());
                    friend.setUserSecRemark(bo.getUserSecRemark());
                    friend.setCreatedAt(bo.getCreatedAt());
                    friend.setUpdatedAt(bo.getUpdatedAt());
                    return friendConvertMapper.toVO(friend);
                })
                .collect(Collectors.toList());
    }

    @Override
    public Boolean handleFriendRequest(FriendDTO friendDTO) {
        UpdateWrapper<FriendPO> updateWrapper = new UpdateWrapper<>();
        updateWrapper.eq("user_one", friendDTO.getUserOne())
                .eq("user_sec", friendDTO.getUserSec())
                .set("status", friendDTO.getStatus())
                .set("updated_at", System.currentTimeMillis());
        
        int result = friendMapper.update(null, updateWrapper);
        
        // 如果处理成功，通过WebSocket通知申请人处理结果
        if (result > 0) {
            try {
                // 发送处理结果通知给申请人
                String statusText = "ACCEPTED".equals(friendDTO.getStatus()) ? "已接受" : "已拒绝";
                TextMessage message = new TextMessage("{\"type\": \"friendRequestResult\", \"from\": \"" + friendDTO.getUserSec() + "\", \"status\": \"" + friendDTO.getStatus() + "\", \"statusText\": \"" + statusText + "\"}");
                ChatWebSocketHandler.sendMessageToUser(friendDTO.getUserOne(), message);
                logger.info("已通过WebSocket向用户 {} 发送好友申请处理结果通知", friendDTO.getUserOne());
            } catch (Exception e) {
                logger.error("通过WebSocket发送好友申请处理结果通知失败", e);
            }
        }
        
        return result > 0;
    }
}