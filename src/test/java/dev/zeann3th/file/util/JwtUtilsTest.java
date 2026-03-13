package dev.zeann3th.file.util;

import org.junit.jupiter.api.Test;
import java.util.Base64;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class JwtUtilsTest {

    @Test
    void testExtractClaims() {
        String sub = "user-123";
        String username = "testuser";
        String email = "test@example.com";
        List<String> roles = List.of("user", "admin");

        String payloadJson = String.format(
            "{\"sub\":\"%s\",\"username\":\"%s\",\"email\":\"%s\",\"realm_access\":{\"roles\":[\"user\",\"admin\"]}}",
            sub, username, email
        );
        String encodedPayload = Base64.getUrlEncoder().withoutPadding().encodeToString(payloadJson.getBytes());
        String token = "header." + encodedPayload + ".signature";
        String authHeader = "Bearer " + token;

        JwtClaims claims = JwtUtils.extractClaims(authHeader);

        assertNotNull(claims);
        assertEquals(sub, claims.sub());
        assertEquals(username, claims.username());
        assertEquals(email, claims.email());
        assertTrue(claims.roles().containsAll(roles));
    }

    @Test
    void testExtractClaimsWithRootRoles() {
        String sub = "user-123";
        List<String> roles = List.of("user", "admin");

        String payloadJson = String.format(
            "{\"sub\":\"%s\",\"roles\":[\"user\",\"admin\"]}",
            sub
        );
        String encodedPayload = Base64.getUrlEncoder().withoutPadding().encodeToString(payloadJson.getBytes());
        String token = "header." + encodedPayload + ".signature";
        String authHeader = "Bearer " + token;

        JwtClaims claims = JwtUtils.extractClaims(authHeader);

        assertNotNull(claims);
        assertEquals(sub, claims.sub());
        assertTrue(claims.roles().containsAll(roles));
    }

    @Test
    void testExtractClaimsNull() {
        assertNull(JwtUtils.extractClaims(null));
        assertNull(JwtUtils.extractClaims("Invalid"));
        assertNull(JwtUtils.extractClaims("Bearer invalid.token"));
    }
}
