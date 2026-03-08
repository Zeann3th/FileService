package dev.zeann3th.file.controller;

import dev.zeann3th.file.dto.PresignResponse;
import dev.zeann3th.file.service.FileService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/files")
@RequiredArgsConstructor
public class FileController {

    private final FileService fileService;

    /**
     * Get a presigned upload URL.
     * POST /api/files/presign/upload?filename=photo.jpg
     * Returns: { presignUrl, fileUrl, key }
     */
    @PostMapping("/presign/upload")
    public ResponseEntity<PresignResponse> presignUpload(@RequestParam String filename) throws Exception {
        return ResponseEntity.ok(fileService.presignUpload(filename));
    }

    /**
     * Get a presigned download URL (redirects or returns the URL).
     * GET /api/files/presign/download/{*key}
     */
    @GetMapping("/presign/download/{*key}")
    public ResponseEntity<Map<String, String>> presignDownload(@PathVariable String key) throws Exception {
        String url = fileService.presignDownload(normalizeKey(key));
        return ResponseEntity.ok(Map.of("url", url));
    }

    /**
     * Serve/proxy the file directly (e.g. for images in browser).
     * GET /api/files/{*key}
     */
    @GetMapping("/{*key}")
    public ResponseEntity<Resource> serveFile(@PathVariable String key) throws Exception {
        String normalizedKey = normalizeKey(key);
        String contentType = fileService.getContentType(normalizedKey);
        Resource resource = fileService.getFile(normalizedKey);

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CACHE_CONTROL, "max-age=86400")
                .body(resource);
    }

    private String normalizeKey(String key) {
        return key.startsWith("/") ? key.substring(1) : key;
    }
}
