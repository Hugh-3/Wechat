package com.linliquan.service;

import com.linliquan.model.entity.Order;
import com.linliquan.model.entity.User;
import com.linliquan.model.enums.VerificationStatus;
import com.linliquan.common.Result;
import com.linliquan.common.ResultCode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
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

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private PointService pointService;

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
        String sql = "SELECT o.user_id, p.title FROM orders o INNER JOIN posts p ON o.post_id = p.id WHERE o.id = :orderId";
        Query checkQuery = entityManager.createNativeQuery(sql);
        checkQuery.setParameter("orderId", orderId);
        List<Object> result = checkQuery.getResultList();
        if (result.isEmpty()) {
            return Result.fail(ResultCode.NOT_FOUND);
        }
        Object[] row = (Object[]) result.get(0);
        Long ownerId = ((Number) row[0]).longValue();
        String orderTitle = row[1] != null ? row[1].toString() : "";
        if (ownerId.equals(user.getId())) {
            return Result.fail(ResultCode.BAD_REQUEST);
        }

        // 更新状态为进行中
        String updateSql = "UPDATE orders SET helper_user_id = :helperId, status = 2, updated_at = NOW() WHERE id = :orderId";
        Query updateQuery = entityManager.createNativeQuery(updateSql);
        updateQuery.setParameter("helperId", user.getId());
        updateQuery.setParameter("orderId", orderId);
        updateQuery.executeUpdate();

        // 通知任务发布者有人接单
        notificationService.sendOrderNotification(ownerId, user.getId(), orderTitle);

        // 积分奖励：接单者 +20
        pointService.addPoints(user.getId(), PointService.ORDER_ACCEPT,
                "order_accept", "order", orderId, "接单");

        return Result.success(null);
    }

    /**
     * 完成互助任务
     * 将订单状态置为已完成（status=3），并通知双方可以互相评价
     *
     * 仅订单的发布者或帮助者可以触发完成，且订单必须处于"进行中"状态（status=2）
     */
    @Transactional
    public Result<Void> completeOrder(Long orderId, User user) {
        if (orderId == null) {
            return Result.fail(ResultCode.BAD_REQUEST);
        }

        // 查询订单及其关联任务标题
        String sql = "SELECT o.user_id, o.helper_user_id, o.status, p.title " +
                "FROM orders o INNER JOIN posts p ON o.post_id = p.id WHERE o.id = :orderId";
        Query query = entityManager.createNativeQuery(sql);
        query.setParameter("orderId", orderId);
        @SuppressWarnings("unchecked")
        List<Object[]> rows = query.getResultList();
        if (rows.isEmpty()) {
            return Result.fail(ResultCode.NOT_FOUND);
        }
        Object[] row = rows.get(0);
        Long ownerId = row[0] != null ? ((Number) row[0]).longValue() : null;
        Long helperId = row[1] != null ? ((Number) row[1]).longValue() : null;
        Integer currentStatus = row[2] != null ? ((Number) row[2]).intValue() : null;
        String orderTitle = row[3] != null ? row[3].toString() : "";

        // 校验当前用户为订单参与方
        boolean isOwner = ownerId != null && ownerId.equals(user.getId());
        boolean isHelper = helperId != null && helperId.equals(user.getId());
        if (!isOwner && !isHelper) {
            return Result.fail(ResultCode.FORBIDDEN_UNVERIFIED.getCode(), "无权操作此订单");
        }

        // 校验订单状态为进行中
        if (currentStatus == null || currentStatus != 2) {
            return Result.fail(ResultCode.BAD_REQUEST.getCode(), "仅进行中的订单可完成");
        }
        if (helperId == null) {
            return Result.fail(ResultCode.BAD_REQUEST.getCode(), "该订单尚无帮助者，无法完成");
        }

        // 更新状态为已完成
        String updateSql = "UPDATE orders SET status = 3, updated_at = NOW() WHERE id = :orderId";
        Query updateQuery = entityManager.createNativeQuery(updateSql);
        updateQuery.setParameter("orderId", orderId);
        updateQuery.executeUpdate();

        // 通知双方可以互相评价
        String title = "任务已完成，可以评价了";
        String content = "您的任务「" + truncate(orderTitle, 20) + "」已完成，请尽快给对方评价";
        notificationService.sendNotification(ownerId, NotificationService.TYPE_ORDER,
                title, content, "order", orderId);
        notificationService.sendNotification(helperId, NotificationService.TYPE_ORDER,
                title, content, "order", orderId);

        return Result.success(null);
    }

    /**
     * 获取互助任务详情
     */
    @SuppressWarnings("unchecked")
    public Result<Map<String, Object>> getOrderDetail(Long orderId, Long userId) {
        String sql = """
            SELECT
                o.id, o.post_id, o.user_id, o.helper_user_id,
                o.help_type, o.reward_amount, o.status,
                o.created_at, o.updated_at,
                p.title, p.content, p.images,
                u.nickname as user_name, u.avatar_url as user_avatar,
                h.nickname as helper_name
            FROM orders o
            INNER JOIN posts p ON o.post_id = p.id
            INNER JOIN users u ON o.user_id = u.id
            LEFT JOIN users h ON o.helper_user_id = h.id
            WHERE o.id = :orderId
            """;

        Query query = entityManager.createNativeQuery(sql);
        query.setParameter("orderId", orderId);

        List<Object[]> rows = query.getResultList();
        if (rows.isEmpty()) {
            return Result.fail(ResultCode.NOT_FOUND);
        }

        Object[] row = rows.get(0);
        Map<String, Object> order = new HashMap<>();
        order.put("id", ((Number) row[0]).longValue());
        order.put("postId", row[1] != null ? ((Number) row[1]).longValue() : null);
        order.put("userId", ((Number) row[2]).longValue());
        order.put("helperUserId", row[3] != null ? ((Number) row[3]).longValue() : null);
        order.put("helpType", ((Number) row[4]).intValue());
        order.put("rewardAmount", row[5] != null ? new BigDecimal(row[5].toString()) : BigDecimal.ZERO);
        order.put("status", ((Number) row[6]).intValue());
        order.put("createdAt", row[7]);
        order.put("updatedAt", row[8]);
        order.put("title", row[9]);
        order.put("content", row[10]);
        order.put("images", row[11]);
        order.put("userName", row[12]);
        order.put("userAvatar", row[13]);
        order.put("helperName", row[14]);

        return Result.success(order);
    }

    /**
     * 获取用户参与的互助任务列表
     * @param userId 用户ID
     * @param role 角色：published-我发布的，helped-我帮助的，all-全部
     */
    @SuppressWarnings("unchecked")
    public Result<Map<String, Object>> getMyOrders(Long userId, String role, Integer page, Integer pageSize) {
        StringBuilder sqlBuilder = new StringBuilder("""
            SELECT
                o.id, o.post_id, o.user_id, o.helper_user_id,
                o.help_type, o.reward_amount, o.status,
                o.created_at, o.updated_at,
                p.title, p.content,
                u.nickname as user_name, u.avatar_url as user_avatar,
                h.nickname as helper_name
            FROM orders o
            INNER JOIN posts p ON o.post_id = p.id
            INNER JOIN users u ON o.user_id = u.id
            LEFT JOIN users h ON o.helper_user_id = h.id
            WHERE 1=1
            """);

        if ("published".equals(role)) {
            sqlBuilder.append(" AND o.user_id = :userId");
        } else if ("helped".equals(role)) {
            sqlBuilder.append(" AND o.helper_user_id = :userId");
        } else {
            sqlBuilder.append(" AND (o.user_id = :userId OR o.helper_user_id = :userId)");
        }

        sqlBuilder.append(" ORDER BY o.created_at DESC LIMIT :limit OFFSET :offset");

        Query query = entityManager.createNativeQuery(sqlBuilder.toString());
        query.setParameter("userId", userId);
        query.setParameter("limit", pageSize);
        query.setParameter("offset", (page - 1) * pageSize);

        List<Object[]> rows = query.getResultList();
        List<Map<String, Object>> orders = new ArrayList<>();
        for (Object[] row : rows) {
            Map<String, Object> order = new HashMap<>();
            order.put("id", ((Number) row[0]).longValue());
            order.put("postId", row[1] != null ? ((Number) row[1]).longValue() : null);
            order.put("userId", ((Number) row[2]).longValue());
            order.put("helperUserId", row[3] != null ? ((Number) row[3]).longValue() : null);
            order.put("helpType", ((Number) row[4]).intValue());
            order.put("rewardAmount", row[5] != null ? new BigDecimal(row[5].toString()) : BigDecimal.ZERO);
            order.put("status", ((Number) row[6]).intValue());
            order.put("createdAt", row[7]);
            order.put("updatedAt", row[8]);
            order.put("title", row[9]);
            order.put("content", row[10]);
            order.put("userName", row[11]);
            order.put("userAvatar", row[12]);
            order.put("helperName", row[13]);
            orders.add(order);
        }

        Map<String, Object> result = new HashMap<>();
        result.put("list", orders);
        result.put("total", orders.size());
        result.put("page", page);
        result.put("pageSize", pageSize);

        return Result.success(result);
    }

    /**
     * 截断字符串，超长部分以省略号表示
     */
    private String truncate(String text, int maxLen) {
        if (text == null) {
            return "";
        }
        if (text.length() <= maxLen) {
            return text;
        }
        return text.substring(0, maxLen) + "...";
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
