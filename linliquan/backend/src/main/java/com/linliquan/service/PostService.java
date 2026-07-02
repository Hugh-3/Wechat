package com.linliquan.service;

import com.linliquan.model.entity.Post;
import com.linliquan.model.entity.PostLike;
import com.linliquan.model.entity.User;
import com.linliquan.model.enums.VerificationStatus;
import com.linliquan.repository.PostRepository;
import com.linliquan.repository.PostLikeRepository;
import com.linliquan.common.Result;
import com.linliquan.common.ResultCode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;

import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 帖子服务
 * 【红线强制】所有写操作接口必须校验用户认证状态
 */
@Service
public class PostService {

    @Autowired
    private PostRepository postRepository;

    @Autowired
    private PostLikeRepository postLikeRepository;

    @Autowired
    private CacheService cacheService;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private PointService pointService;

    /**
     * 【核心方法】发布动态
     *
     * 【红线强制】状态校验：if (user.verificationStatus != VERIFIED) { return 403; }
     */
    @Transactional
    public Result<Post> createPost(User user, Map<String, Object> params) {
        // 【红线强制】状态校验：最前方，严禁降级放行
        if (!user.isVerified()) {
            if (user.isPending()) {
                return Result.fail(ResultCode.FORBIDDEN_PENDING_VERIFICATION);
            }
            return Result.fail(ResultCode.FORBIDDEN_UNVERIFIED);
        }

        // 参数校验
        String title = (String) params.get("title");
        String content = (String) params.get("content");
        Integer postType = (Integer) params.get("type");
        Double lat = params.get("latitude") != null ? ((Number) params.get("latitude")).doubleValue() : null;
        Double lng = params.get("longitude") != null ? ((Number) params.get("longitude")).doubleValue() : null;

        // 基础校验
        if (title == null || title.trim().isEmpty() || title.length() > 100) {
            return Result.fail(ResultCode.BAD_REQUEST);
        }
        if (content == null || content.trim().isEmpty() || content.length() > 2000) {
            return Result.fail(ResultCode.BAD_REQUEST);
        }
        if (postType == null || (postType != 1 && postType != 2)) {
            return Result.fail(ResultCode.BAD_REQUEST);
        }

        // 互助类型必须有位置
        if (postType == 2 && (lat == null || lng == null)) {
            return Result.fail(ResultCode.BAD_REQUEST);
        }

        // 【核心】经纬度校验（防脏数据）
        if (lat != null && lng != null) {
            if (!(-90 <= lat && lat <= 90)) {
                return Result.fail(ResultCode.BAD_REQUEST);
            }
            if (!(-180 <= lng && lng <= 180)) {
                return Result.fail(ResultCode.BAD_REQUEST);
            }
            // 排除(0,0)无效坐标
            if (lat == 0 && lng == 0) {
                return Result.fail(ResultCode.BAD_REQUEST);
            }
        }

        // 创建PostGIS坐标点（SRID=4326为WGS84坐标系）
        Point location = null;
        if (lat != null && lng != null) {
            Coordinate coordinate = new Coordinate(lng, lat);
            GeometryFactory factory = new GeometryFactory();
            location = factory.createPoint(coordinate);
            location.setSRID(4326);
        }

        // 构建实体
        Post post = new Post();
        post.setUserId(user.getId());
        post.setPostType(postType);
        post.setTitle(title.trim());
        post.setContent(content.trim());
        post.setImages((java.util.List<String>) params.get("images"));
        post.setLocation(location);
        post.setLikeCount(0);
        post.setCommentCount(0);
        post.setViewCount(0);
        post.setStatus(1);
        post.setCreatedAt(LocalDateTime.now());
        post.setUpdatedAt(LocalDateTime.now());

        // 写入数据库
        Post savedPost = postRepository.save(post);

        // 【关键】Cache Aside策略：先写DB，再删缓存
        cacheService.invalidatePostListCache(String.valueOf(postType), null);

        // 积分奖励：动态+10，互助任务+15
        if (postType == 2) {
            pointService.addPoints(user.getId(), PointService.ORDER_PUBLISH,
                    "order_publish", "post", savedPost.getId(), "发布互助任务");
        } else {
            pointService.addPoints(user.getId(), PointService.POST_CREATE,
                    "post_create", "post", savedPost.getId(), "发布动态");
        }

        return Result.success(savedPost);
    }

    /**
     * 获取帖子列表（分页）
     * 读操作，支持缓存
     */
    public Result<Map<String, Object>> getPostList(Integer postType, Integer page, Integer pageSize) {
        // 构建缓存Key
        String cacheKey = String.format("posts:list:%s:page:%d", postType != null ? postType : "all", page);

        // 尝试从缓存读取
        String cached = cacheService.get(cacheKey);
        if (cached != null) {
            // 反序列化并返回（简化处理）
            Map<String, Object> result = new HashMap<>();
            result.put("fromCache", true);
            // 实际项目中需要JSON反序列化
            return Result.success(result);
        }

        // 从数据库查询
        Pageable pageable = PageRequest.of(page - 1, pageSize);
        Page<Post> postPage;
        if (postType != null) {
            postPage = postRepository.findByPostTypeAndStatusOrderByCreatedAtDesc(postType, 1, pageable);
        } else {
            postPage = postRepository.findByStatusOrderByCreatedAtDesc(1, pageable);
        }

        // 构建返回结果
        Map<String, Object> result = new HashMap<>();
        result.put("list", postPage.getContent());
        result.put("total", postPage.getTotalElements());
        result.put("page", page);
        result.put("pageSize", pageSize);
        result.put("totalPages", postPage.getTotalPages());

        // 写入缓存（300秒过期）
        // cacheService.set(cacheKey, JSON.toJSONString(result));

        return Result.success(result);
    }

    /**
     * 获取附近互助任务（PostGIS空间查询）
     */
    @SuppressWarnings("unchecked")
    public Result<Map<String, Object>> getNearbyOrders(Double userLat, Double userLng, Double radiusKm, Integer page, Integer pageSize) {
        // PostGIS距离查询SQL
        String sql = """
            SELECT p.*,
                   ST_Distance(p.location::geography, ST_SetSRID(ST_MakePoint(:lng, :lat), 4326)::geography) as distance_meters
            FROM posts p
            WHERE p.post_type = 2
              AND p.status = 1
              AND ST_DWithin(p.location::geography, ST_SetSRID(ST_MakePoint(:lng, :lat), 4326)::geography, :radius)
            ORDER BY distance_meters ASC
            LIMIT :limit OFFSET :offset
            """;

        Query query = entityManager.createNativeQuery(sql);
        query.setParameter("lat", userLat);
        query.setParameter("lng", userLng);
        query.setParameter("radius", radiusKm * 1000); // 转换为米
        query.setParameter("limit", pageSize);
        query.setParameter("offset", (page - 1) * pageSize);

        @SuppressWarnings("rawtypes")
        java.util.List resultList = query.getResultList();

        // 转换为Post对象（简化处理）
        Map<String, Object> result = new HashMap<>();
        result.put("list", resultList);
        result.put("page", page);
        result.put("pageSize", pageSize);

        return Result.success(result);
    }

    /**
     * 点赞/取消点赞
     * @param liked true-点赞, false-取消点赞
     */
    @Transactional
    public Result<Map<String, Object>> toggleLike(Long postId, User user, boolean liked) {
        // 【红线强制】状态校验
        if (!user.isVerified()) {
            return Result.fail(ResultCode.FORBIDDEN_UNVERIFIED);
        }

        Post post = postRepository.findById(postId).orElse(null);
        if (post == null || post.getStatus() != 1) {
            return Result.fail(ResultCode.NOT_FOUND);
        }

        boolean exists = postLikeRepository.existsByPostIdAndUserId(postId, user.getId());

        if (liked && !exists) {
            // 点赞
            PostLike postLike = new PostLike();
            postLike.setPostId(postId);
            postLike.setUserId(user.getId());
            postLike.setCreatedAt(LocalDateTime.now());
            postLikeRepository.save(postLike);

            post.setLikeCount(post.getLikeCount() + 1);
            postRepository.save(post);

            // 积分奖励：帖子作者获得点赞 +5（不给自己点赞加分）
            if (!post.getUserId().equals(user.getId())) {
                pointService.addPoints(post.getUserId(), PointService.LIKE_RECEIVED,
                        "like_received", "post", postId, "获得点赞");
            }
        } else if (!liked && exists) {
            // 取消点赞
            postLikeRepository.deleteByPostIdAndUserId(postId, user.getId());

            post.setLikeCount(Math.max(0, post.getLikeCount() - 1));
            postRepository.save(post);
        }

        Map<String, Object> result = new HashMap<>();
        result.put("liked", liked);
        result.put("likeCount", post.getLikeCount());

        return Result.success(result);
    }

    /**
     * 获取用户发布的帖子列表（分页）
     */
    public Result<Map<String, Object>> getMyPosts(Long userId, Integer page, Integer pageSize) {
        Pageable pageable = PageRequest.of(page - 1, pageSize);
        Page<Post> postPage = postRepository.findByUserIdAndStatusOrderByCreatedAtDesc(userId, 1, pageable);

        Map<String, Object> result = new HashMap<>();
        result.put("list", postPage.getContent());
        result.put("total", postPage.getTotalElements());
        result.put("page", page);
        result.put("pageSize", pageSize);
        result.put("totalPages", postPage.getTotalPages());

        return Result.success(result);
    }

    /**
     * 搜索帖子
     */
    @SuppressWarnings("unchecked")
    public Result<Map<String, Object>> searchPosts(String keyword, Integer type, Integer page, Integer pageSize) {
        StringBuilder sql = new StringBuilder("""
            SELECT
                p.id, p.user_id, p.post_type, p.title, p.content, p.images,
                p.like_count, p.comment_count, p.view_count, p.status, p.created_at,
                u.nickname as user_name, u.avatar_url as user_avatar
            FROM posts p
            INNER JOIN users u ON p.user_id = u.id
            WHERE p.status = 1 AND (p.title ILIKE :keyword OR p.content ILIKE :keyword)
            """);

        if (type != null) {
            sql.append(" AND p.post_type = :type");
        }

        sql.append(" ORDER BY p.created_at DESC LIMIT :limit OFFSET :offset");

        Query query = entityManager.createNativeQuery(sql.toString());
        query.setParameter("keyword", "%" + keyword + "%");
        if (type != null) {
            query.setParameter("type", type);
        }
        query.setParameter("limit", pageSize);
        query.setParameter("offset", (page - 1) * pageSize);

        List<Object[]> rows = query.getResultList();
        List<Map<String, Object>> list = new ArrayList<>();
        for (Object[] row : rows) {
            Map<String, Object> post = new HashMap<>();
            post.put("id", ((Number) row[0]).longValue());
            post.put("userId", ((Number) row[1]).longValue());
            post.put("postType", ((Number) row[2]).intValue());
            post.put("title", row[3]);
            post.put("content", row[4]);
            post.put("images", row[5]);
            post.put("likeCount", row[6] != null ? ((Number) row[6]).intValue() : 0);
            post.put("commentCount", row[7] != null ? ((Number) row[7]).intValue() : 0);
            post.put("viewCount", row[8] != null ? ((Number) row[8]).intValue() : 0);
            post.put("status", ((Number) row[9]).intValue());
            post.put("createdAt", row[10]);
            post.put("userName", row[11]);
            post.put("userAvatar", row[12]);
            list.add(post);
        }

        // 总数
        StringBuilder countSql = new StringBuilder(
            "SELECT COUNT(*) FROM posts WHERE status = 1 AND (title ILIKE :keyword OR content ILIKE :keyword)"
        );
        if (type != null) {
            countSql.append(" AND post_type = :type");
        }
        Query countQuery = entityManager.createNativeQuery(countSql.toString());
        countQuery.setParameter("keyword", "%" + keyword + "%");
        if (type != null) {
            countQuery.setParameter("type", type);
        }
        long total = ((Number) countQuery.getSingleResult()).longValue();

        Map<String, Object> result = new HashMap<>();
        result.put("list", list);
        result.put("total", total);
        result.put("page", page);
        result.put("pageSize", pageSize);

        return Result.success(result);
    }

    /**
     * 检查用户是否已点赞
     */
    public boolean checkLiked(Long postId, Long userId) {
        return postLikeRepository.existsByPostIdAndUserId(postId, userId);
    }
}
