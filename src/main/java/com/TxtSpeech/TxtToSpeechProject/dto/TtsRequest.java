package com.TxtSpeech.TxtToSpeechProject.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class TtsRequest {

    @NotBlank(message = "Text cannot be empty")
    @Size(max = 1000, message = "Text exceeds the maximum allowed length of 1000 characters")
    private String text;

    @NotBlank(message = "language must be selected")
    private String language;

    @NotBlank(message = "voice must be selected")
    private String voice;

    // Optional user-provided API key overriding default
    private String apiKey;
}