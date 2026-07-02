package com.linliquan.controller;

import com.linliquan.annotation.RateLimit;
import com.linliquan.common.Result;
import com.linliquan.common.ResultCode;
import com.linliquan.model.entity.User;
import com.linliquan.service.FavoriteService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;

/**
 * 收藏控制器
 * 【红线强制】所有写接口已接入@VerifiedOnly拦截器
 */
@RestController
@RequestMapping("/v1/favorites")
public class FavoriteController {

    @Autowired
    private FavoriteService favoriteService;

    /**
     * 【写操作】收藏/取消收藏
     * 【红线强制】已接入权限拦截器，非VERIFIED用户返回403
     */
    @PostMapping("/{postId}")
    @RateLimit(maxRequests = 30, windowSeconds = 60)
    public Result<?> toggleFavorite(
            HttpServletRequest request,
            @PathVariable Long postId,
            @RequestBody Map<String, Object> params) {
        User currentUser = (User) request.getAttribute("currentUser");
        if (currentUser == null) {
            return Result.fail(ResultCode.UNAUTHORIZED);
        }
        Boolean favorited = params.get("favorited") != null && Boolean.parseBoolean(params.get("favorited").toString());
        return favoriteService.toggleFavorite(postId, currentUser.getId(), favorited);
    }

    /**
     * 【读操作】获取当前用户的收藏列表
     */
    @GetMapping("/my")
    public Result<?> getMyFavorites(
            HttpServletRequest request,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        User currentUser = (User) request.getAttribute("currentUser");
        if (currentUser == null) {
            return Result.fail(ResultCode.UNAUTHORIZED);
        }
        return favoriteService.getMyFavorites(currentUser.getId(), page, pageSize);
    }
}
