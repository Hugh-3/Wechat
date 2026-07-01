package com.linliquan.service;

import com.linliquan.common.Result;
import com.linliquan.common.ResultCode;
import com.linliquan.model.entity.Comment;
import com.linliquan.model.entity.Post;
import com.linliquan.model.entity.User;
import com.linliquan.repository.CommentRepository;
import com.linliquan.repository.PostRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * 评论服务
 * 【红线强制】所有写操作接口必须校验用户认证状态
 */
@Service
public class CommentService {

    @Autowired
    private CommentRepository commentRepository;

    @Autowired
    private PostRepository postRepository;

    @Autowired
    private CacheService cacheService;

    @Autowired
    private PointService pointService;

    @Autowired
    private NotificationService notificationService;

    /**
     * 发表评论
     * 【红线强制】状态校验
     */
    @Transactional
    public Result<Comment> createComment(User user, Long postId, String content, Long parentId, Long replyToUserId) {
        // 【红线强制】状态校验
        if (!user.isVerified()) {
            if (user.isPending()) {
                return Result.fail(ResultCode.FORBIDDEN_PENDING_VERIFICATION);
            }
            return Result.fail(ResultCode.FORBIDDEN_UNVERIFIED);
        }

        // 参数校验
        if (content == null || content.trim().isEmpty() || content.length() > 500) {
            return Result.fail(ResultCode.BAD_REQUEST);
        }

        // 检查帖子是否存在
        Post post = postRepository.findById(postId).orElse(null);
        if (post == null || post.getStatus() != 1) {
            return Result.fail(ResultCode.NOT_FOUND);
        }

        // 如果是回复，检查父评论是否存在
        if (parentId != null) {
            Comment parentComment = commentRepository.findById(parentId).orElse(null);
            if (parentComment == null || parentComment.getStatus() != 1) {
                return Result.fail(ResultCode.BAD_REQUEST);
            }
        }

        // 构建评论实体
        Comment comment = new Comment();
        comment.setPostId(postId);
        comment.setUserId(user.getId());
        comment.setParentId(parentId);
        comment.setReplyToUserId(replyToUserId);
        comment.setContent(content.trim());
        comment.setLikeCount(0);
        comment.setStatus(1);
        comment.setCreatedAt(LocalDateTime.now());
        comment.setUpdatedAt(LocalDateTime.now());

        // 保存评论
        Comment savedComment = commentRepository.save(comment);

        // 更新帖子评论数
        post.setCommentCount(post.getCommentCount() + 1);
        postRepository.save(post);

        // 清除缓存
        cacheService.invalidatePostCommentsCache(postId);

        // 通知帖子作者收到新评论
        notificationService.sendCommentNotification(post.getUserId(), user.getId(), post.getTitle());

        // 积分奖励：帖子作者收到评论 +3（不给自己评论加分）
        if (!post.getUserId().equals(user.getId())) {
            pointService.addPoints(post.getUserId(), PointService.COMMENT_RECEIVED,
                    "comment_received", "post", postId, "收到评论");
        }

        return Result.success(savedComment);
    }

    /**
     * 获取评论列表
     */
    public Result<Map<String, Object>> getCommentList(Long postId, Integer page, Integer pageSize) {
        // 检查帖子是否存在
        Post post = postRepository.findById(postId).orElse(null);
        if (post == null || post.getStatus() != 1) {
            return Result.fail(ResultCode.NOT_FOUND);
        }

        Pageable pageable = PageRequest.of(page - 1, pageSize);
        Page<Comment> commentPage = commentRepository.findByPostIdAndStatusOrderByCreatedAtDesc(postId, 1, pageable);

        Map<String, Object> result = new HashMap<>();
        result.put("list", commentPage.getContent());
        result.put("total", commentPage.getTotalElements());
        result.put("page", page);
        result.put("pageSize", pageSize);
        result.put("totalPages", commentPage.getTotalPages());

        return Result.success(result);
    }

    /**
     * 删除评论
     */
    @Transactional
    public Result<Void> deleteComment(User user, Long commentId) {
        Comment comment = commentRepository.findById(commentId).orElse(null);
        if (comment == null || comment.getStatus() != 1) {
            return Result.fail(ResultCode.NOT_FOUND);
        }

        // 只能删除自己的评论
        if (!comment.getUserId().equals(user.getId())) {
            return Result.fail(ResultCode.FORBIDDEN_UNVERIFIED);
        }

        // 软删除
        comment.setStatus(2);
        comment.setUpdatedAt(LocalDateTime.now());
        commentRepository.save(comment);

        // 更新帖子评论数
        Post post = postRepository.findById(comment.getPostId()).orElse(null);
        if (post != null) {
            post.setCommentCount(Math.max(0, post.getCommentCount() - 1));
            postRepository.save(post);
        }

        // 清除缓存
        cacheService.invalidatePostCommentsCache(comment.getPostId());

        return Result.success(null);
    }

    /**
     * 点赞评论
     */
    @Transactional
    public Result<Void> likeComment(Long commentId, User user) {
        // 【红线强制】状态校验
        if (!user.isVerified()) {
            return Result.fail(ResultCode.FORBIDDEN_UNVERIFIED);
        }

        Comment comment = commentRepository.findById(commentId).orElse(null);
        if (comment == null || comment.getStatus() != 1) {
            return Result.fail(ResultCode.NOT_FOUND);
        }

        comment.setLikeCount(comment.getLikeCount() + 1);
        commentRepository.save(comment);

        return Result.success(null);
    }
}
