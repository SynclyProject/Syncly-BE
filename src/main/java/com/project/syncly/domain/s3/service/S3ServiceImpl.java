package com.project.syncly.domain.s3.service;
import com.project.syncly.domain.s3.converter.S3Converter;
import com.project.syncly.domain.s3.dto.S3RequestDTO;
import com.project.syncly.domain.s3.dto.S3ResponseDTO;
import com.project.syncly.global.redis.core.RedisStorage;
import com.project.syncly.domain.s3.util.S3Util;
import com.project.syncly.global.redis.enums.RedisKeyPrefix;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class S3ServiceImpl implements S3Service {
    private final RedisStorage redisStorage;
    private final S3Util s3Util;

    @Override
    public S3ResponseDTO.PreSignedUrl generatePresignedPutUrl(Long memberId, S3RequestDTO.PreSignedUrl request) {
        request.isFileNameAndMimeTypeMatch();
        String objectKey = UUID.randomUUID().toString();
        String redisKey = RedisKeyPrefix.S3_AUTH_OBJECT_KEY.get(memberId + '_' + request.fileName());
        String url = s3Util.createPresignedUrl(objectKey, request.mimeType());

        redisStorage.set(redisKey, objectKey, Duration.ofMinutes(10));

        return S3Converter.toPreSignedUrlResDTO(request.fileName(), url, objectKey);
    }


}
