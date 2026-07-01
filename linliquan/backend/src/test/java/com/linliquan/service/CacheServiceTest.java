package com.linliquan.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * CacheService单元测试
 * 测试Redis缓存服务的各项功能
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("CacheService单元测试")
class CacheServiceTest {

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @InjectMocks
    private CacheService cacheService;

    private static final String TEST_KEY = "test:key";
    private static final String TEST_VALUE = "test:value";

    @BeforeEach
    void setUp() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    }

    // ==================== 基本操作测试 ====================

    @Test
    @DisplayName("测试set - 基本写入")
    void testSet() {
        doNothing().when(valueOperations).set(anyString(), anyString(), any(Duration.class));

        cacheService.set(TEST_KEY, TEST_VALUE);

        verify(valueOperations, times(1)).set(eq(TEST_KEY), eq(TEST_VALUE), any(Duration.class));
    }

    @Test
    @DisplayName("测试set - 指定TTL")
    void testSetWithTTL() {
        Duration ttl = Duration.ofMinutes(10);
        doNothing().when(valueOperations).set(anyString(), anyString(), any(Duration.class));

        cacheService.set(TEST_KEY, TEST_VALUE, ttl);

        verify(valueOperations, times(1)).set(TEST_KEY, TEST_VALUE, ttl);
    }

    @Test
    @DisplayName("测试setEx - 使用秒数设置TTL")
    void testSetEx() {
        doNothing().when(valueOperations).set(anyString(), anyString(), anyLong(), any(TimeUnit.class));

        cacheService.setEx(TEST_KEY, TEST_VALUE, 300);

        verify(valueOperations, times(1)).set(TEST_KEY, TEST_VALUE, 300L, TimeUnit.SECONDS);
    }

    @Test
    @DisplayName("测试get - 成功读取")
    void testGet_Success() {
        when(valueOperations.get(TEST_KEY)).thenReturn(TEST_VALUE);

        String result = cacheService.get(TEST_KEY);

        assertEquals(TEST_VALUE, result);
        verify(valueOperations, times(1)).get(TEST_KEY);
    }

    @Test
    @DisplayName("测试get - key不存在")
    void testGet_NotFound() {
        when(valueOperations.get(TEST_KEY)).thenReturn(null);

        String result = cacheService.get(TEST_KEY);

        assertNull(result);
    }

    @Test
    @DisplayName("测试get - 带类型转换")
    void testGetWithClass() {
        String jsonValue = "{\"id\":1,\"name\":\"test\"}";
        when(valueOperations.get(TEST_KEY)).thenReturn(jsonValue);

        TestObject result = cacheService.get(TEST_KEY, TestObject.class);

        assertNotNull(result);
        assertEquals(1, result.getId());
        assertEquals("test", result.getName());
    }

    @Test
    @DisplayName("测试delete - 删除key")
    void testDelete() {
        when(redisTemplate.delete(TEST_KEY)).thenReturn(true);

        cacheService.delete(TEST_KEY);

        verify(redisTemplate, times(1)).delete(TEST_KEY);
    }

    @Test
    @DisplayName("测试delete - 批量删除")
    void testDeleteCollection() {
        List<String> keys = Arrays.asList("key1", "key2", "key3");
        when(redisTemplate.delete(keys)).thenReturn(3L);

        cacheService.delete(keys);

        verify(redisTemplate, times(1)).delete(keys);
    }

    @Test
    @DisplayName("测试deleteByPattern - 模式删除")
    void testDeleteByPattern() {
        Set<String> matchedKeys = new HashSet<>(Arrays.asList("test:1", "test:2"));
        when(redisTemplate.keys("test:*")).thenReturn(matchedKeys);
        when(redisTemplate.delete(matchedKeys)).thenReturn(2L);

        cacheService.deleteByPattern("test:*");

        verify(redisTemplate, times(1)).keys("test:*");
        verify(redisTemplate, times(1)).delete(matchedKeys);
    }

    @Test
    @DisplayName("测试deleteByPattern - 无匹配key")
    void testDeleteByPattern_NoMatch() {
        when(redisTemplate.keys("nonexist:*")).thenReturn(Collections.emptySet());

        cacheService.deleteByPattern("nonexist:*");

        verify(redisTemplate, times(1)).keys("nonexist:*");
        verify(redisTemplate, never()).delete(anyCollection());
    }

    @Test
    @DisplayName("测试exists - key存在")
    void testExists_True() {
        when(redisTemplate.hasKey(TEST_KEY)).thenReturn(true);

        assertTrue(cacheService.exists(TEST_KEY));
    }

    @Test
    @DisplayName("测试exists - key不存在")
    void testExists_False() {
        when(redisTemplate.hasKey(TEST_KEY)).thenReturn(false);

        assertFalse(cacheService.exists(TEST_KEY));
    }

    @Test
    @DisplayName("测试expire - 设置过期时间")
    void testExpire() {
        Duration ttl = Duration.ofMinutes(5);
        when(redisTemplate.expire(TEST_KEY, ttl)).thenReturn(true);

        boolean result = cacheService.expire(TEST_KEY, ttl);

        assertTrue(result);
        verify(redisTemplate, times(1)).expire(TEST_KEY, ttl);
    }

    @Test
    @DisplayName("测试getExpire - 获取剩余TTL")
    void testGetExpire() {
        when(redisTemplate.getExpire(TEST_KEY, TimeUnit.SECONDS)).thenReturn(300L);

        Long expire = cacheService.getExpire(TEST_KEY);

        assertEquals(300L, expire);
    }

    // ==================== 自增自减测试 ====================

    @Test
    @DisplayName("测试increment - 自增")
    void testIncrement() {
        when(valueOperations.increment(TEST_KEY)).thenReturn(1L);

        Long result = cacheService.increment(TEST_KEY);

        assertEquals(1L, result);
        verify(valueOperations, times(1)).increment(TEST_KEY);
    }

    @Test
    @DisplayName("测试increment - 指定步长")
    void testIncrementWithDelta() {
        when(valueOperations.increment(TEST_KEY, 5)).thenReturn(5L);

        Long result = cacheService.increment(TEST_KEY, 5);

        assertEquals(5L, result);
        verify(valueOperations, times(1)).increment(TEST_KEY, 5);
    }

    @Test
    @DisplayName("测试decrement - 自减")
    void testDecrement() {
        when(valueOperations.decrement(TEST_KEY)).thenReturn(-1L);

        Long result = cacheService.decrement(TEST_KEY);

        assertEquals(-1L, result);
    }

    // ==================== 分布式锁测试 ====================

    @Test
    @DisplayName("测试setIfAbsent - 设置成功")
    void testSetIfAbsent_Success() {
        when(valueOperations.setIfAbsent(eq(TEST_KEY), eq(TEST_VALUE), any(Duration.class)))
            .thenReturn(true);

        boolean result = cacheService.setIfAbsent(TEST_KEY, TEST_VALUE, Duration.ofMinutes(5));

        assertTrue(result);
    }

    @Test
    @DisplayName("测试setIfAbsent - key已存在")
    void testSetIfAbsent_AlreadyExists() {
        when(valueOperations.setIfAbsent(eq(TEST_KEY), eq(TEST_VALUE), any(Duration.class)))
            .thenReturn(false);

        boolean result = cacheService.setIfAbsent(TEST_KEY, TEST_VALUE, Duration.ofMinutes(5));

        assertFalse(result);
    }

    // ==================== 缓存Key生成测试 ====================

    @Test
    @DisplayName("测试getUserCacheKey")
    void testGetUserCacheKey() {
        String key = cacheService.getUserCacheKey(12345L);
        assertEquals("user:12345", key);
    }

    @Test
    @DisplayName("测试getTokenCacheKey")
    void testGetTokenCacheKey() {
        String key = cacheService.getTokenCacheKey("abc123");
        assertEquals("token:abc123", key);
    }

    @Test
    @DisplayName("测试generateNearbyCacheKey - 精度测试")
    void testGenerateNearbyCacheKey() {
        String key = cacheService.generateNearbyCacheKey(39.9042, 116.4074, 5.0);
        assertEquals("orders:nearby:39.9042:116.4074:5.0", key);
    }

    // ==================== 用户缓存测试 ====================

    @Test
    @DisplayName("测试cacheUser - 用户信息缓存")
    void testCacheUser() {
        String userJson = "{\"id\":1,\"name\":\"test\"}";
        doNothing().when(valueOperations).set(anyString(), anyString(), any(Duration.class));

        cacheService.cacheUser(1L, userJson);

        verify(valueOperations, times(1)).set(eq("user:1"), eq(userJson), eq(Duration.ofMinutes(30)));
    }

    @Test
    @DisplayName("测试getCachedUser - 获取用户缓存")
    void testGetCachedUser() {
        String userJson = "{\"id\":1,\"name\":\"test\"}";
        when(valueOperations.get("user:1")).thenReturn(userJson);

        String result = cacheService.getCachedUser(1L);

        assertEquals(userJson, result);
    }

    @Test
    @DisplayName("测试invalidateUserCache - 清除用户缓存")
    void testInvalidateUserCache() {
        when(redisTemplate.delete("user:1")).thenReturn(true);

        cacheService.invalidateUserCache(1L);

        verify(redisTemplate, times(1)).delete("user:1");
    }

    // ==================== Token黑名单测试 ====================

    @Test
    @DisplayName("测试addTokenToBlacklist - 加入黑名单")
    void testAddTokenToBlacklist() {
        when(valueOperations.setIfAbsent(anyString(), anyString(), any(Duration.class)))
            .thenReturn(true);

        cacheService.addTokenToBlacklist("token123", 3600);

        verify(valueOperations, times(1)).setIfAbsent(
            eq("token:token123"),
            eq("blacklisted"),
            eq(Duration.ofSeconds(3600))
        );
    }

    @Test
    @DisplayName("测试isTokenBlacklisted - 检查黑名单")
    void testIsTokenBlacklisted() {
        when(redisTemplate.hasKey("token:token123")).thenReturn(true);

        boolean result = cacheService.isTokenBlacklisted("token123");

        assertTrue(result);
    }

    @Test
    @DisplayName("测试isTokenBlacklisted - 不在黑名单")
    void testIsTokenBlacklisted_NotInBlacklist() {
        when(redisTemplate.hasKey("token:token123")).thenReturn(false);

        boolean result = cacheService.isTokenBlacklisted("token123");

        assertFalse(result);
    }

    // ==================== 缓存失效测试 ====================

    @Test
    @DisplayName("测试invalidatePostListCache - 失效帖子列表缓存")
    void testInvalidatePostListCache() {
        when(redisTemplate.delete(anyString())).thenReturn(true);

        cacheService.invalidatePostListCache("1", null);

        // 验证删除了前5页缓存
        verify(redisTemplate, times(5)).delete(anyString());
    }

    @Test
    @DisplayName("测试invalidatePostListCache - 带geohash前缀")
    void testInvalidatePostListCacheWithGeohash() {
        Set<String> matchedKeys = new HashSet<>(Arrays.asList("posts:nearby:wx4:abc", "posts:nearby:wx4:def"));
        when(redisTemplate.keys("posts:nearby:wx4:*")).thenReturn(matchedKeys);
        when(redisTemplate.delete(anyString())).thenReturn(true);
        when(redisTemplate.delete(matchedKeys)).thenReturn(2L);

        cacheService.invalidatePostListCache("2", "wx4");

        verify(redisTemplate, times(5)).delete(anyString()); // 列表缓存
        verify(redisTemplate, times(1)).keys("posts:nearby:wx4:*");
        verify(redisTemplate, times(1)).delete(matchedKeys);
    }

    @Test
    @DisplayName("测试invalidateOrderNearbyCache")
    void testInvalidateOrderNearbyCache() {
        when(redisTemplate.delete(anyString())).thenReturn(true);

        cacheService.invalidateOrderNearbyCache(39.9042, 116.4074, 5.0);

        verify(redisTemplate, times(1)).delete("orders:nearby:39.9042:116.4074:5.0");
    }

    // ==================== getOrLoad模式测试 ====================

    @Test
    @DisplayName("测试getOrLoad - 缓存命中")
    void testGetOrLoad_CacheHit() {
        String cachedJson = "{\"id\":1,\"name\":\"cached\"}";
        when(valueOperations.get(TEST_KEY)).thenReturn(cachedJson);

        TestObject result = cacheService.getOrLoad(TEST_KEY, TestObject.class, () -> null);

        assertNotNull(result);
        assertEquals("cached", result.getName());
        verify(valueOperations, never()).set(anyString(), anyString(), any(Duration.class));
    }

    @Test
    @DisplayName("测试getOrLoad - 缓存未命中，加载并缓存")
    void testGetOrLoad_CacheMiss() {
        when(valueOperations.get(TEST_KEY)).thenReturn(null);
        doNothing().when(valueOperations).set(anyString(), anyString(), any(Duration.class));

        TestObject loaded = new TestObject();
        loaded.setId(2);
        loaded.setName("loaded");

        TestObject result = cacheService.getOrLoad(TEST_KEY, TestObject.class, () -> loaded);

        assertNotNull(result);
        assertEquals("loaded", result.getName());
        verify(valueOperations, times(1)).set(eq(TEST_KEY), anyString(), any(Duration.class));
    }

    @Test
    @DisplayName("测试getOrLoad - loader返回null不缓存")
    void testGetOrLoad_LoaderReturnsNull() {
        when(valueOperations.get(TEST_KEY)).thenReturn(null);

        TestObject result = cacheService.getOrLoad(TEST_KEY, TestObject.class, () -> null);

        assertNull(result);
        verify(valueOperations, never()).set(anyString(), anyString(), any(Duration.class));
    }

    // ==================== 边界测试 ====================

    @Test
    @DisplayName("边界测试 - null key")
    void testNullKey() {
        // CacheService不显式校验null key，由Redis底层处理；
        // 由于redisTemplate是mock的，null key不会抛异常，返回默认值
        assertNull(cacheService.get(null));
        assertDoesNotThrow(() -> cacheService.delete((String) null));
        assertFalse(cacheService.exists(null));
    }

    @Test
    @DisplayName("边界测试 - 空key")
    void testEmptyKey() {
        when(valueOperations.get("")).thenReturn(null);
        assertNull(cacheService.get(""));
    }

    @Test
    @DisplayName("边界测试 - 特殊字符key")
    void testSpecialCharacterKey() {
        String specialKey = "key:with:colons";
        when(valueOperations.get(specialKey)).thenReturn("value");

        assertEquals("value", cacheService.get(specialKey));
    }

    @Test
    @DisplayName("边界测试 - 超长value")
    void testLongValue() {
        String longValue = "x".repeat(1000000);
        doNothing().when(valueOperations).set(anyString(), anyString(), any(Duration.class));

        cacheService.set(TEST_KEY, longValue);

        verify(valueOperations, times(1)).set(eq(TEST_KEY), eq(longValue), any(Duration.class));
    }

    // ==================== 集成场景测试 ====================

    @Test
    @DisplayName("场景测试 - 用户登录缓存流程")
    void testUserLoginCacheFlow() {
        // 1. 登录后缓存用户信息
        String userJson = "{\"id\":1,\"name\":\"test\"}";
        doNothing().when(valueOperations).set(anyString(), anyString(), any(Duration.class));
        cacheService.cacheUser(1L, userJson);
        verify(valueOperations, times(1)).set(eq("user:1"), eq(userJson), any(Duration.class));

        // 2. 获取用户信息（从缓存）
        when(valueOperations.get("user:1")).thenReturn(userJson);
        String cached = cacheService.getCachedUser(1L);
        assertEquals(userJson, cached);

        // 3. 用户信息变更，失效缓存
        when(redisTemplate.delete("user:1")).thenReturn(true);
        cacheService.invalidateUserCache(1L);
        verify(redisTemplate, times(1)).delete("user:1");
    }

    @Test
    @DisplayName("场景测试 - Token登出黑名单流程")
    void testTokenBlacklistFlow() {
        // 1. 用户登出，将Token加入黑名单
        when(valueOperations.setIfAbsent(anyString(), anyString(), any(Duration.class)))
            .thenReturn(true);
        cacheService.addTokenToBlacklist("token123", 3600);

        // 2. 验证Token已被加入黑名单
        when(redisTemplate.hasKey("token:token123")).thenReturn(true);
        assertTrue(cacheService.isTokenBlacklisted("token123"));
    }

    @Test
    @DisplayName("场景测试 - 点赞计数场景")
    void testLikeCountScenario() {
        // 1. 获取初始点赞数
        when(valueOperations.increment("post:1:likes")).thenReturn(10L);
        assertEquals(10L, cacheService.increment("post:1:likes"));

        // 2. 增加点赞
        when(valueOperations.increment("post:1:likes")).thenReturn(11L);
        assertEquals(11L, cacheService.increment("post:1:likes"));

        // 3. 用户取消点赞
        when(valueOperations.decrement("post:1:likes")).thenReturn(10L);
        assertEquals(10L, cacheService.decrement("post:1:likes"));
    }

    @Test
    @DisplayName("性能测试 - 批量缓存操作")
    void testBatchCacheOperations() {
        when(redisTemplate.delete(anyString())).thenReturn(true);
        doNothing().when(valueOperations).set(anyString(), anyString(), any(Duration.class));

        // 模拟100次写操作
        for (int i = 0; i < 100; i++) {
            cacheService.set("key:" + i, "value:" + i);
        }

        verify(valueOperations, times(100)).set(anyString(), anyString(), any(Duration.class));
    }

    // 辅助测试类
    static class TestObject {
        private int id;
        private String name;

        public int getId() { return id; }
        public void setId(int id) { this.id = id; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
    }
}
