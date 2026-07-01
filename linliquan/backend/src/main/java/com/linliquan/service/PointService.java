package com.linliquan.service;

import com.linliquan.common.Result;
import com.linliquan.common.ResultCode;
import com.linliquan.model.entity.PointExchangeItem;
import com.linliquan.model.entity.PointExchangeRecord;
import com.linliquan.model.entity.PointTransaction;
import com.linliquan.model.entity.UserPoints;
import com.linliquan.repository.PointExchangeItemRepository;
import com.linliquan.repository.PointExchangeRecordRepository;
import com.linliquan.repository.PointTransactionRepository;
import com.linliquan.repository.UserPointsRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * 积分服务
 * 负责积分账户管理、积分增减、流水记录、兑换商品等
 */
@Service
public class PointService {

    // ==================== 积分规则常量 ====================

    /** 发布动态 +10 */
    public static final int POST_CREATE = 10;

    /** 发布互助任务 +15 */
    public static final int ORDER_PUBLISH = 15;

    /** 接单 +20 */
    public static final int ORDER_ACCEPT = 20;

    /** 完成互助任务 +30 */
    public static final int ORDER_COMPLETE = 30;

    /** 获得点赞 +5 */
    public static final int LIKE_RECEIVED = 5;

    /** 收到评论 +3 */
    public static final int COMMENT_RECEIVED = 3;

    /** 每日登录 +1 */
    public static final int DAILY_LOGIN = 1;

    /** 交易类型：获取 */
    private static final int TYPE_EARN = 1;

    /** 交易类型：消耗 */
    private static final int TYPE_CONSUME = 2;

    @Autowired
    private UserPointsRepository userPointsRepository;

    @Autowired
    private PointTransactionRepository pointTransactionRepository;

    @Autowired
    private PointExchangeItemRepository pointExchangeItemRepository;

    @Autowired
    private PointExchangeRecordRepository pointExchangeRecordRepository;

    /**
     * 获取或创建用户积分账户
     */
    @Transactional
    public UserPoints getOrCreateUserPoints(Long userId) {
        return userPointsRepository.findByUserId(userId).orElseGet(() -> {
            UserPoints userPoints = new UserPoints();
            userPoints.setUserId(userId);
            userPoints.setTotalPoints(0);
            userPoints.setTotalEarned(0);
            userPoints.setTotalConsumed(0);
            userPoints.setUpdatedAt(LocalDateTime.now());
            return userPointsRepository.save(userPoints);
        });
    }

    /**
     * 查询用户当前积分余额
     */
    public int getPointsBalance(Long userId) {
        UserPoints userPoints = getOrCreateUserPoints(userId);
        return userPoints.getTotalPoints();
    }

    /**
     * 增加积分（含流水记录）
     */
    @Transactional
    public void addPoints(Long userId, int points, String action, String relatedType, Long relatedId, String remark) {
        if (points <= 0) {
            return;
        }
        // 确保账户存在
        UserPoints userPoints = getOrCreateUserPoints(userId);

        // 更新余额（累加）
        userPointsRepository.updatePointsBalance(userId, points, points, 0);

        // 刷新最新余额
        int balanceAfter = userPoints.getTotalPoints() + points;

        // 写入流水
        PointTransaction transaction = new PointTransaction();
        transaction.setUserId(userId);
        transaction.setType(TYPE_EARN);
        transaction.setPoints(points);
        transaction.setAction(action);
        transaction.setRelatedType(relatedType);
        transaction.setRelatedId(relatedId);
        transaction.setRemark(remark);
        transaction.setBalanceAfter(balanceAfter);
        transaction.setCreatedAt(LocalDateTime.now());
        pointTransactionRepository.save(transaction);
    }

    /**
     * 扣减积分（含流水记录）
     * 并发安全：先校验余额充足，再原子扣减
     */
    @Transactional
    public boolean deductPoints(Long userId, int points, String action, String relatedType, Long relatedId, String remark) {
        if (points <= 0) {
            return false;
        }
        // 校验余额
        UserPoints userPoints = getOrCreateUserPoints(userId);
        if (userPoints.getTotalPoints() < points) {
            return false;
        }

        // 原子扣减余额（含并发安全校验）
        int affected = userPointsRepository.deductPointsIfSufficient(userId, points);
        if (affected == 0) {
            // 并发竞争导致余额不足
            return false;
        }

        int balanceAfter = userPoints.getTotalPoints() - points;

        // 写入流水
        PointTransaction transaction = new PointTransaction();
        transaction.setUserId(userId);
        transaction.setType(TYPE_CONSUME);
        transaction.setPoints(points);
        transaction.setAction(action);
        transaction.setRelatedType(relatedType);
        transaction.setRelatedId(relatedId);
        transaction.setRemark(remark);
        transaction.setBalanceAfter(balanceAfter);
        transaction.setCreatedAt(LocalDateTime.now());
        pointTransactionRepository.save(transaction);

        return true;
    }

    /**
     * 分页查询用户积分流水
     */
    public Result<Map<String, Object>> getTransactionList(Long userId, int page, int pageSize) {
        Pageable pageable = PageRequest.of(Math.max(page - 1, 0), Math.max(pageSize, 1));
        Page<PointTransaction> transactionPage = pointTransactionRepository
                .findByUserIdOrderByCreatedAtDesc(userId, pageable);

        Map<String, Object> result = new HashMap<>();
        result.put("list", transactionPage.getContent());
        result.put("total", transactionPage.getTotalElements());
        result.put("page", page);
        result.put("pageSize", pageSize);
        result.put("totalPages", transactionPage.getTotalPages());

        return Result.success(result);
    }

    /**
     * 分页查询上架的兑换商品
     */
    public Result<Map<String, Object>> getExchangeItemList(int page, int pageSize) {
        Pageable pageable = PageRequest.of(Math.max(page - 1, 0), Math.max(pageSize, 1));
        Page<PointExchangeItem> itemPage = pointExchangeItemRepository
                .findByStatusOrderByCreatedAtDesc(1, pageable);

        Map<String, Object> result = new HashMap<>();
        result.put("list", itemPage.getContent());
        result.put("total", itemPage.getTotalElements());
        result.put("page", page);
        result.put("pageSize", pageSize);
        result.put("totalPages", itemPage.getTotalPages());

        return Result.success(result);
    }

    /**
     * 兑换商品
     * 流程：校验商品 -> 校验余额 -> 扣减积分 -> 创建兑换记录 -> 扣减库存
     */
    @Transactional
    public Result<Void> exchangeItem(Long userId, Long itemId) {
        // 校验商品
        PointExchangeItem item = pointExchangeItemRepository.findById(itemId).orElse(null);
        if (item == null || item.getStatus() != 1) {
            return Result.fail(ResultCode.NOT_FOUND);
        }
        if (item.getStock() <= 0) {
            return Result.fail(ResultCode.BAD_REQUEST.getCode(), "商品库存不足");
        }

        // 校验余额并扣减积分
        boolean deducted = deductPoints(userId, item.getPointsRequired(),
                "exchange", "point_exchange_item", itemId, "兑换：" + item.getName());
        if (!deducted) {
            return Result.fail(ResultCode.BAD_REQUEST.getCode(), "积分不足");
        }

        // 创建兑换记录（待发放）
        PointExchangeRecord record = new PointExchangeRecord();
        record.setUserId(userId);
        record.setItemId(itemId);
        record.setPointsCost(item.getPointsRequired());
        record.setStatus(1);
        record.setCreatedAt(LocalDateTime.now());
        pointExchangeRecordRepository.save(record);

        // 扣减库存
        item.setStock(item.getStock() - 1);
        item.setUpdatedAt(LocalDateTime.now());
        pointExchangeItemRepository.save(item);

        return Result.success(null);
    }

    /**
     * 分页查询用户兑换记录
     */
    public Result<Map<String, Object>> getExchangeRecords(Long userId, int page, int pageSize) {
        Pageable pageable = PageRequest.of(Math.max(page - 1, 0), Math.max(pageSize, 1));
        Page<PointExchangeRecord> recordPage = pointExchangeRecordRepository
                .findByUserIdOrderByCreatedAtDesc(userId, pageable);

        // 填充商品名称和图片
        for (PointExchangeRecord record : recordPage.getContent()) {
            PointExchangeItem item = pointExchangeItemRepository.findById(record.getItemId()).orElse(null);
            if (item != null) {
                record.setItemName(item.getName());
                record.setItemImageUrl(item.getImageUrl());
            }
        }

        Map<String, Object> result = new HashMap<>();
        result.put("list", recordPage.getContent());
        result.put("total", recordPage.getTotalElements());
        result.put("page", page);
        result.put("pageSize", pageSize);
        result.put("totalPages", recordPage.getTotalPages());

        return Result.success(result);
    }
}
