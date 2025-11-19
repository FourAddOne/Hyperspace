package com.lihuahua.hyperspace.enums;

public enum LoginFail {

    EMAIL_NOT_EXIST("LOGIN_001", "邮箱不存在"),
    PASSWORD_ERROR("LOGIN_002", "密码错误"),
    USER_NOT_EXIST("LOGIN_003", "用户不存在"),
    USER_ID_NOT_EXIST("LOGIN_004", "用户ID不存在"),
    EMAIL_EXIST("LOGIN_005", "邮箱已存在"),
    USER_NAME_EXIST("LOGIN_006", "用户名已存在"),
    USER_ID_EXIST("LOGIN_007", "用户ID已存在"),
    USER_ID_OR_EMAIL_NOT_EXIST("LOGIN_008", "用户ID或邮箱不存在"),
    USER_ID_OR_EMAIL_ERROR("LOGIN_009", "用户ID或邮箱错误"),
    USER_ID_OR_EMAIL_PASSWORD_ERROR("LOGIN_010", "用户ID或邮箱密码错误"),
    USER_ID_OR_EMAIL_NOT_EXIST_PASSWORD_ERROR("LOGIN_011", "用户ID或邮箱不存在或密码错误"),
    USER_ID_OR_EMAIL_IS_EMPTY("LOGIN_012", "用户ID或邮箱为空"),
    USER_NOT_LOGIN("LOGIN_013", "用户未登录");

    private final String code;
    private final String message;

    LoginFail(String code, String message) {
        this.code = code;
        this.message = message;
    }

    // 添加getter方法
    public String getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }
}