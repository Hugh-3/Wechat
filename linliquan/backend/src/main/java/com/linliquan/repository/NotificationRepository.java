package com.linliquan.repository;

import com.linliquan.model.entity.Notification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 消息通知Repository
 */
@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {

    /**
     * 分页查询用户的通知列表（按创建时间倒序）
     */
    Page<Notification> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

    /**
     * 查询用户指定已读/未读状态的通知列表
     */
    List<Notification> findByUserIdAndIsReadOrderByCreatedAtDesc(Long userId, Integer isRead);

    /**
     * 统计用户未读通知数量
     */
    long countByUserIdAndIsRead(Long userId, Integer isRead);

    /**
     * 标记用户所有未读通知为已读
     */
    @Modifying
    @Query("UPDATE Notification n SET n.isRead = 1 WHERE n.userId = :userId AND n.isRead = 0")
    int markAllAsRead(@Param("userId") Long userId);

    /**
     * 标记单条通知为已读
     */
    @Modifying
    @Query("UPDATE Notification n SET n.isRead = 1 WHERE n.id = :id AND n.userId = :userId")
    int markAsRead(@Param("id") Long id, @Param("userId") Long userId);
}
