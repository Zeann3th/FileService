package dev.zeann3th.file.util;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.List;

public class JwtUtils {

    private static final JsonMapper JSON_MAPPER = JsonMapper.builder().build();

    public static JwtClaims extractClaims(String authorizationHeader) {
        if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
            return null;
        }

        String token = authorizationHeader.substring(7);
        String[] parts = token.split("\\.");
        if (parts.length < 2) {
            return null;
        }

        try {
            String payload = new String(Base64.getUrlDecoder().decode(parts[1]));
            JsonNode node = JSON_MAPPER.readTree(payload);

            String sub = node.has("sub") ? node.get("sub").asString(null) : null;
            String username = node.has("username") ? node.get("username").asString(null) : null;
            String email = node.has("email") ? node.get("email").asString(null) : null;

            List<String> roles = new ArrayList<>();
            if (node.has("realm_access") && node.get("realm_access").has("roles")) {
                for (JsonNode role : node.get("realm_access").get("roles")) {
                    roles.add(role.asString());
                }
            }

            return new JwtClaims(sub, username, email, Collections.unmodifiableList(roles));
        } catch (Exception _) {
            return null;
        }
    }

    private JwtUtils() {
    }
}
