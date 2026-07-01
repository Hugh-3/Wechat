package com.linliquan.model.enums;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * VerificationStatus枚举单元测试
 */
public class VerificationStatusTest {

    @Test
    public void testFromCode() {
        assertEquals(VerificationStatus.UNAUTH, VerificationStatus.fromCode(0));
        assertEquals(VerificationStatus.PENDING, VerificationStatus.fromCode(1));
        assertEquals(VerificationStatus.VERIFIED, VerificationStatus.fromCode(2));
    }

    @Test
    public void testInvalidCode() {
        assertThrows(IllegalArgumentException.class, () -> {
            VerificationStatus.fromCode(99);
        });
    }

    @Test
    public void testIsVerified() {
        assertTrue(VerificationStatus.VERIFIED.isVerified());
        assertFalse(VerificationStatus.UNAUTH.isVerified());
        assertFalse(VerificationStatus.PENDING.isVerified());
    }

    @Test
    public void testCanWrite() {
        assertTrue(VerificationStatus.VERIFIED.canWrite());
        assertFalse(VerificationStatus.UNAUTH.canWrite());
        assertFalse(VerificationStatus.PENDING.canWrite());
    }

    @Test
    public void testCanRead() {
        // 三种状态均可读
        assertTrue(VerificationStatus.UNAUTH.canRead());
        assertTrue(VerificationStatus.PENDING.canRead());
        assertTrue(VerificationStatus.VERIFIED.canRead());
    }
}
