package com.linliquan.model.entity;

import lombok.Data;
import org.locationtech.jts.geom.Point;

import javax.persistence.*;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 帖子实体（信息广场 + 邻里互助）
 */
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

    /**
     * 帖子类型：1-信息广场, 2-邻里互助
     */
    private Integer postType;

    private String title;

    private String content;

    /**
     * 图片URL列表
     */
    private List<String> images;

    /**
     * 纬度（用于距离计算）
     */
    @Column(name = "latitude")
    private Double latitude;

    /**
     * 经度（用于距离计算）
     */
    @Column(name = "longitude")
    private Double longitude;

    private Integer likeCount;

    private Integer commentCount;

    private Integer viewCount;

    /**
     * 状态：1-正常, 2-已删除, 3-被屏蔽
     */
    @Column(name = "status", nullable = false)
    private Integer status;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // 以下为关联查询字段
    private String userName;
    private String userAvatar;
    private String distance;  // 格式化距离字符串
    private Double distanceMeters;  // 距离（米）
}
