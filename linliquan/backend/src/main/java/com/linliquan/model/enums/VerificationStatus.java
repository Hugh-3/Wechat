package com.linliquan.model.enums;

/**
 * 业主身份认证状态枚举
 * 【红线强制】三态模型定义
 *
 * 状态值定义：
 * - UNAUTH(0): 仅用手机号注册，未绑定任何业主凭证，仅可浏览（列表、详情）
 * - PENDING(1): 已提交房产证/物业码，人工审核队列中，仅可浏览
 * - VERIFIED(2): 物业白名单匹配成功 / 人工审核通过，全功能开放
 *
 * 所有涉及发布、私信、组局的接口，必须在业务逻辑层前置校验该状态，
 * 非VERIFIED一律返回HTTP 403。
 */
public enum VerificationStatus {

    /**
     * 未认证
     * 权限：仅浏览（列表、详情）
     */
    UNAUTH(0, "未认证"),

    /**
     * 认证中
     * 权限：仅浏览（待审核通过）
     */
    PENDING(1, "认证中"),

    /**
     * 已认证业主
     * 权限：全功能开放
     */
    VERIFIED(2, "已认证");

    private final int code;
    private final String description;

    VerificationStatus(int code, String description) {
        this.code = code;
        this.description = description;
    }

    public int getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    /**
     * 根据code值获取枚举
     */
    public static VerificationStatus fromCode(int code) {
        for (VerificationStatus status : values()) {
            if (status.code == code) {
                return status;
            }
        }
        throw new IllegalArgumentException("Unknown verification status code: " + code);
    }

    /**
     * 判断是否为已认证状态
     */
    public boolean isVerified() {
        return this == VERIFIED;
    }

    /**
     * 判断是否为认证中状态
     */
    public boolean isPending() {
        return this == PENDING;
    }

    /**
     * 判断是否为未认证状态
     */
    public boolean isUnauth() {
        return this == UNAUTH;
    }

    /**
     * 判断是否可进行写操作（发布、评论、私信等）
     * 仅VERIFIED状态可写
     */
    public boolean canWrite() {
        return this == VERIFIED;
    }

    /**
     * 判断是否可浏览
     * 三种状态均可浏览
     */
    public boolean canRead() {
        return true;
    }
}
