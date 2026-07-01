package com.linliquan.controller;

import com.linliquan.annotation.RateLimit;
import com.linliquan.common.Result;
import com.linliquan.model.entity.Comment;
import com.linliquan.model.entity.User;
import com.linliquan.service.CommentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;

/**
 * 评论控制器
 * 【红线强制】所有写接口已接入权限拦截器
 */
@RestController
@RequestMapping("/v1/comments")
public class CommentController {

    @Autowired
    private CommentService commentService;

    /**
     * 【读操作】获取评论列表
     * 三种状态均可访问
     */
    @GetMapping
    public Result<?> getCommentList(
            @RequestParam Long postId,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "20") Integer pageSize) {
        return commentService.getCommentList(postId, page, pageSize);
    }

    /**
     * 【写操作】发表评论
     * 【红线强制】已接入权限拦截器
     */
    @PostMapping
    @RateLimit(maxRequests = 30, windowSeconds = 60)
    public Result<Comment> createComment(HttpServletRequest request, @RequestBody Map<String, Object> params) {
        User currentUser = (User) request.getAttribute("currentUser");
        Long postId = params.get("postId") != null ? Long.valueOf(params.get("postId").toString()) : null;
        String content = (String) params.get("content");
        Long parentId = params.get("parentId") != null ? Long.valueOf(params.get("parentId").toString()) : null;
        Long replyToUserId = params.get("replyToUserId") != null ? Long.valueOf(params.get("replyToUserId").toString()) : null;
        return commentService.createComment(currentUser, postId, content, parentId, replyToUserId);
    }

    /**
     * 【写操作】删除评论
     * 【红线强制】已接入权限拦截器
     */
    @DeleteMapping("/{id}")
    public Result<Void> deleteComment(HttpServletRequest request, @PathVariable Long id) {
        User currentUser = (User) request.getAttribute("currentUser");
        return commentService.deleteComment(currentUser, id);
    }

    /**
     * 【写操作】点赞评论
     * 【红线强制】已接入权限拦截器
     */
    @PostMapping("/{id}/like")
    public Result<Void> likeComment(HttpServletRequest request, @PathVariable Long id) {
        User currentUser = (User) request.getAttribute("currentUser");
        return commentService.likeComment(id, currentUser);
    }
}
