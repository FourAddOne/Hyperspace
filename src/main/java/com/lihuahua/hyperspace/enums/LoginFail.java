package com.lihuahua.hyperspace.enums;

public enum LoginFail {

    EMAIL_NOT_EXIST("邮箱不存在"),
    PASSWORD_ERROR("密码错误"),
    USER_NOT_EXIST("用户不存在"),
    USER_ID_NOT_EXIST("用户ID不存在"),
    EMAIL_EXIST("邮箱已存在"),
    USER_NAME_EXIST("用户名已存在"),
    USER_ID_EXIST("用户ID已存在"),
    USER_ID_OR_EMAIL_NOT_EXIST("用户ID或邮箱不存在"),
    USER_ID_OR_EMAIL_ERROR("用户ID或邮箱错误"),
    USER_ID_OR_EMAIL_PASSWORD_ERROR("用户ID或邮箱密码错误"),
    USER_ID_OR_EMAIL_NOT_EXIST_PASSWORD_ERROR("用户ID或邮箱不存在或密码错误"),
    USER_ID_OR_EMAIL_IS_EMPTY("用户ID或邮箱为空"),
    USER_NOT_LOGIN("用户未登录");

    private final String message;

    LoginFail(String message) {
        this.message = message;
    }

    // 添加getter方法
    public String getMessage() {
        return message;
    }
}
