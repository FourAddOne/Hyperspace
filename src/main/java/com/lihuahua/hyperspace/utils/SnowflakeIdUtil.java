package com.lihuahua.hyperspace.utils;

/**
 * Twitter Snowflake算法的Java实现
 * 生成全局唯一的ID
 * 64位ID结构: 1位符号位 + 41位时间戳 + 10位工作机器ID + 12位序列号
 */
public class SnowflakeIdUtil {
    // 起始时间戳 (2022-01-01)
    private final static long START_STMP = 1640995200000L;
    
    // 各部分位数
    private final static long SEQUENCE_BIT = 12; // 序列号占用位数
    private final static long MACHINE_BIT = 10;  // 机器ID占用位数
    
    // 各部分最大值
    private final static long MAX_SEQUENCE = ~(-1L << SEQUENCE_BIT);
    private final static long MAX_MACHINE_NUM = ~(-1L << MACHINE_BIT);
    
    // 各部分偏移量
    private final static long MACHINE_LEFT = SEQUENCE_BIT;
    private final static long TIMESTMP_LEFT = SEQUENCE_BIT + MACHINE_BIT;
    
    private static long machineId;     // 工作机器ID
    private static long sequence = 0L; // 序列号
    private static long lastStmp = -1L;// 上次时间戳
    
    static {
        machineId = 1; // 简单起见，这里直接设置机器ID为1
    }
    
    /**
     * 生成下一个唯一ID
     * @return 唯一ID
     */
    public static synchronized String nextId() {
        long currStmp = getNewstmp();
        // 检查时钟回拨
        if (currStmp < lastStmp) {
            throw new RuntimeException("时钟向后移动，拒绝生成ID");
        }
        
        if (currStmp == lastStmp) {
            // 相同毫秒内，序列号自增
            sequence = (sequence + 1) & MAX_SEQUENCE;
            // 同一毫秒的序列数已经达到最大
            if (sequence == 0L) {
                currStmp = getNextMill();
            }
        } else {
            // 不同毫秒内，序列号置为0
            sequence = 0L;
        }
        
        lastStmp = currStmp;
        
        // 时间戳部分
        long timestamp = (currStmp - START_STMP) << TIMESTMP_LEFT;
        // 工作机器ID部分
        long machineIdPart = machineId << MACHINE_LEFT;
        // 序列号部分
        long sequencePart = sequence;
        
        // 拼接各部分
        long id = timestamp | machineIdPart | sequencePart;
        
        return String.valueOf(id);
    }
    
    private static long getNextMill() {
        long mill = getNewstmp();
        while (mill <= lastStmp) {
            mill = getNewstmp();
        }
        return mill;
    }
    
    private static long getNewstmp() {
        return System.currentTimeMillis();
    }
}