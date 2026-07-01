package com.linliquan.controller;

import com.linliquan.common.Result;
import com.linliquan.common.ResultCode;
import com.linliquan.model.entity.Review;
import com.linliquan.model.entity.User;
import com.linliquan.service.ReviewService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;

/**
 * 评价控制器
 * 【红线强制】写接口已接入权限拦截器
 */
@RestController
@RequestMapping("/v1/reviews")
public class ReviewController {

    @Autowired
    private ReviewService reviewService;

    /**
     * 【写操作】创建评价
     * 【红线强制】已接入权限拦截器
     */
    @PostMapping
    public Result<Review> createReview(HttpServletRequest request, @RequestBody Map<String, Object> params) {
        User currentUser = (User) request.getAttribute("currentUser");
        if (currentUser == null) {
            return Result.fail(ResultCode.UNAUTHORIZED);
        }

        Long orderId = params.get("orderId") != null ? Long.valueOf(params.get("orderId").toString()) : null;
        int rating = params.get("rating") != null ? Integer.parseInt(params.get("rating").toString()) : 0;
        String content = params.get("content") != null ? params.get("content").toString() : null;

        @SuppressWarnings("unchecked")
        List<String> images = params.get("images") != null ? (List<String>) params.get("images") : null;

        return reviewService.createReview(currentUser, orderId, rating, content, images);
    }

    /**
     * 【读操作】获取订单的所有评价
     */
    @GetMapping("/order/{orderId}")
    public Result<List<Review>> getOrderReviews(@PathVariable Long orderId) {
        return reviewService.getOrderReviews(orderId);
    }

    /**
     * 【读操作】获取用户收到的评价（分页）
     */
    @GetMapping("/user/{userId}")
    public Result<?> getUserReviews(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        return reviewService.getUserReviews(userId, page, pageSize);
    }

    /**
     * 【读操作】获取用户评分汇总（平均分、总数、1-5星分布）
     */
    @GetMapping("/user/{userId}/summary")
    public Result<?> getUserRatingSummary(@PathVariable Long userId) {
        return reviewService.getUserRatingSummary(userId);
    }

    /**
     * 【读操作】检查当前用户是否可评价该订单
     */
    @GetMapping("/order/{orderId}/can-review")
    public Result<?> canReview(HttpServletRequest request, @PathVariable Long orderId) {
        User currentUser = (User) request.getAttribute("currentUser");
        if (currentUser == null) {
            return Result.fail(ResultCode.UNAUTHORIZED);
        }
        return reviewService.canReview(orderId, currentUser.getId());
    }
}
