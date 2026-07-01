package com.linliquan.model.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * 用户积分账户实体
 */
@Entity
@Table(name = "user_points")
public class UserPoints {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false, unique = true)
    private Long userId;

    @Column(name = "total_points", nullable = false)
    private Integer totalPoints;

    @Column(name = "total_earned", nullable = false)
    private Integer totalEarned;

    @Column(name = "total_consumed", nullable = false)
    private Integer totalConsumed;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

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

    public Integer getTotalPoints() {
        return totalPoints;
    }

    public void setTotalPoints(Integer totalPoints) {
        this.totalPoints = totalPoints;
    }

    public Integer getTotalEarned() {
        return totalEarned;
    }

    public void setTotalEarned(Integer totalEarned) {
        this.totalEarned = totalEarned;
    }

    public Integer getTotalConsumed() {
        return totalConsumed;
    }

    public void setTotalConsumed(Integer totalConsumed) {
        this.totalConsumed = totalConsumed;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
