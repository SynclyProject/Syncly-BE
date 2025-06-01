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
    public S3ResponseDTO.PreSignedUrl generatePresignedPutUrl(Long memberId, S3RequestDTO.UploadPreSignedUrl request) {
        String extension = request.mimeType().getExtension();
        String objectKey = "uploads/" + UUID.randomUUID() + "." + extension;
        String redisKey = RedisKeyPrefix.S3_AUTH_OBJECT_KEY.get(memberId + ':' + request.fileName() + ':' + objectKey);
        String url = s3Util.createPresignedUrl(objectKey, request.mimeType());

        redisStorage.set(redisKey, request, Duration.ofMinutes(10));

        return S3Converter.toPreSignedUrlResDTO(request.fileName(), url, objectKey);
    }

    @Override
    public S3ResponseDTO.GetUrl generatePresignedGetViewUrl(S3RequestDTO.GetViewUrl request) {
        String url = s3Util.createPresignedGetUrlForView(request.objectKey());
        return S3Converter.toGetUrlResDTO(url);
    }

    @Override
    public S3ResponseDTO.GetUrl generatePresignedGetDownloadUrl(S3RequestDTO.GetDownloadUrl request, Long memberId) {
        //S3RequestDTO.GetDownloadUrl 가 아닌 fileId를 받아와서 db조회 후 memberId가 workSpace에 포함되는지 확인하는 로직으로 변경하면 더 좋을 것 같습니다.
        String url = s3Util.createPresignedGetUrlForDownload(request.objectKey(), request.fileName());
        return S3Converter.toGetUrlResDTO(url);
    }
}
