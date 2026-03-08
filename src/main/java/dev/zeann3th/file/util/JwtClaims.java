package dev.zeann3th.file.util;

import java.util.List;

public record JwtClaims(
        String sub,
        String username,
        String email,
        List<String> roles
) {
}
