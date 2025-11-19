package com.lihuahua.hyperspace.enums;

public enum RegisterFail {

    EMAIL_EXIST("REG_001", "邮箱已存在"),
    USERNAME_EXIST("REG_002", "用户名已存在");

    private final String code;
    private final String message;

    RegisterFail(String code, String message) {
        this.code = code;
        this.message = message;
    }

    public String getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }
}