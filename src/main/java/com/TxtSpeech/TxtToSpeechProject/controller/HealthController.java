package com.TxtSpeech.TxtToSpeechProject.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@CrossOrigin(origins = "*")
@Tag(name = "Health Check", description = "Server health and status endpoints")
public class HealthController {

    @Operation(summary = "Root Health Check")
    @GetMapping({"/health", "/"})
    public ResponseEntity<Map<String, Object>> getHealth() {
        Map<String, Object> status = new HashMap<>();
        status.put("status", "UP");
        status.put("service", "TextToSpeech Backend");
        status.put("timestamp", System.currentTimeMillis());
        return ResponseEntity.ok(status);
    }
}

