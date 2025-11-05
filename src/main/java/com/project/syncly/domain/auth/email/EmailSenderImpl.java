package com.project.syncly.domain.auth.email;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ClassPathResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EmailSenderImpl implements EmailSender {

    private final JavaMailSender javaMailSender;

    @Override
    public void send(String to, String subject, String content) {
        try {
            MimeMessage message = javaMailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setTo(to);
            helper.setSubject(subject);

            String body = createEmailTemplate(content);
            helper.setText(body, true);

            // 돌고래 이미지 첨부
            ClassPathResource dolphinImage = new ClassPathResource("static/syncly_dolphin.png");
            helper.addInline("dolphinLogo", dolphinImage);

            javaMailSender.send(message);
        } catch (MessagingException e) {
            throw new RuntimeException("이메일 전송에 실패했습니다.", e);
        }
    }

    private String createEmailTemplate(String verificationCode) {
        StringBuilder body = new StringBuilder();
        body.append("<!DOCTYPE html>");
        body.append("<html lang=\"ko\">");
        body.append("<head>");
        body.append("  <meta charset=\"UTF-8\">");
        body.append("  <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">");
        body.append("  <style>");
        body.append("    * { margin: 0; padding: 0; box-sizing: border-box; }");
        body.append("    body { font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'Helvetica Neue', Arial, sans-serif; background-color: #f5f5f5; }");
        body.append("  </style>");
        body.append("</head>");
        body.append("<body style=\"margin: 0; padding: 0; background-color: #f5f5f5;\">");
        body.append("  <table role=\"presentation\" cellpadding=\"0\" cellspacing=\"0\" border=\"0\" width=\"100%\" style=\"background-color: #f5f5f5; padding: 40px 0;\">");
        body.append("    <tr>");
        body.append("      <td align=\"center\">");
        body.append("        <!-- 메인 컨테이너 -->");
        body.append("        <table role=\"presentation\" cellpadding=\"0\" cellspacing=\"0\" border=\"0\" width=\"600\" style=\"max-width: 600px; background-color: #ffffff; border-radius: 16px; box-shadow: 0 4px 24px rgba(0, 0, 0, 0.08); overflow: hidden;\">");
        body.append("          <!-- 헤더 -->");
        body.append("          <tr>");
        body.append("            <td style=\"background: linear-gradient(135deg, #028090 0%, #456990 100%); padding: 48px 40px; text-align: center;\">");
        body.append("              <div style=\"width: 80px; height: 80px; margin: 0 auto 20px; overflow: hidden;\">");
        body.append("                <img src=\"cid:dolphinLogo\" alt=\"Syncly Dolphin\" style=\"width: 100%; height: 100%; object-fit: cover; border-radius: 50%;\" />");
        body.append("              </div>");
        body.append("              <h1 style=\"color: #ffffff; font-size: 28px; font-weight: 700; margin: 0; letter-spacing: -0.5px;\">이메일 인증</h1>");
        body.append("              <p style=\"color: #e4fde1; font-size: 16px; margin: 12px 0 0 0; opacity: 0.95;\">회원가입을 위한 인증코드입니다</p>");
        body.append("            </td>");
        body.append("          </tr>");
        body.append("          <!-- 본문 -->");
        body.append("          <tr>");
        body.append("            <td style=\"padding: 48px 40px;\">");
        body.append("              <p style=\"color: #333333; font-size: 16px; line-height: 1.6; margin: 0 0 24px 0;\">안녕하세요,</p>");
        body.append("              <p style=\"color: #333333; font-size: 16px; line-height: 1.6; margin: 0 0 32px 0;\">");
        body.append("                Syncly 회원가입을 환영합니다.<br>");
        body.append("                아래 인증코드를 입력하여 이메일 인증을 완료해 주세요.");
        body.append("              </p>");
        body.append("              <!-- 인증코드 박스 -->");
        body.append("              <div style=\"text-align: center; margin: 40px 0;\">");
        body.append("                <div style=\"display: inline-block; background: linear-gradient(135deg, #f8f9fa 0%, #e9ecef 100%); padding: 24px 48px; border-radius: 12px; border: 2px solid #028090;\">");
        body.append("                  <p style=\"color: #028090; font-size: 36px; font-weight: 700; margin: 0; letter-spacing: 8px; monospace;\">").append(verificationCode).append("</p>");
        body.append("                </div>");
        body.append("              </div>");
        body.append("              <!-- 안내 정보 -->");
        body.append("              <div style=\"background-color: #f8f9fa; border-left: 4px solid #456990; padding: 20px; margin: 32px 0; border-radius: 4px;\">");
        body.append("                <p style=\"color: #666666; font-size: 13px; margin: 0 0 8px 0; font-weight: 600;\">보안 안내</p>");
        body.append("                <p style=\"color: #666666; font-size: 13px; margin: 0; line-height: 1.5;\">");
        body.append("                  • 이 인증코드는 10분간 유효합니다<br>");
        body.append("                  • 타인에게 코드를 공유하지 마세요<br>");
        body.append("                  • 본인이 요청하지 않은 경우 이메일을 무시하세요");
        body.append("                </p>");
        body.append("              </div>");
        body.append("              <p style=\"color: #999999; font-size: 14px; line-height: 1.6; margin: 24px 0 0 0;\">");
        body.append("                만약 회원가입을 요청하지 않으셨다면, 이 이메일을 무시하셔도 됩니다.");
        body.append("              </p>");
        body.append("            </td>");
        body.append("          </tr>");
        body.append("          <!-- 푸터 -->");
        body.append("          <tr>");
        body.append("            <td style=\"background-color: #f8f9fa; padding: 32px 40px; border-top: 1px solid #e9ecef;\">");
        body.append("              <p style=\"color: #999999; font-size: 13px; line-height: 1.6; margin: 0; text-align: center;\">");
        body.append("                © 2025 Syncly. All rights reserved.<br>");
        body.append("                함께 만들어가는 협업 워크스페이스");
        body.append("              </p>");
        body.append("            </td>");
        body.append("          </tr>");
        body.append("        </table>");
        body.append("      </td>");
        body.append("    </tr>");
        body.append("  </table>");
        body.append("</body>");
        body.append("</html>");

        return body.toString();
    }
}
