package com.linliquan.model.entity;

import lombok.Data;
import org.locationtech.jts.geom.Point;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

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

    private Long helperUserId;

    private Integer helpType;

    private BigDecimal rewardAmount;

    @Column(name = "location", columnDefinition = "geography(Point,4326)", nullable = false)
    private Point location;

    @Transient
    private Double distanceMeters;

    @Column(name = "status", nullable = false)
    private Integer status;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Transient
    private String title;

    @Transient
    private String content;

    @Transient
    private String userName;

    @Transient
    private String userAvatar;

    @Transient
    private String helperUserName;
}
