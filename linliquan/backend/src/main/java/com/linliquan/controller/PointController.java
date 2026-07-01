package com.linliquan.controller;

import com.linliquan.annotation.RateLimit;
import com.linliquan.common.Result;
import com.linliquan.common.ResultCode;
import com.linliquan.model.entity.User;
import com.linliquan.service.PointService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Map;

/**
 * 积分控制器
 * - 查询积分余额、积分流水
 * - 兑换商品、兑换记录
 */
@RestController
@RequestMapping("/v1/points")
public class PointController {

    @Autowired
    private PointService pointService;

    /**
     * 获取当前用户积分余额
     */
    @GetMapping("/balance")
    public Result<Map<String, Object>> getBalance(HttpServletRequest request) {
        User currentUser = getCurrentUser(request);
        if (currentUser == null) {
            return Result.fail(ResultCode.UNAUTHORIZED);
        }
        int balance = pointService.getPointsBalance(currentUser.getId());
        Map<String, Object> data = new HashMap<>();
        data.put("totalPoints", balance);
        return Result.success(data);
    }

    /**
     * 获取积分流水记录（分页）
     */
    @GetMapping("/transactions")
    public Result<?> getTransactions(
            HttpServletRequest request,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        User currentUser = getCurrentUser(request);
        if (currentUser == null) {
            return Result.fail(ResultCode.UNAUTHORIZED);
        }
        return pointService.getTransactionList(currentUser.getId(), page, pageSize);
    }

    /**
     * 获取兑换商品列表（分页）
     */
    @GetMapping("/exchange/items")
    public Result<?> getExchangeItems(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        return pointService.getExchangeItemList(page, pageSize);
    }

    /**
     * 兑换商品
     */
    @PostMapping("/exchange/{itemId}")
    @RateLimit(maxRequests = 10, windowSeconds = 60)
    public Result<Void> exchangeItem(HttpServletRequest request, @PathVariable Long itemId) {
        User currentUser = getCurrentUser(request);
        if (currentUser == null) {
            return Result.fail(ResultCode.UNAUTHORIZED);
        }
        return pointService.exchangeItem(currentUser.getId(), itemId);
    }

    /**
     * 获取用户兑换记录（分页）
     */
    @GetMapping("/exchange/records")
    public Result<?> getExchangeRecords(
            HttpServletRequest request,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        User currentUser = getCurrentUser(request);
        if (currentUser == null) {
            return Result.fail(ResultCode.UNAUTHORIZED);
        }
        return pointService.getExchangeRecords(currentUser.getId(), page, pageSize);
    }

    /**
     * 从请求中获取当前登录用户
     */
    private User getCurrentUser(HttpServletRequest request) {
        return (User) request.getAttribute("currentUser");
    }
}
