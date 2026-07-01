package com.linliquan.model.entity;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 评价实体
 * 互助任务完成后，双方互相评价
 *
 * role:
 *  1 - 发布者评价帮助者
 *  2 - 帮助者评价发布者
 *
 * rating: 1-5 星
 */
@Entity
@Table(name = "reviews", indexes = {
    @Index(name = "idx_review_order_id", columnList = "order_id"),
    @Index(name = "idx_review_reviewer_id", columnList = "reviewer_id"),
    @Index(name = "idx_review_reviewee_id", columnList = "reviewee_id")
}, uniqueConstraints = {
    @UniqueConstraint(name = "uk_order_reviewer", columnNames = {"order_id", "reviewer_id"})
})
public class Review {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "order_id", nullable = false)
    private Long orderId;

    @Column(name = "reviewer_id", nullable = false)
    private Long reviewerId;

    @Column(name = "reviewee_id", nullable = false)
    private Long revieweeId;

    @Column(nullable = false)
    private Short role;

    @Column(nullable = false)
    private Short rating;

    private String content;

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "images", columnDefinition = "text[]")
    private List<String> images;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    // ==================== 关联展示字段（@Transient） ====================

    @Transient
    private String reviewerName;

    @Transient
    private String reviewerAvatar;

    @Transient
    private String revieweeName;

    @Transient
    private String revieweeAvatar;

    @Transient
    private String orderTitle;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getOrderId() {
        return orderId;
    }

    public void setOrderId(Long orderId) {
        this.orderId = orderId;
    }

    public Long getReviewerId() {
        return reviewerId;
    }

    public void setReviewerId(Long reviewerId) {
        this.reviewerId = reviewerId;
    }

    public Long getRevieweeId() {
        return revieweeId;
    }

    public void setRevieweeId(Long revieweeId) {
        this.revieweeId = revieweeId;
    }

    public Short getRole() {
        return role;
    }

    public void setRole(Short role) {
        this.role = role;
    }

    public Short getRating() {
        return rating;
    }

    public void setRating(Short rating) {
        this.rating = rating;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public List<String> getImages() {
        return images;
    }

    public void setImages(List<String> images) {
        this.images = images;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public String getReviewerName() {
        return reviewerName;
    }

    public void setReviewerName(String reviewerName) {
        this.reviewerName = reviewerName;
    }

    public String getReviewerAvatar() {
        return reviewerAvatar;
    }

    public void setReviewerAvatar(String reviewerAvatar) {
        this.reviewerAvatar = reviewerAvatar;
    }

    public String getRevieweeName() {
        return revieweeName;
    }

    public void setRevieweeName(String revieweeName) {
        this.revieweeName = revieweeName;
    }

    public String getRevieweeAvatar() {
        return revieweeAvatar;
    }

    public void setRevieweeAvatar(String revieweeAvatar) {
        this.revieweeAvatar = revieweeAvatar;
    }

    public String getOrderTitle() {
        return orderTitle;
    }

    public void setOrderTitle(String orderTitle) {
        this.orderTitle = orderTitle;
    }
}
