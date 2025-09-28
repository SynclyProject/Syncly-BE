package com.project.syncly.domain.auth.service;

import com.project.syncly.domain.auth.whitelist.RefreshWhitelistService;
import com.project.syncly.domain.auth.whitelist.RotateResult;
import com.project.syncly.domain.member.dto.request.MemberRequestDTO;
import com.project.syncly.domain.member.entity.Member;
import com.project.syncly.domain.member.exception.MemberErrorCode;
import com.project.syncly.domain.member.exception.MemberException;
import com.project.syncly.domain.member.repository.MemberRepository;
import com.project.syncly.global.jwt.JwtProvider;
import com.project.syncly.global.jwt.dto.IssuedTokens;
import com.project.syncly.global.jwt.dto.RefreshClaims;
import com.project.syncly.global.jwt.enums.TokenType;
import com.project.syncly.global.jwt.exception.JwtErrorCode;
import com.project.syncly.global.jwt.exception.JwtException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseCookie;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtProvider jwtProvider;
    private final RefreshWhitelistService whitelist;

    private static final String REFRESH_COOKIE = TokenType.REFRESH.toString();
    private static final String REFRESH_COOKIE_PATH = "/api/auth";


    @Override
    public IssuedTokens  login(MemberRequestDTO.Login dto, HttpServletRequest request, HttpServletResponse response) {
        Member member = memberRepository.findByEmail(dto.email())
                .orElseThrow(() -> new MemberException(MemberErrorCode.MEMBER_NOT_FOUND_BY_EMAIL));

        if (!passwordEncoder.matches(dto.password(), member.getPassword())) {
            throw new MemberException(MemberErrorCode.PASSWORD_NOT_MATCHED);
        }

        String deviceId = extractDeviceIdFromCookie(request);

        IssuedTokens issued = issueNewTokens(member.getId(), deviceId);
        return issued;
    }

    @Override
    public IssuedTokens issueNewTokens(Long memberId, String deviceId) {
        String jti = jwtProvider.newJti();
        IssuedTokens issued = jwtProvider.issueNewTokens(memberId, deviceId, jti);

        // 화이트리스트 current 등록
        whitelist.setCurrent(memberId, deviceId, jti, issued.refreshExpiresInSec());
        return issued;
    }

    private String extractRefreshFromCookie(HttpServletRequest req) {
        return Arrays.stream(Optional.ofNullable(req.getCookies()).orElse(new Cookie[0]))
                .filter(c -> REFRESH_COOKIE.equals(c.getName()))
                .map(Cookie::getValue)
                .findFirst()
                .orElse(null);
    }

    @Override
    public String extractDeviceIdFromCookie(HttpServletRequest request) {
        // 기존 쿠키 탐색
        if (request.getCookies() != null) {
            for (Cookie c : request.getCookies()) {
                if ("X-DEVICE-ID".equals(c.getName())) {
                    return c.getValue();
                }
            }
        }
        // 없으면 새 deviceId 생성
        String newDeviceId = UUID.randomUUID().toString();
        return newDeviceId;
    }

    @Override
    public IssuedTokens reissue(HttpServletRequest request) {
        // refresh token 추출
        String refreshJwt = extractRefreshFromCookie(request);
        if (refreshJwt == null) {
            throw new JwtException(JwtErrorCode.EMPTY_REFRESH_TOKEN);
        }
        //uaHash 추출
        String ua = request.getHeader("User-Agent");
        String uaHash = (ua != null && !ua.isBlank()) ? sha256(ua) : null;
        //refresh token검증
        RefreshClaims c = jwtProvider.parseAndValidateRefresh(refreshJwt);


        // 선발급
        String newJti = jwtProvider.newJti();
        IssuedTokens issued = jwtProvider.issueTokens(c.userId(), c.deviceId(), newJti, c.expiresAt());
        long rtTtl = issued.refreshExpiresInSec();

        System.out.println(c.jti());
        System.out.println(newJti);
        RotateResult result = whitelist.rotateAtomic(c.userId(), c.deviceId(), c.jti(), newJti, rtTtl, uaHash);

        if (result == RotateResult.SUCCESS) {
            whitelist.cashUaHash(uaHash, c.userId(), c.deviceId());
            return issued;
        }
        else if (result == RotateResult.MISMATCH) {
            if (uaHash != null) {
                //이전 요청과 uaHash가 동일한 요청인지 검증(네트워크이슈 ux 증진)
                if(whitelist.validateUaHash(uaHash, c.userId(), c.deviceId())){
                    //화이트리스트 강제 초기화
                    whitelist.setCurrent(c.userId(), c.deviceId(), newJti, rtTtl);
                    return issued;
                }
            }
        }
        //화이트리스트 제거(모든 해당 종류 토큰 접근 불가)
        whitelist.evict(c.userId(), c.deviceId());
        throw new JwtException(JwtErrorCode.REFRESH_TOKEN_BLACKLISTED);
    }
    //user Agent sha256으로 해시
    private static String sha256(String s) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] out = md.digest(s.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : out) {
                String h = Integer.toHexString(0xff & b);
                if (h.length() == 1) sb.append('0');
                sb.append(h);
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    //쿠키에 refresh token 삽입
    @Override
    public ResponseCookie buildRefreshCookie(String refreshToken, long ttlSec) {
        return ResponseCookie.from(REFRESH_COOKIE, refreshToken)
                .httpOnly(true)
                .secure(true)
                .sameSite("None")
                .domain(".syncly-io.com")
                .path(REFRESH_COOKIE_PATH)
                .maxAge(Duration.ofSeconds(ttlSec))
                .build();
    }



    // 로그아웃 로직
    @Override
    public void logout(HttpServletRequest request, HttpServletResponse response) {
        // 쿠키 없음 → 멱등 처리(성공 응답)
        String refresh = extractRefreshFromCookie(request);
        if (refresh == null || refresh.isBlank()) {
            // 쿠키만 지워서 클라이언트 정리
            expireRefreshCookie(response);
            return;
        }

        RefreshClaims claims;
        try {
            // RT 검증 & 클레임 파싱 (서명/만료/형식)
            claims = jwtProvider.parseAndValidateRefresh(refresh);
        } catch (JwtException ex) {
            // 유효하지 않은 토큰: 어차피 더 못쓰니 쿠키만 제거하고 반환(멱등)
            expireRefreshCookie(response);
            return;
        }
        whitelist.evict(claims.userId(), claims.deviceId());

        // 클라이언트 쿠키 삭제 (Path/속성 동일하게 맞춰야 브라우저가 확실히 지움)
        expireRefreshCookie(response);
    }

    private void expireRefreshCookie(HttpServletResponse response) {
        ResponseCookie expired = ResponseCookie.from(REFRESH_COOKIE, "")
                .httpOnly(true)
                .secure(true)
                .sameSite("None")
                .path(REFRESH_COOKIE_PATH)
                .maxAge(Duration.ZERO) // Max-Age=0 → 삭제
                .build();
        response.addHeader("Set-Cookie", expired.toString());
    }
}
