package com.shopping.security;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class TokenUtilTest {

    @Test
    public void testGenerateAndVerifyToken() {
        Long userId = 101L;
        String username = "john_doe";
        String role = "USER";

        String token = TokenUtil.generateToken(userId, username, role);
        assertNotNull(token);
        assertFalse(token.isEmpty());

        TokenUtil.TokenData data = TokenUtil.verifyToken(token);
        assertNotNull(data);
        assertEquals(userId, data.getUserId());
        assertEquals(username, data.getUsername());
        assertEquals(role, data.getRole());
    }

    @Test
    public void testVerifyInvalidToken() {
        // Null or empty tokens
        assertNull(TokenUtil.verifyToken(null));
        assertNull(TokenUtil.verifyToken(""));
        assertNull(TokenUtil.verifyToken("randomStuff"));

        // Tampered token (modifying signature)
        String realToken = TokenUtil.generateToken(1L, "alice", "ADMIN");
        String decoded = new String(java.util.Base64.getUrlDecoder().decode(realToken));
        int dotIndex = decoded.lastIndexOf('.');
        String payload = decoded.substring(0, dotIndex);
        
        // Append tampered signature
        String tamperedToken = java.util.Base64.getUrlEncoder().withoutPadding().encodeToString(
            (payload + "." + "TamperedSignatureValueHere123").getBytes()
        );
        
        assertNull(TokenUtil.verifyToken(tamperedToken));
    }
}
