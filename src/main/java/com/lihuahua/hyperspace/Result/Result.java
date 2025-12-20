package com.lihuahua.hyperspace.Result;

public class Result <T>{
    private String code;
    private String msg;
    private T data;
    
    public Result(String code, String msg, T data) {
        this.code = code;
        this.msg = msg;
        this.data = data;
    }
    
    public static <T> Result<T> success(T data) {
        return new Result<>("200", "success", data);
    }
    
    public static  <T> Result<T> success() {
        return new Result<>("200", "success", null);
    }
    
    public static <T> Result<T> error(String msg) {
        return new Result<>("500", msg, null);
    }
    
    // Getter methods
    public String getCode() {
        return code;
    }
    
    public String getMsg() {
        return msg;
    }
    
    public T getData() {
        return data;
    }
    
    // Setter methods
    public void setCode(String code) {
        this.code = code;
    }
    
    public void setMsg(String msg) {
        this.msg = msg;
    }
    
    public void setData(T data) {
        this.data = data;
    }
}

