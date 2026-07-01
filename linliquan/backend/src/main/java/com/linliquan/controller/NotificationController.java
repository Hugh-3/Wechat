package com.linliquan.controller;

import com.linliquan.common.Result;
import com.linliquan.common.ResultCode;
import com.linliquan.model.entity.User;
import com.linliquan.service.NotificationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;

/**
 * 消息通知控制器
 *
 * 读操作（GET）三种用户状态均可访问；
 * 写操作（标记已读）仅校验登录态，无认证状态要求。
 */
@RestController
@RequestMapping("/v1/notifications")
public class NotificationController {

    @Autowired
    private NotificationService notificationService;

    /**
     * 获取通知列表（分页）
     */
    @GetMapping
    public Result<?> getNotificationList(
            HttpServletRequest request,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        User currentUser = getCurrentUser(request);
        if (currentUser == null) {
            return Result.fail(ResultCode.UNAUTHORIZED);
        }
        return notificationService.getNotificationList(currentUser.getId(), page, pageSize);
    }

    /**
     * 获取未读通知数量
     */
    @GetMapping("/unread-count")
    public Result<?> getUnreadCount(HttpServletRequest request) {
        User currentUser = getCurrentUser(request);
        if (currentUser == null) {
            return Result.fail(ResultCode.UNAUTHORIZED);
        }
        return notificationService.getUnreadCount(currentUser.getId());
    }

    /**
     * 标记单条通知为已读
     */
    @PostMapping("/{id}/read")
    public Result<Void> markAsRead(HttpServletRequest request, @PathVariable Long id) {
        User currentUser = getCurrentUser(request);
        if (currentUser == null) {
            return Result.fail(ResultCode.UNAUTHORIZED);
        }
        return notificationService.markAsRead(id, currentUser.getId());
    }

    /**
     * 标记全部通知为已读
     */
    @PostMapping("/read-all")
    public Result<Void> markAllAsRead(HttpServletRequest request) {
        User currentUser = getCurrentUser(request);
        if (currentUser == null) {
            return Result.fail(ResultCode.UNAUTHORIZED);
        }
        return notificationService.markAllAsRead(currentUser.getId());
    }

    /**
     * 从拦截器设置的请求属性中获取当前登录用户
     */
    private User getCurrentUser(HttpServletRequest request) {
        return (User) request.getAttribute("currentUser");
    }
}
