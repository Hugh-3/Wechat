package com.linliquan.repository;

import com.linliquan.model.entity.Order;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

/**
 * OrderRepository单元测试
 * 测试Order实体的数据访问层方法
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("OrderRepository单元测试")
class OrderRepositoryTest {

    @Mock
    private JpaRepository<Order, Long> jpaRepository;

    @InjectMocks
    private OrderRepository orderRepository;

    private Order testOrder1;
    private Order testOrder2;

    @BeforeEach
    void setUp() {
        testOrder1 = createTestOrder(1L, 1L, 1L, 1, BigDecimal.valueOf(10), 1);
        testOrder2 = createTestOrder(2L, 2L, 2L, 2, BigDecimal.valueOf(20), 1);
    }

    @Test
    @DisplayName("测试findById - 成功查找订单")
    void testFindById_Success() {
        when(jpaRepository.findById(1L)).thenReturn(Optional.of(testOrder1));

        Optional<Order> result = orderRepository.findById(1L);

        assertTrue(result.isPresent());
        assertEquals(BigDecimal.valueOf(10), result.get().getRewardAmount());
        verify(jpaRepository, times(1)).findById(1L);
    }

    @Test
    @DisplayName("测试findById - 订单不存在")
    void testFindById_NotFound() {
        when(jpaRepository.findById(999L)).thenReturn(Optional.empty());

        Optional<Order> result = orderRepository.findById(999L);

        assertFalse(result.isPresent());
    }

    @Test
    @DisplayName("测试findByStatusOrderByCreatedAtDesc - 按状态分页查询")
    void testFindByStatus() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Order> page = new PageImpl<>(List.of(testOrder1, testOrder2));
        when(jpaRepository.findAll(any(org.springframework.data.jpa.domain.Specification.class), any(Pageable.class)))
            .thenReturn(page);

        Page<Order> result = orderRepository.findByStatusOrderByCreatedAtDesc(1, pageable);

        assertNotNull(result);
        assertEquals(2, result.getContent().size());
    }

    @Test
    @DisplayName("测试findByUserIdAndStatusOrderByCreatedAtDesc - 查询用户发布的订单")
    void testFindByUserIdAndStatus() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Order> page = new PageImpl<>(List.of(testOrder1));
        when(jpaRepository.findAll(any(org.springframework.data.jpa.domain.Specification.class), any(Pageable.class)))
            .thenReturn(page);

        Page<Order> result = orderRepository.findByUserIdAndStatusOrderByCreatedAtDesc(1L, 1, pageable);

        assertNotNull(result);
        assertEquals(1, result.getContent().size());
        assertEquals(1L, result.getContent().get(0).getUserId());
    }

    @Test
    @DisplayName("测试findByHelperUserIdAndStatus - 查询用户接单记录")
    void testFindByHelperUserIdAndStatus() {
        Order acceptedOrder = createTestOrder(3L, 1L, 2L, 2, BigDecimal.valueOf(15), 2);
        when(jpaRepository.findAll(any(org.springframework.data.jpa.domain.Specification.class)))
            .thenReturn(List.of(acceptedOrder));

        List<Order> result = orderRepository.findByHelperUserIdAndStatus(2L, 2);

        assertEquals(1, result.size());
        assertEquals(2L, result.get(0).getHelperUserId());
        assertEquals(2, result.get(0).getStatus());
    }

    @Test
    @DisplayName("测试findByHelperUserIdAndStatus - 无接单记录")
    void testFindByHelperUserIdAndStatus_Empty() {
        when(jpaRepository.findAll(any(org.springframework.data.jpa.domain.Specification.class)))
            .thenReturn(List.of());

        List<Order> result = orderRepository.findByHelperUserIdAndStatus(999L, 2);

        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("测试save - 保存新订单")
    void testSave_NewOrder() {
        when(jpaRepository.save(any(Order.class))).thenReturn(testOrder1);

        Order savedOrder = orderRepository.save(testOrder1);

        assertNotNull(savedOrder);
        assertEquals(1L, savedOrder.getId());
        verify(jpaRepository, times(1)).save(testOrder1);
    }

    @Test
    @DisplayName("测试save - 更新订单状态")
    void testSave_UpdateOrderStatus() {
        testOrder1.setStatus(2); // 进行中
        when(jpaRepository.save(any(Order.class))).thenReturn(testOrder1);

        Order updated = orderRepository.save(testOrder1);

        assertEquals(2, updated.getStatus());
    }

    @Test
    @DisplayName("测试deleteById - 删除订单")
    void testDeleteById() {
        doNothing().when(jpaRepository).deleteById(anyLong());

        orderRepository.deleteById(1L);

        verify(jpaRepository, times(1)).deleteById(1L);
    }

    @Test
    @DisplayName("测试findAll - 查询所有订单")
    void testFindAll() {
        when(jpaRepository.findAll()).thenReturn(Arrays.asList(testOrder1, testOrder2));

        List<Order> orders = orderRepository.findAll();

        assertEquals(2, orders.size());
    }

    @Test
    @DisplayName("边界测试 - 空结果集")
    void testFindAll_EmptyResult() {
        when(jpaRepository.findAll()).thenReturn(List.of());

        List<Order> orders = orderRepository.findAll();

        assertTrue(orders.isEmpty());
    }

    @Test
    @DisplayName("边界测试 - null参数处理")
    void testNullParameters() {
        assertThrows(Exception.class, () -> orderRepository.findById(null));
    }

    @Test
    @DisplayName("场景测试 - 订单完整生命周期")
    void testOrderLifecycle() {
        // 1. 创建订单（待接单）
        when(jpaRepository.save(any(Order.class))).thenAnswer(invocation -> {
            Order o = invocation.getArgument(0);
            if (o.getId() == null) {
                o.setId(1L);
            }
            return o;
        });

        Order newOrder = createTestOrder(null, 1L, null, 1, BigDecimal.valueOf(50), 1);
        Order saved = orderRepository.save(newOrder);
        assertNotNull(saved.getId());
        assertEquals(1, saved.getStatus());
        assertNull(saved.getHelperUserId());

        // 2. 查询待接单订单
        when(jpaRepository.findAll(any(org.springframework.data.jpa.domain.Specification.class)))
            .thenReturn(List.of(saved));
        List<Order> pendingOrders = orderRepository.findByHelperUserIdAndStatus(null, 1);
        assertTrue(pendingOrders.isEmpty() || pendingOrders.stream().allMatch(o -> o.getStatus() == 1));

        // 3. 接单
        saved.setHelperUserId(2L);
        saved.setStatus(2);
        when(jpaRepository.save(any(Order.class))).thenReturn(saved);
        Order accepted = orderRepository.save(saved);
        assertEquals(2L, accepted.getHelperUserId());
        assertEquals(2, accepted.getStatus());

        // 4. 完成订单
        accepted.setStatus(3);
        when(jpaRepository.save(any(Order.class))).thenReturn(accepted);
        Order completed = orderRepository.save(accepted);
        assertEquals(3, completed.getStatus());

        // 5. 取消订单
        Order cancelOrder = createTestOrder(4L, 3L, null, 1, BigDecimal.valueOf(30), 1);
        cancelOrder.setStatus(4);
        when(jpaRepository.save(any(Order.class))).thenReturn(cancelOrder);
        Order cancelled = orderRepository.save(cancelOrder);
        assertEquals(4, cancelled.getStatus());
    }

    @Test
    @DisplayName("场景测试 - 不同互助类型查询")
    void testDifferentHelpTypes() {
        Order type1 = createTestOrder(1L, 1L, null, 1, BigDecimal.valueOf(10), 1); // 拼单团购
        Order type2 = createTestOrder(2L, 2L, null, 2, BigDecimal.valueOf(20), 1); // 代取代买
        Order type3 = createTestOrder(3L, 3L, null, 3, BigDecimal.valueOf(0), 1);  // 生活求助
        Order type4 = createTestOrder(4L, 4L, null, 4, BigDecimal.valueOf(100), 1); // 技能交换

        when(jpaRepository.findAll()).thenReturn(Arrays.asList(type1, type2, type3, type4));

        List<Order> allOrders = orderRepository.findAll();
        assertEquals(4, allOrders.size());

        // 验证各类型
        assertEquals(1, allOrders.stream().filter(o -> o.getHelpType() == 1).count());
        assertEquals(1, allOrders.stream().filter(o -> o.getHelpType() == 2).count());
        assertEquals(1, allOrders.stream().filter(o -> o.getHelpType() == 3).count());
        assertEquals(1, allOrders.stream().filter(o -> o.getHelpType() == 4).count());
    }

    @Test
    @DisplayName("性能测试 - 批量查询")
    void testBatchQuery() {
        when(jpaRepository.findAll()).thenReturn(Arrays.asList(testOrder1, testOrder2));

        for (int i = 0; i < 50; i++) {
            orderRepository.findAll();
        }

        verify(jpaRepository, times(50)).findAll();
    }

    private Order createTestOrder(Long id, Long userId, Long helperUserId, Integer helpType, BigDecimal reward, Integer status) {
        Order order = new Order();
        order.setId(id);
        order.setPostId(id != null ? id : 1L);
        order.setUserId(userId);
        order.setHelperUserId(helperUserId);
        order.setHelpType(helpType);
        order.setRewardAmount(reward);
        order.setLatitude(39.9042);
        order.setLongitude(116.4074);
        order.setStatus(status);
        order.setCreatedAt(LocalDateTime.now());
        order.setUpdatedAt(LocalDateTime.now());
        return order;
    }
}
