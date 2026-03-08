package dev.zeann3th.file.service;

import dev.zeann3th.file.dto.PresignResponse;
import io.minio.*;
import io.minio.http.Method;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class FileService {

    private final MinioClient minioClient;

    @Value("${minio.bucket}")
    private String bucket;

    @Value("${file-service.public-url}")
    private String publicUrl;

    /**
     * Generate a presigned PUT URL so clients can upload directly to MinIO.
     * Returns the presign URL + the permanent file URL other services can reference.
     */
    public PresignResponse presignUpload(String filename) throws Exception {
        String key = UUID.randomUUID() + "/" + filename;

        String presignUrl = minioClient.getPresignedObjectUrl(
                io.minio.GetPresignedObjectUrlArgs.builder()
                        .method(Method.PUT)
                        .bucket(bucket)
                        .object(key)
                        .expiry(15, TimeUnit.MINUTES)
                        .build()
        );

        String fileUrl = publicUrl + "/api/files/" + key;

        return PresignResponse.builder()
                .presignUrl(presignUrl)
                .fileUrl(fileUrl)
                .key(key)
                .build();
    }

    /**
     * Generate a presigned GET URL for direct download from MinIO.
     */
    public String presignDownload(String key) throws Exception {
        return minioClient.getPresignedObjectUrl(
                io.minio.GetPresignedObjectUrlArgs.builder()
                        .method(Method.GET)
                        .bucket(bucket)
                        .object(key)
                        .expiry(15, TimeUnit.MINUTES)
                        .build()
        );
    }

    /**
     * Serve the file through this service (proxy from MinIO).
     */
    public Resource getFile(String key) throws Exception {
        InputStream stream = minioClient.getObject(
                GetObjectArgs.builder()
                        .bucket(bucket)
                        .object(key)
                        .build()
        );
        return new InputStreamResource(stream);
    }

    /**
     * Get the content type of a stored object.
     */
    public String getContentType(String key) throws Exception {
        StatObjectResponse stat = minioClient.statObject(
                StatObjectArgs.builder()
                        .bucket(bucket)
                        .object(key)
                        .build()
        );
        return stat.contentType();
    }
}
