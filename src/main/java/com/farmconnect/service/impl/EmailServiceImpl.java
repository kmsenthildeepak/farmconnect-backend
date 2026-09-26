package com.farmconnect.service.impl;

import com.farmconnect.service.EmailService;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.io.UnsupportedEncodingException;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailServiceImpl implements EmailService {

    private final JavaMailSender mailSender;

    @Value("${app.mail.sender-email:noreply@farmconnect.com}")
    private String senderEmail;

    @Value("${app.mail.sender-name:FarmConnect}")
    private String senderName;

    @Value("${app.reset-password.expiration-minutes:30}")
    private int expirationMinutes;

    @Value("${app.mail.reset-base-url:${RESET_BASE_URL:http://localhost:8080}}")
    private String resetBaseUrl;

    @Override
    public String buildResetPasswordUrl(String token) {
        String base = (resetBaseUrl != null && !resetBaseUrl.isBlank()) ? resetBaseUrl.trim() : "http://localhost:8080";
        if (base.endsWith("/")) {
            base = base.substring(0, base.length() - 1);
        }
        return base + "/reset-password?token=" + token;
    }

    @Override
    public void sendPasswordResetEmail(String toEmail, String userName, String resetUrlOrToken) {
        try {
            String resetUrl;
            if (resetUrlOrToken != null && (resetUrlOrToken.startsWith("http://") || resetUrlOrToken.startsWith("https://"))) {
                resetUrl = resetUrlOrToken;
            } else {
                resetUrl = buildResetPasswordUrl(resetUrlOrToken);
            }

            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(senderEmail, senderName);
            helper.setTo(toEmail);
            helper.setSubject("FarmConnect Password Reset");

            String htmlContent = buildResetPasswordEmailHtml(userName, resetUrl);
            helper.setText(htmlContent, true);

            mailSender.send(message);
            log.info("Password reset email sent to {}", toEmail);
        } catch (MailException e) {
            log.error("SMTP failure sending password reset email to {}: [{}] {}", toEmail, e.getClass().getName(), e.getMessage(), e);
        } catch (MessagingException | UnsupportedEncodingException e) {
            log.error("Failed to construct password reset email to {}: [{}] {}", toEmail, e.getClass().getName(), e.getMessage(), e);
        }
    }

    private String buildResetPasswordEmailHtml(String userName, String resetUrl) {
        String greeting = (userName != null && !userName.isBlank()) ? "Hello " + userName + "," : "Hello,";
        return "<!DOCTYPE html>"
                + "<html>"
                + "<head><meta charset='UTF-8'></head>"
                + "<body style='font-family: Arial, sans-serif; background-color: #f4f6f8; margin: 0; padding: 24px;'>"
                + "<table align='center' border='0' cellpadding='0' cellspacing='0' width='100%' style='max-width: 600px; background-color: #ffffff; border-radius: 8px; overflow: hidden; box-shadow: 0 2px 8px rgba(0,0,0,0.06);'>"
                + "  <tr>"
                + "    <td style='background-color: #2E7D32; padding: 24px 32px; text-align: center;'>"
                + "      <h1 style='color: #ffffff; margin: 0; font-size: 24px;'>FarmConnect</h1>"
                + "    </td>"
                + "  </tr>"
                + "  <tr>"
                + "    <td style='padding: 32px;'>"
                + "      <p style='color: #333333; font-size: 16px; margin: 0 0 16px;'>" + greeting + "</p>"
                + "      <p style='color: #555555; font-size: 15px; line-height: 1.5; margin: 0 0 24px;'>"
                + "        We received a request to reset your FarmConnect password.<br/>"
                + "        Click the button below to create a new password:"
                + "      </p>"
                + "      <div style='text-align: center; margin: 32px 0;'>"
                + "        <a href=\"" + resetUrl + "\" style=\"background-color: #2E7D32; color: #ffffff; padding: 14px 28px; text-decoration: none; border-radius: 6px; font-weight: bold; font-size: 16px; display: inline-block;\">Reset Password</a>"
                + "      </div>"
                + "      <p style='color: #777777; font-size: 13px; line-height: 1.5; margin: 0 0 16px;'>"
                + "        This link will expire in " + expirationMinutes + " minutes.<br/>"
                + "        If the button above doesn't work, copy and paste this link into your browser:<br/>"
                + "        <a href=\"" + resetUrl + "\" style=\"color: #2E7D32; word-break: break-all;\">" + resetUrl + "</a>"
                + "      </p>"
                + "      <p style='color: #888888; font-size: 13px; line-height: 1.5; margin: 24px 0 0; border-top: 1px solid #eeeeee; padding-top: 16px;'>"
                + "        If you did not request a password reset, you can safely ignore this email.<br/><br/>"
                + "        Regards,<br/><strong>FarmConnect Team</strong>"
                + "      </p>"
                + "    </td>"
                + "  </tr>"
                + "</table>"
                + "</body>"
                + "</html>";
    }
}
