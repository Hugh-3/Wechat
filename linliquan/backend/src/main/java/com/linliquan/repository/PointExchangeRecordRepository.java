package com.linliquan.repository;

import com.linliquan.model.entity.PointExchangeRecord;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * 积分兑换记录Repository
 */
@Repository
public interface PointExchangeRecordRepository extends JpaRepository<PointExchangeRecord, Long> {

    /**
     * 分页查询用户兑换记录（按创建时间倒序）
     */
    Page<PointExchangeRecord> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);
}
