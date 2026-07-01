package com.linliquan.controller;

import com.linliquan.annotation.RateLimit;
import com.linliquan.common.Result;
import com.linliquan.model.entity.Post;
import com.linliquan.model.entity.User;
import com.linliquan.service.PostService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.Map;

/**
 * 帖子控制器
 * 【红线强制】所有写接口已接入@VerifiedOnly拦截器
 */
@RestController
@RequestMapping("/v1/posts")
public class PostController {

    @Autowired
    private PostService postService;

    /**
     * 【写操作】发布动态
     * 【红线强制】已接入权限拦截器，非VERIFIED用户返回403
     */
    @PostMapping
    @RateLimit(maxRequests = 20, windowSeconds = 60)
    public Result<Post> createPost(HttpServletRequest request, @RequestBody @Valid Map<String, Object> params) {
        User currentUser = (User) request.getAttribute("currentUser");
        return postService.createPost(currentUser, params);
    }

    /**
     * 【读操作】获取帖子列表
     * 三种状态均可访问
     */
    @GetMapping
    public Result<?> getPostList(
            @RequestParam(required = false) Integer type,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        return postService.getPostList(type, page, pageSize);
    }

    /**
     * 【读操作】获取附近互助任务
     * 三种状态均可访问
     */
    @GetMapping("/nearby")
    public Result<?> getNearbyOrders(
            @RequestParam Double lat,
            @RequestParam Double lng,
            @RequestParam(defaultValue = "5") Double radius,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        return postService.getNearbyOrders(lat, lng, radius, page, pageSize);
    }

    /**
     * 【读操作】获取帖子详情
     * 三种状态均可访问
     */
    @GetMapping("/{id}")
    public Result<Post> getPostDetail(@PathVariable Long id) {
        // 实现详情查询
        return Result.success(null);
    }

    /**
     * 【写操作】点赞/取消点赞
     * 【红线强制】已接入权限拦截器
     */
    @PostMapping("/{id}/like")
    public Result<?> toggleLike(HttpServletRequest request, @PathVariable Long id, @RequestBody Map<String, Object> params) {
        User currentUser = (User) request.getAttribute("currentUser");
        boolean liked = params.get("liked") != null && Boolean.parseBoolean(params.get("liked").toString());
        return postService.toggleLike(id, currentUser, liked);
    }
}
