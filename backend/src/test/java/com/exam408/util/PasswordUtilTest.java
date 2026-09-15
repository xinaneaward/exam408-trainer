package com.exam408.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PasswordUtilTest {

    @Test
    void hashAndMatchSucceeds() {
        String hash = PasswordUtil.hash("123456");
        assertTrue(PasswordUtil.isPbkdf2(hash));
        assertTrue(PasswordUtil.matches("123456", hash));
    }

    @Test
    void wrongPasswordFails() {
        String hash = PasswordUtil.hash("correct");
        assertFalse(PasswordUtil.matches("wrong", hash));
    }

    @Test
    void hashUsesRandomSalt() {
        assertNotEquals(PasswordUtil.hash("same"), PasswordUtil.hash("same"));
    }

    @Test
    void legacyMd5Detection() {
        assertTrue(PasswordUtil.isLegacyMd5("e10adc3949ba59abbe56e057f20f883e"));
        assertFalse(PasswordUtil.isLegacyMd5("pbkdf2:120000:Ab:Cd"));
        assertFalse(PasswordUtil.isLegacyMd5("not-a-hash"));
    }

    @Test
    void malformedStoredValueDoesNotThrow() {
        assertFalse(PasswordUtil.matches("x", null));
        assertFalse(PasswordUtil.matches("x", "pbkdf2:abc"));
        assertFalse(PasswordUtil.matches("x", "not-pbkdf2:1:zzz:zzz"));
    }
}