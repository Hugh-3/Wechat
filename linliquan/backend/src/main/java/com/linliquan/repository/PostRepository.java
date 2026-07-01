package com.linliquan.repository;

import com.linliquan.model.entity.Post;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * 帖子Repository
 */
@Repository
public interface PostRepository extends JpaRepository<Post, Long>, JpaSpecificationExecutor<Post> {

    /**
     * 分页查询帖子列表（按类型筛选）
     */
    Page<Post> findByPostTypeAndStatusOrderByCreatedAtDesc(Integer postType, Integer status, Pageable pageable);

    /**
     * 分页查询所有帖子
     */
    Page<Post> findByStatusOrderByCreatedAtDesc(Integer status, Pageable pageable);

    /**
     * 查询用户发布的帖子
     */
    Page<Post> findByUserIdAndStatusOrderByCreatedAtDesc(Long userId, Integer status, Pageable pageable);

    /**
     * 统计用户发布的帖子数
     */
    long countByUserIdAndStatus(Long userId, Integer status);
}
