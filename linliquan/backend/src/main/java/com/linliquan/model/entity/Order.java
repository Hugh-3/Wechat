package com.linliquan.model.entity;

import lombok.Data;
import org.locationtech.jts.geom.Point;

import javax.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 互助任务实体
 */
@Data
@Entity
@Table(name = "orders", indexes = {
    @Index(name = "idx_order_status", columnList = "status"),
    @Index(name = "idx_order_user_id", columnList = "user_id"),
    @Index(name = "idx_order_helper_user_id", columnList = "helper_user_id")
})
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "post_id", nullable = false)
    private Long postId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    /**
     * 接单者ID
     */
    private Long helperUserId;

    /**
     * 互助类型：1-拼单团购, 2-代取代买, 3-生活求助, 4-技能交换
     */
    private Integer helpType;

    /**
     * 酬劳金额（0表示免费/AA）
     */
    private BigDecimal rewardAmount;

    private Double latitude;

    private Double longitude;

    /**
     * 距离（米）
     */
    private Double distanceMeters;

    /**
     * 状态：1-待接单, 2-进行中, 3-已完成, 4-已取消
     */
    @Column(name = "status", nullable = false)
    private Integer status;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // 关联字段
    private String title;
    private String content;
    private String userName;
    private String userAvatar;
    private String helperUserName;
}
