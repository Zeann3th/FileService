package dev.zeann3th.file.service;

import dev.zeann3th.file.entity.FileEntity;
import dev.zeann3th.file.dto.PresignResponse;
import dev.zeann3th.file.repository.FileRecordRepository;
import io.minio.*;
import io.minio.http.Method;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.InputStream;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class FileService {

    private final MinioClient minioClient;
    @Qualifier("presignMinioClient")
    private final MinioClient presignMinioClient;
    private final FileRecordRepository fileRecordRepository;

    @Value("${minio.bucket}")
    private String bucket;

    @Value("${file-service.public-url}")
    private String publicUrl;

    @Transactional
    public PresignResponse presignUpload(String filename, boolean isPrivate, String keyPath, String sub) throws Exception {
        String extension = "";
        int dotIndex = filename.lastIndexOf('.');
        if (dotIndex >= 0) {
            extension = filename.substring(dotIndex);
        }

        String date = LocalDate.now().toString();
        String key;

        if (isPrivate) {
            key = "private/" + sub + "/" + date;
            if (keyPath != null && !keyPath.isBlank()) {
                key += "/" + keyPath.strip();
            }
            key += "/" + UUID.randomUUID() + extension;
        } else {
            key = "public/" + date;
            if (keyPath != null && !keyPath.isBlank()) {
                key += "/" + keyPath.strip();
            }
            key += "/" + UUID.randomUUID() + extension;
        }

        String presignUrl = presignMinioClient.getPresignedObjectUrl(
                GetPresignedObjectUrlArgs.builder()
                        .method(Method.PUT)
                        .bucket(bucket)
                        .object(key)
                        .expiry(15, TimeUnit.MINUTES)
                        .build()
        );

        String fileUrl = publicUrl + "/api/v1/files/" + key;

        // Save metadata
        FileEntity fileEntity = FileEntity.builder()
                .fileKey(key)
                .ownerSub(sub)
                .originalFilename(filename)
                .isPrivate(isPrivate)
                .build();
        fileRecordRepository.save(fileEntity);

        return PresignResponse.builder()
                .presignUrl(presignUrl)
                .fileUrl(fileUrl)
                .key(key)
                .build();
    }

    public String presignDownload(String key) throws Exception {
        return presignMinioClient.getPresignedObjectUrl(
                GetPresignedObjectUrlArgs.builder()
                        .method(Method.GET)
                        .bucket(bucket)
                        .object(key)
                        .expiry(15, TimeUnit.MINUTES)
                        .build()
        );
    }

    public Resource getFile(String key) throws Exception {
        InputStream stream = minioClient.getObject(
                GetObjectArgs.builder()
                        .bucket(bucket)
                        .object(key)
                        .build()
        );
        return new InputStreamResource(stream);
    }

    public String getContentType(String key) throws Exception {
        StatObjectResponse stat = minioClient.statObject(
                StatObjectArgs.builder()
                        .bucket(bucket)
                        .object(key)
                        .build()
        );
        return stat.contentType();
    }

    @Transactional
    public void deleteFile(String key) throws Exception {
        minioClient.removeObject(
                RemoveObjectArgs.builder()
                        .bucket(bucket)
                        .object(key)
                        .build()
        );
        fileRecordRepository.findByFileKey(key).ifPresent(fileRecordRepository::delete);
    }

    public Optional<FileEntity> getFileRecord(String key) {
        return fileRecordRepository.findByFileKey(key);
    }
}
