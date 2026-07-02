package com.linliquan.controller;

import com.linliquan.annotation.RateLimit;
import com.linliquan.common.Result;
import com.linliquan.model.entity.Order;
import com.linliquan.model.entity.User;
import com.linliquan.service.OrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;

/**
 * 互助任务控制器
 * 【红线强制】发布/接单接口已接入权限拦截器
 */
@RestController
@RequestMapping("/v1/orders")
public class OrderController {

    @Autowired
    private OrderService orderService;

    /**
     * 【写操作】发布互助任务
     * 【红线强制】已接入权限拦截器
     */
    @PostMapping
    @RateLimit(maxRequests = 20, windowSeconds = 60)
    public Result<Order> createOrder(HttpServletRequest request, @RequestBody Map<String, Object> params) {
        User currentUser = (User) request.getAttribute("currentUser");
        return orderService.createOrder(currentUser, params);
    }

    /**
     * 【读操作】获取附近互助任务
     * 三种状态均可访问
     */
    @GetMapping("/nearby")
    public Result<?> getNearbyOrders(
            @RequestParam Double lat,
            @RequestParam Double lng,
            @RequestParam(defaultValue = "5") Double radius) {
        return orderService.getNearbyOrders(lat, lng, radius);
    }

    /**
     * 【读操作】获取当前用户参与的互助任务
     * @param role published-我发布的, helped-我帮助的, all-全部
     */
    @GetMapping("/my")
    public Result<?> getMyOrders(
            HttpServletRequest request,
            @RequestParam(defaultValue = "all") String role,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        User currentUser = (User) request.getAttribute("currentUser");
        if (currentUser == null) {
            return Result.fail(com.linliquan.common.ResultCode.UNAUTHORIZED);
        }
        return orderService.getMyOrders(currentUser.getId(), role, page, pageSize);
    }

    /**
     * 【读操作】获取互助任务详情
     */
    @GetMapping("/{id}")
    public Result<?> getOrderDetail(HttpServletRequest request, @PathVariable Long id) {
        User currentUser = (User) request.getAttribute("currentUser");
        if (currentUser == null) {
            return Result.fail(com.linliquan.common.ResultCode.UNAUTHORIZED);
        }
        return orderService.getOrderDetail(id, currentUser.getId());
    }

    /**
     * 【写操作】接单
     * 【红线强制】已接入权限拦截器
     */
    @PostMapping("/{id}/accept")
    public Result<Void> acceptOrder(HttpServletRequest request, @PathVariable Long id) {
        User currentUser = (User) request.getAttribute("currentUser");
        return orderService.acceptOrder(id, currentUser);
    }

    /**
     * 【写操作】完成订单（完成后双方可互相评价）
     * 【红线强制】已接入权限拦截器
     */
    @PostMapping("/{id}/complete")
    public Result<Void> completeOrder(HttpServletRequest request, @PathVariable Long id) {
        User currentUser = (User) request.getAttribute("currentUser");
        return orderService.completeOrder(id, currentUser);
    }
}
