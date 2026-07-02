package com.linliquan.service;

import com.linliquan.common.Result;
import com.linliquan.repository.FavoriteRepository;
import com.linliquan.repository.PostRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

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
    private EntityManager entityManager;

    /**
     * 获取用户统计数据
     */
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
