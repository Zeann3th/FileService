package dev.zeann3th.file.dto;

import lombok.Builder;

@Builder
public record PresignResponse(
        String presignUrl,
        String fileUrl,
        String key
) {
}
