package dev.zeann3th.file.controller;

import dev.zeann3th.file.dto.PresignResponse;
import dev.zeann3th.file.dto.PresignUploadRequest;
import dev.zeann3th.file.exception.CommandExceptionBuilder;
import dev.zeann3th.file.exception.ErrorCode;
import dev.zeann3th.file.exception.ResponseWrapper;
import dev.zeann3th.file.service.FileService;
import dev.zeann3th.file.util.JwtClaims;
import dev.zeann3th.file.util.JwtUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequiredArgsConstructor
public class FileController {

    private final FileService fileService;

    @PostMapping("/api/v1/files/presign/upload")
    @ResponseWrapper
    public PresignResponse presignUpload(
            @RequestBody PresignUploadRequest request,
            @RequestHeader(HttpHeaders.AUTHORIZATION) String authorization) throws Exception {
        JwtClaims claims = requireClaims(authorization);
        return fileService.presignUpload(
                request.filename(),
                request.isPrivate(),
                request.key(),
                claims.sub()
        );
    }

    @GetMapping("/api/v1/files/presign/download/{*key}")
    @ResponseWrapper
    public Map<String, String> presignDownload(
            @PathVariable String key,
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization) throws Exception {
        String normalizedKey = normalizeKey(key);
        checkPrivateAccess(normalizedKey, authorization);
        String url = fileService.presignDownload(normalizedKey);
        return Map.of("url", url);
    }

    @GetMapping("/{*key}")
    public ResponseEntity<Resource> serveFile(
            @PathVariable String key,
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization) throws Exception {
        String normalizedKey = normalizeKey(key);
        checkPrivateAccess(normalizedKey, authorization);
        String contentType = fileService.getContentType(normalizedKey);
        Resource resource = fileService.getFile(normalizedKey);

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CACHE_CONTROL, "max-age=86400")
                .body(resource);
    }

    private void checkPrivateAccess(String key, String authorization) {
        if (!key.startsWith("private/")) {
            return;
        }

        JwtClaims claims = requireClaims(authorization);

        // key format: private/<sub>/<date>/...
        String[] parts = key.split("/");
        if (parts.length < 2) {
            throw CommandExceptionBuilder.exception(ErrorCode.FS0005);
        }

        String pathSub = parts[1];
        if (!pathSub.equals(claims.sub())) {
            throw CommandExceptionBuilder.exception(ErrorCode.FS0005);
        }
    }

    private JwtClaims requireClaims(String authorization) {
        JwtClaims claims = JwtUtils.extractClaims(authorization);
        if (claims == null || claims.sub() == null) {
            throw CommandExceptionBuilder.exception(ErrorCode.FS0006);
        }
        return claims;
    }

    private String normalizeKey(String key) {
        return key.startsWith("/") ? key.substring(1) : key;
    }
}
