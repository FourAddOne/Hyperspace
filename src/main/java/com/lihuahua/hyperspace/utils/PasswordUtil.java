package com.lihuahua.hyperspace.utils;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class PasswordUtil {

/**
 * 加密方法
 * 该方法用于对输入的密码进行加密处理
 *
 * @param password 需要加密的原始密码字符串
 * @return 加密后的密码字符串，当前实现为SHA-256哈希
 */
    public static String encrypt(String password) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hashedBytes = md.digest(password.getBytes());
            StringBuilder sb = new StringBuilder();
            for (byte b : hashedBytes) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("加密算法不可用", e);
        }
    }

/**
 * 检查密码是否匹配的方法
 * 使用SHA-256算法验证输入的密码与存储的密码是否一致
 *
 * @param password 需要验证的密码字符串
 * @param storedPw 存储的密码字符串（通常是经过SHA-256加密后的哈希值）
 * @return Boolean类型，返回密码是否匹配的结果
 */
    public static Boolean checkPw(String password, String storedPw) {
        return encrypt(password).equals(storedPw);
    }
}