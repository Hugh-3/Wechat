package com.linliquan.annotation;

import java.lang.annotation.*;

/**
 * 接口限流注解
 * 【P2性能优化】基于Redis的分布式限流，防止接口被刷
 * - maxRequests: 时间窗口内允许的最大请求数
 * - windowSeconds: 时间窗口（秒）
 * - key: 自定义限流key，为空时使用方法名+客户端IP
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RateLimit {
    int maxRequests() default 100;      // max requests in window
    int windowSeconds() default 60;     // time window in seconds
    String key() default "";            // custom key, empty = use method name + IP
}
