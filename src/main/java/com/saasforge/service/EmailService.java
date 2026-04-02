package com.saasforge.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${app.base-url}")
    private String baseUrl;

    @Async
    public void sendPasswordResetEmail(String toEmail, String token) {
        String resetLink = baseUrl + "/reset-password?token=" + token;
        String subject = "Reset Your SaaSForge Password";
        String body = "<h2>Password Reset Request</h2>"
                + "<p>Click the link below to reset your password. This link expires in 15 minutes.</p>"
                + "<p><a href='" + resetLink + "'>Reset Password</a></p>"
                + "<p>If you did not request this, ignore this email.</p>";
        sendEmail(toEmail, subject, body);
    }

    @Async
    public void sendInvitationEmail(String toEmail, String token, String tenantName) {
        String inviteLink = baseUrl + "/accept-invitation?token=" + token;
        String subject = "You're invited to join " + tenantName + " on SaaSForge";
        String body = "<h2>You've been invited!</h2>"
                + "<p>You have been invited to join <strong>" + tenantName + "</strong>.</p>"
                + "<p><a href='" + inviteLink + "'>Accept Invitation</a></p>"
                + "<p>This invitation expires in 7 days.</p>";
        sendEmail(toEmail, subject, body);
    }

    private void sendEmail(String to, String subject, String htmlBody) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlBody, true);
            mailSender.send(message);
            log.info("Email sent successfully to {}", to);
        } catch (MessagingException e) {
            log.error("Failed to send email to {}: {}", to, e.getMessage());
        }
    }
}
