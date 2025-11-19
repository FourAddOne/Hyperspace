package com.lihuahua.hyperspace.handler;

import com.lihuahua.hyperspace.exception.LoginException;
import com.lihuahua.hyperspace.models.vo.ResVO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.validation.BindException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * 处理自定义登录异常
     */
    @ExceptionHandler(LoginException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ResVO handleLoginException(LoginException e) {
        logger.error("登录异常: ", e);
        if (e.getCode() != null) {
            Map<String, Object> errorData = new HashMap<>();
            errorData.put("code", e.getCode());
            errorData.put("message", e.getMessage());
            return new ResVO(false, "操作失败", errorData);
        }
        return ResVO.fail(e.getMessage());
    }

    /**
     * 处理参数验证异常（@Validated）
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ResVO handleMethodArgumentNotValidException(MethodArgumentNotValidException e) {
        BindingResult bindingResult = e.getBindingResult();
        Map<String, String> errors = new HashMap<>();
        for (FieldError fieldError : bindingResult.getFieldErrors()) {
            errors.put(fieldError.getField(), fieldError.getDefaultMessage());
        }
        logger.error("参数验证异常: {}", errors);
        return ResVO.fail("参数验证失败: " + errors.toString());
    }

    /**
     * 处理参数绑定异常
     */
    @ExceptionHandler(BindException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ResVO handleBindException(BindException e) {
        BindingResult bindingResult = e.getBindingResult();
        Map<String, String> errors = new HashMap<>();
        for (FieldError fieldError : bindingResult.getFieldErrors()) {
            errors.put(fieldError.getField(), fieldError.getDefaultMessage());
        }
        logger.error("参数绑定异常: {}", errors);
        return ResVO.fail("参数绑定失败: " + errors.toString());
    }

    /**
     * 处理通用异常
     */
    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ResVO handleException(Exception e) {
        logger.error("系统异常: ", e);
        return ResVO.fail("系统内部错误，请联系管理员");
    }
}