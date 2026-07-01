package com.linliquan.model.entity;

import lombok.Data;
import org.locationtech.jts.geom.Point;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Entity
@Table(name = "posts", indexes = {
    @Index(name = "idx_post_type_status", columnList = "post_type, status"),
    @Index(name = "idx_user_id", columnList = "user_id"),
    @Index(name = "idx_created_at", columnList = "created_at")
})
public class Post {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    private Integer postType;

    private String title;

    private String content;

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "images", columnDefinition = "text[]")
    private List<String> images;

    @Column(name = "location", columnDefinition = "geography(Point,4326)")
    private Point location;

    private Integer likeCount;

    private Integer commentCount;

    private Integer viewCount;

    @Column(name = "status", nullable = false)
    private Integer status;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Transient
    private String userName;

    @Transient
    private String userAvatar;

    @Transient
    private String distance;

    @Transient
    private Double distanceMeters;
}
