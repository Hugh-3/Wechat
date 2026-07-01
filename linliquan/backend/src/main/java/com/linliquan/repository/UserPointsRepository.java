package com.linliquan.repository;

import com.linliquan.model.entity.UserPoints;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * 用户积分账户Repository
 */
@Repository
public interface UserPointsRepository extends JpaRepository<UserPoints, Long> {

    /**
     * 根据用户ID查询积分账户
     */
    Optional<UserPoints> findByUserId(Long userId);

    /**
     * 更新用户积分余额（用于增加积分，无余额校验）
     * @param userId        用户ID
     * @param pointsDelta   积分变化量（正数加，负数减）
     * @param earnedDelta   累计获取变化量
     * @param consumedDelta 累计消耗变化量
     * @return 受影响行数
     */
    @Modifying
    @Query("UPDATE UserPoints up SET up.totalPoints = up.totalPoints + :pointsDelta, " +
           "up.totalEarned = up.totalEarned + :earnedDelta, " +
           "up.totalConsumed = up.totalConsumed + :consumedDelta, " +
           "up.updatedAt = CURRENT_TIMESTAMP WHERE up.userId = :userId")
    int updatePointsBalance(@Param("userId") Long userId,
                            @Param("pointsDelta") int pointsDelta,
                            @Param("earnedDelta") int earnedDelta,
                            @Param("consumedDelta") int consumedDelta);

    /**
     * 原子扣减积分（含并发安全校验：仅当余额充足时才扣减）
     * @param userId 用户ID
     * @param points 待扣减积分数
     * @return 受影响行数（1-成功，0-余额不足）
     */
    @Modifying
    @Query("UPDATE UserPoints up SET up.totalPoints = up.totalPoints - :points, " +
           "up.totalConsumed = up.totalConsumed + :points, " +
           "up.updatedAt = CURRENT_TIMESTAMP " +
           "WHERE up.userId = :userId AND up.totalPoints >= :points")
    int deductPointsIfSufficient(@Param("userId") Long userId, @Param("points") int points);
}
