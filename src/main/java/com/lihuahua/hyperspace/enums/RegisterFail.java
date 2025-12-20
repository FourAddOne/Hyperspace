package com.lihuahua.hyperspace.enums;

public enum RegisterFail {

    EMAIL_EXIST("邮箱已存在"),
    USERNAME_EXIST("用户名已存在");

    private final String msg;

    RegisterFail(String msg) {
        this.msg = msg;
    }

    public String getMessage() {
        return msg;
    }
}
