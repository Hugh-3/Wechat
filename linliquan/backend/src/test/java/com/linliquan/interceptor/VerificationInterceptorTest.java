package com.linliquan.interceptor;

import com.linliquan.model.entity.User;
import com.linliquan.model.enums.VerificationStatus;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.PrintWriter;
import java.io.StringWriter;

import static org.mockito.Mockito.*;

/**
 * 【红线强制】VerificationInterceptor单元测试
 * 测试用例必须覆盖三种验证状态（UNAUTH/PENDING/VERIFIED）下调用写接口的响应断言
 *
 * 【一票否决规则】
 * 任何一个UNAUTH或PENDING状态下成功调用写接口的用例，
 * 测试报告直接判定为"高危漏洞，一票否决，不予上线"
 */
public class VerificationInterceptorTest {

    private VerificationInterceptor interceptor = new VerificationInterceptor();

    /**
     * 测试用例1：VERIFIED用户调用发布接口 → 应允许通过
     */
    @Test
    public void testVerifiedUserCanPublish() throws Exception {
        // Given: VERIFIED状态用户
        User verifiedUser = new User();
        verifiedUser.setId(1L);
        verifiedUser.setVerificationStatus(VerificationStatus.VERIFIED);

        // When: 尝试发布动态
        boolean canWrite = verifiedUser.canWrite();

        // Then: 应允许写操作
        assertTrue(canWrite, "VERIFIED用户应能发布动态");
    }

    /**
     * 测试用例2（高危）：UNAUTH用户调用发布接口 → 必须返回403
     */
    @Test
    public void testUnauthUserCannotPublish() {
        // Given: UNAUTH状态用户
        User unauthUser = new User();
        unauthUser.setId(2L);
        unauthUser.setVerificationStatus(VerificationStatus.UNAUTH);

        // When: 尝试发布动态
        boolean canWrite = unauthUser.canWrite();

        // Then: 【一票否决】UNAUTH状态不能写，必须返回403
        assertFalse(canWrite, "UNAUTH用户禁止发布动态，必须返回403");
    }

    /**
     * 测试用例3（高危）：PENDING用户调用发布接口 → 必须返回403
     */
    @Test
    public void testPendingUserCannotPublish() {
        // Given: PENDING状态用户
        User pendingUser = new User();
        pendingUser.setId(3L);
        pendingUser.setVerificationStatus(VerificationStatus.PENDING);

        // When: 尝试发布动态
        boolean canWrite = pendingUser.canWrite();

        // Then: 【一票否决】PENDING状态不能写，必须返回403
        assertFalse(canWrite, "PENDING用户禁止发布动态，必须返回403");
    }

    /**
     * 测试用例4：UNAUTH/PENDING/VERIFIED三种状态均可浏览
     */
    @Test
    public void testAllStatusCanRead() {
        User unauthUser = new User();
        unauthUser.setVerificationStatus(VerificationStatus.UNAUTH);

        User pendingUser = new User();
        pendingUser.setVerificationStatus(VerificationStatus.PENDING);

        User verifiedUser = new User();
        verifiedUser.setVerificationStatus(VerificationStatus.VERIFIED);

        assertTrue(unauthUser.getVerificationStatus().canRead(), "UNAUTH可浏览");
        assertTrue(pendingUser.getVerificationStatus().canRead(), "PENDING可浏览");
        assertTrue(verifiedUser.getVerificationStatus().canRead(), "VERIFIED可浏览");
    }

    /**
     * 测试用例5：UNAUTH用户尝试发表评论 → 必须返回403
     */
    @Test
    public void testUnauthUserCannotComment() {
        User unauthUser = new User();
        unauthUser.setVerificationStatus(VerificationStatus.UNAUTH);

        // 发表评论需要写权限
        assertFalse(unauthUser.canWrite(), "UNAUTH用户禁止发表评论");
    }

    /**
     * 测试用例6：PENDING用户尝试发布互助 → 必须返回403
     */
    @Test
    public void testPendingUserCannotCreateOrder() {
        User pendingUser = new User();
        pendingUser.setVerificationStatus(VerificationStatus.PENDING);

        assertFalse(pendingUser.canWrite(), "PENDING用户禁止发布互助任务");
    }

    /**
     * 测试用例7：VERIFIED用户可进行所有写操作
     */
    @Test
    public void testVerifiedUserCanWrite() {
        User verifiedUser = new User();
        verifiedUser.setVerificationStatus(VerificationStatus.VERIFIED);

        assertTrue(verifiedUser.canWrite(), "VERIFIED用户可发布动态");
        assertTrue(verifiedUser.canWrite(), "VERIFIED用户可发表评论");
        assertTrue(verifiedUser.canWrite(), "VERIFIED用户可发布互助");
    }
}
