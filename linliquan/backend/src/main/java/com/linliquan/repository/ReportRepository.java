package com.linliquan.repository;

import com.linliquan.model.entity.Report;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * 举报 Repository
 */
@Repository
public interface ReportRepository extends JpaRepository<Report, Long> {

    /**
     * 按状态查询举报列表（分页，按创建时间倒序）
     */
    Page<Report> findByStatusOrderByCreatedAtDesc(Short status, Pageable pageable);

    /**
     * 查询某用户发起的举报（分页，按创建时间倒序）
     */
    Page<Report> findByReporterIdOrderByCreatedAtDesc(Long reporterId, Pageable pageable);

    /**
     * 统计某状态下的举报数量
     */
    long countByStatus(Short status);

    /**
     * 多条件搜索举报（status、targetType、reason均可选）
     */
    @Query("SELECT r FROM Report r WHERE " +
           "(:status IS NULL OR r.status = :status) AND " +
           "(:targetType IS NULL OR r.targetType = :targetType) AND " +
           "(:reason IS NULL OR r.reason = :reason) " +
           "ORDER BY r.createdAt DESC")
    Page<Report> searchReports(@Param("status") Short status,
                               @Param("targetType") Short targetType,
                               @Param("reason") Short reason,
                               Pageable pageable);

    /**
     * 检查同一举报人对同一目标是否已有待处理的举报
     */
    boolean existsByReporterIdAndTargetTypeAndTargetIdAndStatus(Long reporterId,
                                                                 Short targetType,
                                                                 Long targetId,
                                                                 Short status);
}
