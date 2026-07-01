package com.linliquan.model.entity;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 互助任务实体
 */
@Data
public class Order {

    private Long id;

    private Long postId;

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
    private Integer status;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    // 关联字段
    private String title;
    private String content;
    private String userName;
    private String userAvatar;
    private String helperUserName;
}
