package com.linliquan.interceptor;

import com.linliquan.model.entity.User;
import com.linliquan.common.Result;
import com.linliquan.common.ResultCode;
import com.linliquan.service.AuthService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class VerificationInterceptor implements HandlerInterceptor {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private AuthService authService;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws Exception {

        String token = request.getHeader("Authorization");
        User currentUser = null;

        if (token != null && token.startsWith("Bearer ")) {
            currentUser = authService.getUserByToken(token.substring(7));
        }

        request.setAttribute("currentUser", currentUser);

        String path = request.getRequestURI();
        String method = request.getMethod();

        if (requiresVerification(path, method)) {
            if (!checkVerificationStatus(currentUser)) {
                sendForbiddenResponse(response, currentUser);
                return false;
            }
        }

        return true;
    }

    private boolean requiresVerification(String path, String method) {
        if ("POST".equalsIgnoreCase(method) || "PUT".equalsIgnoreCase(method) ||
            "PATCH".equalsIgnoreCase(method) || "DELETE".equalsIgnoreCase(method)) {

            if (path.startsWith("/api/v1/posts")) {
                return true;
            }
            if (path.startsWith("/api/v1/comments")) {
                return true;
            }
            if (path.startsWith("/api/v1/orders")) {
                return true;
            }
        }
        return false;
    }

    private boolean checkVerificationStatus(User user) {
        if (user == null) {
            return false;
        }
        return user.isVerified();
    }

    private void sendForbiddenResponse(HttpServletResponse response, User user) throws Exception {
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
