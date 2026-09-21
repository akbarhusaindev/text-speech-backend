package com.TxtSpeech.TxtToSpeechProject.service;

import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final JavaMailSender mailSender;
    private final RestTemplate restTemplate;

    @Value("${spring.mail.username:}")
    private String mailUsername;

    @Value("${spring.mail.from:${spring.mail.username:projectworksjwt@gmail.com}}")
    private String fromEmail;

    @Value("${mail.resend.api.key:}")
    private String resendApiKey;

    @Value("${mail.resend.from-email:onboarding@resend.dev}")
    private String resendFromEmail;

    /**
     * Sends OTP verification email.
     * Returns true if sent successfully, false if delivery failed or hosting blocked outbound SMTP.
     */
    public boolean sendOtpEmail(String toEmail, String otp, String name) {
        String displayName = (name != null && !name.trim().isEmpty()) ? name.trim() : "Creator";
        String htmlContent = buildOtpEmailHtml(displayName, otp);

        // 1. Try Resend HTTP REST API if API Key is configured (works on all cloud platforms including Render free tier)
        if (resendApiKey != null && !resendApiKey.trim().isEmpty()) {
            try {
                boolean resendSuccess = sendViaResend(toEmail, otp, displayName, htmlContent);
                if (resendSuccess) {
                    log.info("Sent verification OTP email to {} via Resend HTTP API", toEmail);
                    return true;
                }
            } catch (Exception e) {
                log.warn("Resend API delivery failed: {}", e.getMessage());
            }
        }

        // 2. Try SMTP via JavaMailSender
        if (mailUsername == null || mailUsername.trim().isEmpty()) {
            log.info("No MAIL_USERNAME configured. Outbound SMTP skipped for {}", toEmail);
            return false;
        }

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            String sender = (fromEmail != null && !fromEmail.trim().isEmpty()) ? fromEmail.trim() : mailUsername.trim();
            helper.setFrom(sender, "SonicCraft Studio");
            helper.setTo(toEmail);
            helper.setSubject("Your SonicCraft Verification Code: " + otp);
            helper.setText(htmlContent, true);

            mailSender.send(message);
            log.info("Sent verification OTP email to {} via SMTP", toEmail);
            return true;
        } catch (Exception e) {
            log.warn("SMTP email delivery to {} failed (host may restrict outbound SMTP ports 25/465/587): {}", toEmail, e.getMessage());
            return false;
        }
    }

    private boolean sendViaResend(String toEmail, String otp, String displayName, String htmlContent) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(resendApiKey.trim());

        Map<String, Object> payload = new HashMap<>();
        payload.put("from", resendFromEmail);
        payload.put("to", List.of(toEmail));
        payload.put("subject", "Your SonicCraft Verification Code: " + otp);
        payload.put("html", htmlContent);

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(payload, headers);
        ResponseEntity<String> response = restTemplate.postForEntity("https://api.resend.com/emails", request, String.class);
        return response.getStatusCode().is2xxSuccessful();
    }

    private String buildOtpEmailHtml(String displayName, String otp) {
        return "<!DOCTYPE html>"
                + "<html>"
                + "<head>"
                + "<meta charset='utf-8'>"
                + "<style>"
                + "body { font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Helvetica, Arial, sans-serif; background-color: #0f172a; color: #f8fafc; margin: 0; padding: 24px; }"
                + ".card { max-width: 520px; margin: 0 auto; background: #1e293b; border-radius: 16px; border: 1px solid #334155; padding: 32px; box-shadow: 0 10px 25px rgba(0,0,0,0.4); }"
                + ".header { text-align: center; margin-bottom: 24px; }"
                + ".brand { font-size: 20px; font-weight: 800; letter-spacing: -0.5px; color: #38bdf8; text-transform: uppercase; font-family: monospace; }"
                + ".title { font-size: 20px; font-weight: 700; color: #ffffff; margin-top: 8px; margin-bottom: 4px; }"
                + ".subtitle { font-size: 13px; color: #94a3b8; }"
                + ".otp-box { background: linear-gradient(135deg, rgba(56, 189, 248, 0.1), rgba(99, 102, 241, 0.15)); border: 2px dashed #38bdf8; border-radius: 12px; padding: 20px; text-align: center; margin: 28px 0; }"
                + ".otp-code { font-family: 'SFMono-Regular', Consolas, Menlo, monospace; font-size: 36px; font-weight: 800; letter-spacing: 8px; color: #38bdf8; margin: 0; }"
                + ".expiry { font-size: 12px; color: #94a3b8; margin-top: 8px; }"
                + ".info { font-size: 13px; line-height: 1.6; color: #cbd5e1; margin-bottom: 20px; }"
                + ".footer { border-top: 1px solid #334155; padding-top: 20px; font-size: 11px; color: #64748b; text-align: center; line-height: 1.5; }"
                + "</style>"
                + "</head>"
                + "<body>"
                + "<div class='card'>"
                + "  <div class='header'>"
                + "    <div class='brand'>SONIC•CRAFT STUDIO</div>"
                + "    <div class='title'>Verification Code</div>"
                + "    <div class='subtitle'>Hello " + displayName + ", verify your email to get started</div>"
                + "  </div>"
                + "  <p class='info'>Use the verification code below to complete your registration on SonicCraft Neural Audio Studio:</p>"
                + "  <div class='otp-box'>"
                + "    <div class='otp-code'>" + otp + "</div>"
                + "    <div class='expiry'>⏱️ Valid for 5 minutes</div>"
                + "  </div>"
                + "  <p class='info'>If you did not request this verification code, please ignore this email. Never share this code with anyone.</p>"
                + "  <div class='footer'>"
                + "    SonicCraft Neural Speech & Acoustic Workstation<br>"
                + "    Automated Security System • Please do not reply to this email"
                + "  </div>"
                + "</div>"
                + "</body>"
                + "</html>";
    }
}
