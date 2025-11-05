package com.project.syncly.domain.workspace.service;

import com.project.syncly.domain.workspace.exception.WorkspaceErrorCode;
import com.project.syncly.global.apiPayload.exception.CustomException;
import com.project.syncly.global.util.RedisUtil;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.MessagingException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
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
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(senderEmail);
            helper.setTo(mail);
            helper.setSubject("이메일 인증");

            String body = "";
            body += "<!DOCTYPE html>";
            body += "<html lang=\"ko\">";
            body += "<head>";
            body += "  <meta charset=\"UTF-8\">";
            body += "  <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">";
            body += "  <style>";
            body += "    * { margin: 0; padding: 0; box-sizing: border-box; }";
            body += "    body { font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'Helvetica Neue', Arial, sans-serif; background-color: #f5f5f5; }";
            body += "  </style>";
            body += "</head>";
            body += "<body style=\"margin: 0; padding: 0; background-color: #f5f5f5;\">";
            body += "  <table role=\"presentation\" cellpadding=\"0\" cellspacing=\"0\" border=\"0\" width=\"100%\" style=\"background-color: #f5f5f5; padding: 40px 0;\">";
            body += "    <tr>";
            body += "      <td align=\"center\">";
            body += "        <!-- 메인 컨테이너 -->";
            body += "        <table role=\"presentation\" cellpadding=\"0\" cellspacing=\"0\" border=\"0\" width=\"600\" style=\"max-width: 600px; background-color: #ffffff; border-radius: 16px; box-shadow: 0 4px 24px rgba(0, 0, 0, 0.08); overflow: hidden;\">";
            body += "          <!-- 헤더 -->";
            body += "          <tr>";
            body += "            <td style=\"background: linear-gradient(135deg, #028090 0%, #456990 100%); padding: 48px 40px; text-align: center;\">";
            body += "              <div style=\"width: 80px; height: 80px; margin: 0 auto 20px; overflow: hidden;\">";
            body += "                <img src=\"cid:dolphinLogo\" alt=\"Syncly Dolphin\" style=\"width: 100%; height: 100%; object-fit: cover; border-radius: 50%;\" />";
            body += "              </div>";
            body += "              <h1 style=\"color: #ffffff; font-size: 28px; font-weight: 700; margin: 0; letter-spacing: -0.5px;\">워크스페이스 초대</h1>";
            body += "              <p style=\"color: #e4fde1; font-size: 16px; margin: 12px 0 0 0; opacity: 0.95;\">Syncly 워크스페이스에 초대되었습니다</p>";
            body += "            </td>";
            body += "          </tr>";
            body += "          <!-- 본문 -->";
            body += "          <tr>";
            body += "            <td style=\"padding: 48px 40px;\">";
            body += "              <p style=\"color: #333333; font-size: 16px; line-height: 1.6; margin: 0 0 24px 0;\">안녕하세요,</p>";
            body += "              <p style=\"color: #333333; font-size: 16px; line-height: 1.6; margin: 0 0 32px 0;\">";
            body += "                새로운 워크스페이스에서 함께 협업하실 수 있도록 초대장을 보내드립니다.<br>";
            body += "                아래 버튼을 클릭하여 초대를 수락해 주세요.";
            body += "              </p>";
            body += "              <!-- CTA 버튼 -->";
            body += "              <div style=\"text-align: center; margin: 40px 0;\">";
            body += "                <a href=\"" + number + "\" style=\"display: inline-block; background: linear-gradient(135deg, #028090 0%, #456990 100%); color: #ffffff; text-decoration: none; padding: 16px 48px; border-radius: 8px; font-size: 16px; font-weight: 600; letter-spacing: 0.5px; box-shadow: 0 4px 12px rgba(2, 128, 144, 0.3); transition: all 0.3s ease;\">";
            body += "                  초대 수락하기";
            body += "                </a>";
            body += "              </div>";
            body += "              <!-- 링크 정보 -->";
            body += "              <div style=\"background-color: #f8f9fa; border-left: 4px solid #028090; padding: 20px; margin: 32px 0; border-radius: 4px;\">";
            body += "                <p style=\"color: #666666; font-size: 13px; margin: 0 0 8px 0; font-weight: 600;\">버튼이 작동하지 않나요?</p>";
            body += "                <p style=\"color: #666666; font-size: 13px; margin: 0; word-break: break-all; line-height: 1.5;\">";
            body += "                  아래 링크를 복사하여 브라우저에 붙여넣으세요:<br>";
            body += "                  <span style=\"color: #028090;\">" + number + "</span>";
            body += "                </p>";
            body += "              </div>";
            body += "              <p style=\"color: #999999; font-size: 14px; line-height: 1.6; margin: 24px 0 0 0;\">";
            body += "                이 초대장은 7일간 유효합니다. 만약 이 이메일을 예상하지 못했다면 무시하셔도 됩니다.";
            body += "              </p>";
            body += "            </td>";
            body += "          </tr>";
            body += "          <!-- 푸터 -->";
            body += "          <tr>";
            body += "            <td style=\"background-color: #f8f9fa; padding: 32px 40px; border-top: 1px solid #e9ecef;\">";
            body += "              <p style=\"color: #999999; font-size: 13px; line-height: 1.6; margin: 0; text-align: center;\">";
            body += "                © 2025 Syncly. All rights reserved.<br>";
            body += "                함께 만들어가는 협업 워크스페이스";
            body += "              </p>";
            body += "            </td>";
            body += "          </tr>";
            body += "        </table>";
            body += "      </td>";
            body += "    </tr>";
            body += "  </table>";
            body += "</body>";
            body += "</html>";

            helper.setText(body, true);

            // 이미지 첨부 (CID 방식)
            ClassPathResource dolphinImage = new ClassPathResource("static/syncly_dolphin.png");
            helper.addInline("dolphinLogo", dolphinImage);

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
