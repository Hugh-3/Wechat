package com.linliquan.service;

import com.linliquan.common.Result;
import com.linliquan.common.ResultCode;
import com.linliquan.model.entity.Order;
import com.linliquan.model.entity.Post;
import com.linliquan.model.entity.Review;
import com.linliquan.model.entity.User;
import com.linliquan.repository.OrderRepository;
import com.linliquan.repository.PostRepository;
import com.linliquan.repository.ReviewRepository;
import com.linliquan.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 评价服务
 *
 * 业务规则：
 *  1. 仅已完成（status=3）的订单可以评价
 *  2. 仅订单的发布者或帮助者可以评价
 *  3. 同一订单同一用户只能评价一次（数据库唯一约束保证）
 *  4. 双方均完成评价后，给双方发放 ORDER_COMPLETE 积分
 */
@Service
public class ReviewService {

    /** 订单状态：已完成 */
    private static final int ORDER_STATUS_COMPLETED = 3;

    /** role：发布者评价帮助者 */
    private static final short ROLE_OWNER_REVIEW_HELPER = 1;

    /** role：帮助者评价发布者 */
    private static final short ROLE_HELPER_REVIEW_OWNER = 2;

    @Autowired
    private ReviewRepository reviewRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PostRepository postRepository;

    @Autowired
    private PointService pointService;

    @Autowired
    private NotificationService notificationService;

    /**
     * 创建评价
     *
     * @param user     当前登录用户（评价人）
     * @param orderId  订单ID
     * @param rating   评分 1-5
     * @param content  评价内容（可选）
     * @param images   评价图片（可选，最多3张）
     */
    @Transactional
    public Result<Review> createReview(User user, Long orderId, int rating, String content, List<String> images) {
        // 参数校验
        if (orderId == null || orderId <= 0) {
            return Result.fail(ResultCode.BAD_REQUEST);
        }
        if (rating < 1 || rating > 5) {
            return Result.fail(ResultCode.BAD_REQUEST.getCode(), "评分需在1-5星之间");
        }

        // 校验订单存在
        Order order = orderRepository.findById(orderId).orElse(null);
        if (order == null) {
            return Result.fail(ResultCode.NOT_FOUND);
        }

        // 校验订单状态为已完成
        if (order.getStatus() == null || order.getStatus() != ORDER_STATUS_COMPLETED) {
            return Result.fail(ResultCode.BAD_REQUEST.getCode(), "仅已完成的订单可评价");
        }

        // 校验当前用户为订单的发布者或帮助者
        boolean isOwner = order.getUserId() != null && order.getUserId().equals(user.getId());
        boolean isHelper = order.getHelperUserId() != null && order.getHelperUserId().equals(user.getId());
        if (!isOwner && !isHelper) {
            return Result.fail(ResultCode.FORBIDDEN_UNVERIFIED.getCode(), "无权评价此订单");
        }

        // 校验帮助者存在（帮助者未接单的情况无法评价）
        if (order.getHelperUserId() == null) {
            return Result.fail(ResultCode.BAD_REQUEST.getCode(), "该订单尚无帮助者，无法评价");
        }

        // 唯一性校验：每人每单仅可评价一次
        if (reviewRepository.existsByOrderIdAndReviewerId(orderId, user.getId())) {
            return Result.fail(ResultCode.CONFLICT.getCode(), "您已评价过此订单");
        }

        // 确定角色与被评价人
        short role;
        Long revieweeId;
        if (isOwner) {
            // 发布者评价帮助者
            role = ROLE_OWNER_REVIEW_HELPER;
            revieweeId = order.getHelperUserId();
        } else {
            // 帮助者评价发布者
            role = ROLE_HELPER_REVIEW_OWNER;
            revieweeId = order.getUserId();
        }

        // 构建评价实体
        Review review = new Review();
        review.setOrderId(orderId);
        review.setReviewerId(user.getId());
        review.setRevieweeId(revieweeId);
        review.setRole(role);
        review.setRating((short) rating);
        review.setContent(content);
        review.setImages(images);
        review.setCreatedAt(LocalDateTime.now());

        Review saved = reviewRepository.save(review);

        // 填充展示字段
        fillTransientFields(saved);

        // 通知被评价人
        try {
            String revieweeName = saved.getRevieweeName() != null ? saved.getRevieweeName() : "您";
            String title = "收到新评价";
            String notifContent = (rating >= 4 ? "感谢您的优秀服务，" : "") + "您收到一条 " + rating + " 星评价";
            notificationService.sendNotification(revieweeId, NotificationService.TYPE_ORDER,
                    title, notifContent, "order", orderId);
        } catch (Exception ignored) {
            // 通知失败不影响主流程
        }

        // 双方评价完成后，给双方发放完成积分
        tryAwardBothPartiesOnCompletion(order);

        return Result.success(saved);
    }

    /**
     * 获取订单的所有评价
     */
    public Result<List<Review>> getOrderReviews(Long orderId) {
        if (orderId == null) {
            return Result.fail(ResultCode.BAD_REQUEST);
        }
        List<Review> reviews = reviewRepository.findByOrderId(orderId);
        for (Review r : reviews) {
            fillTransientFields(r);
        }
        return Result.success(reviews);
    }

    /**
     * 获取用户收到的评价（分页）
     */
    public Result<Map<String, Object>> getUserReviews(Long userId, int page, int pageSize) {
        if (userId == null) {
            return Result.fail(ResultCode.BAD_REQUEST);
        }
        Pageable pageable = PageRequest.of(Math.max(page - 1, 0), Math.max(pageSize, 1));
        Page<Review> reviewPage = reviewRepository.findByRevieweeIdOrderByCreatedAtDesc(userId, pageable);

        for (Review r : reviewPage.getContent()) {
            fillTransientFields(r);
        }

        Map<String, Object> result = new HashMap<>();
        result.put("list", reviewPage.getContent());
        result.put("total", reviewPage.getTotalElements());
        result.put("page", page);
        result.put("pageSize", pageSize);
        result.put("totalPages", reviewPage.getTotalPages());

        return Result.success(result);
    }

    /**
     * 获取用户评分汇总：平均分、评价总数、1-5星分布
     */
    public Result<Map<String, Object>> getUserRatingSummary(Long userId) {
        if (userId == null) {
            return Result.fail(ResultCode.BAD_REQUEST);
        }

        Double avg = reviewRepository.findAverageRatingByRevieweeId(userId);
        double averageRating = avg != null ? Math.round(avg * 10) / 10.0 : 0.0;
        long total = reviewRepository.countByRevieweeId(userId);

        // 星级分布
        Map<Integer, Long> distribution = new HashMap<>();
        for (int star = 1; star <= 5; star++) {
            distribution.put(star, 0L);
        }
        List<Object[]> grouped = reviewRepository.countByRatingGroupByRevieweeId(userId);
        if (grouped != null) {
            for (Object[] row : grouped) {
                int star = ((Number) row[0]).intValue();
                long count = ((Number) row[1]).longValue();
                if (star >= 1 && star <= 5) {
                    distribution.put(star, count);
                }
            }
        }

        Map<String, Object> result = new HashMap<>();
        result.put("averageRating", averageRating);
        result.put("totalReviews", total);
        result.put("distribution", distribution);

        return Result.success(result);
    }

    /**
     * 检查用户是否可评价该订单（订单已完成且尚未评价）
     */
    public Result<Map<String, Object>> canReview(Long orderId, Long userId) {
        Map<String, Object> result = new HashMap<>();
        if (orderId == null || userId == null) {
            result.put("canReview", false);
            result.put("reason", "参数错误");
            return Result.success(result);
        }

        Order order = orderRepository.findById(orderId).orElse(null);
        if (order == null) {
            result.put("canReview", false);
            result.put("reason", "订单不存在");
            return Result.success(result);
        }

        if (order.getStatus() == null || order.getStatus() != ORDER_STATUS_COMPLETED) {
            result.put("canReview", false);
            result.put("reason", "订单未完成");
            return Result.success(result);
        }

        boolean isOwner = order.getUserId() != null && order.getUserId().equals(userId);
        boolean isHelper = order.getHelperUserId() != null && order.getHelperUserId().equals(userId);
        if (!isOwner && !isHelper) {
            result.put("canReview", false);
            result.put("reason", "您不是该订单的参与方");
            return Result.success(result);
        }

        if (order.getHelperUserId() == null) {
            result.put("canReview", false);
            result.put("reason", "该订单尚无帮助者");
            return Result.success(result);
        }

        if (reviewRepository.existsByOrderIdAndReviewerId(orderId, userId)) {
            result.put("canReview", false);
            result.put("reason", "您已评价过此订单");
            return Result.success(result);
        }

        result.put("canReview", true);
        result.put("role", isOwner ? ROLE_OWNER_REVIEW_HELPER : ROLE_HELPER_REVIEW_OWNER);
        return Result.success(result);
    }

    // ==================== 内部辅助方法 ====================

    /**
     * 填充展示字段（评价人/被评价人昵称头像、订单标题）
     */
    private void fillTransientFields(Review review) {
        if (review == null) {
            return;
        }
        if (review.getReviewerId() != null) {
            userRepository.findById(review.getReviewerId()).ifPresent(u -> {
                review.setReviewerName(u.getNickname());
                review.setReviewerAvatar(u.getAvatarUrl());
            });
        }
        if (review.getRevieweeId() != null) {
            userRepository.findById(review.getRevieweeId()).ifPresent(u -> {
                review.setRevieweeName(u.getNickname());
                review.setRevieweeAvatar(u.getAvatarUrl());
            });
        }
        if (review.getOrderId() != null) {
            orderRepository.findById(review.getOrderId()).ifPresent(order -> {
                // 通过订单关联的 postId 获取任务标题
                if (order.getPostId() != null) {
                    postRepository.findById(order.getPostId()).ifPresent(post -> {
                        review.setOrderTitle(post.getTitle());
                    });
                }
            });
        }
    }

    /**
     * 双方都评价完成后，给双方发放 ORDER_COMPLETE 积分
     */
    private void tryAwardBothPartiesOnCompletion(Order order) {
        try {
            Long ownerId = order.getUserId();
            Long helperId = order.getHelperUserId();
            if (ownerId == null || helperId == null) {
                return;
            }
            boolean ownerReviewed = reviewRepository.existsByOrderIdAndReviewerId(order.getId(), ownerId);
            boolean helperReviewed = reviewRepository.existsByOrderIdAndReviewerId(order.getId(), helperId);
            if (ownerReviewed && helperReviewed) {
                // 双方均完成评价，发放完成积分
                pointService.addPoints(ownerId, PointService.ORDER_COMPLETE,
                        "order_complete", "order", order.getId(), "互助任务完成（双方评价完成）");
                pointService.addPoints(helperId, PointService.ORDER_COMPLETE,
                        "order_complete", "order", order.getId(), "互助任务完成（双方评价完成）");
            }
        } catch (Exception ignored) {
            // 积分发放失败不影响评价主流程
        }
    }
}
