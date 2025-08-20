package com.project.syncly.domain.auth.email;


import com.project.syncly.global.redis.core.RedisStorage;
import com.project.syncly.global.redis.enums.RedisKeyPrefix;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
@Service
@RequiredArgsConstructor
public class EmailAuthServiceImpl implements EmailAuthService {

    private final RedisStorage redisStorage;
    private final EmailSender emailSender;

    // TTL 설정 - application.yml에서 가져옴
    @Value("${spring.mail.code-ttl-seconds}")
    private long codeTtlSeconds;

    @Value("${spring.mail.verified-ttl-seconds}")
    private long verifiedTtlSeconds;

    @Override
    public void sendAuthCode(String email) {
        String authCode = UUID.randomUUID().toString().substring(0, 6);
        saveCode(email, authCode);
        emailSender.send(email, "이메일 인증 코드", "인증 코드: " + authCode);
    }

    /**
     * 인증코드 확인 및 검증 성공 처리 메서드
     * - 인증코드 일치 시 인증 마크 저장
     * - 기존 코드 키는 삭제
     */
    @Override
    public boolean verifyCodeAndMarkVerified(String email, String inputCode) {
        String stored = getCode(email);
        if (stored == null || !stored.equals(inputCode)) {
            return false;
        }

        deleteCode(email);       // 인증 성공 시 코드 삭제
        markVerified(email);     // 인증 성공 처리
        return true;
    }


 /*
    === 실제 전송로직 ===
*/
    @Override
    public void saveCode(String email, String code) {
        String key = RedisKeyPrefix.EMAIL_AUTH_CODE.get(email);
        redisStorage.set(key, code, Duration.ofSeconds(codeTtlSeconds));
    }


    @Override
    public String getCode(String email) {
        String key = RedisKeyPrefix.EMAIL_AUTH_CODE.get(email);
        return redisStorage.getValueAsString(key);
    }

    @Override
    public void deleteCode(String email) {
        String key = RedisKeyPrefix.EMAIL_AUTH_CODE.get(email);
        redisStorage.delete(key);
    }

    @Override
    public void markVerified(String email) {
        String key = RedisKeyPrefix.EMAIL_AUTH_VERIFIED.get(email);
        redisStorage.set(key, "true", Duration.ofSeconds(verifiedTtlSeconds));
    }


    @Override
    public boolean isVerified(String email) {
        String key = RedisKeyPrefix.EMAIL_AUTH_VERIFIED.get(email);
        return "true".equals(redisStorage.getValueAsString(key));
    }

    @Override
    public void clearVerified(String email) {
        String key = RedisKeyPrefix.EMAIL_AUTH_VERIFIED.get(email);
        redisStorage.delete(key);
    }

}
