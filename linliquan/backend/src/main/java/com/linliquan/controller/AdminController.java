package com.linliquan.controller;

import com.linliquan.common.Result;
import com.linliquan.common.ResultCode;
import com.linliquan.model.entity.AdminUser;
import com.linliquan.service.AdminService;
import com.linliquan.service.ReportService;
import com.linliquan.util.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Map;

/**
 * 管理后台接口
 * - /api/admin/login、/api/admin/login-mock 不需鉴权（在WebMvcConfig中排除）
 * - 其余 /api/admin/** 接口需通过 AdminInterceptor 鉴权
 */
@RestController
@RequestMapping("/admin")
public class AdminController {

    @Autowired
    private AdminService adminService;

    @Autowired
    private ReportService reportService;

    /**
     * 管理员登录
     */
    @PostMapping("/login")
    public Result<Map<String, Object>> login(
            @RequestParam String username,
            @RequestParam String password,
            HttpServletRequest request) {
        String ip = getClientIp(request);
        return adminService.adminLogin(username, password, ip);
    }

    /**
     * 模拟登录（仅用于测试，返回模拟管理员Token）
     */
    @PostMapping("/login-mock")
    public Result<Map<String, Object>> loginMock(@RequestParam(defaultValue = "1") Long adminId,
                                                  @RequestParam(defaultValue = "admin") String username) {
        String token = JwtUtil.generateAdminToken(adminId, username);

        Map<String, Object> adminInfo = new HashMap<>();
        adminInfo.put("id", adminId);
        adminInfo.put("username", username);
        adminInfo.put("nickname", "测试管理员");
        adminInfo.put("role", 2);

        Map<String, Object> result = new HashMap<>();
        result.put("admin", adminInfo);
        result.put("accessToken", token);
        return Result.success(result);
    }

    /**
     * 数据看板统计
     */
    @GetMapping("/dashboard")
    public Result<Map<String, Object>> dashboard(HttpServletRequest request) {
        AdminUser admin = (AdminUser) request.getAttribute("currentAdmin");
        if (admin == null) {
            return Result.fail(ResultCode.UNAUTHORIZED);
        }
        return adminService.getStatistics();
    }

    /**
     * 获取待审核业主认证列表
     */
    @GetMapping("/verifications")
    public Result<Map<String, Object>> verificationList(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize,
            HttpServletRequest request) {
        AdminUser admin = (AdminUser) request.getAttribute("currentAdmin");
        if (admin == null) {
            return Result.fail(ResultCode.UNAUTHORIZED);
        }
        return adminService.getVerificationList(page, pageSize);
    }

    /**
     * 通过业主认证
     */
    @PostMapping("/verifications/{userId}/approve")
    public Result<Void> approveVerification(@PathVariable Long userId, HttpServletRequest request) {
        AdminUser admin = (AdminUser) request.getAttribute("currentAdmin");
        if (admin == null) {
            return Result.fail(ResultCode.UNAUTHORIZED);
        }
        return adminService.approveVerification(userId, admin.getId());
    }

    /**
     * 驳回业主认证
     */
    @PostMapping("/verifications/{userId}/reject")
    public Result<Void> rejectVerification(@PathVariable Long userId,
                                            @RequestBody(required = false) Map<String, Object> params,
                                            HttpServletRequest request) {
        AdminUser admin = (AdminUser) request.getAttribute("currentAdmin");
        if (admin == null) {
            return Result.fail(ResultCode.UNAUTHORIZED);
        }
        String reason = params != null ? (String) params.get("reason") : null;
        return adminService.rejectVerification(userId, admin.getId(), reason);
    }

    /**
     * 获取帖子审核列表
     */
    @GetMapping("/posts/audit")
    public Result<Map<String, Object>> postAuditList(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize,
            @RequestParam(required = false) Integer status,
            HttpServletRequest request) {
        AdminUser admin = (AdminUser) request.getAttribute("currentAdmin");
        if (admin == null) {
            return Result.fail(ResultCode.UNAUTHORIZED);
        }
        return adminService.getPostAuditList(page, pageSize, status);
    }

    /**
     * 审核帖子
     * params: pass (true-通过, false-拒绝), reason
     */
    @PostMapping("/posts/{postId}/audit")
    public Result<Void> auditPost(@PathVariable Long postId,
                                   @RequestBody Map<String, Object> params,
                                   HttpServletRequest request) {
        AdminUser admin = (AdminUser) request.getAttribute("currentAdmin");
        if (admin == null) {
            return Result.fail(ResultCode.UNAUTHORIZED);
        }
        Boolean pass = params.get("pass") != null ? ((Boolean) params.get("pass")) : null;
        if (pass == null) {
            // 兼容数字传参
            Object passObj = params.get("pass");
            if (passObj instanceof Number) {
                pass = ((Number) passObj).intValue() == 1;
            }
        }
        if (pass == null) {
            return Result.fail(ResultCode.BAD_REQUEST);
        }
        String reason = (String) params.get("reason");
        return adminService.auditPost(postId, admin.getId(), pass, reason);
    }

    /**
     * 获取评论审核列表
     */
    @GetMapping("/comments/audit")
    public Result<Map<String, Object>> commentAuditList(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize,
            @RequestParam(required = false) Integer status,
            HttpServletRequest request) {
        AdminUser admin = (AdminUser) request.getAttribute("currentAdmin");
        if (admin == null) {
            return Result.fail(ResultCode.UNAUTHORIZED);
        }
        return adminService.getCommentAuditList(page, pageSize, status);
    }

    /**
     * 审核评论
     */
    @PostMapping("/comments/{commentId}/audit")
    public Result<Void> auditComment(@PathVariable Long commentId,
                                      @RequestBody Map<String, Object> params,
                                      HttpServletRequest request) {
        AdminUser admin = (AdminUser) request.getAttribute("currentAdmin");
        if (admin == null) {
            return Result.fail(ResultCode.UNAUTHORIZED);
        }
        Boolean pass = params.get("pass") != null ? ((Boolean) params.get("pass")) : null;
        if (pass == null) {
            Object passObj = params.get("pass");
            if (passObj instanceof Number) {
                pass = ((Number) passObj).intValue() == 1;
            }
        }
        if (pass == null) {
            return Result.fail(ResultCode.BAD_REQUEST);
        }
        String reason = (String) params.get("reason");
        return adminService.auditComment(commentId, admin.getId(), pass, reason);
    }

    /**
     * 获取用户列表（支持关键字搜索）
     */
    @GetMapping("/users")
    public Result<Map<String, Object>> userList(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize,
            @RequestParam(required = false) String keyword,
            HttpServletRequest request) {
        AdminUser admin = (AdminUser) request.getAttribute("currentAdmin");
        if (admin == null) {
            return Result.fail(ResultCode.UNAUTHORIZED);
        }
        return adminService.getUserList(page, pageSize, keyword);
    }

    /**
     * 封禁用户
     */
    @PostMapping("/users/{userId}/ban")
    public Result<Void> banUser(@PathVariable Long userId,
                                 @RequestBody(required = false) Map<String, Object> params,
                                 HttpServletRequest request) {
        AdminUser admin = (AdminUser) request.getAttribute("currentAdmin");
        if (admin == null) {
            return Result.fail(ResultCode.UNAUTHORIZED);
        }
        String reason = params != null ? (String) params.get("reason") : null;
        return adminService.banUser(userId, admin.getId(), reason);
    }

    /**
     * 解封用户
     */
    @PostMapping("/users/{userId}/unban")
    public Result<Void> unbanUser(@PathVariable Long userId, HttpServletRequest request) {
        AdminUser admin = (AdminUser) request.getAttribute("currentAdmin");
        if (admin == null) {
            return Result.fail(ResultCode.UNAUTHORIZED);
        }
        return adminService.unbanUser(userId, admin.getId());
    }

    /**
     * 获取举报列表（支持按状态、目标类型过滤）
     */
    @GetMapping("/reports")
    public Result<Map<String, Object>> reportList(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) Integer targetType,
            HttpServletRequest request) {
        AdminUser admin = (AdminUser) request.getAttribute("currentAdmin");
        if (admin == null) {
            return Result.fail(ResultCode.UNAUTHORIZED);
        }
        return reportService.getReportList(page, pageSize, status, targetType);
    }

    /**
     * 获取举报详情
     */
    @GetMapping("/reports/{reportId}")
    public Result<?> reportDetail(@PathVariable Long reportId, HttpServletRequest request) {
        AdminUser admin = (AdminUser) request.getAttribute("currentAdmin");
        if (admin == null) {
            return Result.fail(ResultCode.UNAUTHORIZED);
        }
        return reportService.getReportDetail(reportId);
    }

    /**
     * 处理举报
     * params: valid (true-有效, false-无效), handleRemark (处理备注)
     */
    @PostMapping("/reports/{reportId}/handle")
    public Result<Void> handleReport(@PathVariable Long reportId,
                                      @RequestBody Map<String, Object> params,
                                      HttpServletRequest request) {
        AdminUser admin = (AdminUser) request.getAttribute("currentAdmin");
        if (admin == null) {
            return Result.fail(ResultCode.UNAUTHORIZED);
        }

        Boolean valid = params.get("valid") != null ? ((Boolean) params.get("valid")) : null;
        if (valid == null) {
            Object validObj = params.get("valid");
            if (validObj instanceof Number) {
                valid = ((Number) validObj).intValue() == 1;
            }
        }
        if (valid == null) {
            return Result.fail(ResultCode.BAD_REQUEST);
        }
        String handleRemark = params.get("handleRemark") != null ? params.get("handleRemark").toString() : null;

        return reportService.handleReport(reportId, admin.getId(), valid, handleRemark);
    }

    /**
     * 获取客户端真实IP
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
        // 多级代理时取第一个非unknown的IP
        if (ip != null && ip.contains(",")) {
            String[] ips = ip.split(",");
            for (String s : ips) {
                if (!"unknown".equalsIgnoreCase(s.trim())) {
                    ip = s.trim();
                    break;
                }
            }
        }
        return ip;
    }
}
