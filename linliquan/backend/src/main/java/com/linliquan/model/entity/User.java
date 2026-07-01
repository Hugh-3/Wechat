package com.linliquan.model.entity;

import com.linliquan.model.enums.VerificationStatus;
import lombok.Data;

import javax.persistence.*;
import java.time.LocalDateTime;

/**
 * 用户实体
 * 【红线强制】verification_status字段必须存在，用于权限拦截
 */
@Data
@Entity
@Table(name = "users", indexes = {
    @Index(name = "idx_phone_hash", columnList = "phone_hash"),
    @Index(name = "idx_verification_status", columnList = "verification_status")
})
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 手机号SHA-256哈希（用于登录查找）
     */
    private String phoneHash;

    /**
     * 手机号AES-256加密存储
     */
    private String phoneEncrypted;

    /**
     * 昵称
     */
    private String nickname;

    /**
     * 头像URL
     */
    private String avatarUrl;

    /**
     * 【红线强制】认证状态：0-UNAUTH, 1-PENDING, 2-VERIFIED
     * 必须使用TINYINT类型存储，建立独立索引供权限拦截中间件高频查询
     */
    @Enumerated(EnumType.ORDINAL)
    @Column(name = "verification_status", nullable = false)
    private VerificationStatus verificationStatus;

    /**
     * 认证申请提交时间
     */
    private LocalDateTime verificationApplyTime;

    /**
     * 认证通过时间
     */
    private LocalDateTime verificationPassTime;

    /**
     * 身份证号AES-256加密
     */
    private String idCardEncrypted;

    /**
     * 房号AES-256加密
     */
    private String houseNumberEncrypted;

    /**
     * 认证材料存储路径（COS）
     */
    private String certificateUrl;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    /**
     * 判断用户是否可以进行写操作
     */
    public boolean canWrite() {
        return verificationStatus != null && verificationStatus.canWrite();
    }

    /**
     * 判断用户是否已认证
     */
    public boolean isVerified() {
        return verificationStatus == VerificationStatus.VERIFIED;
    }

    /**
     * 判断用户是否认证中
     */
    public boolean isPending() {
        return verificationStatus == VerificationStatus.PENDING;
    }
}
