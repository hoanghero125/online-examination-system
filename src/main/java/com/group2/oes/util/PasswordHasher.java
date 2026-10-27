package com.group2.oes.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * Băm mật khẩu bằng SHA-256 kèm salt ngẫu nhiên, chỉ dùng thư viện chuẩn của JDK.
 * Chuỗi lưu trong CSDL có dạng: salt$hash (cả hai mã hóa Base64).
 */
public final class PasswordHasher {

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final int SALT_BYTES = 16;

    private PasswordHasher() {
    }

    public static String hash(String raw) {
        byte[] salt = new byte[SALT_BYTES];
        RANDOM.nextBytes(salt);
        return encode(salt) + "$" + encode(digest(salt, raw));
    }

    public static boolean matches(String raw, String stored) {
        if (raw == null || stored == null) {
            return false;
        }
        int sep = stored.indexOf('$');
        if (sep < 0) {
            return false;
        }
        byte[] salt = Base64.getDecoder().decode(stored.substring(0, sep));
        byte[] expected = Base64.getDecoder().decode(stored.substring(sep + 1));
        // so sánh thời gian hằng, tránh đoán mật khẩu qua thời gian phản hồi
        return MessageDigest.isEqual(expected, digest(salt, raw));
    }

    private static byte[] digest(byte[] salt, String raw) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            md.update(salt);
            return md.digest(raw.getBytes(StandardCharsets.UTF_8));
        } catch (NoSuchAlgorithmException e) {
            // SHA-256 luôn có trong mọi JDK, nhánh này không bao giờ chạy tới
            throw new IllegalStateException("JDK thiếu SHA-256", e);
        }
    }

    private static String encode(byte[] bytes) {
        return Base64.getEncoder().encodeToString(bytes);
    }
}
