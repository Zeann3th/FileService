package dev.zeann3th.file.dto;

public record PresignUploadRequest(
        String filename,
        boolean isPrivate,
        String key
) {
}
