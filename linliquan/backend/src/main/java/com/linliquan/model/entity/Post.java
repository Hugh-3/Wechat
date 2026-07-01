package com.linliquan.model.entity;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 帖子实体（信息广场 + 邻里互助）
 */
@Data
public class Post {

    private Long id;

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
    private Double latitude;

    /**
     * 经度（用于距离计算）
     */
    private Double longitude;

    private Integer likeCount;

    private Integer commentCount;

    private Integer viewCount;

    /**
     * 状态：1-正常, 2-已删除, 3-被屏蔽
     */
    private Integer status;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    // 以下为关联查询字段
    private String userName;
    private String userAvatar;
    private String distance;  // 格式化距离字符串
    private Double distanceMeters;  // 距离（米）
}
