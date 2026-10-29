package com.shopping.security;

import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

public class TokenUtil {

    private static final String SECRET_KEY = "ShoppingAppSecretKeyForSigningHmacSha256Tokens!SecureAndLongEnoughToAvoidErrors";
    private static final String HMAC_ALGORITHM = "HmacSHA256";

    public static String generateToken(Long userId, String username, String role) {
        long expiry = System.currentTimeMillis() + (24 * 60 * 60 * 1000); // 1 day
        String payload = userId + ":" + username + ":" + role + ":" + expiry;
        String signature = sign(payload);
        String fullToken = payload + "." + signature;
        return Base64.getUrlEncoder().withoutPadding().encodeToString(fullToken.getBytes(StandardCharsets.UTF_8));
    }

    public static TokenData verifyToken(String base64Token) {
        if (base64Token == null || base64Token.isEmpty()) {
            return null;
        }
        try {
            String decoded = new String(Base64.getUrlDecoder().decode(base64Token), StandardCharsets.UTF_8);
            int dotIndex = decoded.lastIndexOf('.');
            if (dotIndex == -1) {
                return null;
            }
            String payload = decoded.substring(0, dotIndex);
            String signature = decoded.substring(dotIndex + 1);

            String expectedSignature = sign(payload);
            // Constant-time comparison to prevent timing attacks
            if (!MessageDigestEquals(signature, expectedSignature)) {
                return null;
            }

            String[] parts = payload.split(":");
            if (parts.length != 4) {
                return null;
            }

            long userId = Long.parseLong(parts[0]);
            String username = parts[1];
            String role = parts[2];
            long expiry = Long.parseLong(parts[3]);

            if (System.currentTimeMillis() > expiry) {
                return null; // Expired
            }

            return new TokenData(userId, username, role);
        } catch (Exception e) {
            return null;
        }
    }

    private static String sign(String data) {
        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            SecretKeySpec secretKeySpec = new SecretKeySpec(SECRET_KEY.getBytes(StandardCharsets.UTF_8), HMAC_ALGORITHM);
            mac.init(secretKeySpec);
            byte[] rawHmac = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(rawHmac);
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            throw new RuntimeException("Error signing token payload", e);
        }
    }

    private static boolean MessageDigestEquals(String a, String b) {
        byte[] aBytes = a.getBytes(StandardCharsets.UTF_8);
        byte[] bBytes = b.getBytes(StandardCharsets.UTF_8);
        if (aBytes.length != bBytes.length) {
            return false;
        }
        int result = 0;
        for (int i = 0; i < aBytes.length; i++) {
            result |= aBytes[i] ^ bBytes[i];
        }
        return result == 0;
    }

    public static class TokenData {
        private final Long userId;
        private final String username;
        private final String role;

        public TokenData(Long userId, String username, String role) {
            this.userId = userId;
            this.username = username;
            this.role = role;
        }

        public Long getUserId() {
            return userId;
        }

        public String getUsername() {
            return username;
        }

        public String getRole() {
            return role;
        }
    }
}
