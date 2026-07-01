package com.linliquan.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Set;

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
     * 读取缓存
     */
    public String get(String key) {
        return redisTemplate.opsForValue().get(key);
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
     * 【核心方法】发布动态后，删除相关列表缓存
     */
    public void invalidatePostListCache(String postType, String geohashPrefix) {
        // 删除该类型的前N页缓存
        for (int i = 1; i <= 5; i++) {
            delete(String.format("posts:list:%s:page:%d", postType, i));
        }
        // 删除附近缓存
        if (geohashPrefix != null) {
            deleteByPattern(String.format("posts:nearby:%s:*", geohashPrefix));
        }
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
}
