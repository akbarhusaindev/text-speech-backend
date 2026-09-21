package com.TxtSpeech.TxtToSpeechProject.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TtsResponse {

    private boolean success;

    @JsonProperty("audioUrl")
    @JsonAlias({"addUrl", "audio_url"})
    private String audioUrl;

    private String message;
}

