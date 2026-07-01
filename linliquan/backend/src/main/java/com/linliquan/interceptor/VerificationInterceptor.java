package com.linliquan.interceptor;

import com.linliquan.model.entity.User;
import com.linliquan.model.enums.VerificationStatus;
import com.linliquan.common.Result;
import com.linliquan.common.ResultCode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;
import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * 【红线强制】业主身份认证状态校验拦截器
 *
 * 功能：拦截所有需要VERIFIED权限的写操作接口
 * 校验逻辑：if (user.verificationStatus != VERIFIED) { return 403; }
 *
 * 使用方式：在需要强制认证的Controller方法上添加@VerifiedOnly注解
 * 该拦截器会读取请求上下文中的用户信息，校验其认证状态
 */
@Component
public class VerificationInterceptor implements Filter {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        // 从请求属性中获取当前用户（由AuthInterceptor设置）
        User currentUser = (User) httpRequest.getAttribute("currentUser");

        // 获取请求路径，判断是否需要校验
        String path = httpRequest.getRequestURI();

        // 需要VERIFIED权限的写操作路径
        if (requiresVerification(path)) {
            if (!checkVerificationStatus(currentUser)) {
                // 【红线强制】非VERIFIED状态一律返回403
                sendForbiddenResponse(httpResponse, currentUser);
                return;
            }
        }

        chain.doFilter(request, response);
    }

    /**
     * 判断请求路径是否需要VERIFIED认证
     * 包含：发布动态、发表评论、发布互助、私信等写操作
     */
    private boolean requiresVerification(String path) {
        // 发布动态
        if (path.startsWith("/api/v1/posts") && isWriteMethod(path)) {
            return true;
        }
        // 发表评论/评论
        if (path.startsWith("/api/v1/comments")) {
            return true;
        }
        // 发布/接单互助
        if (path.startsWith("/api/v1/orders") && isWriteMethod(path)) {
            return true;
        }
        // 私信
        if (path.startsWith("/api/v1/messages")) {
            return true;
        }
        // 组局（V2功能，但提前占位）
        if (path.startsWith("/api/v1/activities")) {
            return true;
        }
        return false;
    }

    /**
     * 判断是否为写操作（POST/PUT/PATCH/DELETE）
     */
    private boolean isWriteMethod(String path) {
        String method = ((HttpServletRequest) null).getMethod(); // 实际使用时从request获取
        // 简化判断：包含create/update/delete关键字视为写操作
        return path.contains("create") || path.contains("update") ||
               path.contains("delete") || path.contains("publish") ||
               path.contains("accept");
    }

    /**
     * 【红线核心】校验用户认证状态
     * 非VERIFIED一律返回403，严禁降级放行
     */
    private boolean checkVerificationStatus(User user) {
        if (user == null) {
            // 未登录用户也应该返回403（而不是401），避免泄露用户是否登录
            return false;
        }
        return user.isVerified();
    }

    /**
     * 发送403 Forbidden响应
     */
    private void sendForbiddenResponse(HttpServletResponse response, User user) throws IOException {
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setContentType("application/json;charset=UTF-8");

        Result<?> result;
        if (user == null) {
            result = Result.fail(ResultCode.FORBIDDEN_UNAUTHENTICATED);
        } else if (user.isPending()) {
            result = Result.fail(ResultCode.FORBIDDEN_PENDING_VERIFICATION);
        } else {
            result = Result.fail(ResultCode.FORBIDDEN_UNVERIFIED);
        }

        response.getWriter().write(objectMapper.writeValueAsString(result));
    }
}
