package com.lihuahua.hyperspace.models.vo;

import lombok.Data;

@Data
public class ResVO<T> {

    private int code;
    private String msg;
    private T data;

    public ResVO(int code, String msg, T data) {
        this.code = code;
        this.msg = msg;
        this.data = data;
    }

    public static <T> ResVO<T> success(T data) {
        return new ResVO<>(200, "success", data);
    }

    public static <T> ResVO<T> success() {
        return new ResVO<>(200, "success", null);
    }

    public static <T> ResVO<T> fail(String msg) {
        return new ResVO<>(400, msg, null);
    }
    
    public static <T> ResVO<T> error(int code, String msg) {
        return new ResVO<>(code, msg, null);
    }
    
    public static <T> ResVO<T> unauthorized(String msg) {
        return new ResVO<>(401, msg, null);
    }
    
    public static <T> ResVO<T> forbidden(String msg) {
        return new ResVO<>(403, msg, null);
    }
}