package com.lihuahua.hyperspace.models.vo;

import lombok.Data;

@Data
public class ResVO {

    private boolean success;
    private String msg;
    private Object data;

    public ResVO(boolean success, String msg, Object data) {
        this.success = success;
        this.msg = msg;
        this.data = data;
    }

    public static ResVO success(Object data) {
        return new ResVO(true, "success", data);
    }

    public static ResVO fail(String msg) {
        return new ResVO(false, msg, null);
    }
}
