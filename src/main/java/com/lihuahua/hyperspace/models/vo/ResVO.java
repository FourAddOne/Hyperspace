package com.lihuahua.hyperspace.models.vo;

import lombok.Data;

@Data
public class ResVO {

    private boolean success;
    private String msg;
    private Object data;
    private String code; // 错误码

    public ResVO() {
    }

    public ResVO(boolean success, String msg, Object data) {
        this.success = success;
        this.msg = msg;
        this.data = data;
    }

    public ResVO(boolean success, String code, String msg, Object data) {
        this.success = success;
        this.code = code;
        this.msg = msg;
        this.data = data;
    }

    public static ResVO success(Object data) {
        return new ResVO(true, "success", data);
    }

    public static ResVO success(String msg, Object data) {
        return new ResVO(true, msg, data);
    }

    public static ResVO fail(String msg) {
        return new ResVO(false, msg, null);
    }

    public static ResVO fail(String code, String msg) {
        ResVO resVO = new ResVO(false, msg, null);
        resVO.setCode(code);
        return resVO;
    }
}