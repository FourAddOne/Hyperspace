package com.lihuahua.hyperspace.utils;

import com.lihuahua.hyperspace.mapper.GroupMapper;
import com.lihuahua.hyperspace.models.dto.GroupDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Random;

/**
 * ID生成器工具类
 */
@Component
public class IdGenerator {

    @Autowired
    private GroupMapper groupMapper;

    private final Random random = new Random();

    /**
     * 生成以非零数字开头的11位数字群组ID
     *
     * @return 11位群组ID
     */
    public String generateGroupId() {
        StringBuilder sb = new StringBuilder();
        
        // 第一位不能为0
        sb.append(random.nextInt(9) + 1);

        // 生成后面10位数字
        for (int i = 0; i < 10; i++) {
            sb.append(random.nextInt(10));
        }

        return sb.toString();
    }

    /**
     * 生成唯一的群组ID
     * 通过与数据库比较确保唯一性
     *
     * @return 唯一的群组ID
     */
    public String generateUniqueGroupId() {
        String groupId;
        int attempts = 0;
        final int maxAttempts = 10; // 最大尝试次数

        do {
            groupId = generateGroupId();
            attempts++;

            // 防止无限循环
            if (attempts >= maxAttempts) {
                throw new RuntimeException("无法生成唯一的群组ID，已达到最大尝试次数");
            }
        } while (isGroupIdExists(groupId));

        return groupId;
    }

    /**
     * 检查群组ID是否已存在
     *
     * @param groupId 群组ID
     * @return 是否存在
     */
    private boolean isGroupIdExists(String groupId) {
        try {
            // 使用现有方法检查群组是否存在
            GroupDTO group = groupMapper.getGroupById(groupId);
            return group != null;
        } catch (Exception e) {
            // 如果检查过程出错，默认认为ID已存在以保证安全性
            return true;
        }
    }
}