package com.linliquan.service;

import com.linliquan.model.entity.Order;
import com.linliquan.model.entity.User;
import com.linliquan.model.enums.VerificationStatus;
import com.linliquan.common.Result;
import com.linliquan.common.ResultCode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;
import javax.persistence.Query;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 互助任务服务
 * 【核心】基于PostGIS的空间查询（附近5公里）
 */
@Service
public class OrderService {

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private CacheService cacheService;

    /**
     * 发布互助任务
     * 【红线强制】仅VERIFIED用户可发布
     */
    @Transactional
    public Result<Order> createOrder(User user, Map<String, Object> params) {
        // 【红线强制】状态校验
        if (!user.isVerified()) {
            if (user.isPending()) {
                return Result.fail(ResultCode.FORBIDDEN_PENDING_VERIFICATION);
            }
            return Result.fail(ResultCode.FORBIDDEN_UNVERIFIED);
        }

        // 参数校验
        Integer helpType = (Integer) params.get("helpType");
        String title = (String) params.get("title");
        String content = (String) params.get("content");
        Double lat = params.get("latitude") != null ? ((Number) params.get("latitude")).doubleValue() : null;
        Double lng = params.get("longitude") != null ? ((Number) params.get("longitude")).doubleValue() : null;
        BigDecimal rewardAmount = params.get("rewardAmount") != null ?
            new BigDecimal(params.get("rewardAmount").toString()) : BigDecimal.ZERO;

        if (helpType == null || helpType < 1 || helpType > 4) {
            return Result.fail(ResultCode.BAD_REQUEST);
        }
        if (title == null || title.trim().isEmpty()) {
            return Result.fail(ResultCode.BAD_REQUEST);
        }
        if (lat == null || lng == null) {
            return Result.fail(ResultCode.BAD_REQUEST);
        }

        // 【核心】经纬度防脏数据校验
        if (!(-90 <= lat && lat <= 90) || !(-180 <= lng && lng <= 180)) {
            return Result.fail(ResultCode.BAD_REQUEST);
        }
        if (lat == 0 && lng == 0) {
            return Result.fail(ResultCode.BAD_REQUEST);
        }

        // 先创建Post
        Long postId = createPostForOrder(user, title, content, lat, lng);

        // 创建Order
        Order order = new Order();
        order.setPostId(postId);
        order.setUserId(user.getId());
        order.setHelpType(helpType);
        order.setRewardAmount(rewardAmount);
        order.setLatitude(lat);
        order.setLongitude(lng);
        order.setStatus(1); // 待接单
        order.setCreatedAt(LocalDateTime.now());
        order.setUpdatedAt(LocalDateTime.now());

        // 使用原生SQL插入Order（带空间坐标）
        String insertSql = """
            INSERT INTO orders (post_id, user_id, help_type, reward_amount, location, status, created_at, updated_at)
            VALUES (:postId, :userId, :helpType, :rewardAmount,
                    ST_SetSRID(ST_MakePoint(:lng, :lat), 4326),
                    :status, :createdAt, :updatedAt)
            RETURNING id
            """;

        Query query = entityManager.createNativeQuery(insertSql);
        query.setParameter("postId", postId);
        query.setParameter("userId", user.getId());
        query.setParameter("helpType", helpType);
        query.setParameter("rewardAmount", rewardAmount);
        query.setParameter("lat", lat);
        query.setParameter("lng", lng);
        query.setParameter("status", 1);
        query.setParameter("createdAt", LocalDateTime.now());
        query.setParameter("updatedAt", LocalDateTime.now());

        Long orderId = ((Number) query.getSingleResult()).longValue();
        order.setId(orderId);

        // 清除附近缓存
        cacheService.invalidatePostListCache("help", null);

        return Result.success(order);
    }

    /**
     * 创建关联的Post记录
     */
    private Long createPostForOrder(User user, String title, String content, Double lat, Double lng) {
        String insertSql = """
            INSERT INTO posts (user_id, post_type, title, content, location, like_count, comment_count, view_count, status, created_at, updated_at)
            VALUES (:userId, 2, :title, :content,
                    ST_SetSRID(ST_MakePoint(:lng, :lat), 4326),
                    0, 0, 0, 1, :createdAt, :updatedAt)
            RETURNING id
            """;

        Query query = entityManager.createNativeQuery(insertSql);
        query.setParameter("userId", user.getId());
        query.setParameter("title", title);
        query.setParameter("content", content != null ? content : "");
        query.setParameter("lat", lat);
        query.setParameter("lng", lng);
        query.setParameter("createdAt", LocalDateTime.now());
        query.setParameter("updatedAt", LocalDateTime.now());

        return ((Number) query.getSingleResult()).longValue();
    }

    /**
     * 【核心方法】查询附近5公里的互助任务（PostGIS空间查询）
     *
     * 使用ST_DWithin进行范围查询（高效，支持索引）
     * 使用ST_Distance进行距离计算并排序
     *
     * @param userLat 用户纬度
     * @param userLng 用户经度
     * @param radiusKm 搜索半径（默认5公里）
     */
    @SuppressWarnings("unchecked")
    public Result<Map<String, Object>> getNearbyOrders(Double userLat, Double userLng, Double radiusKm) {
        // 尝试从缓存读取
        String cacheKey = String.format("orders:nearby:%.4f:%.4f:%.1f",
            userLat, userLng, radiusKm);
        String cached = cacheService.get(cacheKey);
        if (cached != null) {
            Map<String, Object> result = new HashMap<>();
            result.put("fromCache", true);
            return Result.success(result);
        }

        // 【核心SQL】PostGIS附近查询
        // ST_DWithin：高效范围查询，支持空间索引
        // ST_Distance：计算两点间距离（米）
        // ST_MakePoint(lng, lat)：创建坐标点（注意经度在前）
        // ::geography：转换为地理类型（用于距离计算）
        String sql = """
            SELECT
                o.id,
                o.post_id,
                o.user_id,
                o.helper_user_id,
                o.help_type,
                o.reward_amount,
                o.status,
                p.title,
                p.content,
                u.nickname as user_name,
                u.avatar_url as user_avatar,
                ST_Distance(o.location::geography,
                    ST_SetSRID(ST_MakePoint(:lng, :lat), 4326)::geography) as distance_meters
            FROM orders o
            INNER JOIN posts p ON o.post_id = p.id
            INNER JOIN users u ON o.user_id = u.id
            WHERE o.status = 1  -- 仅待接单
              AND p.status = 1
              AND ST_DWithin(o.location::geography,
                  ST_SetSRID(ST_MakePoint(:lng, :lat), 4326)::geography,
                  :radius)
            ORDER BY distance_meters ASC
            LIMIT 50
            """;

        Query query = entityManager.createNativeQuery(sql);
        query.setParameter("lat", userLat);
        query.setParameter("lng", userLng);
        query.setParameter("radius", radiusKm * 1000); // 转换为米

        List<Object[]> rows = query.getResultList();

        // 转换为Order对象列表
        List<Map<String, Object>> orders = new ArrayList<>();
        for (Object[] row : rows) {
            Map<String, Object> order = new HashMap<>();
            order.put("id", ((Number) row[0]).longValue());
            order.put("postId", ((Number) row[1]).longValue());
            order.put("userId", ((Number) row[2]).longValue());
            order.put("helperUserId", row[3] != null ? ((Number) row[3]).longValue() : null);
            order.put("helpType", ((Number) row[4]).intValue());
            order.put("rewardAmount", row[5] != null ? new BigDecimal(row[5].toString()) : BigDecimal.ZERO);
            order.put("status", ((Number) row[6]).intValue());
            order.put("title", row[7]);
            order.put("content", row[8]);
            order.put("userName", row[9]);
            order.put("userAvatar", row[10]);
            order.put("distanceMeters", row[11] != null ? ((Number) row[11]).doubleValue() : null);
            order.put("distanceText", formatDistance(((Number) row[11]).doubleValue()));
            orders.add(order);
        }

        Map<String, Object> result = new HashMap<>();
        result.put("list", orders);
        result.put("total", orders.size());

        // 写入缓存（300秒）
        // cacheService.set(cacheKey, JSON.toJSONString(result));

        return Result.success(result);
    }

    /**
     * 接单互助任务
     * 【红线强制】仅VERIFIED用户可接单
     */
    @Transactional
    public Result<Void> acceptOrder(Long orderId, User user) {
        // 【红线强制】状态校验
        if (!user.isVerified()) {
            return Result.fail(ResultCode.FORBIDDEN_UNVERIFIED);
        }

        // 不能接自己的单
        String sql = "SELECT user_id FROM orders WHERE id = :orderId";
        Query checkQuery = entityManager.createNativeQuery(sql);
        checkQuery.setParameter("orderId", orderId);
        List<Object> result = checkQuery.getResultList();
        if (result.isEmpty()) {
            return Result.fail(ResultCode.NOT_FOUND);
        }
        Long ownerId = ((Number) result.get(0)).longValue();
        if (ownerId.equals(user.getId())) {
            return Result.fail(ResultCode.BAD_REQUEST);
        }

        // 更新状态为进行中
        String updateSql = "UPDATE orders SET helper_user_id = :helperId, status = 2, updated_at = NOW() WHERE id = :orderId";
        Query updateQuery = entityManager.createNativeQuery(updateSql);
        updateQuery.setParameter("helperId", user.getId());
        updateQuery.setParameter("orderId", orderId);
        updateQuery.executeUpdate();

        return Result.success(null);
    }

    /**
     * 格式化距离显示
     */
    private String formatDistance(double meters) {
        if (meters < 1000) {
            return String.format("约%d米", (int) meters);
        } else {
            return String.format("约%.1f公里", meters / 1000);
        }
    }
}
