package com.linliquan.controller;

import com.linliquan.common.Result;
import com.linliquan.common.ResultCode;
import com.linliquan.model.entity.User;
import com.linliquan.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;

/**
 * 用户控制器
 */
@RestController
@RequestMapping("/v1/users")
public class UserController {

    @Autowired
    private UserService userService;

    /**
     * 【读操作】获取当前用户统计数据
     */
    @GetMapping("/stats")
    public Result<?> getMyStats(HttpServletRequest request) {
        User currentUser = (User) request.getAttribute("currentUser");
        if (currentUser == null) {
            return Result.fail(ResultCode.UNAUTHORIZED);
        }
        return userService.getMyStats(currentUser.getId());
    }

    /**
     * 【写操作】更新用户资料（昵称、头像）
     */
    @PutMapping("/profile")
    public Result<?> updateProfile(HttpServletRequest request, @RequestBody Map<String, Object> params) {
        User currentUser = (User) request.getAttribute("currentUser");
        if (currentUser == null) {
            return Result.fail(ResultCode.UNAUTHORIZED);
        }
        String nickname = params.get("nickname") != null ? params.get("nickname").toString() : null;
        String avatarUrl = params.get("avatarUrl") != null ? params.get("avatarUrl").toString() : null;
        return userService.updateProfile(currentUser.getId(), nickname, avatarUrl);
    }

    /**
     * 【读操作】获取当前用户获赞记录
     */
    @GetMapping("/likes")
    public Result<?> getMyLikes(
            HttpServletRequest request,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        User currentUser = (User) request.getAttribute("currentUser");
        if (currentUser == null) {
            return Result.fail(ResultCode.UNAUTHORIZED);
        }
        return userService.getMyLikes(currentUser.getId(), page, pageSize);
    }

    /**
     * 【读操作】获取当前用户评论记录
     */
    @GetMapping("/comments")
    public Result<?> getMyComments(
            HttpServletRequest request,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        User currentUser = (User) request.getAttribute("currentUser");
        if (currentUser == null) {
            return Result.fail(ResultCode.UNAUTHORIZED);
        }
        return userService.getMyComments(currentUser.getId(), page, pageSize);
    }
}
