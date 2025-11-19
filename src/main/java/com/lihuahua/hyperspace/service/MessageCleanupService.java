package com.lihuahua.hyperspace.service;

import com.lihuahua.hyperspace.mapper.MessageMapper;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.Date;

/**
 * 消息清理服务
 * 定期清理过期消息以节省数据库空间
 */
@Service
public class MessageCleanupService {
    
    @Resource
    private MessageMapper messageMapper;
    
    /**
     * 每天凌晨2点执行消息清理任务
     * 清理30天前的消息
     */
    @Scheduled(cron = "0 0 2 * * ?")
    public void cleanupOldMessages() {
        try {
            // 计算30天前的时间
            Date thirtyDaysAgo = new Date(System.currentTimeMillis() - 30L * 24 * 60 * 60 * 1000);
            
            // 删除30天前的消息
            int deletedCount = messageMapper.deleteMessagesOlderThan(thirtyDaysAgo);
            
            System.out.println("清理了 " + deletedCount + " 条过期消息");
        } catch (Exception e) {
            System.err.println("清理过期消息时出错: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * 应用启动时初始化
     */
    @PostConstruct
    public void init() {
        System.out.println("消息清理服务已启动，将每天凌晨2点清理30天前的消息");
    }
}