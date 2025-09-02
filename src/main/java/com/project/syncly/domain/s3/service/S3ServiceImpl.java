package com.project.syncly.domain.s3.service;
import com.project.syncly.domain.s3.converter.S3Converter;
import com.project.syncly.domain.s3.dto.S3RequestDTO;
import com.project.syncly.domain.s3.dto.S3ResponseDTO;
import com.project.syncly.domain.s3.util.CloudFrontUtil;
import com.project.syncly.global.redis.core.RedisStorage;
import com.project.syncly.domain.s3.util.S3Util;
import com.project.syncly.global.redis.enums.RedisKeyPrefix;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class S3ServiceImpl implements S3Service {
    private final RedisStorage redisStorage;
    private final S3Util s3Util;
    private final CloudFrontUtil cloudFrontUtil;

    @Override
    public S3ResponseDTO.PreSignedUrl generatePresignedPutUrl(Long memberId, S3RequestDTO.UploadPreSignedUrl request) {
        String extension = request.mimeType().getExtension();
        String objectKey = "uploads/" + UUID.randomUUID() + "." + extension;
        String redisKey = RedisKeyPrefix.S3_AUTH_OBJECT_KEY.get(memberId.toString() + ':' + request.fileName() + ':' + objectKey);
        String url = s3Util.createPresignedUrl(objectKey, request.mimeType());

        redisStorage.set(redisKey, request, Duration.ofMinutes(10));

        return S3Converter.toPreSignedUrlResDTO(request.fileName(), url, objectKey);
    }

    @Override
    public ResponseEntity<Void> generateSignedCookieForView(S3RequestDTO.GetViewUrl request, HttpServletResponse response) {
        String resourcePath = request.objectKey();

        Map<String, String> cookies = cloudFrontUtil.generateSignedCookies(resourcePath, Duration.ofMinutes(10));

        cookies.forEach((name, value) -> {
            ResponseCookie cookie = ResponseCookie.from(name, value)
                    .domain(".syncly-io.com")//cloudFront 도메인
                    .path("/") // or specific resource
                    .httpOnly(true)
                    .secure(true)
                    .sameSite("None")
                    .maxAge(Duration.ofMinutes(10))
                    .build();
            response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
        });

        // 클라이언트는 이제 https://d123.cloudfront.net/uploads/abc123.jpg 직접 접근 가능
        return ResponseEntity.noContent().build();
    }

    @Override
    public S3ResponseDTO.GetUrl generatePresignedGetDownloadUrl(S3RequestDTO.GetDownloadUrl request, Long memberId) {
        //S3RequestDTO.GetDownloadUrl 가 아닌 fileId를 받아와서 db조회 후 memberId가 workSpace에 포함되는지 확인하는 로직으로 변경하면 더 좋을 것 같습니다.
        String url = s3Util.createPresignedGetUrlForDownload(request.objectKey(), request.fileName());
        return S3Converter.toGetUrlResDTO(url);
    }
}
