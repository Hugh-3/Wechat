package com.linliquan.repository;

import com.linliquan.model.entity.PointExchangeItem;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * 积分兑换商品Repository
 */
@Repository
public interface PointExchangeItemRepository extends JpaRepository<PointExchangeItem, Long> {

    /**
     * 分页查询指定状态的商品（按创建时间倒序）
     */
    Page<PointExchangeItem> findByStatusOrderByCreatedAtDesc(Integer status, Pageable pageable);
}
