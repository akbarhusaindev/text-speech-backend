package com.TxtSpeech.TxtToSpeechProject.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
@Slf4j
public class OtpService {

    private final EmailService emailService;
    private final SecureRandom secureRandom = new SecureRandom();

    // 5 minutes expiry
    private static final long OTP_VALID_DURATION_MS = 5 * 60 * 1000L;

    private static class OtpEntry {
        final String otp;
        final long expiresAt;

        OtpEntry(String otp, long expiresAt) {
            this.otp = otp;
            this.expiresAt = expiresAt;
        }

        boolean isExpired() {
            return System.currentTimeMillis() > expiresAt;
        }
    }

    private final Map<String, OtpEntry> otpStorage = new ConcurrentHashMap<>();

    public String generateAndSendOtp(String email, String name) {
        String normalizedEmail = email.trim().toLowerCase();

        int code = 100000 + secureRandom.nextInt(900000);
        String otp = String.valueOf(code);

        long expiresAt = System.currentTimeMillis() + OTP_VALID_DURATION_MS;
        otpStorage.put(normalizedEmail, new OtpEntry(otp, expiresAt));

        log.info("Generated OTP for {}: (Expires in 5 minutes)", normalizedEmail);

        emailService.sendOtpEmail(normalizedEmail, otp, name);

        return otp;
    }

    public boolean verifyOtp(String email, String otp) {
        String normalizedEmail = email.trim().toLowerCase();
        OtpEntry entry = otpStorage.get(normalizedEmail);

        if (entry == null) {
            throw new IllegalArgumentException("No OTP found for this email. Please request a new verification code.");
        }

        if (entry.isExpired()) {
            otpStorage.remove(normalizedEmail);
            throw new IllegalArgumentException("The verification code has expired. Please request a new one.");
        }

        if (!entry.otp.equals(otp.trim())) {
            throw new IllegalArgumentException("Invalid verification code. Please check and try again.");
        }

        // OTP verified successfully, consume it
        otpStorage.remove(normalizedEmail);
        log.info("OTP verified successfully for {}", normalizedEmail);
        return true;
    }
}
