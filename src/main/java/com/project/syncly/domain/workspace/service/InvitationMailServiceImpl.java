package com.project.syncly.domain.workspace.service;

import com.project.syncly.domain.workspace.exception.WorkspaceErrorCode;
import com.project.syncly.global.apiPayload.exception.CustomException;
import com.project.syncly.global.util.RedisUtil;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.MessagingException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.util.Random;

@Service
@RequiredArgsConstructor
public class InvitationMailServiceImpl implements InvitationMailService {
    @Value("${spring.mail.username}")
    private String senderEmail;

    private final JavaMailSender javaMailSender;

    //토큰 중복 제거를 위해 redis set 사용
    private final RedisUtil redisUtil;
    private static final String REDIS_SET_KEY = "invite:tokens"; // 초대코드 저장용 Set 키
    private static final int MAX_RETRY = 5;

    @Override
    public String generateUniqueToken() {
        int retry = 0;

        while (retry < MAX_RETRY) {
            String token = generateRandomToken();

            // Redis SET에 token 추가(TTL 7일) 및 중복 여부 확인
            boolean added = redisUtil.addToSetWithTTL(REDIS_SET_KEY, token);

            if (added) {
                return token; // 중복이 아니므로 토큰 반환
            }
            retry++;
        }

        throw new CustomException(WorkspaceErrorCode.TOKEN_GENERATION_FAILED);
    }

    // 무작위 8자리 영문+숫자 토큰 생성
    private String generateRandomToken() {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
        Random random = new Random();
        StringBuilder sb = new StringBuilder(8);

        for (int i = 0; i < 8; i++) {
            sb.append(chars.charAt(random.nextInt(chars.length())));
        }
        return sb.toString();
    }


    //이메일 전송
    @Override
    public MimeMessage createMail(String mail, String number) {
        try {
            MimeMessage message = javaMailSender.createMimeMessage();

            message.setFrom(senderEmail);
            message.setRecipients(MimeMessage.RecipientType.TO, mail);
            message.setSubject("이메일 인증");

            String body = "";
            body += "<h3>워크스페이스 초대 수락 링크입니다.</h3>";
            body += "<h1><a href=\"" + number + "\">" + number + "</a></h1>";
            body += "<h3>감사합니다.</h3>";

            message.setText(body, "UTF-8", "html");

            return message;

        } catch (MessagingException e) {
            throw new CustomException(WorkspaceErrorCode.MESSAGE_CREATION_FAILED);
        }
    }

    @Override
    public void sendSimpleMessage(String sendEmail, String code) {
        MimeMessage message = createMail(sendEmail, code);

        try {
            javaMailSender.send(message); // 메일 발송
        } catch (MailException e) {
            throw new CustomException(WorkspaceErrorCode.MAIL_SENDING_FAILED);
        }
    }


}
