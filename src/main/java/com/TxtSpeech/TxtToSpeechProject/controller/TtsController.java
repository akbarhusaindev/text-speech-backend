package com.TxtSpeech.TxtToSpeechProject.controller;

import com.TxtSpeech.TxtToSpeechProject.dto.TtsRequest;
import com.TxtSpeech.TxtToSpeechProject.dto.TtsResponse;
import com.TxtSpeech.TxtToSpeechProject.service.TtsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
@Tag(name = "Text to Speech API", description = "Endpoints for converting text to audio")
public class TtsController {

    private final TtsService ttsService;

    public TtsController(TtsService ttsService) {
        this.ttsService = ttsService;
    }

    @Operation(summary = "Generate Speech", description = "Accepts text, language, and voice preferences to generate an MP3 audio file.")
    @PostMapping("/tts")
    public ResponseEntity<TtsResponse> generateSpeech(@Valid @RequestBody TtsRequest request) {
        TtsResponse response = ttsService.processTtsRequest(request);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Health Check", description = "Verifies the backend server is running.")
    @GetMapping("/health")
    public ResponseEntity<String> healthCheck() {
        return ResponseEntity.ok("TTS Backend is running and healthy.");
    }
}