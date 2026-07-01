package com.linliquan.service;

import com.linliquan.common.Result;
import com.linliquan.common.ResultCode;
import com.linliquan.model.entity.AdminUser;
import com.linliquan.model.entity.Comment;
import com.linliquan.model.entity.Post;
import com.linliquan.model.entity.Report;
import com.linliquan.model.entity.User;
import com.linliquan.repository.AdminUserRepository;
import com.linliquan.repository.CommentRepository;
import com.linliquan.repository.PostRepository;
import com.linliquan.repository.ReportRepository;
import com.linliquan.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityManager;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * 举报服务
 *
 * 业务规则：
 *  1. 仅VERIFIED用户可发起举报
 *  2. 举报前校验目标是否存在（帖子/评论/用户）
 *  3. 同一举报人对同一目标不可重复举报（存在待处理举报时）
 *  4. 管理员处理举报：
 *     - 有效且目标为帖子：帖子 status=3、audit_status=3
 *     - 有效且目标为评论：评论 status=2
 *     - 有效且目标为用户：封禁用户 ban_status=1
 */
@Service
public class ReportService {

    /** 举报目标类型：1-帖子, 2-评论, 3-用户 */
    private static final short TARGET_TYPE_POST = 1;
    private static final short TARGET_TYPE_COMMENT = 2;
    private static final short TARGET_TYPE_USER = 3;

    /** 举报状态：0-待处理, 1-已处理(有效), 2-已处理(无效) */
    private static final short STATUS_PENDING = 0;
    private static final short STATUS_VALID = 1;
    private static final short STATUS_INVALID = 2;

    @Autowired
    private ReportRepository reportRepository;

    @Autowired
    private PostRepository postRepository;

    @Autowired
    private CommentRepository commentRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AdminUserRepository adminUserRepository;

    @Autowired
    private EntityManager entityManager;

    // ==================== 用户端 ====================

    /**
     * 创建举报
     */
    @Transactional
    public Result<Report> createReport(User reporter, int targetType, Long targetId, int reason, String description) {
        if (reporter == null) {
            return Result.fail(ResultCode.UNAUTHORIZED);
        }
        // 仅认证业主可举报
        if (!reporter.isVerified()) {
            return Result.fail(ResultCode.FORBIDDEN_UNVERIFIED);
        }
        if (targetId == null || targetId <= 0) {
            return Result.fail(ResultCode.BAD_REQUEST);
        }
        if (targetType < TARGET_TYPE_POST || targetType > TARGET_TYPE_USER) {
            return Result.fail(ResultCode.BAD_REQUEST.getCode(), "非法的举报目标类型");
        }
        if (reason < 1 || reason > 5) {
            return Result.fail(ResultCode.BAD_REQUEST.getCode(), "非法的举报原因");
        }

        // 校验目标存在
        boolean targetExists = false;
        if (targetType == TARGET_TYPE_POST) {
            targetExists = postRepository.existsById(targetId);
        } else if (targetType == TARGET_TYPE_COMMENT) {
            targetExists = commentRepository.existsById(targetId);
        } else if (targetType == TARGET_TYPE_USER) {
            targetExists = userRepository.existsById(targetId);
            // 不允许举报自己
            if (targetExists && targetId.equals(reporter.getId())) {
                return Result.fail(ResultCode.BAD_REQUEST.getCode(), "不能举报自己");
            }
        }
        if (!targetExists) {
            return Result.fail(ResultCode.NOT_FOUND);
        }

        // 防止重复举报（同一举报人对同一目标已有待处理举报）
        if (reportRepository.existsByReporterIdAndTargetTypeAndTargetIdAndStatus(
                reporter.getId(), (short) targetType, targetId, STATUS_PENDING)) {
            return Result.fail(ResultCode.CONFLICT.getCode(), "您已举报过该内容，正在处理中");
        }

        Report report = new Report();
        report.setReporterId(reporter.getId());
        report.setTargetType((short) targetType);
        report.setTargetId(targetId);
        report.setReason((short) reason);
        report.setDescription(description);
        report.setStatus(STATUS_PENDING);
        report.setCreatedAt(LocalDateTime.now());

        Report saved = reportRepository.save(report);
        fillTransientFields(saved);

        return Result.success(saved);
    }

    /**
     * 用户查看自己的举报记录
     */
    public Result<Map<String, Object>> getMyReports(Long userId, int page, int pageSize) {
        if (userId == null) {
            return Result.fail(ResultCode.BAD_REQUEST);
        }
        Pageable pageable = PageRequest.of(Math.max(page - 1, 0), Math.max(pageSize, 1));
        Page<Report> reportPage = reportRepository.findByReporterIdOrderByCreatedAtDesc(userId, pageable);

        for (Report r : reportPage.getContent()) {
            fillTransientFields(r);
        }

        Map<String, Object> result = new HashMap<>();
        result.put("list", reportPage.getContent());
        result.put("total", reportPage.getTotalElements());
        result.put("page", page);
        result.put("pageSize", pageSize);
        result.put("totalPages", reportPage.getTotalPages());

        return Result.success(result);
    }

    // ==================== 管理端 ====================

    /**
     * 管理端举报列表（支持按状态、目标类型过滤）
     */
    public Result<Map<String, Object>> getReportList(int page, int pageSize, Integer status, Integer targetType) {
        Pageable pageable = PageRequest.of(Math.max(page - 1, 0), Math.max(pageSize, 1));

        Short statusShort = (status != null) ? status.shortValue() : null;
        Short targetTypeShort = (targetType != null) ? targetType.shortValue() : null;

        Page<Report> reportPage = reportRepository.searchReports(statusShort, targetTypeShort, null, pageable);

        for (Report r : reportPage.getContent()) {
            fillTransientFields(r);
        }

        Map<String, Object> result = new HashMap<>();
        result.put("list", reportPage.getContent());
        result.put("total", reportPage.getTotalElements());
        result.put("page", page);
        result.put("pageSize", pageSize);
        result.put("totalPages", reportPage.getTotalPages());

        return Result.success(result);
    }

    /**
     * 处理举报
     *
     * @param valid        true-有效, false-无效
     * @param handleRemark 处理备注
     */
    @Transactional
    public Result<Void> handleReport(Long reportId, Long adminId, boolean valid, String handleRemark) {
        Report report = reportRepository.findById(reportId).orElse(null);
        if (report == null) {
            return Result.fail(ResultCode.NOT_FOUND);
        }
        if (report.getStatus() != null && report.getStatus() != STATUS_PENDING) {
            return Result.fail(ResultCode.BAD_REQUEST.getCode(), "该举报已处理");
        }

        // 若有效：对目标执行处置
        if (valid) {
            short targetType = report.getTargetType();
            Long targetId = report.getTargetId();
            if (targetType == TARGET_TYPE_POST) {
                // 帖子 status=3（下架）且 audit_status=3
                entityManager.createNativeQuery(
                    "UPDATE posts SET status = 3, audit_status = 3, updated_at = :now WHERE id = :targetId"
                ).setParameter("now", LocalDateTime.now())
                 .setParameter("targetId", targetId)
                 .executeUpdate();
            } else if (targetType == TARGET_TYPE_COMMENT) {
                // 评论 status=2（已删除）
                entityManager.createNativeQuery(
                    "UPDATE comments SET status = 2, updated_at = :now WHERE id = :targetId"
                ).setParameter("now", LocalDateTime.now())
                 .setParameter("targetId", targetId)
                 .executeUpdate();
            } else if (targetType == TARGET_TYPE_USER) {
                // 封禁用户 ban_status=1
                entityManager.createNativeQuery(
                    "UPDATE users SET ban_status = 1, ban_reason = :reason, banned_at = :now, banned_by = :adminId, updated_at = :now WHERE id = :targetId"
                ).setParameter("reason", handleRemark != null ? handleRemark : "举报处理-封禁")
                 .setParameter("now", LocalDateTime.now())
                 .setParameter("adminId", adminId)
                 .setParameter("targetId", targetId)
                 .executeUpdate();
            }
        }

        report.setStatus(valid ? STATUS_VALID : STATUS_INVALID);
        report.setHandleRemark(handleRemark);
        report.setHandledBy(adminId);
        report.setHandledAt(LocalDateTime.now());
        reportRepository.save(report);

        return Result.success(null);
    }

    /**
     * 获取举报详情（含目标信息）
     */
    public Result<Report> getReportDetail(Long reportId) {
        if (reportId == null) {
            return Result.fail(ResultCode.BAD_REQUEST);
        }
        Report report = reportRepository.findById(reportId).orElse(null);
        if (report == null) {
            return Result.fail(ResultCode.NOT_FOUND);
        }
        fillTransientFields(report);
        return Result.success(report);
    }

    // ==================== 内部辅助方法 ====================

    /**
     * 填充展示字段（举报人昵称头像、目标标题/内容、处理人昵称）
     */
    private void fillTransientFields(Report report) {
        if (report == null) {
            return;
        }
        if (report.getReporterId() != null) {
            userRepository.findById(report.getReporterId()).ifPresent(u -> {
                report.setReporterName(u.getNickname());
                report.setReporterAvatar(u.getAvatarUrl());
            });
        }
        if (report.getTargetId() != null && report.getTargetType() != null) {
            short type = report.getTargetType();
            if (type == TARGET_TYPE_POST) {
                postRepository.findById(report.getTargetId()).ifPresent(p -> {
                    report.setTargetTitle(p.getTitle());
                    report.setTargetContent(p.getContent());
                });
            } else if (type == TARGET_TYPE_COMMENT) {
                commentRepository.findById(report.getTargetId()).ifPresent(c -> {
                    report.setTargetTitle("评论 #" + c.getId());
                    report.setTargetContent(c.getContent());
                });
            } else if (type == TARGET_TYPE_USER) {
                userRepository.findById(report.getTargetId()).ifPresent(u -> {
                    report.setTargetTitle("用户 #" + u.getId());
                    report.setTargetContent(u.getNickname());
                });
            }
        }
        if (report.getHandledBy() != null) {
            adminUserRepository.findById(report.getHandledBy()).ifPresent(a -> {
                report.setHandlerName(a.getNickname() != null ? a.getNickname() : a.getUsername());
            });
        }
    }
}
