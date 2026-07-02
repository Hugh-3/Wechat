package com.linliquan.service;

import com.linliquan.common.Result;
import com.linliquan.model.entity.Favorite;
import com.linliquan.repository.FavoriteRepository;
import com.linliquan.repository.PostRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import java.time.LocalDateTime;
import java.util.*;

/**
 * 收藏服务
 */
@Service
public class FavoriteService {

    @Autowired
    private FavoriteRepository favoriteRepository;

    @Autowired
    private PostRepository postRepository;

    @Autowired
    private EntityManager entityManager;

    /**
     * 切换收藏状态
     */
    public Result<Map<String, Object>> toggleFavorite(Long postId, Long userId, Boolean favorited) {
        if (!postRepository.existsById(postId)) {
            return Result.fail(404, "帖子不存在");
        }

        boolean exists = favoriteRepository.existsByPostIdAndUserId(postId, userId);

        if (favorited && !exists) {
            Favorite favorite = new Favorite();
            favorite.setPostId(postId);
            favorite.setUserId(userId);
            favorite.setCreatedAt(LocalDateTime.now());
            favoriteRepository.save(favorite);
        } else if (!favorited && exists) {
            favoriteRepository.deleteByPostIdAndUserId(postId, userId);
        }

        Map<String, Object> result = new HashMap<>();
        result.put("favorited", favorited);
        result.put("favoriteCount", favoriteRepository.countByPostId(postId));
        return Result.success(result);
    }

    /**
     * 获取用户收藏列表（联表查询帖子信息）
     */
    @SuppressWarnings("unchecked")
    public Result<Map<String, Object>> getMyFavorites(Long userId, Integer page, Integer pageSize) {
        String sql = """
            SELECT
                p.id, p.title, p.content, p.images, p.like_count,
                p.comment_count, p.view_count, p.post_type, p.status,
                p.created_at, f.created_at as favorited_at,
                u.nickname as user_name, u.avatar_url as user_avatar
            FROM favorites f
            INNER JOIN posts p ON f.post_id = p.id
            INNER JOIN users u ON p.user_id = u.id
            WHERE f.user_id = :userId AND p.status = 1
            ORDER BY f.created_at DESC
            LIMIT :limit OFFSET :offset
            """;

        Query query = entityManager.createNativeQuery(sql);
        query.setParameter("userId", userId);
        query.setParameter("limit", pageSize);
        query.setParameter("offset", (page - 1) * pageSize);

        List<Object[]> rows = query.getResultList();
        List<Map<String, Object>> list = new ArrayList<>();
        for (Object[] row : rows) {
            Map<String, Object> post = new HashMap<>();
            post.put("id", ((Number) row[0]).longValue());
            post.put("title", row[1]);
            post.put("content", row[2]);
            post.put("images", row[3]);
            post.put("likeCount", row[4] != null ? ((Number) row[4]).intValue() : 0);
            post.put("commentCount", row[5] != null ? ((Number) row[5]).intValue() : 0);
            post.put("viewCount", row[6] != null ? ((Number) row[6]).intValue() : 0);
            post.put("postType", ((Number) row[7]).intValue());
            post.put("status", ((Number) row[8]).intValue());
            post.put("createdAt", row[9]);
            post.put("favoritedAt", row[10]);
            post.put("userName", row[11]);
            post.put("userAvatar", row[12]);
            list.add(post);
        }

        long total = favoriteRepository.countByUserId(userId);

        Map<String, Object> result = new HashMap<>();
        result.put("list", list);
        result.put("total", total);
        result.put("page", page);
        result.put("pageSize", pageSize);

        return Result.success(result);
    }

    /**
     * 检查是否已收藏
     */
    public boolean checkFavorited(Long postId, Long userId) {
        return favoriteRepository.existsByPostIdAndUserId(postId, userId);
    }
}
