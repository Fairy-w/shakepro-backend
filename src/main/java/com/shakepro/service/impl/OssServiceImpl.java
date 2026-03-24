package com.shakepro.service.impl;

import com.shakepro.common.exception.BusinessException;
import com.shakepro.common.result.ErrorCode;
import com.shakepro.config.MinioConfig;
import com.shakepro.dto.request.FileRecordRequest;
import com.shakepro.dto.request.PresignRequest;
import com.shakepro.dto.response.PresignResponse;
import com.shakepro.entity.FileRecord;
import com.shakepro.repository.FileRecordRepository;
import com.shakepro.service.OssService;
import io.minio.GetPresignedObjectUrlArgs;
import io.minio.MinioClient;
import io.minio.http.Method;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class OssServiceImpl implements OssService {

    private final MinioClient minioClient;
    private final MinioConfig minioConfig;
    private final FileRecordRepository fileRecordRepository;

    private static final Set<String> ALLOWED_IMAGE_TYPES = Set.of(
            "image/jpeg", "image/png", "image/gif", "image/webp", "image/svg+xml"
    );
    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024; // 10MB

    @Override
    public PresignResponse generatePresignUrl(PresignRequest request, Long userId) {
        // Validate file type
        if (!ALLOWED_IMAGE_TYPES.contains(request.getContentType())) {
            throw new BusinessException(ErrorCode.FILE_TYPE_NOT_ALLOWED,
                    "仅支持图片类型: " + String.join(", ", ALLOWED_IMAGE_TYPES));
        }

        // Validate file size
        if (request.getSize() != null && request.getSize() > MAX_FILE_SIZE) {
            throw new BusinessException(ErrorCode.FILE_SIZE_EXCEEDED, "文件大小不能超过10MB");
        }

        try {
            String extension = getFileExtension(request.getFilename());
            String objectKey = generateObjectKey(userId, extension);
            int expireSeconds = minioConfig.getPresignExpireSeconds();

            String uploadUrl = minioClient.getPresignedObjectUrl(
                    GetPresignedObjectUrlArgs.builder()
                            .method(Method.PUT)
                            .bucket(minioConfig.getBucket())
                            .object(objectKey)
                            .expiry(expireSeconds, TimeUnit.SECONDS)
                            .build()
            );

            String publicUrl = minioConfig.getPublicBaseUrl() + "/" + objectKey;

            log.info("Generated presign URL for user={}, objectKey={}", userId, objectKey);

            return PresignResponse.builder()
                    .uploadUrl(uploadUrl)
                    .objectKey(objectKey)
                    .publicUrl(publicUrl)
                    .expireSeconds(expireSeconds)
                    .build();

        } catch (Exception e) {
            log.error("Failed to generate presign URL: {}", e.getMessage(), e);
            throw new BusinessException(ErrorCode.OSS_ERROR, "生成预签名URL失败");
        }
    }

    @Override
    public Long saveFileRecord(FileRecordRequest request, Long userId) {
        FileRecord record = FileRecord.builder()
                .userId(userId)
                .objectKey(request.getObjectKey())
                .url(request.getUrl())
                .contentType(request.getContentType())
                .size(request.getSize())
                .build();
        record = fileRecordRepository.save(record);
        return record.getId();
    }

    private String generateObjectKey(Long userId, String extension) {
        String uuid = UUID.randomUUID().toString().replace("-", "");
        return String.format("uploads/%d/%s%s", userId, uuid, extension);
    }

    private String getFileExtension(String filename) {
        int dotIndex = filename.lastIndexOf('.');
        return dotIndex >= 0 ? filename.substring(dotIndex) : "";
    }
}
