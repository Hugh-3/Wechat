package com.linliquan.model.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * 举报实体
 *
 * target_type:
 *  1 - 帖子
 *  2 - 评论
 *  3 - 用户
 *
 * reason:
 *  1 - 垃圾广告
 *  2 - 违法违规
 *  3 - 色情低俗
 *  4 - 侮辱谩骂
 *  5 - 其他
 *
 * status:
 *  0 - 待处理
 *  1 - 已处理(有效)
 *  2 - 已处理(无效)
 */
@Entity
@Table(name = "reports", indexes = {
    @Index(name = "idx_report_reporter_id", columnList = "reporter_id"),
    @Index(name = "idx_report_target", columnList = "target_type,target_id"),
    @Index(name = "idx_report_status", columnList = "status"),
    @Index(name = "idx_report_created_at", columnList = "created_at")
})
public class Report {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "reporter_id", nullable = false)
    private Long reporterId;

    /** 举报目标类型：1-帖子, 2-评论, 3-用户 */
    @Column(name = "target_type", nullable = false)
    private Short targetType;

    @Column(name = "target_id", nullable = false)
    private Long targetId;

    /** 举报原因：1-垃圾广告, 2-违法违规, 3-色情低俗, 4-侮辱谩骂, 5-其他 */
    @Column(nullable = false)
    private Short reason;

    @Column(columnDefinition = "TEXT")
    private String description;

    /** 状态：0-待处理, 1-已处理(有效), 2-已处理(无效) */
    @Column(nullable = false)
    private Short status;

    @Column(name = "handle_remark", length = 255)
    private String handleRemark;

    @Column(name = "handled_by")
    private Long handledBy;

    @Column(name = "handled_at")
    private LocalDateTime handledAt;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    // ==================== 关联展示字段（@Transient） ====================

    @Transient
    private String reporterName;

    @Transient
    private String reporterAvatar;

    @Transient
    private String targetTitle;

    @Transient
    private String targetContent;

    @Transient
    private String handlerName;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getReporterId() {
        return reporterId;
    }

    public void setReporterId(Long reporterId) {
        this.reporterId = reporterId;
    }

    public Short getTargetType() {
        return targetType;
    }

    public void setTargetType(Short targetType) {
        this.targetType = targetType;
    }

    public Long getTargetId() {
        return targetId;
    }

    public void setTargetId(Long targetId) {
        this.targetId = targetId;
    }

    public Short getReason() {
        return reason;
    }

    public void setReason(Short reason) {
        this.reason = reason;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Short getStatus() {
        return status;
    }

    public void setStatus(Short status) {
        this.status = status;
    }

    public String getHandleRemark() {
        return handleRemark;
    }

    public void setHandleRemark(String handleRemark) {
        this.handleRemark = handleRemark;
    }

    public Long getHandledBy() {
        return handledBy;
    }

    public void setHandledBy(Long handledBy) {
        this.handledBy = handledBy;
    }

    public LocalDateTime getHandledAt() {
        return handledAt;
    }

    public void setHandledAt(LocalDateTime handledAt) {
        this.handledAt = handledAt;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public String getReporterName() {
        return reporterName;
    }

    public void setReporterName(String reporterName) {
        this.reporterName = reporterName;
    }

    public String getReporterAvatar() {
        return reporterAvatar;
    }

    public void setReporterAvatar(String reporterAvatar) {
        this.reporterAvatar = reporterAvatar;
    }

    public String getTargetTitle() {
        return targetTitle;
    }

    public void setTargetTitle(String targetTitle) {
        this.targetTitle = targetTitle;
    }

    public String getTargetContent() {
        return targetContent;
    }

    public void setTargetContent(String targetContent) {
        this.targetContent = targetContent;
    }

    public String getHandlerName() {
        return handlerName;
    }

    public void setHandlerName(String handlerName) {
        this.handlerName = handlerName;
    }
}
