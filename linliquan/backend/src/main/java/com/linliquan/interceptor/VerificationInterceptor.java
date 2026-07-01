package com.linliquan.interceptor;

import com.linliquan.model.entity.User;
import com.linliquan.model.enums.VerificationStatus;
import com.linliquan.common.Result;
import com.linliquan.common.ResultCode;
import com.linliquan.service.AuthService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@Component
public class VerificationInterceptor implements Filter {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private AuthService authService;

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        String token = httpRequest.getHeader("Authorization");
        User currentUser = null;

        if (token != null && token.startsWith("Bearer ")) {
            currentUser = authService.getUserByToken(token.substring(7));
        }

        httpRequest.setAttribute("currentUser", currentUser);

        String path = httpRequest.getRequestURI();

        if (requiresVerification(path, httpRequest.getMethod())) {
            if (!checkVerificationStatus(currentUser)) {
                sendForbiddenResponse(httpResponse, currentUser);
                return;
            }
        }

        chain.doFilter(request, response);
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
            if (path.startsWith("/api/v1/messages")) {
                return true;
            }
            if (path.startsWith("/api/v1/activities")) {
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
