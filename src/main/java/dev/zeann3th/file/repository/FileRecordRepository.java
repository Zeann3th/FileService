package dev.zeann3th.file.repository;

import dev.zeann3th.file.entity.FileEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface FileRecordRepository extends JpaRepository<FileEntity, Long> {
    Optional<FileEntity> findByFileKey(String fileKey);
}
