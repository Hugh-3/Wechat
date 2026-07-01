package com.linliquan.controller;

import com.linliquan.common.Result;
import com.linliquan.common.ResultCode;
import com.linliquan.model.entity.User;
import com.linliquan.model.enums.VerificationStatus;
import com.linliquan.service.AuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/v1/auth")
public class AuthController {

    @Autowired
    private AuthService authService;

    @PostMapping("/login")
    public Result<Map<String, Object>> login(@RequestParam String phone) {
        return authService.login(phone);
    }

    @PostMapping("/login-mock")
    public Result<Map<String, Object>> loginMock(@RequestParam(defaultValue = "0") int statusType) {
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

    @PostMapping("/apply-verification")
    public Result<Void> applyVerification(HttpServletRequest request, @RequestBody Map<String, Object> params) {
        User currentUser = (User) request.getAttribute("currentUser");
        if (currentUser == null) {
            return Result.fail(ResultCode.UNAUTHORIZED);
        }

        String idCard = (String) params.get("idCard");
        String houseNumber = (String) params.get("houseNumber");
        String certificateUrl = (String) params.get("certificateUrl");

        return authService.applyVerification(currentUser.getId(), idCard, houseNumber, certificateUrl);
    }

    @GetMapping("/status")
    public Result<Map<String, Object>> getStatus(HttpServletRequest request) {
        User currentUser = (User) request.getAttribute("currentUser");
        if (currentUser != null) {
            return authService.getUserStatus(currentUser.getId());
        }

        Map<String, Object> status = new HashMap<>();
        status.put("verificationStatus", VerificationStatus.UNAUTH.getCode());
        status.put("description", VerificationStatus.UNAUTH.getDescription());
        status.put("canWrite", VerificationStatus.UNAUTH.canWrite());
        status.put("canRead", VerificationStatus.UNAUTH.canRead());
        return Result.success(status);
    }
}
