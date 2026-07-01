package com.linliquan.repository;

import com.linliquan.model.entity.Comment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CommentRepository extends JpaRepository<Comment, Long> {

    Page<Comment> findByPostIdAndStatusOrderByCreatedAtDesc(Long postId, Integer status, Pageable pageable);

    int countByPostIdAndStatus(Long postId, Integer status);
}
