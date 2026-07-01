package com.linliquan.repository;

import com.linliquan.model.entity.PointTransaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * 积分流水Repository
 */
@Repository
public interface PointTransactionRepository extends JpaRepository<PointTransaction, Long> {

    /**
     * 分页查询用户积分流水（按创建时间倒序）
     */
    Page<PointTransaction> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);
}
