package com.erp.common.api;

import lombok.Getter;

/**
 * 业务错误码定义。
 */
@Getter
public enum ErrorCode {

    SUCCESS(0, "success"),
    BAD_REQUEST(40000, "请求参数不合法"),
    UNAUTHORIZED(40100, "未登录或登录已过期"),
    BAD_CREDENTIALS(40101, "用户名或密码错误"),
    ACCOUNT_DISABLED(40102, "账号已停用"),
    FORBIDDEN(40300, "无访问权限"),
    NOT_FOUND(40400, "数据不存在"),
    CONFLICT(40900, "数据状态冲突"),
    DUPLICATE(40901, "数据已存在"),
    STOCK_NOT_ENOUGH(40902, "库存不足"),
    IDEMPOTENT_REPEAT(40903, "请勿重复提交"),
    BIZ_RULE_VIOLATION(42200, "业务规则校验失败"),
    INTERNAL_ERROR(50000, "系统内部错误");

    private final int code;
    private final String message;

    ErrorCode(int code, String message) {
        this.code = code;
        this.message = message;
    }
}
