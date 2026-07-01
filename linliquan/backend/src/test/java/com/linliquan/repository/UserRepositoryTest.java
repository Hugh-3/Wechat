package com.linliquan.repository;

import com.linliquan.model.entity.User;
import com.linliquan.model.enums.VerificationStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

/**
 * UserRepository单元测试
 * 测试User实体的数据访问层方法
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("UserRepository单元测试")
class UserRepositoryTest {

    @Mock
    private JpaRepository<User, Long> jpaRepository;

    @InjectMocks
    private UserRepository userRepository;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = createTestUser(1L, "13800138000", VerificationStatus.VERIFIED);
    }

    @Test
    @DisplayName("测试findById - 成功查找用户")
    void testFindById_Success() {
        when(jpaRepository.findById(1L)).thenReturn(Optional.of(testUser));

        Optional<User> result = userRepository.findById(1L);

        assertTrue(result.isPresent());
        assertEquals("13800138000", result.get().getPhoneHash());
        verify(jpaRepository, times(1)).findById(1L);
    }

    @Test
    @DisplayName("测试findById - 用户不存在")
    void testFindById_NotFound() {
        when(jpaRepository.findById(999L)).thenReturn(Optional.empty());

        Optional<User> result = userRepository.findById(999L);

        assertFalse(result.isPresent());
        verify(jpaRepository, times(1)).findById(999L);
    }

    @Test
    @DisplayName("测试findByPhoneHash - 成功查找")
    void testFindByPhoneHash_Success() {
        when(jpaRepository.findAll()).thenReturn(java.util.List.of(testUser));

        Optional<User> result = userRepository.findByPhoneHash("13800138000");

        assertTrue(result.isPresent());
        assertEquals(VerificationStatus.VERIFIED, result.get().getVerificationStatus());
    }

    @Test
    @DisplayName("测试findByPhoneHash - 找不到对应手机号")
    void testFindByPhoneHash_NotFound() {
        when(jpaRepository.findAll()).thenReturn(java.util.List.of(testUser));

        Optional<User> result = userRepository.findByPhoneHash("non_existent_phone");

        assertFalse(result.isPresent());
    }

    @Test
    @DisplayName("测试save - 保存新用户")
    void testSave_NewUser() {
        when(jpaRepository.save(any(User.class))).thenReturn(testUser);

        User savedUser = userRepository.save(testUser);

        assertNotNull(savedUser);
        assertEquals(1L, savedUser.getId());
        verify(jpaRepository, times(1)).save(testUser);
    }

    @Test
    @DisplayName("测试save - 更新现有用户")
    void testSave_UpdateUser() {
        testUser.setNickname("新昵称");
        when(jpaRepository.save(any(User.class))).thenReturn(testUser);

        User updatedUser = userRepository.save(testUser);

        assertEquals("新昵称", updatedUser.getNickname());
        verify(jpaRepository, times(1)).save(testUser);
    }

    @Test
    @DisplayName("测试deleteById - 删除用户")
    void testDeleteById() {
        doNothing().when(jpaRepository).deleteById(anyLong());

        userRepository.deleteById(1L);

        verify(jpaRepository, times(1)).deleteById(1L);
    }

    @Test
    @DisplayName("测试findAll - 查询所有用户")
    void testFindAll() {
        User user2 = createTestUser(2L, "13900139000", VerificationStatus.PENDING);
        when(jpaRepository.findAll()).thenReturn(java.util.List.of(testUser, user2));

        var users = userRepository.findAll();

        assertEquals(2, users.size());
        verify(jpaRepository, times(1)).findAll();
    }

    @Test
    @DisplayName("测试count - 统计用户数量")
    void testCount() {
        when(jpaRepository.count()).thenReturn(100L);

        long count = userRepository.count();

        assertEquals(100L, count);
        verify(jpaRepository, times(1)).count();
    }

    @Test
    @DisplayName("测试existsById - 检查用户是否存在")
    void testExistsById() {
        when(jpaRepository.existsById(1L)).thenReturn(true);
        when(jpaRepository.existsById(999L)).thenReturn(false);

        assertTrue(userRepository.existsById(1L));
        assertFalse(userRepository.existsById(999L));
        verify(jpaRepository, times(1)).existsById(1L);
        verify(jpaRepository, times(1)).existsById(999L);
    }

    @Test
    @DisplayName("边界测试 - findById参数为null")
    void testFindById_NullId() {
        when(jpaRepository.findById(null)).thenThrow(new IllegalArgumentException());

        assertThrows(IllegalArgumentException.class, () -> userRepository.findById(null));
    }

    @Test
    @DisplayName("边界测试 - 空数据库查询")
    void testFindAll_EmptyDatabase() {
        when(jpaRepository.findAll()).thenReturn(java.util.List.of());

        var users = userRepository.findAll();

        assertTrue(users.isEmpty());
    }

    @Test
    @DisplayName("边界测试 - save空对象")
    void testSave_NullUser() {
        when(jpaRepository.save(null)).thenThrow(new IllegalArgumentException());

        assertThrows(IllegalArgumentException.class, () -> userRepository.save(null));
    }

    @Test
    @DisplayName("性能测试 - 多次查询")
    void testMultipleQueries() {
        when(jpaRepository.findById(anyLong())).thenReturn(Optional.of(testUser));

        for (int i = 0; i < 100; i++) {
            userRepository.findById(1L);
        }

        verify(jpaRepository, times(100)).findById(1L);
    }

    @Test
    @DisplayName("集成场景 - 用户认证状态更新流程")
    void testVerificationStatusUpdateFlow() {
        // 初始状态：未认证
        User unverifiedUser = createTestUser(1L, "13800138000", VerificationStatus.UNAUTH);
        when(jpaRepository.findById(1L)).thenReturn(Optional.of(unverifiedUser));
        when(jpaRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // 查找用户
        Optional<User> found = userRepository.findById(1L);
        assertTrue(found.isPresent());
        assertEquals(VerificationStatus.UNAUTH, found.get().getVerificationStatus());

        // 更新为认证中
        found.get().setVerificationStatus(VerificationStatus.PENDING);
        User updated = userRepository.save(found.get());
        assertEquals(VerificationStatus.PENDING, updated.getVerificationStatus());

        // 更新为已认证
        updated.setVerificationStatus(VerificationStatus.VERIFIED);
        updated.setVerificationPassTime(LocalDateTime.now());
        User verified = userRepository.save(updated);
        assertEquals(VerificationStatus.VERIFIED, verified.getVerificationStatus());
        assertNotNull(verified.getVerificationPassTime());

        verify(jpaRepository, times(3)).save(any(User.class));
    }

    private User createTestUser(Long id, String phoneHash, VerificationStatus status) {
        User user = new User();
        user.setId(id);
        user.setPhoneHash(phoneHash);
        user.setPhoneEncrypted(phoneHash);
        user.setNickname("测试用户" + id);
        user.setAvatarUrl("https://example.com/avatar" + id + ".png");
        user.setVerificationStatus(status);
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());
        return user;
    }
}
