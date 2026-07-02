package com.linliquan.service;

import com.linliquan.common.Result;
import com.linliquan.common.ResultCode;
import com.linliquan.repository.FavoriteRepository;
import com.linliquan.repository.PostRepository;
import com.linliquan.repository.UserRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 用户服务
 */
@Service
public class UserService {

    @Autowired
    private PostRepository postRepository;

    @Autowired
    private FavoriteRepository favoriteRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EntityManager entityManager;

    /**
     * 获取用户统计数据
     */
    /**
     * 更新用户资料（昵称、头像）
     */
    @org.springframework.transaction.annotation.Transactional
    public Result<Map<String, Object>> updateProfile(Long userId, String nickname, String avatarUrl) {
        com.linliquan.model.entity.User user = userRepository.findById(userId).orElse(null);
        if (user == null) {
            return Result.fail(ResultCode.NOT_FOUND);
        }

        if (nickname != null && !nickname.trim().isEmpty()) {
            if (nickname.length() > 20) {
                return Result.fail(ResultCode.BAD_REQUEST.getCode(), "昵称不能超过20个字符");
            }
            user.setNickname(nickname.trim());
        }

        if (avatarUrl != null && !avatarUrl.isEmpty()) {
            user.setAvatarUrl(avatarUrl);
        }

        user.setUpdatedAt(java.time.LocalDateTime.now());
        userRepository.save(user);

        Map<String, Object> result = new HashMap<>();
        result.put("id", user.getId());
        result.put("nickname", user.getNickname());
        result.put("avatarUrl", user.getAvatarUrl());
        return Result.success(result);
    }

    /**
     * 获取用户获赞记录
     */
    @SuppressWarnings("unchecked")
    public Result<Map<String, Object>> getMyLikes(Long userId, Integer page, Integer pageSize) {
        String sql = """
            SELECT
                pl.id, pl.post_id, pl.user_id as liker_user_id, pl.created_at,
                p.title, p.content,
                u.nickname as liker_name, u.avatar_url as liker_avatar
            FROM post_likes pl
            INNER JOIN posts p ON pl.post_id = p.id
            INNER JOIN users u ON pl.user_id = u.id
            WHERE p.user_id = :userId AND p.status = 1
            ORDER BY pl.created_at DESC
            LIMIT :limit OFFSET :offset
            """;

        Query query = entityManager.createNativeQuery(sql);
        query.setParameter("userId", userId);
        query.setParameter("limit", pageSize);
        query.setParameter("offset", (page - 1) * pageSize);

        List<Object[]> rows = query.getResultList();
        List<Map<String, Object>> list = new ArrayList<>();
        for (Object[] row : rows) {
            Map<String, Object> record = new HashMap<>();
            record.put("id", ((Number) row[0]).longValue());
            record.put("postId", ((Number) row[1]).longValue());
            record.put("likerUserId", ((Number) row[2]).longValue());
            record.put("createdAt", row[3]);
            record.put("title", row[4]);
            record.put("content", row[5]);
            record.put("likerName", row[6]);
            record.put("likerAvatar", row[7]);
            list.add(record);
        }

        // 获赞总数
        Query countQuery = entityManager.createNativeQuery(
            "SELECT COUNT(*) FROM post_likes pl INNER JOIN posts p ON pl.post_id = p.id WHERE p.user_id = :userId AND p.status = 1"
        );
        countQuery.setParameter("userId", userId);
        long total = ((Number) countQuery.getSingleResult()).longValue();

        Map<String, Object> result = new HashMap<>();
        result.put("list", list);
        result.put("total", total);
        result.put("page", page);
        result.put("pageSize", pageSize);

        return Result.success(result);
    }

    /**
     * 获取用户评论记录
     */
    @SuppressWarnings("unchecked")
    public Result<Map<String, Object>> getMyComments(Long userId, Integer page, Integer pageSize) {
        String sql = """
            SELECT
                c.id, c.post_id, c.content, c.created_at,
                p.title as post_title
            FROM comments c
            INNER JOIN posts p ON c.post_id = p.id
            WHERE c.user_id = :userId AND c.status = 1
            ORDER BY c.created_at DESC
            LIMIT :limit OFFSET :offset
            """;

        Query query = entityManager.createNativeQuery(sql);
        query.setParameter("userId", userId);
        query.setParameter("limit", pageSize);
        query.setParameter("offset", (page - 1) * pageSize);

        List<Object[]> rows = query.getResultList();
        List<Map<String, Object>> list = new ArrayList<>();
        for (Object[] row : rows) {
            Map<String, Object> record = new HashMap<>();
            record.put("id", ((Number) row[0]).longValue());
            record.put("postId", ((Number) row[1]).longValue());
            record.put("content", row[2]);
            record.put("createdAt", row[3]);
            record.put("postTitle", row[4]);
            list.add(record);
        }

        // 评论总数
        Query countQuery = entityManager.createNativeQuery(
            "SELECT COUNT(*) FROM comments WHERE user_id = :userId AND status = 1"
        );
        countQuery.setParameter("userId", userId);
        long total = ((Number) countQuery.getSingleResult()).longValue();

        Map<String, Object> result = new HashMap<>();
        result.put("list", list);
        result.put("total", total);
        result.put("page", page);
        result.put("pageSize", pageSize);

        return Result.success(result);
    }

    public Result<Map<String, Object>> getMyStats(Long userId) {
        Map<String, Object> stats = new HashMap<>();

        // 发布帖子数
        long postCount = postRepository.countByUserIdAndStatus(userId, 1);
        stats.put("postCount", postCount);

        // 获赞数（用户所有帖子的点赞数之和）
        Query likeQuery = entityManager.createNativeQuery(
            "SELECT COALESCE(SUM(like_count), 0) FROM posts WHERE user_id = :userId AND status = 1"
        );
        likeQuery.setParameter("userId", userId);
        Object likeResult = likeQuery.getSingleResult();
        stats.put("likeCount", likeResult != null ? ((Number) likeResult).longValue() : 0L);

        // 评论数（用户发表的评论数）
        Query commentQuery = entityManager.createNativeQuery(
            "SELECT COUNT(*) FROM comments WHERE user_id = :userId AND status = 1"
        );
        commentQuery.setParameter("userId", userId);
        Object commentResult = commentQuery.getSingleResult();
        stats.put("commentCount", commentResult != null ? ((Number) commentResult).longValue() : 0L);

        // 收藏数
        long favoriteCount = favoriteRepository.countByUserId(userId);
        stats.put("favoriteCount", favoriteCount);

        // 互助任务数（作为发布者或帮助者参与的订单数）
        Query orderQuery = entityManager.createNativeQuery(
            "SELECT COUNT(*) FROM orders WHERE user_id = :userId OR helper_user_id = :userId"
        );
        orderQuery.setParameter("userId", userId);
        Object orderResult = orderQuery.getSingleResult();
        stats.put("orderCount", orderResult != null ? ((Number) orderResult).longValue() : 0L);

        return Result.success(stats);
    }
}
