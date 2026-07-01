package com.linliquan.controller;

import com.linliquan.common.Result;
import com.linliquan.model.entity.User;
import com.linliquan.model.enums.VerificationStatus;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 认证状态控制器
 * 用于前端Mock演示三种认证状态下的UI差异
 */
@RestController
@RequestMapping("/v1/auth")
public class AuthController {

    /**
     * 模拟用户登录，返回不同认证状态
     * 用于前端开发时切换用户状态
     */
    @PostMapping("/login-mock")
    public Result<Map<String, Object>> loginMock(@RequestParam(defaultValue = "0") int statusType) {
        // statusType: 0=UNAUTH, 1=PENDING, 2=VERIFIED
        VerificationStatus status = VerificationStatus.fromCode(statusType);

        Map<String, Object> user = new HashMap<>();
        user.put("id", 1L);
        user.put("nickname", "测试用户");
        user.put("avatarUrl", "https://example.com/avatar.png");
        user.put("verificationStatus", status.getCode());
        user.put("verificationDesc", status.getDescription());

        Map<String, Object> result = new HashMap<>();
        result.put("user", user);
        result.put("accessToken", "mock_token_" + status.name());

        return Result.success(result);
    }

    /**
     * 获取当前用户认证状态
     */
    @GetMapping("/status")
    public Result<Map<String, Object>> getStatus() {
        // 实际从Session/Token中获取
        Map<String, Object> status = new HashMap<>();
        status.put("verificationStatus", VerificationStatus.VERIFIED.getCode());
        status.put("description", VerificationStatus.VERIFIED.getDescription());
        status.put("canWrite", VerificationStatus.VERIFIED.canWrite());
        status.put("canRead", VerificationStatus.VERIFIED.canRead());
        return Result.success(status);
    }
}
