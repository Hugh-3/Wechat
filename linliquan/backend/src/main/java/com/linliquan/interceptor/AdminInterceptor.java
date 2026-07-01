package com.linliquan.interceptor;

import com.linliquan.common.Result;
import com.linliquan.common.ResultCode;
import com.linliquan.model.entity.AdminUser;
import com.linliquan.repository.AdminUserRepository;
import com.linliquan.util.JwtUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class AdminInterceptor implements HandlerInterceptor {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private AdminUserRepository adminUserRepository;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String auth = request.getHeader("Authorization");
        if (auth == null || !auth.startsWith("Bearer ")) {
            sendError(response, ResultCode.UNAUTHORIZED);
            return false;
        }

        String token = auth.substring(7);
        if (!JwtUtil.isAdminToken(token)) {
            sendError(response, ResultCode.FORBIDDEN_ADMIN_REQUIRED);
            return false;
        }

        try {
            Long adminId = JwtUtil.getAdminIdFromToken(token);
            AdminUser admin = adminUserRepository.findById(adminId).orElse(null);
            if (admin == null || admin.getStatus() != 1) {
                sendError(response, ResultCode.ADMIN_DISABLED);
                return false;
            }
            request.setAttribute("currentAdmin", admin);
        } catch (Exception e) {
            sendError(response, ResultCode.UNAUTHORIZED);
            return false;
        }

        return true;
    }

    private void sendError(HttpServletResponse response, ResultCode code) throws Exception {
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write(objectMapper.writeValueAsString(Result.fail(code)));
    }
}
