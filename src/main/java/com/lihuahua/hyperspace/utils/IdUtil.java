package com.lihuahua.hyperspace.utils;


public class IdUtil {
    
    public static String generateUserId() {
        // 生成11位首位不为0的随机数
        StringBuilder sb = new StringBuilder();
        // 第一位数字不能为0，范围是1-9
        sb.append((int) (Math.random() * 9 + 1));
        // 后面10位数字可以是0-9
        for (int i = 0; i < 10; i++) {
            sb.append((int) (Math.random() * 10));
        }
        return sb.toString();
    }
}