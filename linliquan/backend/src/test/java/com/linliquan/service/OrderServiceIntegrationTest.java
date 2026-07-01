package com.linliquan.service;

import com.linliquan.model.entity.Order;
import com.linliquan.model.entity.User;
import com.linliquan.model.enums.VerificationStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * OrderService集成测试
 * 测试互助任务发布、附近查询、接单功能
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("OrderService集成测试")
class OrderServiceIntegrationTest {

    @Mock
    private EntityManager entityManager;

    @Mock
    private CacheService cacheService;

    @Mock
    private Query query;

    @InjectMocks
    private OrderService orderService;

    private User verifiedUser;
    private User anotherUser;
    private User pendingUser;
    private User unauthUser;

    @BeforeEach
    void setUp() {
        verifiedUser = new User();
        verifiedUser.setId(1L);
        verifiedUser.setNickname("已认证用户");
        verifiedUser.setVerificationStatus(VerificationStatus.VERIFIED);

        anotherUser = new User();
        anotherUser.setId(2L);
        anotherUser.setNickname("另一个用户");
        anotherUser.setVerificationStatus(VerificationStatus.VERIFIED);

        pendingUser = new User();
        pendingUser.setId(3L);
        pendingUser.setNickname("认证中用户");
        pendingUser.setVerificationStatus(VerificationStatus.PENDING);

        unauthUser = new User();
        unauthUser.setId(4L);
        unauthUser.setNickname("未认证用户");
        unauthUser.setVerificationStatus(VerificationStatus.UNAUTH);

        // 全局兜底：所有未显式mock的entityManager原生查询都返回一个mock query，
        // 避免因未mock导致NPE。各测试可使用更具体的matcher覆盖。
        when(entityManager.createNativeQuery(anyString())).thenReturn(query);
        when(query.setParameter(anyString(), any())).thenReturn(query);
        when(query.getResultList()).thenReturn(Collections.emptyList());
        when(query.getSingleResult()).thenReturn(1L);
    }

    // ==================== 发布互助任务测试 ====================

    @Test
    @DisplayName("集成测试 - 已认证用户发布互助任务")
    void testCreateOrder_Success() {
        Map<String, Object> params = createOrderParams(1, "帮忙取快递", "内容", 39.9042, 116.4074, "10");

        // 模拟Post创建
        when(entityManager.createNativeQuery(contains("INSERT INTO posts")))
            .thenReturn(query);
        when(query.setParameter(anyString(), any())).thenReturn(query);
        when(query.getSingleResult()).thenReturn(1L);

        // 模拟Order创建
        when(entityManager.createNativeQuery(contains("INSERT INTO orders")))
            .thenReturn(query);
        when(query.getSingleResult()).thenReturn(1L);

        var result = orderService.createOrder(verifiedUser, params);

        assertTrue(result.isSuccess());
        assertNotNull(result.getData());
        assertEquals(1, result.getData().getHelpType());
        verify(cacheService, times(1)).invalidatePostListCache(eq("help"), isNull());
    }

    @Test
    @DisplayName("集成测试 - 不同互助类型发布")
    void testCreateOrder_DifferentHelpTypes() {
        // 测试4种互助类型
        int[] helpTypes = {1, 2, 3, 4};

        for (int helpType : helpTypes) {
            Map<String, Object> params = createOrderParams(helpType, "任务", "内容", 39.9042, 116.4074, "0");

            when(entityManager.createNativeQuery(contains("INSERT INTO posts")))
                .thenReturn(query);
            when(query.setParameter(anyString(), any())).thenReturn(query);
            when(query.getSingleResult()).thenReturn((long) helpType);

            when(entityManager.createNativeQuery(contains("INSERT INTO orders")))
                .thenReturn(query);
            when(query.getSingleResult()).thenReturn((long) helpType);

            var result = orderService.createOrder(verifiedUser, params);

            assertTrue(result.isSuccess(), "类型 " + helpType + " 应该成功");
            assertEquals(helpType, result.getData().getHelpType());
        }
    }

    @Test
    @DisplayName("集成测试 - 未认证用户发布被拒绝")
    void testCreateOrder_UnauthUser_Forbidden() {
        Map<String, Object> params = createOrderParams(1, "任务", "内容", 39.9042, 116.4074, "10");

        var result = orderService.createOrder(unauthUser, params);

        assertFalse(result.isSuccess());
        assertEquals(40301, result.getCode());
    }

    @Test
    @DisplayName("集成测试 - 认证中用户发布被拒绝")
    void testCreateOrder_PendingUser_Forbidden() {
        Map<String, Object> params = createOrderParams(1, "任务", "内容", 39.9042, 116.4074, "10");

        var result = orderService.createOrder(pendingUser, params);

        assertFalse(result.isSuccess());
        assertEquals(40302, result.getCode());
    }

    @Test
    @DisplayName("集成测试 - 无效互助类型")
    void testCreateOrder_InvalidHelpType() {
        Map<String, Object> params = createOrderParams(5, "任务", "内容", 39.9042, 116.4074, "10");

        var result = orderService.createOrder(verifiedUser, params);

        assertFalse(result.isSuccess());
        assertEquals(400, result.getCode());
    }

    @Test
    @DisplayName("集成测试 - 互助类型为0")
    void testCreateOrder_HelpTypeZero() {
        Map<String, Object> params = createOrderParams(0, "任务", "内容", 39.9042, 116.4074, "10");

        var result = orderService.createOrder(verifiedUser, params);

        assertFalse(result.isSuccess());
        assertEquals(400, result.getCode());
    }

    @Test
    @DisplayName("集成测试 - 标题为空")
    void testCreateOrder_EmptyTitle() {
        Map<String, Object> params = createOrderParams(1, "", "内容", 39.9042, 116.4074, "10");

        var result = orderService.createOrder(verifiedUser, params);

        assertFalse(result.isSuccess());
        assertEquals(400, result.getCode());
    }

    @Test
    @DisplayName("集成测试 - 缺少位置")
    void testCreateOrder_MissingLocation() {
        Map<String, Object> params = new HashMap<>();
        params.put("helpType", 1);
        params.put("title", "任务");
        params.put("content", "内容");
        // 缺少 latitude 和 longitude

        var result = orderService.createOrder(verifiedUser, params);

        assertFalse(result.isSuccess());
        assertEquals(400, result.getCode());
    }

    @Test
    @DisplayName("集成测试 - 无效坐标")
    void testCreateOrder_InvalidCoordinate() {
        Map<String, Object> params = createOrderParams(1, "任务", "内容", 100.0, 200.0, "10");

        var result = orderService.createOrder(verifiedUser, params);

        assertFalse(result.isSuccess());
        assertEquals(400, result.getCode());
    }

    @Test
    @DisplayName("集成测试 - (0,0)坐标被拒绝")
    void testCreateOrder_ZeroCoordinate() {
        Map<String, Object> params = createOrderParams(1, "任务", "内容", 0.0, 0.0, "10");

        var result = orderService.createOrder(verifiedUser, params);

        assertFalse(result.isSuccess());
        assertEquals(400, result.getCode());
    }

    @Test
    @DisplayName("集成测试 - 免费任务（酬劳为0）")
    void testCreateOrder_FreeHelp() {
        Map<String, Object> params = createOrderParams(3, "免费帮忙", "内容", 39.9042, 116.4074, "0");

        when(entityManager.createNativeQuery(contains("INSERT INTO posts")))
            .thenReturn(query);
        when(query.setParameter(anyString(), any())).thenReturn(query);
        when(query.getSingleResult()).thenReturn(1L);

        when(entityManager.createNativeQuery(contains("INSERT INTO orders")))
            .thenReturn(query);
        when(query.getSingleResult()).thenReturn(1L);

        var result = orderService.createOrder(verifiedUser, params);

        assertTrue(result.isSuccess());
        assertEquals(BigDecimal.ZERO, result.getData().getRewardAmount());
    }

    // ==================== 附近任务查询测试 ====================

    @Test
    @DisplayName("集成测试 - 查询附近任务（缓存未命中）")
    void testGetNearbyOrders_CacheMiss() {
        when(cacheService.get(anyString())).thenReturn(null);
        when(entityManager.createNativeQuery(anyString())).thenReturn(query);
        when(query.setParameter(anyString(), any())).thenReturn(query);
        when(query.getResultList()).thenReturn(createMockOrderRows());

        var result = orderService.getNearbyOrders(39.9042, 116.4074, 5.0);

        assertTrue(result.isSuccess());
        assertNotNull(result.getData().get("list"));
    }

    @Test
    @DisplayName("集成测试 - 查询附近任务（缓存命中）")
    void testGetNearbyOrders_CacheHit() {
        when(cacheService.get(anyString())).thenReturn("cached_data");

        var result = orderService.getNearbyOrders(39.9042, 116.4074, 5.0);

        assertTrue(result.isSuccess());
        assertTrue((Boolean) result.getData().get("fromCache"));
    }

    @Test
    @DisplayName("集成测试 - 附近无任务")
    void testGetNearbyOrders_Empty() {
        when(cacheService.get(anyString())).thenReturn(null);
        when(entityManager.createNativeQuery(anyString())).thenReturn(query);
        when(query.setParameter(anyString(), any())).thenReturn(query);
        when(query.getResultList()).thenReturn(List.of());

        var result = orderService.getNearbyOrders(39.9042, 116.4074, 5.0);

        assertTrue(result.isSuccess());
        assertTrue(((List) result.getData().get("list")).isEmpty());
    }

    @Test
    @DisplayName("集成测试 - 不同半径查询")
    void testGetNearbyOrders_DifferentRadius() {
        when(cacheService.get(anyString())).thenReturn(null);
        when(entityManager.createNativeQuery(anyString())).thenReturn(query);
        when(query.setParameter(anyString(), any())).thenReturn(query);
        when(query.getResultList()).thenReturn(List.of());

        // 测试1公里
        var result1 = orderService.getNearbyOrders(39.9042, 116.4074, 1.0);
        assertTrue(result1.isSuccess());

        // 测试10公里
        var result10 = orderService.getNearbyOrders(39.9042, 116.4074, 10.0);
        assertTrue(result10.isSuccess());
    }

    // ==================== 接单功能测试 ====================

    @Test
    @DisplayName("集成测试 - 已认证用户接单成功")
    void testAcceptOrder_Success() {
        // 模拟查询订单所有者
        when(entityManager.createNativeQuery(contains("SELECT user_id FROM orders")))
            .thenReturn(query);
        when(query.setParameter("orderId", 1L)).thenReturn(query);
        when(query.getResultList()).thenReturn(List.of(1L)); // 订单属于userId=1（发布者）

        // 模拟更新
        when(entityManager.createNativeQuery(contains("UPDATE orders")))
            .thenReturn(query);
        when(query.setParameter(anyString(), any())).thenReturn(query);

        var result = orderService.acceptOrder(1L, anotherUser); // userId=2接单

        assertTrue(result.isSuccess());
    }

    @Test
    @DisplayName("集成测试 - 不能接自己的单")
    void testAcceptOrder_CannotAcceptOwn() {
        // 订单属于userId=1
        when(entityManager.createNativeQuery(contains("SELECT user_id FROM orders")))
            .thenReturn(query);
        when(query.setParameter("orderId", 1L)).thenReturn(query);
        when(query.getResultList()).thenReturn(List.of(1L));

        var result = orderService.acceptOrder(1L, verifiedUser); // userId=1尝试接自己的单

        assertFalse(result.isSuccess());
        assertEquals(400, result.getCode());
    }

    @Test
    @DisplayName("集成测试 - 未认证用户接单被拒绝")
    void testAcceptOrder_UnauthUser_Forbidden() {
        var result = orderService.acceptOrder(1L, unauthUser);

        assertFalse(result.isSuccess());
        assertEquals(40301, result.getCode());
    }

    @Test
    @DisplayName("集成测试 - 认证中用户接单被拒绝")
    void testAcceptOrder_PendingUser_Forbidden() {
        var result = orderService.acceptOrder(1L, pendingUser);

        assertFalse(result.isSuccess());
        assertEquals(40301, result.getCode());
    }

    @Test
    @DisplayName("集成测试 - 接不存在的订单")
    void testAcceptOrder_OrderNotFound() {
        when(entityManager.createNativeQuery(contains("SELECT user_id FROM orders")))
            .thenReturn(query);
        when(query.setParameter("orderId", 999L)).thenReturn(query);
        when(query.getResultList()).thenReturn(List.of());

        var result = orderService.acceptOrder(999L, anotherUser);

        assertFalse(result.isSuccess());
        assertEquals(404, result.getCode());
    }

    // ==================== 完整业务流程测试 ====================

    @Test
    @DisplayName("完整流程 - 发布→查询→接单→完成")
    void testCompleteOrderFlow() {
        // 1. 用户A发布互助任务
        Map<String, Object> params = createOrderParams(1, "帮忙取快递", "有谁能帮忙取个快递", 39.9042, 116.4074, "5");

        when(entityManager.createNativeQuery(contains("INSERT INTO posts")))
            .thenReturn(query);
        when(query.setParameter(anyString(), any())).thenReturn(query);
        when(query.getSingleResult()).thenReturn(10L);

        when(entityManager.createNativeQuery(contains("INSERT INTO orders")))
            .thenReturn(query);
        when(query.getSingleResult()).thenReturn(10L);

        var createResult = orderService.createOrder(verifiedUser, params);
        assertTrue(createResult.isSuccess());
        Long orderId = createResult.getData().getId();

        // 2. 用户B查询附近任务
        when(cacheService.get(anyString())).thenReturn(null);
        when(entityManager.createNativeQuery(anyString())).thenReturn(query);
        when(query.setParameter(anyString(), any())).thenReturn(query);
        when(query.getResultList()).thenReturn(List.of());

        var queryResult = orderService.getNearbyOrders(39.9042, 116.4074, 5.0);
        assertTrue(queryResult.isSuccess());

        // 3. 用户B接单
        when(entityManager.createNativeQuery(contains("SELECT user_id FROM orders")))
            .thenReturn(query);
        when(query.setParameter("orderId", orderId)).thenReturn(query);
        when(query.getResultList()).thenReturn(List.of(1L));

        when(entityManager.createNativeQuery(contains("UPDATE orders")))
            .thenReturn(query);
        when(query.setParameter(anyString(), any())).thenReturn(query);

        var acceptResult = orderService.acceptOrder(orderId, anotherUser);
        assertTrue(acceptResult.isSuccess());
    }

    @Test
    @DisplayName("完整流程 - 发布多种互助类型")
    void testCompleteMultipleHelpTypesFlow() {
        // 4种互助类型：拼单团购、代取代买、生活求助、技能交换
        String[] titles = {"拼单买水果", "帮忙代取快递", "借工具修门", "教老年人用手机"};
        int[] helpTypes = {1, 2, 3, 4};

        for (int i = 0; i < 4; i++) {
            Map<String, Object> params = createOrderParams(helpTypes[i], titles[i], "内容", 39.9042, 116.4074, String.valueOf(i * 5));

            when(entityManager.createNativeQuery(contains("INSERT INTO posts")))
                .thenReturn(query);
            when(query.setParameter(anyString(), any())).thenReturn(query);
            when(query.getSingleResult()).thenReturn((long) i);

            when(entityManager.createNativeQuery(contains("INSERT INTO orders")))
                .thenReturn(query);
            when(query.getSingleResult()).thenReturn((long) i);

            var result = orderService.createOrder(verifiedUser, params);
            assertTrue(result.isSuccess(), "类型 " + helpTypes[i] + " 应该成功");
        }
    }

    @Test
    @DisplayName("权限测试 - 不同状态用户发布对比")
    void testPermissionForCreate() {
        Map<String, Object> params = createOrderParams(1, "任务", "内容", 39.9042, 116.4074, "10");

        // 已认证 - 成功
        var verifiedResult = orderService.createOrder(verifiedUser, params);
        assertTrue(verifiedResult.isSuccess());

        // 未认证 - 拒绝
        var unauthResult = orderService.createOrder(unauthUser, params);
        assertFalse(unauthResult.isSuccess());
        assertEquals(40301, unauthResult.getCode());

        // 认证中 - 拒绝
        var pendingResult = orderService.createOrder(pendingUser, params);
        assertFalse(pendingResult.isSuccess());
        assertEquals(40302, pendingResult.getCode());
    }

    @Test
    @DisplayName("权限测试 - 不同状态用户接单对比")
    void testPermissionForAccept() {
        // 已认证 - 需要模拟订单存在且不属于该用户
        when(entityManager.createNativeQuery(contains("SELECT user_id FROM orders")))
            .thenReturn(query);
        when(query.setParameter("orderId", 1L)).thenReturn(query);
        when(query.getResultList()).thenReturn(List.of(99L)); // 订单属于其他用户

        when(entityManager.createNativeQuery(contains("UPDATE orders")))
            .thenReturn(query);
        when(query.setParameter(anyString(), any())).thenReturn(query);

        var verifiedResult = orderService.acceptOrder(1L, verifiedUser);
        assertTrue(verifiedResult.isSuccess());

        // 未认证 - 拒绝
        var unauthResult = orderService.acceptOrder(1L, unauthUser);
        assertFalse(unauthResult.isSuccess());
        assertEquals(40301, unauthResult.getCode());
    }

    @Test
    @DisplayName("边界测试 - 酬劳金额边界")
    void testRewardAmountBoundary() {
        // 测试0
        Map<String, Object> paramsZero = createOrderParams(1, "任务", "内容", 39.9042, 116.4074, "0");
        assertNotNull(paramsZero.get("rewardAmount"));

        // 测试极大值
        Map<String, Object> paramsMax = createOrderParams(1, "任务", "内容", 39.9042, 116.4074, "999999999");
        assertNotNull(paramsMax.get("rewardAmount"));
    }

    @Test
    @DisplayName("边界测试 - 位置边界值")
    void testLocationBoundary() {
        // 中国经纬度范围
        double[][] validCoords = {
            {18.0, 73.0},   // 中国最南、最西
            {54.0, 135.0},  // 中国最北、最东
            {39.9, 116.4},  // 北京
            {31.2, 121.5},  // 上海
        };

        for (double[] coords : validCoords) {
            Map<String, Object> params = createOrderParams(1, "任务", "内容", coords[0], coords[1], "10");

            when(entityManager.createNativeQuery(contains("INSERT INTO posts")))
                .thenReturn(query);
            when(query.setParameter(anyString(), any())).thenReturn(query);
            when(query.getSingleResult()).thenReturn(1L);

            when(entityManager.createNativeQuery(contains("INSERT INTO orders")))
                .thenReturn(query);
            when(query.getSingleResult()).thenReturn(1L);

            var result = orderService.createOrder(verifiedUser, params);
            assertTrue(result.isSuccess(), "坐标 " + coords[0] + ", " + coords[1] + " 应该在有效范围内");
        }
    }

    private Map<String, Object> createOrderParams(Integer helpType, String title, String content, Double lat, Double lng, String reward) {
        Map<String, Object> params = new HashMap<>();
        params.put("helpType", helpType);
        params.put("title", title);
        params.put("content", content);
        params.put("latitude", lat);
        params.put("longitude", lng);
        params.put("rewardAmount", reward);
        return params;
    }

    private List<Object[]> createMockOrderRows() {
        return List.of(
            new Object[]{1L, 1L, 2L, null, 1, BigDecimal.valueOf(10), 1, "帮忙取快递", "内容", "用户1", "avatar1", 500.0},
            new Object[]{2L, 3L, 3L, null, 2, BigDecimal.valueOf(20), 1, "拼单买水果", "内容", "用户3", "avatar3", 1000.0}
        );
    }
}
