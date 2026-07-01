package com.linliquan.interceptor;

import com.linliquan.annotation.RateLimit;
import com.linliquan.common.Result;
import com.linliquan.common.ResultCode;
import com.linliquan.service.CacheService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * 限流拦截器
 * 【P2性能优化】基于Redis的分布式限流（INCR + EXPIRE 固定窗口算法）
 *
 * 限流key规则：rate_limit:{methodName}:{clientIP}:{currentWindow}
 * - currentWindow = currentTimeSeconds / windowSeconds（同一时间窗口内共享计数）
 * - 首次请求 INCR 后结果为1，此时设置 EXPIRE 为 windowSeconds
 * - 当窗口内计数超过 maxRequests 时，返回 429 Too Many Requests
 */
@Component
public class RateLimitInterceptor implements HandlerInterceptor {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private CacheService cacheService;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws Exception {

        // 仅对Controller方法生效
        if (!(handler instanceof HandlerMethod handlerMethod)) {
            return true;
        }

        RateLimit rateLimit = handlerMethod.getMethodAnnotation(RateLimit.class);
        if (rateLimit == null) {
            return true;
        }

        int maxRequests = rateLimit.maxRequests();
        int windowSeconds = rateLimit.windowSeconds();

        // 构建限流key：自定义key优先，否则使用方法名 + 客户端IP
        String rateKey = rateLimit.key();
        if (rateKey == null || rateKey.isEmpty()) {
            String methodName = handlerMethod.getMethod().getName();
            String clientIp = getClientIp(request);
            rateKey = methodName + ":" + clientIp;
        }

        // 当前时间窗口编号（同一窗口内共享计数）
        long currentTimeSeconds = System.currentTimeMillis() / 1000;
        long currentWindow = currentTimeSeconds / windowSeconds;

        String redisKey = "rate_limit:" + rateKey + ":" + currentWindow;

        // INCR 计数
        Long count = cacheService.increment(redisKey);

        // 首次请求设置过期时间（窗口结束后自动清理）
        if (count != null && count == 1) {
            cacheService.expire(redisKey, java.time.Duration.ofSeconds(windowSeconds));
        }

        // 超过阈值，拒绝请求
        if (count != null && count > maxRequests) {
            sendTooManyRequestsResponse(response);
            return false;
        }

        return true;
    }

    /**
     * 获取客户端真实IP（穿透代理）
     */
    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        // 多级代理时取第一个
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip == null ? "unknown" : ip;
    }

    /**
     * 返回429 Too Many Requests响应
     */
    private void sendTooManyRequestsResponse(HttpServletResponse response) throws Exception {
        response.setStatus(429); // 429 Too Many Requests
        response.setContentType("application/json;charset=UTF-8");
        Result<?> result = Result.fail(ResultCode.TOO_MANY_REQUESTS);
        response.getWriter().write(objectMapper.writeValueAsString(result));
    }
}
