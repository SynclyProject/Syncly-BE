package com.project.syncly.domain.s3.util;

import com.project.syncly.domain.s3.exception.S3ErrorCode;
import com.project.syncly.domain.s3.exception.S3Exception;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.Signature;
import java.security.spec.PKCS8EncodedKeySpec;
import java.time.Duration;
import java.util.Base64;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class CloudFrontUtil {

    @Value("${aws.cloudfront.domain}")
    private String cloudFrontDomain;

    @Value("${aws.cloudfront.key-pair-id}")
    private String keyPairId;

    @Value("${aws.cloudfront.private-key-path}")
    private String privateKeyPath;

    public Map<String, String> generateSignedCookies(String resourcePath, Duration duration) {
        try {
            Date expiresAt = new Date(System.currentTimeMillis() + duration.toMillis());

            String policy = String.format("""
                {
                  "Statement": [
                    {
                      "Resource": "https://%s/%s",
                      "Condition": {
                        "DateLessThan": {
                          "AWS:EpochTime": %d
                        }
                      }
                    }
                  ]
                }
                """, cloudFrontDomain, resourcePath, expiresAt.getTime() / 1000);

            // 정책을 RSA로 서명
            PrivateKey privateKey = loadPrivateKey(privateKeyPath);
            String signature = signWithPrivateKey(policy, privateKey);

            // 쿠키 생성
            Map<String, String> cookies = new HashMap<>();
            cookies.put("CloudFront-Policy", Base64.getEncoder().encodeToString(policy.getBytes(StandardCharsets.UTF_8)));
            cookies.put("CloudFront-Signature", signature);
            cookies.put("CloudFront-Key-Pair-Id", keyPairId);

            return cookies;
        } catch (Exception e) {
            throw new S3Exception(S3ErrorCode.CLOUDFRONT_COOKIE_FAIL);
        }
    }

    private PrivateKey loadPrivateKey(String path) throws Exception {
        // PEM 포맷 private key 읽기
        String key = Files.readString(Paths.get(path))
                .replace("-----BEGIN PRIVATE KEY-----", "")
                .replace("-----END PRIVATE KEY-----", "")
                .replaceAll("\\s", "");
        byte[] keyBytes = Base64.getDecoder().decode(key);

        PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(keyBytes);
        KeyFactory factory = KeyFactory.getInstance("RSA");
        return factory.generatePrivate(spec);
    }

    private String signWithPrivateKey(String data, PrivateKey privateKey) throws Exception {
        Signature rsa = Signature.getInstance("SHA256withRSA");
        rsa.initSign(privateKey);
        rsa.update(data.getBytes(StandardCharsets.UTF_8));
        return Base64.getEncoder().encodeToString(rsa.sign());
    }
}

