package com.linliquan.common;

/**
 * 响应码枚举
 * 【红线强制】定义403错误码，供权限拦截使用
 */
public enum ResultCode {

    SUCCESS(200, "操作成功"),

    // 4xx 客户端错误
    BAD_REQUEST(400, "请求参数错误"),
    UNAUTHORIZED(401, "未登录或登录已过期"),
    FORBIDDEN_UNVERIFIED(40301, "仅认证业主可进行此操作"),
    FORBIDDEN_PENDING_VERIFICATION(40302, "您的业主认证正在审核中，审核通过后即可使用"),
    FORBIDDEN_UNAUTHENTICATED(40303, "请先完成业主认证"),
    NOT_FOUND(404, "资源不存在"),
    CONFLICT(409, "资源冲突"),

    // 5xx 服务端错误
    INTERNAL_ERROR(500, "服务器内部错误"),
    SERVICE_UNAVAILABLE(503, "服务暂不可用");

    private final int code;
    private final String message;

    ResultCode(int code, String message) {
        this.code = code;
        this.message = message;
    }

    public int getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }
}
