package com.linliquan.service;

import com.linliquan.common.Result;
import com.linliquan.common.ResultCode;
import com.linliquan.model.entity.Notification;
import com.linliquan.repository.NotificationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * 消息通知服务
 *
 * 通知类型 type：
 *  1-系统消息, 2-评论提醒, 3-接单通知, 4-点赞通知, 5-认证通知
 *
 * 实时推送：通过 SimpMessagingTemplate 推送到 /topic/notifications/{userId}，
 * 已订阅该目的地的客户端可即时收到通知。
 */
@Service
public class NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);

    /** 通知类型常量 */
    public static final int TYPE_SYSTEM = 1;
    public static final int TYPE_COMMENT = 2;
    public static final int TYPE_ORDER = 3;
    public static final int TYPE_LIKE = 4;
    public static final int TYPE_VERIFICATION = 5;

    /** WebSocket 推送目的地前缀 */
    private static final String WS_DESTINATION_PREFIX = "/topic/notifications/";

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    /**
     * 发送通知（核心方法）
     * 持久化到数据库，并通过 WebSocket 实时推送给目标用户
     *
     * @param userId       接收通知的用户ID
     * @param type         通知类型（1-5）
     * @param title        通知标题
     * @param content      通知内容
     * @param relatedType  关联资源类型：post/comment/order/user
     * @param relatedId    关联资源ID
     * @return 持久化后的通知实体
     */
    @Transactional
    public Notification sendNotification(Long userId, int type, String title, String content,
                                        String relatedType, Long relatedId) {
        Notification notification = new Notification();
        notification.setUserId(userId);
        notification.setType(type);
        notification.setTitle(title);
        notification.setContent(content);
        notification.setRelatedType(relatedType);
        notification.setRelatedId(relatedId);
        notification.setIsRead(0);
        notification.setCreatedAt(LocalDateTime.now());

        Notification saved = notificationRepository.save(notification);

        // 通过 WebSocket 实时推送
        pushNotificationViaWebSocket(userId, saved);

        return saved;
    }

    /**
     * 获取通知列表（分页）
     */
    public Result<Map<String, Object>> getNotificationList(Long userId, int page, int pageSize) {
        Pageable pageable = PageRequest.of(Math.max(page - 1, 0), pageSize);
        Page<Notification> notificationPage = notificationRepository
                .findByUserIdOrderByCreatedAtDesc(userId, pageable);

        Map<String, Object> result = new HashMap<>();
        result.put("list", notificationPage.getContent());
        result.put("total", notificationPage.getTotalElements());
        result.put("page", page);
        result.put("pageSize", pageSize);
        result.put("totalPages", notificationPage.getTotalPages());

        return Result.success(result);
    }

    /**
     * 获取未读通知数量
     */
    public Result<Map<String, Object>> getUnreadCount(Long userId) {
        long unreadCount = notificationRepository.countByUserIdAndIsRead(userId, 0);
        Map<String, Object> result = new HashMap<>();
        result.put("unreadCount", unreadCount);
        return Result.success(result);
    }

    /**
     * 标记单条通知为已读
     */
    @Transactional
    public Result<Void> markAsRead(Long notificationId, Long userId) {
        Notification notification = notificationRepository.findById(notificationId).orElse(null);
        if (notification == null || !notification.getUserId().equals(userId)) {
            return Result.fail(ResultCode.NOT_FOUND);
        }
        if (notification.getIsRead() != null && notification.getIsRead() == 1) {
            return Result.success(null);
        }
        notificationRepository.markAsRead(notificationId, userId);
        return Result.success(null);
    }

    /**
     * 标记用户所有未读通知为已读
     */
    @Transactional
    public Result<Void> markAllAsRead(Long userId) {
        notificationRepository.markAllAsRead(userId);
        return Result.success(null);
    }

    /**
     * 发送系统通知（便捷方法）
     */
    public Notification sendSystemNotification(Long userId, String title, String content) {
        return sendNotification(userId, TYPE_SYSTEM, title, content, "user", userId);
    }

    /**
     * 评论提醒：通知帖子作者收到了新评论
     *
     * @param postOwnerId  帖子作者用户ID
     * @param commenterId  评论者用户ID
     * @param postTitle    帖子标题
     */
    public void sendCommentNotification(Long postOwnerId, Long commenterId, String postTitle) {
        // 自己评论自己的帖子不发通知
        if (postOwnerId == null || postOwnerId.equals(commenterId)) {
            return;
        }
        String title = "收到新评论";
        String content = "您的帖子「" + truncate(postTitle, 20) + "」收到了新评论";
        sendNotification(postOwnerId, TYPE_COMMENT, title, content, "post", null);
    }

    /**
     * 接单通知：通知任务发布者有人接单
     *
     * @param ownerId     任务发布者用户ID
     * @param helperId    接单者用户ID
     * @param orderTitle  任务标题
     */
    public void sendOrderNotification(Long ownerId, Long helperId, String orderTitle) {
        if (ownerId == null || ownerId.equals(helperId)) {
            return;
        }
        String title = "您的互助任务被接单";
        String content = "您的任务「" + truncate(orderTitle, 20) + "」已被接单";
        sendNotification(ownerId, TYPE_ORDER, title, content, "order", null);
    }

    /**
     * 通过 WebSocket 推送通知给指定用户
     */
    private void pushNotificationViaWebSocket(Long userId, Notification notification) {
        try {
            messagingTemplate.convertAndSend(WS_DESTINATION_PREFIX + userId, notification);
        } catch (Exception e) {
            // 推送失败不影响主流程，仅记录日志
            log.warn("WebSocket推送通知失败, userId={}, notificationId={}", userId, notification.getId(), e);
        }
    }

    /**
     * 截断字符串，超长部分以省略号表示
     */
    private String truncate(String text, int maxLen) {
        if (text == null) {
            return "";
        }
        if (text.length() <= maxLen) {
            return text;
        }
        return text.substring(0, maxLen) + "...";
    }
}
