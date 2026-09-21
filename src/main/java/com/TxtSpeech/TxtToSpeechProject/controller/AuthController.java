package com.TxtSpeech.TxtToSpeechProject.controller;

import com.TxtSpeech.TxtToSpeechProject.dto.*;
import com.TxtSpeech.TxtToSpeechProject.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "User registration, login, OTP verification, and profile endpoints")
public class AuthController {

    private final AuthService authService;

    @PostMapping("/send-otp")
    @Operation(summary = "Send OTP email", description = "Generates and sends a 6-digit verification code to the specified email address")
    public ResponseEntity<Map<String, Object>> sendOtp(@Valid @RequestBody SendOtpRequest request) {
        Map<String, Object> response = authService.sendOtp(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/verify-otp")
    @Operation(summary = "Verify OTP code", description = "Validates the 6-digit code against the specified email address")
    public ResponseEntity<Map<String, Object>> verifyOtp(@Valid @RequestBody VerifyOtpRequest request) {
        boolean verified = authService.verifyOtp(request);
        return ResponseEntity.ok(Map.of("verified", verified, "message", "OTP verified successfully"));
    }

    @PostMapping("/signup")
    @Operation(summary = "Register a new user", description = "Creates a new verified user account and returns a JWT authentication token")
    public ResponseEntity<AuthResponse> signup(@Valid @RequestBody SignupRequest request) {
        AuthResponse response = authService.signup(request);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @PostMapping("/login")
    @Operation(summary = "Authenticate user", description = "Validates user credentials and returns a JWT authentication token")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        AuthResponse response = authService.login(request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/me")
    @Operation(summary = "Get current authenticated user profile", description = "Fetches the profile details of the currently logged-in user")
    public ResponseEntity<UserDto> getCurrentUser(Principal principal, Authentication authentication) {
        if (principal == null && authentication == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        String email = principal != null ? principal.getName() : authentication.getName();
        UserDto user = authService.getCurrentUser(email);
        return ResponseEntity.ok(user);
    }
}
