package com.lihuahua.hyperspace.models.pojo;

import com.lihuahua.hyperspace.models.enums.TargetType;
import lombok.Data;

import java.io.Serializable;

/**
 * 目标信息类（可以是用户或群组）
 * 用于表示消息接收者的信息
 */
@Data
public class TargetInfo implements Serializable {
    /**
     * 目标ID（用户ID或群组ID）
     */
    private String targetId;
    
    /**
     * 目标类型：USER（用户）或 GROUP（群组）
     */
    private TargetType targetType;
    
    /**
     * 目标名称（用户名或群组名）
     */
    private String targetName;
    
    /**
     * 目标头像URL
     */
    private String targetAvatarUrl;
    
    /**
     * 创建目标信息对象的静态方法（用户类型）
     * @param userId 用户ID
     * @param username 用户名
     * @param avatarUrl 用户头像URL
     * @return TargetInfo对象
     */
    public static TargetInfo ofUser(String userId, String username, String avatarUrl) {
        TargetInfo targetInfo = new TargetInfo();
        targetInfo.targetId = userId;
        targetInfo.targetType = TargetType.USER;
        targetInfo.targetName = username;
        targetInfo.targetAvatarUrl = avatarUrl;
        return targetInfo;
    }
    
    /**
     * 创建目标信息对象的静态方法（群组类型）
     * @param groupId 群组ID
     * @param groupName 群组名
     * @param groupAvatarUrl 群组头像URL
     * @return TargetInfo对象
     */
    public static TargetInfo ofGroup(String groupId, String groupName, String groupAvatarUrl) {
        TargetInfo targetInfo = new TargetInfo();
        targetInfo.targetId = groupId;
        targetInfo.targetType = TargetType.GROUP;
        targetInfo.targetName = groupName;
        targetInfo.targetAvatarUrl = groupAvatarUrl;
        return targetInfo;
    }
}