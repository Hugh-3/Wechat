package com.linliquan.repository;

import com.linliquan.model.entity.Review;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 评价 Repository
 */
@Repository
public interface ReviewRepository extends JpaRepository<Review, Long> {

    /**
     * 查询订单的所有评价
     */
    List<Review> findByOrderId(Long orderId);

    /**
     * 查询某用户发出的评价（分页）
     */
    Page<Review> findByReviewerIdOrderByCreatedAtDesc(Long reviewerId, Pageable pageable);

    /**
     * 查询某用户收到的评价（分页）
     */
    Page<Review> findByRevieweeIdOrderByCreatedAtDesc(Long revieweeId, Pageable pageable);

    /**
     * 检查某订单某用户是否已评价
     */
    boolean existsByOrderIdAndReviewerId(Long orderId, Long reviewerId);

    /**
     * 统计某用户收到的评价总数
     */
    long countByRevieweeId(Long revieweeId);

    /**
     * 计算某用户的平均评分
     */
    @Query("SELECT COALESCE(AVG(r.rating), 0) FROM Review r WHERE r.revieweeId = :userId")
    Double findAverageRatingByRevieweeId(@Param("userId") Long userId);

    /**
     * 按星级统计某用户收到的评价数量
     * @return [rating, count]
     */
    @Query("SELECT r.rating, COUNT(r) FROM Review r WHERE r.revieweeId = :userId GROUP BY r.rating ORDER BY r.rating DESC")
    List<Object[]> countByRatingGroupByRevieweeId(@Param("userId") Long userId);
}
