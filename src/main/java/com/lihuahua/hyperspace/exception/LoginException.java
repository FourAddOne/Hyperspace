package com.lihuahua.hyperspace.exception;

public class LoginException extends RuntimeException {
    private String code;

    public LoginException(String message) {
        super(message);
    }

    public LoginException(String code, String message) {
        super(message);
        this.code = code;
    }

    public LoginException(String message, Throwable cause) {
        super(message, cause);
    }

    public LoginException(String code, String message, Throwable cause) {
        super(message, cause);
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}