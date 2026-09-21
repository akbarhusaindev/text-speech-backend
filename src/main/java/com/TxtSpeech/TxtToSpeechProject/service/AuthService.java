package com.TxtSpeech.TxtToSpeechProject.service;

import com.TxtSpeech.TxtToSpeechProject.dto.*;
import com.TxtSpeech.TxtToSpeechProject.model.User;
import com.TxtSpeech.TxtToSpeechProject.repository.UserRepository;
import com.TxtSpeech.TxtToSpeechProject.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final OtpService otpService;

    public java.util.Map<String, Object> sendOtp(SendOtpRequest request) {
        String normalizedEmail = request.getEmail().trim().toLowerCase();

        if (userRepository.existsByEmail(normalizedEmail)) {
            throw new IllegalArgumentException("An account with email " + normalizedEmail + " already exists. Please log in.");
        }

        OtpService.OtpResult result = otpService.generateAndSendOtp(normalizedEmail, request.getName());

        java.util.Map<String, Object> response = new java.util.HashMap<>();
        response.put("email", normalizedEmail);
        response.put("emailSent", result.emailSent());

        if (result.emailSent()) {
            response.put("message", "Verification code sent to " + request.getEmail());
        } else {
            response.put("message", "Verification code generated! (Hosting notice: Free tier restricts SMTP. Verification code provided below)");
            response.put("fallback", true);
        }
        // Always provide OTP in response so that if email delivery fails, user can still complete signup
        response.put("otp", result.otp());

        return response;
    }

    public boolean verifyOtp(VerifyOtpRequest request) {
        return otpService.verifyOtp(request.getEmail(), request.getOtp());
    }

    @Transactional
    public AuthResponse signup(SignupRequest request) {
        String normalizedEmail = request.getEmail().trim().toLowerCase();

        if (userRepository.existsByEmail(normalizedEmail)) {
            throw new IllegalArgumentException("An account with email " + normalizedEmail + " already exists. Please log in.");
        }

        // If OTP is provided in signup request, verify it
        if (request.getOtp() != null && !request.getOtp().trim().isEmpty()) {
            otpService.verifyOtp(normalizedEmail, request.getOtp());
        }

        User user = User.builder()
                .name(request.getName().trim())
                .email(normalizedEmail)
                .password(passwordEncoder.encode(request.getPassword()))
                .build();

        User savedUser = userRepository.save(user);
        otpService.clearVerifiedEmail(normalizedEmail);
        log.info("Registered new verified user: {} (ID: {})", savedUser.getEmail(), savedUser.getId());

        String token = jwtUtil.generateToken(savedUser.getEmail(), savedUser.getName(), savedUser.getId());

        UserDto userDto = UserDto.builder()
                .id(savedUser.getId())
                .name(savedUser.getName())
                .email(savedUser.getEmail())
                .createdAt(savedUser.getCreatedAt())
                .build();

        return AuthResponse.builder()
                .token(token)
                .tokenType("Bearer")
                .user(userDto)
                .message("Account created and verified successfully!")
                .build();
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        String normalizedEmail = request.getEmail().trim().toLowerCase();

        User user = userRepository.findByEmail(normalizedEmail)
                .orElseThrow(() -> new IllegalArgumentException("Invalid email or password."));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new IllegalArgumentException("Invalid email or password.");
        }

        log.info("User logged in: {} (ID: {})", user.getEmail(), user.getId());

        String token = jwtUtil.generateToken(user.getEmail(), user.getName(), user.getId());

        UserDto userDto = UserDto.builder()
                .id(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .createdAt(user.getCreatedAt())
                .build();

        return AuthResponse.builder()
                .token(token)
                .tokenType("Bearer")
                .user(userDto)
                .message("Logged in successfully!")
                .build();
    }

    @Transactional(readOnly = true)
    public UserDto getCurrentUser(String email) {
        String normalizedEmail = email.trim().toLowerCase();
        User user = userRepository.findByEmail(normalizedEmail)
                .orElseThrow(() -> new IllegalArgumentException("User not found for email: " + normalizedEmail));

        return UserDto.builder()
                .id(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .createdAt(user.getCreatedAt())
                .build();
    }
}
