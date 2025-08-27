package com.project.syncly.domain.auth.email;

///현재 비번변경, 회원가입 모두 이메일 인증을 한 뒤,
/// 다음 회원가입, 비밍번호 변경요청이 들어왔을 때 별도로 동일한 인물인지 확인하지 않음
/// 인증 이후 인증여부를 저장하는 것이 아니라 ttl이 짧은 Jwt를 발급하여 인증에 사용시키는 방식이 적합할 듯 함
/// 추후 리펙토링 해보자!

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
        redisStorage.setValueAsString(key, code, Duration.ofSeconds(codeTtlSeconds));
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
        redisStorage.setValueAsString(key, "true", Duration.ofSeconds(verifiedTtlSeconds));
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

    /// ***
    /// member 삭제 전 이메일 인증
    /// ***
    @Override
    public void sendAuthCodeBeforeChangePassword(String email) {
        String key = RedisKeyPrefix.EMAIL_AUTH_CODE_BEFORE_CHANGE_PASSWORD.get(email);
        String authCode = UUID.randomUUID().toString().substring(0, 6);
        emailSender.send(email, "이메일 인증 코드", "인증 코드: " + authCode);
        redisStorage.setValueAsString(key, authCode, Duration.ofSeconds(codeTtlSeconds));
    }
    @Override
    public boolean verifyCodeAndMarkVerifiedBeforeChangePassword(String email, String inputCode) {
        String toVerifyKey = RedisKeyPrefix.EMAIL_AUTH_CODE_BEFORE_CHANGE_PASSWORD.get(email);
        String markVerifyKey = RedisKeyPrefix.EMAIL_AUTH_VERIFIED_BEFORE_CHANGE_PASSWORD.get(email);
        // 이미 인증한 이력있으면 멱등처리
        if ("true".equals(redisStorage.getValueAsString(markVerifyKey))) {
            return true; // 이미 인증 완료 -> 멱등 처리
        }
        if(!redisStorage.getValueAsString(toVerifyKey).equals(inputCode)){
            return false;
        }
        redisStorage.setValueAsString(markVerifyKey, "true", Duration.ofSeconds(verifiedTtlSeconds));
        redisStorage.delete(toVerifyKey);
        return true;
    }
    @Override
    public boolean isVerifiedBeforeChangePassword(String email) {
        String markVerifyKey = RedisKeyPrefix.EMAIL_AUTH_VERIFIED_BEFORE_CHANGE_PASSWORD.get(email);
        if(!"true".equals(redisStorage.getValueAsString(markVerifyKey))){
            return false;
        }
        redisStorage.delete(markVerifyKey);
        return true;
    }


}
