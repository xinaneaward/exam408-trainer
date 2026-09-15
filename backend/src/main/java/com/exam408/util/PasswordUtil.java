package com.exam408.util;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * 密码摘要工具：使用 JDK 自带的 PBKDF2WithHmacSHA256（无需第三方依赖）。
 * 存储格式：pbkdf2:迭代次数:salt(Base64):hash(Base64)
 * 兼容旧版无盐 MD5 存储（由登录逻辑负责识别并热升级）。
 */
public final class PasswordUtil {

    private static final String PREFIX = "pbkdf2:";
    private static final int ITERATIONS = 120000;
    private static final int KEY_LENGTH = 256;
    private static final SecureRandom RANDOM = new SecureRandom();

    private PasswordUtil() {
    }

    /** 是否为 PBKDF2 格式存储的密码 */
    public static boolean isPbkdf2(String stored) {
        return stored != null && stored.startsWith(PREFIX);
    }

    /** 是否为旧版无盐 MD5 格式（32 位小写十六进制） */
    public static boolean isLegacyMd5(String stored) {
        return stored != null && stored.matches("[0-9a-f]{32}");
    }

    /** 生成 PBKDF2 密码摘要（每次随机盐，结果不可逆） */
    public static String hash(String rawPassword) {
        byte[] salt = new byte[16];
        RANDOM.nextBytes(salt);
        byte[] hash = derive(rawPassword, salt, ITERATIONS);
        return PREFIX + ITERATIONS + ":" + Base64.getEncoder().encodeToString(salt)
                + ":" + Base64.getEncoder().encodeToString(hash);
    }

    /** 校验明文密码与存储摘要是否匹配 */
    public static boolean matches(String rawPassword, String stored) {
        if (stored == null || !stored.startsWith(PREFIX)) {
            return false;
        }
        String[] parts = stored.split(":");
        if (parts.length != 4) {
            return false;
        }
        int iterations;
        byte[] salt;
        byte[] expected;
        try {
            iterations = Integer.parseInt(parts[1]);
            salt = Base64.getDecoder().decode(parts[2]);
            expected = Base64.getDecoder().decode(parts[3]);
        } catch (IllegalArgumentException e) {
            return false;
        }
        byte[] actual = derive(rawPassword, salt, iterations);
        return MessageDigest.isEqual(expected, actual);
    }

    private static byte[] derive(String password, byte[] salt, int iterations) {
        try {
            PBEKeySpec spec = new PBEKeySpec(password.toCharArray(), salt, iterations, KEY_LENGTH);
            return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
                    .generateSecret(spec).getEncoded();
        } catch (Exception e) {
            throw new IllegalStateException("PBKDF2 计算失败", e);
        }
    }
}