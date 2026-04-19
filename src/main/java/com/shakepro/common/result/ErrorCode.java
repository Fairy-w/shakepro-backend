package com.shakepro.common.result;

import lombok.Getter;

@Getter
public enum ErrorCode {

    SUCCESS(0, "OK"),
    PARAM_ERROR(40001, "参数错误"),
    UNAUTHORIZED(40100, "未登录或Token无效"),
    FORBIDDEN(40300, "无权限"),
    NOT_FOUND(40400, "资源不存在"),
    USERNAME_EXISTS(40002, "用户名已存在"),
    LOGIN_FAILED(40003, "用户名或密码错误"),
    SERVER_ERROR(50000, "服务端异常"),
    AI_ERROR(50010, "AI调用失败"),
    OSS_ERROR(50020, "OSS预签名失败"),
    BARCODE_LOOKUP_FAILED(50030, "条码识别失败"),
    FILE_TYPE_NOT_ALLOWED(40004, "不允许的文件类型"),
    FILE_SIZE_EXCEEDED(40005, "文件大小超出限制"),
    BARCODE_NOT_FOUND(40410, "未识别到商品信息");

    private final int code;
    private final String message;

    ErrorCode(int code, String message) {
        this.code = code;
        this.message = message;
    }
}
