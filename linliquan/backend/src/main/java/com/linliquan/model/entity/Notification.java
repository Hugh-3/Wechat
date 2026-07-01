package com.linliquan.model.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * 消息通知实体
 * type: 1-系统消息, 2-评论提醒, 3-接单通知, 4-点赞通知, 5-认证通知
 * isRead: 0-未读, 1-已读
 */
@Entity
@Table(name = "notifications", indexes = {
    @Index(name = "idx_notifications_user_read", columnList = "user_id, is_read, created_at"),
    @Index(name = "idx_notifications_user_created", columnList = "user_id, created_at")
})
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    /**
     * 1-系统消息, 2-评论提醒, 3-接单通知, 4-点赞通知, 5-认证通知
     */
    @Column(nullable = false)
    private Integer type;

    @Column(nullable = false, length = 100)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String content;

    /**
     * 关联资源类型：post/comment/order/user
     */
    @Column(name = "related_type", length = 50)
    private String relatedType;

    @Column(name = "related_id")
    private Long relatedId;

    /**
     * 0-未读, 1-已读
     */
    @Column(name = "is_read", nullable = false)
    private Integer isRead;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Integer getType() {
        return type;
    }

    public void setType(Integer type) {
        this.type = type;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getRelatedType() {
        return relatedType;
    }

    public void setRelatedType(String relatedType) {
        this.relatedType = relatedType;
    }

    public Long getRelatedId() {
        return relatedId;
    }

    public void setRelatedId(Long relatedId) {
        this.relatedId = relatedId;
    }

    public Integer getIsRead() {
        return isRead;
    }

    public void setIsRead(Integer isRead) {
        this.isRead = isRead;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
