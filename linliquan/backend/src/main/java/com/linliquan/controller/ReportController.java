package com.linliquan.controller;

import com.linliquan.common.Result;
import com.linliquan.common.ResultCode;
import com.linliquan.model.entity.Report;
import com.linliquan.model.entity.User;
import com.linliquan.service.ReportService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;

/**
 * 举报控制器（用户端）
 * 【红线强制】创建举报需 VERIFIED 状态，在 Service 层校验 user.isVerified()
 */
@RestController
@RequestMapping("/v1/reports")
public class ReportController {

    @Autowired
    private ReportService reportService;

    /**
     * 【写操作】创建举报（仅认证业主）
     */
    @PostMapping
    public Result<Report> createReport(HttpServletRequest request, @RequestBody Map<String, Object> params) {
        User currentUser = (User) request.getAttribute("currentUser");
        if (currentUser == null) {
            return Result.fail(ResultCode.UNAUTHORIZED);
        }

        Integer targetType = params.get("targetType") != null
                ? Integer.parseInt(params.get("targetType").toString()) : null;
        Long targetId = params.get("targetId") != null
                ? Long.valueOf(params.get("targetId").toString()) : null;
        Integer reason = params.get("reason") != null
                ? Integer.parseInt(params.get("reason").toString()) : null;
        String description = params.get("description") != null
                ? params.get("description").toString() : null;

        if (targetType == null || targetId == null || reason == null) {
            return Result.fail(ResultCode.BAD_REQUEST);
        }

        return reportService.createReport(currentUser, targetType, targetId, reason, description);
    }

    /**
     * 【读操作】查看我的举报记录（分页）
     */
    @GetMapping("/my")
    public Result<?> getMyReports(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer pageSize,
            HttpServletRequest request) {
        User currentUser = (User) request.getAttribute("currentUser");
        if (currentUser == null) {
            return Result.fail(ResultCode.UNAUTHORIZED);
        }
        return reportService.getMyReports(currentUser.getId(), page, pageSize);
    }
}
