package com.linliquan.service;

import com.alibaba.fastjson.JSON;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * 缓存服务（Cache Aside策略）
 *
 * 【关键决策】写操作：先写DB，再删缓存
 * 原因：避免在DB写入完成前有读请求进来，把旧数据回填到缓存
 *
 * 缓存过期时间：300秒（5分钟）
 */
@Service
public class CacheService {

    @Autowired
    private StringRedisTemplate redisTemplate;

    // 默认缓存过期时间：5分钟（300秒）
    private static final Duration DEFAULT_TTL = Duration.ofSeconds(300);

    // 缓存Key前缀
    private static final String PREFIX_POSTS_LIST = "posts:list:";
    private static final String PREFIX_POSTS_NEARBY = "posts:nearby:";
    private static final String PREFIX_ORDERS_NEARBY = "orders:nearby:";
    private static final String PREFIX_USER = "user:";
    private static final String PREFIX_TOKEN = "token:";

    /**
     * 写入缓存
     */
    public void set(String key, String value) {
        set(key, value, DEFAULT_TTL);
    }

    public void set(String key, String value, Duration ttl) {
        redisTemplate.opsForValue().set(key, value, ttl);
    }

    /**
     * 写入缓存（指定秒数）
     */
    public void setEx(String key, String value, long seconds) {
        redisTemplate.opsForValue().set(key, value, seconds, TimeUnit.SECONDS);
    }

    /**
     * 读取缓存
     */
    public String get(String key) {
        return redisTemplate.opsForValue().get(key);
    }

    /**
     * 读取缓存并反序列化为对象
     */
    public <T> T get(String key, Class<T> clazz) {
        String value = get(key);
        if (value == null) {
            return null;
        }
        return JSON.parseObject(value, clazz);
    }

    /**
     * 【核心】缓存读取模式：先查缓存，缓存不存在则查询数据库并写入缓存
     */
    public <T> T getOrLoad(String key, Class<T> clazz, Supplier<T> loader) {
        return getOrLoad(key, clazz, loader, DEFAULT_TTL);
    }

    public <T> T getOrLoad(String key, Class<T> clazz, Supplier<T> loader, Duration ttl) {
        String cached = get(key);
        if (cached != null) {
            return JSON.parseObject(cached, clazz);
        }

        T data = loader.get();
        if (data != null) {
            set(key, JSON.toJSONString(data), ttl);
        }
        return data;
    }

    /**
     * 删除缓存
     * 【关键】Cache Aside写操作时使用：先写DB，再删缓存
     */
    public void delete(String key) {
        redisTemplate.delete(key);
    }

    /**
     * 批量删除缓存（删除多个相关的缓存key）
     */
    public void deleteByPattern(String pattern) {
        Set<String> keys = redisTemplate.keys(pattern);
        if (keys != null && !keys.isEmpty()) {
            redisTemplate.delete(keys);
        }
    }

    /**
     * 批量删除
     */
    public void delete(Collection<String> keys) {
        if (keys != null && !keys.isEmpty()) {
            redisTemplate.delete(keys);
        }
    }

    /**
     * 【核心方法】发布动态后，删除相关列表缓存
     */
    public void invalidatePostListCache(String postType, String geohashPrefix) {
        // 删除该类型的前N页缓存
        for (int i = 1; i <= 5; i++) {
            delete(String.format("%s%s:page:%d", PREFIX_POSTS_LIST, postType, i));
        }
        // 删除附近缓存
        if (geohashPrefix != null) {
            deleteByPattern(PREFIX_POSTS_NEARBY + geohashPrefix + ":*");
        }
    }

    /**
     * 失效附近互助订单缓存
     */
    public void invalidateOrderNearbyCache(double lat, double lng, double radius) {
        String cacheKey = generateNearbyCacheKey(lat, lng, radius);
        delete(cacheKey);
    }

    /**
     * 生成附近缓存Key
     */
    public String generateNearbyCacheKey(double lat, double lng, double radius) {
        // 按精度四舍五入，相同区域共享缓存
        return String.format("%s%.4f:%.4f:%.1f", PREFIX_ORDERS_NEARBY, lat, lng, radius);
    }

    /**
     * 判断key是否存在
     */
    public boolean exists(String key) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(key));
    }

    /**
     * 设置过期时间
     */
    public boolean expire(String key, Duration ttl) {
        return Boolean.TRUE.equals(redisTemplate.expire(key, ttl));
    }

    /**
     * 获取剩余TTL
     */
    public Long getExpire(String key) {
        return redisTemplate.getExpire(key, TimeUnit.SECONDS);
    }

    /**
     * 自增操作
     */
    public Long increment(String key) {
        return redisTemplate.opsForValue().increment(key);
    }

    /**
     * 自增操作（指定步长）
     */
    public Long increment(String key, long delta) {
        return redisTemplate.opsForValue().increment(key, delta);
    }

    /**
     * 自减操作
     */
    public Long decrement(String key) {
        return redisTemplate.opsForValue().decrement(key);
    }

    /**
     * 设置nx（key不存在时才设置，用于分布式锁）
     */
    public boolean setIfAbsent(String key, String value, Duration ttl) {
        return Boolean.TRUE.equals(
            redisTemplate.opsForValue().setIfAbsent(key, value, ttl)
        );
    }

    /**
     * 获取用户缓存Key
     */
    public String getUserCacheKey(Long userId) {
        return PREFIX_USER + userId;
    }

    /**
     * 获取Token缓存Key
     */
    public String getTokenCacheKey(String token) {
        return PREFIX_TOKEN + token;
    }

    /**
     * 用户信息缓存操作
     */
    public void cacheUser(Long userId, String userJson) {
        set(getUserCacheKey(userId), userJson, Duration.ofMinutes(30));
    }

    public String getCachedUser(Long userId) {
        return get(getUserCacheKey(userId));
    }

    public void invalidateUserCache(Long userId) {
        delete(getUserCacheKey(userId));
    }

    /**
     * Token黑名单（用于登出）
     */
    public void addTokenToBlacklist(String token, long remainingSeconds) {
        setIfAbsent(getTokenCacheKey(token), "blacklisted", Duration.ofSeconds(remainingSeconds));
    }

    public boolean isTokenBlacklisted(String token) {
        return exists(getTokenCacheKey(token));
    }
}
