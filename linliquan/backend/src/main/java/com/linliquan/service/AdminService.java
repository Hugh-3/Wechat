package com.linliquan.service;

import com.linliquan.common.Result;
import com.linliquan.common.ResultCode;
import com.linliquan.model.entity.AdminUser;
import com.linliquan.model.entity.Comment;
import com.linliquan.model.entity.Post;
import com.linliquan.model.entity.User;
import com.linliquan.model.enums.VerificationStatus;
import com.linliquan.repository.AdminUserRepository;
import com.linliquan.repository.CommentRepository;
import com.linliquan.repository.OrderRepository;
import com.linliquan.repository.PostRepository;
import com.linliquan.repository.UserRepository;
import com.linliquan.util.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.Map;

/**
 * 管理后台业务服务
 * - 管理员登录
 * - 用户认证审核
 * - 帖子/评论审核
 * - 用户封禁/解封
 * - 数据看板统计
 */
@Service
public class AdminService {

    @Autowired
    private AdminUserRepository adminUserRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PostRepository postRepository;

    @Autowired
    private CommentRepository commentRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private CacheService cacheService;

    @Autowired
    private EntityManager entityManager;

    /**
     * 管理员登录
     */
    @Transactional
    public Result<Map<String, Object>> adminLogin(String username, String password, String ip) {
        AdminUser admin = adminUserRepository.findByUsername(username).orElse(null);

        if (admin == null) {
            return Result.fail(ResultCode.ADMIN_LOGIN_FAILED);
        }

        if (admin.getStatus() != null && admin.getStatus() != 1) {
            return Result.fail(ResultCode.ADMIN_DISABLED);
        }

        // 简易密码校验（生产环境应使用BCrypt）
        if (!verifyPassword(password, admin.getPasswordHash())) {
            return Result.fail(ResultCode.ADMIN_LOGIN_FAILED);
        }

        // 更新登录信息
        admin.setLastLoginAt(LocalDateTime.now());
        admin.setLastLoginIp(ip);
        admin.setUpdatedAt(LocalDateTime.now());
        adminUserRepository.save(admin);

        // 生成管理员Token
        String token = JwtUtil.generateAdminToken(admin.getId(), admin.getUsername());

        Map<String, Object> adminInfo = new HashMap<>();
        adminInfo.put("id", admin.getId());
        adminInfo.put("username", admin.getUsername());
        adminInfo.put("nickname", admin.getNickname());
        adminInfo.put("role", admin.getRole());
        adminInfo.put("lastLoginAt", admin.getLastLoginAt());

        Map<String, Object> result = new HashMap<>();
        result.put("admin", adminInfo);
        result.put("accessToken", token);

        return Result.success(result);
    }

    /**
     * 获取待审核的业主认证列表
     */
    public Result<Map<String, Object>> getVerificationList(int page, int pageSize) {
        Pageable pageable = PageRequest.of(Math.max(page - 1, 0), Math.max(pageSize, 1));

        // 查询认证中(PENDING=1)的用户列表
        Query query = entityManager.createNativeQuery(
            "SELECT * FROM users WHERE verification_status = 1 ORDER BY verification_apply_time DESC",
            User.class
        );
        query.setFirstResult((int) pageable.getOffset());
        query.setMaxResults(pageable.getPageSize());

        java.util.List<User> users = query.getResultList();

        Query countQuery = entityManager.createNativeQuery(
            "SELECT COUNT(*) FROM users WHERE verification_status = 1"
        );
        Long total = ((Number) countQuery.getSingleResult()).longValue();

        java.util.List<Map<String, Object>> list = new java.util.ArrayList<>();
        for (User u : users) {
            list.add(buildVerificationItem(u));
        }

        Map<String, Object> result = new HashMap<>();
        result.put("list", list);
        result.put("page", page);
        result.put("pageSize", pageSize);
        result.put("total", total);
        result.put("totalPages", (total + pageSize - 1) / pageSize);

        return Result.success(result);
    }

    /**
     * 通过业主认证
     */
    @Transactional
    public Result<Void> approveVerification(Long userId, Long adminId) {
        User user = userRepository.findById(userId).orElse(null);
        if (user == null) {
            return Result.fail(ResultCode.NOT_FOUND);
        }

        if (user.getVerificationStatus() != VerificationStatus.PENDING) {
            return Result.fail(ResultCode.BAD_REQUEST);
        }

        user.setVerificationStatus(VerificationStatus.VERIFIED);
        user.setVerificationPassTime(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());
        userRepository.save(user);

        cacheService.invalidateUserCache(userId);

        return Result.success(null);
    }

    /**
     * 驳回业主认证
     */
    @Transactional
    public Result<Void> rejectVerification(Long userId, Long adminId, String reason) {
        User user = userRepository.findById(userId).orElse(null);
        if (user == null) {
            return Result.fail(ResultCode.NOT_FOUND);
        }

        if (user.getVerificationStatus() != VerificationStatus.PENDING) {
            return Result.fail(ResultCode.BAD_REQUEST);
        }

        // 驳回：重置为未认证状态
        user.setVerificationStatus(VerificationStatus.UNAUTH);
        user.setIdCardEncrypted(null);
        user.setHouseNumberEncrypted(null);
        user.setCertificateUrl(null);
        user.setUpdatedAt(LocalDateTime.now());
        userRepository.save(user);

        cacheService.invalidateUserCache(userId);

        return Result.success(null);
    }

    /**
     * 获取帖子审核列表
     *
     * @param status 审核状态：0-待审核, 1-审核通过, 2-审核拒绝, 3-管理员下架；null-全部
     */
    public Result<Map<String, Object>> getPostAuditList(int page, int pageSize, Integer status) {
        Pageable pageable = PageRequest.of(Math.max(page - 1, 0), Math.max(pageSize, 1));

        StringBuilder sql = new StringBuilder("SELECT * FROM posts WHERE 1=1");
        if (status != null) {
            sql.append(" AND audit_status = :status");
        }
        sql.append(" ORDER BY created_at DESC");

        Query query = entityManager.createNativeQuery(sql.toString(), Post.class);
        if (status != null) {
            query.setParameter("status", status);
        }
        query.setFirstResult((int) pageable.getOffset());
        query.setMaxResults(pageable.getPageSize());

        java.util.List<Post> posts = query.getResultList();

        StringBuilder countSql = new StringBuilder("SELECT COUNT(*) FROM posts WHERE 1=1");
        if (status != null) {
            countSql.append(" AND audit_status = :status");
        }
        Query countQuery = entityManager.createNativeQuery(countSql.toString());
        if (status != null) {
            countQuery.setParameter("status", status);
        }
        Long total = ((Number) countQuery.getSingleResult()).longValue();

        java.util.List<Map<String, Object>> list = new java.util.ArrayList<>();
        for (Post p : posts) {
            list.add(buildPostAuditItem(p));
        }

        Map<String, Object> result = new HashMap<>();
        result.put("list", list);
        result.put("page", page);
        result.put("pageSize", pageSize);
        result.put("total", total);
        result.put("totalPages", (total + pageSize - 1) / pageSize);

        return Result.success(result);
    }

    /**
     * 审核帖子
     *
     * @param pass    true-通过, false-拒绝
     * @param reason  审核原因（拒绝时必填）
     */
    @Transactional
    public Result<Void> auditPost(Long postId, Long adminId, boolean pass, String reason) {
        Post post = postRepository.findById(postId).orElse(null);
        if (post == null) {
            return Result.fail(ResultCode.NOT_FOUND);
        }

        // audit_status: 0-待审核, 1-审核通过, 2-审核拒绝, 3-管理员下架
        if (pass) {
            // 设置 audit_status = 1 (审核通过)
            entityManager.createNativeQuery(
                "UPDATE posts SET audit_status = 1, audit_reason = NULL, audited_by = :adminId, audited_at = :now, updated_at = :now WHERE id = :postId"
            ).setParameter("adminId", adminId)
             .setParameter("now", LocalDateTime.now())
             .setParameter("postId", postId)
             .executeUpdate();
        } else {
            // 设置 audit_status = 2 (审核拒绝)，同时下架 status = 3
            entityManager.createNativeQuery(
                "UPDATE posts SET audit_status = 2, audit_reason = :reason, audited_by = :adminId, audited_at = :now, status = 3, updated_at = :now WHERE id = :postId"
            ).setParameter("reason", reason)
             .setParameter("adminId", adminId)
             .setParameter("now", LocalDateTime.now())
             .setParameter("postId", postId)
             .executeUpdate();
        }

        return Result.success(null);
    }

    /**
     * 获取评论审核列表
     *
     * @param status 审核状态：0-待审核, 1-审核通过, 2-审核拒绝；null-全部
     */
    public Result<Map<String, Object>> getCommentAuditList(int page, int pageSize, Integer status) {
        Pageable pageable = PageRequest.of(Math.max(page - 1, 0), Math.max(pageSize, 1));

        StringBuilder sql = new StringBuilder("SELECT * FROM comments WHERE 1=1");
        if (status != null) {
            sql.append(" AND audit_status = :status");
        }
        sql.append(" ORDER BY created_at DESC");

        Query query = entityManager.createNativeQuery(sql.toString(), Comment.class);
        if (status != null) {
            query.setParameter("status", status);
        }
        query.setFirstResult((int) pageable.getOffset());
        query.setMaxResults(pageable.getPageSize());

        java.util.List<Comment> comments = query.getResultList();

        StringBuilder countSql = new StringBuilder("SELECT COUNT(*) FROM comments WHERE 1=1");
        if (status != null) {
            countSql.append(" AND audit_status = :status");
        }
        Query countQuery = entityManager.createNativeQuery(countSql.toString());
        if (status != null) {
            countQuery.setParameter("status", status);
        }
        Long total = ((Number) countQuery.getSingleResult()).longValue();

        java.util.List<Map<String, Object>> list = new java.util.ArrayList<>();
        for (Comment c : comments) {
            list.add(buildCommentAuditItem(c));
        }

        Map<String, Object> result = new HashMap<>();
        result.put("list", list);
        result.put("page", page);
        result.put("pageSize", pageSize);
        result.put("total", total);
        result.put("totalPages", (total + pageSize - 1) / pageSize);

        return Result.success(result);
    }

    /**
     * 审核评论
     */
    @Transactional
    public Result<Void> auditComment(Long commentId, Long adminId, boolean pass, String reason) {
        Comment comment = commentRepository.findById(commentId).orElse(null);
        if (comment == null) {
            return Result.fail(ResultCode.NOT_FOUND);
        }

        if (pass) {
            entityManager.createNativeQuery(
                "UPDATE comments SET audit_status = 1, audit_reason = NULL, audited_by = :adminId, audited_at = :now, updated_at = :now WHERE id = :commentId"
            ).setParameter("adminId", adminId)
             .setParameter("now", LocalDateTime.now())
             .setParameter("commentId", commentId)
             .executeUpdate();
        } else {
            // 拒绝：同时将状态置为已删除 status = 2
            entityManager.createNativeQuery(
                "UPDATE comments SET audit_status = 2, audit_reason = :reason, audited_by = :adminId, audited_at = :now, status = 2, updated_at = :now WHERE id = :commentId"
            ).setParameter("reason", reason)
             .setParameter("adminId", adminId)
             .setParameter("now", LocalDateTime.now())
             .setParameter("commentId", commentId)
             .executeUpdate();
        }

        return Result.success(null);
    }

    /**
     * 获取用户列表（支持关键字搜索）
     */
    public Result<Map<String, Object>> getUserList(int page, int pageSize, String keyword) {
        Pageable pageable = PageRequest.of(Math.max(page - 1, 0), Math.max(pageSize, 1));

        StringBuilder sql = new StringBuilder("SELECT * FROM users WHERE 1=1");
        if (keyword != null && !keyword.trim().isEmpty()) {
            sql.append(" AND (nickname ILIKE :kw)");
        }
        sql.append(" ORDER BY created_at DESC");

        Query query = entityManager.createNativeQuery(sql.toString(), User.class);
        if (keyword != null && !keyword.trim().isEmpty()) {
            query.setParameter("kw", "%" + keyword.trim() + "%");
        }
        query.setFirstResult((int) pageable.getOffset());
        query.setMaxResults(pageable.getPageSize());

        java.util.List<User> users = query.getResultList();

        StringBuilder countSql = new StringBuilder("SELECT COUNT(*) FROM users WHERE 1=1");
        if (keyword != null && !keyword.trim().isEmpty()) {
            countSql.append(" AND (nickname ILIKE :kw)");
        }
        Query countQuery = entityManager.createNativeQuery(countSql.toString());
        if (keyword != null && !keyword.trim().isEmpty()) {
            countQuery.setParameter("kw", "%" + keyword.trim() + "%");
        }
        Long total = ((Number) countQuery.getSingleResult()).longValue();

        java.util.List<Map<String, Object>> list = new java.util.ArrayList<>();
        for (User u : users) {
            list.add(buildUserItem(u));
        }

        Map<String, Object> result = new HashMap<>();
        result.put("list", list);
        result.put("page", page);
        result.put("pageSize", pageSize);
        result.put("total", total);
        result.put("totalPages", (total + pageSize - 1) / pageSize);

        return Result.success(result);
    }

    /**
     * 封禁用户
     */
    @Transactional
    public Result<Void> banUser(Long userId, Long adminId, String reason) {
        User user = userRepository.findById(userId).orElse(null);
        if (user == null) {
            return Result.fail(ResultCode.NOT_FOUND);
        }

        entityManager.createNativeQuery(
            "UPDATE users SET ban_status = 1, ban_reason = :reason, banned_at = :now, banned_by = :adminId, updated_at = :now WHERE id = :userId"
        ).setParameter("reason", reason)
         .setParameter("now", LocalDateTime.now())
         .setParameter("adminId", adminId)
         .setParameter("userId", userId)
         .executeUpdate();

        cacheService.invalidateUserCache(userId);

        return Result.success(null);
    }

    /**
     * 解封用户
     */
    @Transactional
    public Result<Void> unbanUser(Long userId, Long adminId) {
        User user = userRepository.findById(userId).orElse(null);
        if (user == null) {
            return Result.fail(ResultCode.NOT_FOUND);
        }

        entityManager.createNativeQuery(
            "UPDATE users SET ban_status = 0, ban_reason = NULL, banned_at = NULL, banned_by = NULL, updated_at = :now WHERE id = :userId"
        ).setParameter("now", LocalDateTime.now())
         .setParameter("userId", userId)
         .executeUpdate();

        cacheService.invalidateUserCache(userId);

        return Result.success(null);
    }

    /**
     * 获取数据看板统计信息
     */
    public Result<Map<String, Object>> getStatistics() {
        Map<String, Object> stats = new HashMap<>();

        // 用户认证统计
        Query unauthCount = entityManager.createNativeQuery(
            "SELECT COUNT(*) FROM users WHERE verification_status = 0"
        );
        Query pendingCount = entityManager.createNativeQuery(
            "SELECT COUNT(*) FROM users WHERE verification_status = 1"
        );
        Query verifiedCount = entityManager.createNativeQuery(
            "SELECT COUNT(*) FROM users WHERE verification_status = 2"
        );
        Query totalUserCount = entityManager.createNativeQuery(
            "SELECT COUNT(*) FROM users"
        );

        Map<String, Object> userStats = new HashMap<>();
        userStats.put("unauthCount", ((Number) unauthCount.getSingleResult()).longValue());
        userStats.put("pendingCount", ((Number) pendingCount.getSingleResult()).longValue());
        userStats.put("verifiedCount", ((Number) verifiedCount.getSingleResult()).longValue());
        userStats.put("total", ((Number) totalUserCount.getSingleResult()).longValue());
        stats.put("userStats", userStats);

        // 帖子统计
        Query postCount = entityManager.createNativeQuery("SELECT COUNT(*) FROM posts");
        Query pendingPostAudit = entityManager.createNativeQuery(
            "SELECT COUNT(*) FROM posts WHERE audit_status = 0"
        );
        Map<String, Object> postStats = new HashMap<>();
        postStats.put("total", ((Number) postCount.getSingleResult()).longValue());
        postStats.put("pendingAudit", ((Number) pendingPostAudit.getSingleResult()).longValue());
        stats.put("postStats", postStats);

        // 互助订单统计
        Query orderCount = entityManager.createNativeQuery("SELECT COUNT(*) FROM orders");
        Query pendingOrder = entityManager.createNativeQuery(
            "SELECT COUNT(*) FROM orders WHERE status = 1"
        );
        Map<String, Object> orderStats = new HashMap<>();
        orderStats.put("total", ((Number) orderCount.getSingleResult()).longValue());
        orderStats.put("pending", ((Number) pendingOrder.getSingleResult()).longValue());
        stats.put("orderStats", orderStats);

        // 评论统计
        Query commentCount = entityManager.createNativeQuery(
            "SELECT COUNT(*) FROM comments WHERE status = 1"
        );
        stats.put("commentCount", ((Number) commentCount.getSingleResult()).longValue());

        // 今日新增统计
        LocalDateTime todayStart = LocalDateTime.of(LocalDate.now(), LocalTime.MIN);
        Query todayNewUsers = entityManager.createNativeQuery(
            "SELECT COUNT(*) FROM users WHERE created_at >= :todayStart"
        ).setParameter("todayStart", todayStart);
        Query todayNewPosts = entityManager.createNativeQuery(
            "SELECT COUNT(*) FROM posts WHERE created_at >= :todayStart"
        ).setParameter("todayStart", todayStart);

        Map<String, Object> todayStats = new HashMap<>();
        todayStats.put("newUsers", ((Number) todayNewUsers.getSingleResult()).longValue());
        todayStats.put("newPosts", ((Number) todayNewPosts.getSingleResult()).longValue());
        stats.put("todayStats", todayStats);

        return Result.success(stats);
    }

    // ==================== 私有构建方法 ====================

    private Map<String, Object> buildVerificationItem(User u) {
        Map<String, Object> item = new HashMap<>();
        item.put("userId", u.getId());
        item.put("nickname", u.getNickname());
        item.put("avatarUrl", u.getAvatarUrl());
        item.put("verificationStatus", u.getVerificationStatus() != null ? u.getVerificationStatus().getCode() : null);
        item.put("applyTime", u.getVerificationApplyTime());
        item.put("certificateUrl", u.getCertificateUrl());
        return item;
    }

    private Map<String, Object> buildPostAuditItem(Post p) {
        Map<String, Object> item = new HashMap<>();
        item.put("postId", p.getId());
        item.put("userId", p.getUserId());
        item.put("postType", p.getPostType());
        item.put("title", p.getTitle());
        item.put("content", p.getContent());
        item.put("images", p.getImages());
        item.put("status", p.getStatus());
        item.put("createdAt", p.getCreatedAt());
        return item;
    }

    private Map<String, Object> buildCommentAuditItem(Comment c) {
        Map<String, Object> item = new HashMap<>();
        item.put("commentId", c.getId());
        item.put("postId", c.getPostId());
        item.put("userId", c.getUserId());
        item.put("content", c.getContent());
        item.put("status", c.getStatus());
        item.put("createdAt", c.getCreatedAt());
        return item;
    }

    private Map<String, Object> buildUserItem(User u) {
        Map<String, Object> item = new HashMap<>();
        item.put("userId", u.getId());
        item.put("nickname", u.getNickname());
        item.put("avatarUrl", u.getAvatarUrl());
        item.put("verificationStatus", u.getVerificationStatus() != null ? u.getVerificationStatus().getCode() : null);
        item.put("applyTime", u.getVerificationApplyTime());
        item.put("passTime", u.getVerificationPassTime());
        item.put("createdAt", u.getCreatedAt());
        return item;
    }

    /**
     * 密码校验
     * TODO: 生产环境使用 BCryptPasswordEncoder.matches()
     */
    private boolean verifyPassword(String rawPassword, String passwordHash) {
        if (rawPassword == null || passwordHash == null) {
            return false;
        }
        // 当前阶段：直接字符串比对，便于测试账号使用
        // 实际部署时需替换为 BCrypt 校验
        return rawPassword.equals(passwordHash) || passwordHash.equals("$2a$10$mock_" + rawPassword);
    }
}
