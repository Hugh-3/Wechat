package com.linliquan.service;

import com.linliquan.common.Result;
import com.linliquan.common.ResultCode;
import com.linliquan.model.entity.AdminUser;
import com.linliquan.model.entity.Post;
import com.linliquan.model.entity.User;
import com.linliquan.model.enums.VerificationStatus;
import com.linliquan.repository.AdminUserRepository;
import com.linliquan.repository.PostRepository;
import com.linliquan.repository.UserRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * AdminService 集成测试
 * 测试 AdminService 与各 Repository / EntityManager 的集成
 * 使用 @Transactional 自动回滚，保证测试数据隔离
 */
@SpringBootTest
@Transactional
@DisplayName("AdminService集成测试")
class AdminServiceIntegrationTest {

    @Autowired
    private AdminService adminService;

    @Autowired
    private AdminUserRepository adminUserRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PostRepository postRepository;

    @Autowired
    private EntityManager entityManager;

    // 使用 @MockBean 替换 CacheService，避免 Redis 依赖
    @MockBean
    private CacheService cacheService;

    private AdminUser testAdmin;
    private User testUser;
    private Post testPost;

    @BeforeEach
    void setUp() {
        // 创建测试管理员（密码采用直接匹配模式，便于测试）
        testAdmin = new AdminUser();
        testAdmin.setUsername("test_admin_" + System.nanoTime());
        testAdmin.setPasswordHash("admin123");
        testAdmin.setNickname("测试管理员");
        testAdmin.setRole(1);
        testAdmin.setStatus(1);
        testAdmin.setCreatedAt(LocalDateTime.now());
        testAdmin.setUpdatedAt(LocalDateTime.now());
        testAdmin = adminUserRepository.save(testAdmin);

        // 创建测试用户（默认未认证状态）
        testUser = new User();
        testUser.setPhoneHash("hash_test_" + System.nanoTime());
        testUser.setPhoneEncrypted("13800138000");
        testUser.setNickname("测试业主");
        testUser.setVerificationStatus(VerificationStatus.UNAUTH);
        testUser.setCreatedAt(LocalDateTime.now());
        testUser.setUpdatedAt(LocalDateTime.now());
        testUser = userRepository.save(testUser);

        // 创建测试帖子
        testPost = new Post();
        testPost.setUserId(testUser.getId());
        testPost.setPostType(1);
        testPost.setTitle("测试动态");
        testPost.setContent("测试内容");
        testPost.setStatus(1);
        testPost.setLikeCount(0);
        testPost.setCommentCount(0);
        testPost.setViewCount(0);
        testPost.setCreatedAt(LocalDateTime.now());
        testPost.setUpdatedAt(LocalDateTime.now());
        testPost = postRepository.save(testPost);

        entityManager.flush();
    }

    // ==================== 管理员登录测试 ====================

    @Test
    @DisplayName("管理员登录 - 成功")
    void testAdminLogin_Success() {
        Result<Map<String, Object>> result = adminService.adminLogin(
            testAdmin.getUsername(), "admin123", "127.0.0.1");

        assertTrue(result.isSuccess());
        assertNotNull(result.getData());
        assertNotNull(result.getData().get("accessToken"));

        @SuppressWarnings("unchecked")
        Map<String, Object> adminInfo = (Map<String, Object>) result.getData().get("admin");
        assertEquals(testAdmin.getUsername(), adminInfo.get("username"));
    }

    @Test
    @DisplayName("管理员登录 - 密码错误")
    void testAdminLogin_WrongPassword() {
        Result<Map<String, Object>> result = adminService.adminLogin(
            testAdmin.getUsername(), "wrong_password", "127.0.0.1");

        assertFalse(result.isSuccess());
        assertEquals(ResultCode.ADMIN_LOGIN_FAILED.getCode(), result.getCode());
    }

    // ==================== 业主认证审核测试 ====================

    @Test
    @DisplayName("获取待审核业主认证列表")
    void testGetVerificationList() {
        // 设置用户为认证中状态
        testUser.setVerificationStatus(VerificationStatus.PENDING);
        testUser.setVerificationApplyTime(LocalDateTime.now());
        userRepository.save(testUser);
        entityManager.flush();

        Result<Map<String, Object>> result = adminService.getVerificationList(1, 10);

        assertTrue(result.isSuccess());
        assertNotNull(result.getData());
        assertNotNull(result.getData().get("list"));
        assertTrue(((Number) result.getData().get("total")).longValue() >= 1);
    }

    @Test
    @DisplayName("通过业主认证")
    void testApproveVerification() {
        // 设置用户为认证中状态
        testUser.setVerificationStatus(VerificationStatus.PENDING);
        testUser.setVerificationApplyTime(LocalDateTime.now());
        userRepository.save(testUser);
        entityManager.flush();

        Result<Void> result = adminService.approveVerification(testUser.getId(), testAdmin.getId());

        assertTrue(result.isSuccess());

        // 验证用户状态已更新为已认证
        entityManager.clear();
        User updated = userRepository.findById(testUser.getId()).orElse(null);
        assertNotNull(updated);
        assertEquals(VerificationStatus.VERIFIED, updated.getVerificationStatus());
    }

    @Test
    @DisplayName("驳回业主认证")
    void testRejectVerification() {
        // 设置用户为认证中状态并填写认证材料
        testUser.setVerificationStatus(VerificationStatus.PENDING);
        testUser.setIdCardEncrypted("id_card_encrypted");
        testUser.setHouseNumberEncrypted("house_number_encrypted");
        testUser.setCertificateUrl("https://cos.com/cert.jpg");
        testUser.setVerificationApplyTime(LocalDateTime.now());
        userRepository.save(testUser);
        entityManager.flush();

        Result<Void> result = adminService.rejectVerification(testUser.getId(), testAdmin.getId(), "材料不全");

        assertTrue(result.isSuccess());

        // 验证用户状态已重置为未认证，认证材料已清空
        entityManager.clear();
        User updated = userRepository.findById(testUser.getId()).orElse(null);
        assertNotNull(updated);
        assertEquals(VerificationStatus.UNAUTH, updated.getVerificationStatus());
        assertNull(updated.getIdCardEncrypted());
        assertNull(updated.getHouseNumberEncrypted());
        assertNull(updated.getCertificateUrl());
    }

    // ==================== 帖子审核测试 ====================

    @Test
    @DisplayName("审核帖子 - 通过")
    void testAuditPost_Approve() {
        Result<Void> result = adminService.auditPost(testPost.getId(), testAdmin.getId(), true, null);

        assertTrue(result.isSuccess());

        // 通过原生查询验证 audit_status = 1
        Object auditStatus = entityManager.createNativeQuery(
            "SELECT audit_status FROM posts WHERE id = :postId")
            .setParameter("postId", testPost.getId())
            .getSingleResult();
        assertEquals(1, ((Number) auditStatus).intValue());
    }

    @Test
    @DisplayName("审核帖子 - 拒绝")
    void testAuditPost_Reject() {
        Result<Void> result = adminService.auditPost(
            testPost.getId(), testAdmin.getId(), false, "违规内容");

        assertTrue(result.isSuccess());

        // 验证 audit_status = 2（拒绝）且 status = 3（下架）
        Object[] row = (Object[]) entityManager.createNativeQuery(
            "SELECT audit_status, status FROM posts WHERE id = :postId")
            .setParameter("postId", testPost.getId())
            .getSingleResult();
        assertEquals(2, ((Number) row[0]).intValue());
        assertEquals(3, ((Number) row[1]).intValue());
    }

    // ==================== 用户管理测试 ====================

    @Test
    @DisplayName("获取用户列表")
    void testGetUserList() {
        Result<Map<String, Object>> result = adminService.getUserList(1, 10, null);

        assertTrue(result.isSuccess());
        assertNotNull(result.getData());
        assertNotNull(result.getData().get("list"));
        assertTrue(((Number) result.getData().get("total")).longValue() >= 1);
    }

    @Test
    @DisplayName("封禁用户")
    void testBanUser() {
        Result<Void> result = adminService.banUser(testUser.getId(), testAdmin.getId(), "违规行为");

        assertTrue(result.isSuccess());

        // 验证 ban_status = 1
        Object banStatus = entityManager.createNativeQuery(
            "SELECT ban_status FROM users WHERE id = :userId")
            .setParameter("userId", testUser.getId())
            .getSingleResult();
        assertEquals(1, ((Number) banStatus).intValue());
    }

    @Test
    @DisplayName("解封用户")
    void testUnbanUser() {
        // 先封禁用户
        adminService.banUser(testUser.getId(), testAdmin.getId(), "违规");
        entityManager.flush();
        entityManager.clear();

        // 再解封
        Result<Void> result = adminService.unbanUser(testUser.getId(), testAdmin.getId());

        assertTrue(result.isSuccess());

        // 验证 ban_status = 0
        Object banStatus = entityManager.createNativeQuery(
            "SELECT ban_status FROM users WHERE id = :userId")
            .setParameter("userId", testUser.getId())
            .getSingleResult();
        assertEquals(0, ((Number) banStatus).intValue());
    }

    // ==================== 数据看板统计测试 ====================

    @Test
    @DisplayName("获取数据看板统计信息")
    void testGetStatistics() {
        Result<Map<String, Object>> result = adminService.getStatistics();

        assertTrue(result.isSuccess());
        assertNotNull(result.getData());
        assertNotNull(result.getData().get("userStats"));
        assertNotNull(result.getData().get("postStats"));
        assertNotNull(result.getData().get("orderStats"));
        assertNotNull(result.getData().get("todayStats"));

        @SuppressWarnings("unchecked")
        Map<String, Object> userStats = (Map<String, Object>) result.getData().get("userStats");
        assertNotNull(userStats.get("total"));
    }
}
