package com.lihuahua.hyperspace.utils;

import cn.hutool.crypto.digest.BCrypt;

public class PasswordUtil {

/**
 * 加密方法
 * 该方法用于对输入的密码进行加密处理
 *
 * @param password 需要加密的原始密码字符串
 * @return 加密后的密码字符串，当前实现为直接返回原始密码
 */
    public static String encrypt(String password) {

        password = BCrypt.hashpw(password, BCrypt.gensalt());

        return password;
    }

/**
 * 检查密码是否匹配的方法
 * 使用BCrypt算法验证输入的密码与存储的密码是否一致
 *
 * @param password 需要验证的密码字符串
 * @param storgePw 存储的密码字符串（通常是经过BCrypt加密后的哈希值）
 * @return Boolean类型，返回密码是否匹配的结果
 */
    public static Boolean checkPw(String password, String storgePw) {
        return BCrypt.checkpw(password, storgePw);

    }
}
