package com.TxtSpeech.TxtToSpeechProject.service;

import com.TxtSpeech.TxtToSpeechProject.dto.TtsRequest;
import com.TxtSpeech.TxtToSpeechProject.dto.TtsResponse;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

@Service
public class TtsService {

    private static final Logger log = LoggerFactory.getLogger(TtsService.class);

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${murf.api.url:${tts.api.url:https://api.murf.ai/v1/speech/generate}}")
    private String appUrl;

    @Value("${murf.api.key:${tts.api.key:}}")
    private String appKey;

    @Value("${murf.voice.id:${tts.voice.id:en-US-alina}}")
    private String defaultVoiceId;

    @Value("${tts.audio.storage-path:/tmp/audio/}")
    private String storagePath;

    // Supported languages map
    private static final Set<String> SUPPORTED_LANGUAGES = new HashSet<>(Arrays.asList(
            "english", "hindi", "gujarati", "marathi", "spanish", "french", "german", "japanese", "italian", "russian",
            "arabic", "korean", "portuguese", "bangla", "tamil"));

    // Supported Premade Voice IDs for Murf AI
    private static final Map<String, String> VOICE_MAP = new HashMap<>();

    static {
        // English
        VOICE_MAP.put("alina", "en-US-alina");
        VOICE_MAP.put("cooper", "en-US-cooper");
        VOICE_MAP.put("natalie", "en-US-natalie");
        VOICE_MAP.put("marcus", "en-US-marcus");
        VOICE_MAP.put("hazel", "en-UK-hazel");
        VOICE_MAP.put("gabriel", "en-UK-gabriel");
        VOICE_MAP.put("wayne", "en-US-wayne");
        VOICE_MAP.put("daniel", "en-US-daniel");
        VOICE_MAP.put("imani", "en-US-imani");
        VOICE_MAP.put("samantha", "en-US-samantha");
        VOICE_MAP.put("isha", "en-IN-isha");
        VOICE_MAP.put("joyce", "en-AU-joyce");
        VOICE_MAP.put("edmund", "en-US-edmund");
        VOICE_MAP.put("molly", "en-US-molly");

        // Legacy voice names mapped to Murf equivalents
        VOICE_MAP.put("adam", "en-US-cooper");
        VOICE_MAP.put("george", "en-UK-gabriel");
        VOICE_MAP.put("sarah", "en-US-alina");
        VOICE_MAP.put("alice", "en-UK-hazel");
        VOICE_MAP.put("jessica", "en-US-natalie");
        VOICE_MAP.put("lily", "en-US-imani");
        VOICE_MAP.put("brian", "en-US-wayne");
        VOICE_MAP.put("roger", "en-US-daniel");
        VOICE_MAP.put("charlie", "en-US-marcus");
        VOICE_MAP.put("laura", "en-US-samantha");
        VOICE_MAP.put("liam", "en-US-cooper");

        // Hindi
        VOICE_MAP.put("ayushi", "hi-IN-ayushi");
        VOICE_MAP.put("kabir", "hi-IN-kabir");
        VOICE_MAP.put("rahul", "hi-IN-rahul");
        VOICE_MAP.put("shweta", "hi-IN-shweta");
        VOICE_MAP.put("amit", "hi-IN-amit");

        // Spanish
        VOICE_MAP.put("alejandro", "es-MX-alejandro");
        VOICE_MAP.put("luisa", "es-MX-luisa");
        VOICE_MAP.put("teresa", "es-ES-teresa");

        // French
        VOICE_MAP.put("adélie", "fr-FR-adélie");
        VOICE_MAP.put("adelie", "fr-FR-adélie");
        VOICE_MAP.put("maxime", "fr-FR-maxime");
        VOICE_MAP.put("alexis", "fr-CA-alexis");

        // German
        VOICE_MAP.put("josephine", "de-DE-josephine");
        VOICE_MAP.put("erna", "de-DE-erna");
        VOICE_MAP.put("björn", "de-DE-björn");
        VOICE_MAP.put("bjorn", "de-DE-björn");

        // Italian
        VOICE_MAP.put("giorgio", "it-IT-giorgio");

        // Japanese
        VOICE_MAP.put("kenji", "ja-JP-kenji");
        VOICE_MAP.put("denki", "ja-JP-denki");

        // Korean
        VOICE_MAP.put("hwan", "ko-KR-hwan");
        VOICE_MAP.put("seok", "ko-KR-seok");

        // Portuguese
        VOICE_MAP.put("isadora", "pt-BR-isadora");
        VOICE_MAP.put("heitor", "pt-BR-heitor");

        // Bangla
        VOICE_MAP.put("anwesha", "bn-IN-anwesha");
        VOICE_MAP.put("arnab", "bn-IN-arnab");

        // Tamil
        VOICE_MAP.put("sarvesh", "ta-IN-sarvesh");
    }

    public TtsService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public TtsResponse processTtsRequest(TtsRequest request) {
        try {
            // Determine active API Key (user-provided or server default)
            String activeKey = (request.getApiKey() != null && !request.getApiKey().trim().isEmpty())
                    ? request.getApiKey().trim()
                    : appKey.trim();

            if (activeKey.isEmpty()) {
                return new TtsResponse(false, null,
                        "Murf AI API Key is missing. Please configure murf.api.key in application.properties or in settings.");
            }

            // Get voice ID
            String voiceId = getVoiceId(request.getVoice(), request.getLanguage());

            // Headers for Murf AI API
            HttpHeaders headers = new HttpHeaders();
            headers.set("api-key", activeKey);
            headers.set("Content-Type", "application/json");
            headers.set("Accept", "application/json");

            // Request payload
            Map<String, Object> body = new HashMap<>();
            body.put("text", request.getText());
            body.put("voiceId", voiceId);
            body.put("format", "MP3");

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);

            log.info("Sending TTS request to Murf AI for voice '{}' ({})", request.getVoice(), voiceId);

            ResponseEntity<String> response = restTemplate.postForEntity(
                    appUrl,
                    entity,
                    String.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                JsonNode rootNode = objectMapper.readTree(response.getBody());
                String audioFileUrl = rootNode.path("audioFile").asText(null);

                if (audioFileUrl != null && !audioFileUrl.trim().isEmpty()) {
                    String fileName = UUID.randomUUID() + ".mp3";

                    // Download and cache audio locally
                    try {
                        byte[] audioBytes = restTemplate.getForObject(audioFileUrl, byte[].class);
                        if (audioBytes != null && audioBytes.length > 0) {
                            saveAudioToFile(audioBytes, fileName);
                            String localAudioUrl = "/audio/" + fileName;
                            log.info("Speech generated and cached locally: {}", localAudioUrl);
                            return new TtsResponse(true, localAudioUrl, "Speech generated successfully via Murf AI");
                        }
                    } catch (Exception dlEx) {
                        log.warn("Could not cache audio locally, returning remote Murf URL: {}", dlEx.getMessage());
                    }

                    return new TtsResponse(true, audioFileUrl, "Speech generated successfully via Murf AI");
                }
            }

            return new TtsResponse(false, null,
                    "Failed to generate speech: Provider returned status " + response.getStatusCode());

        } catch (IllegalArgumentException e) {
            throw e; // Handled by GlobalExceptionHandler
        } catch (HttpStatusCodeException e) {
            String responseBody = e.getResponseBodyAsString();
            log.error("Murf AI API HTTP error {}: {}", e.getStatusCode(), responseBody);

            String errorMsg = null;
            try {
                JsonNode rootNode = objectMapper.readTree(responseBody);
                if (rootNode.has("message")) {
                    errorMsg = rootNode.get("message").asText();
                } else if (rootNode.has("errorMessage")) {
                    errorMsg = rootNode.get("errorMessage").asText();
                } else if (rootNode.has("detail")) {
                    errorMsg = rootNode.get("detail").asText();
                }
            } catch (Exception parseEx) {
                log.warn("Failed to parse Murf AI error JSON: {}", parseEx.getMessage());
            }

            if (e.getStatusCode() == HttpStatus.UNAUTHORIZED || e.getStatusCode() == HttpStatus.FORBIDDEN) {
                return new TtsResponse(false, null,
                        "Invalid or unauthorized Murf AI API key. Please verify your murf.api.key or update your API key in settings.");
            }

            if (errorMsg != null && !errorMsg.trim().isEmpty()) {
                return new TtsResponse(false, null, "Murf AI Notice: " + errorMsg);
            }

            return new TtsResponse(false, null, "Murf AI error (" + e.getStatusCode() + "): " + e.getStatusText());
        } catch (Exception e) {
            log.error("Unexpected error in TTS processing", e);
            return new TtsResponse(false, null, "Error occurred while generating speech: " + e.getMessage());
        }
    }

    private void saveAudioToFile(byte[] audioData, String fileName) throws IOException {
        Path dirPath = Paths.get(storagePath).toAbsolutePath().normalize();
        if (!Files.exists(dirPath)) {
            Files.createDirectories(dirPath);
        }

        File audioFile = dirPath.resolve(fileName).toFile();
        try (FileOutputStream fos = new FileOutputStream(audioFile)) {
            fos.write(audioData);
        }
    }

    private String getVoiceId(String voiceName, String language) {
        if (voiceName == null || voiceName.trim().isEmpty()) {
            return (defaultVoiceId != null && !defaultVoiceId.trim().isEmpty()) ? defaultVoiceId.trim() : "en-US-alina";
        }

        String trimmed = voiceName.trim();
        // If it already is a Murf voice ID (e.g. en-US-alina, hi-IN-ayushi)
        if (trimmed.contains("-") && trimmed.length() >= 5) {
            return trimmed;
        }

        String normalized = trimmed.toLowerCase();
        if ("default".equals(normalized)) {
            return (defaultVoiceId != null && !defaultVoiceId.trim().isEmpty()) ? defaultVoiceId.trim() : "en-US-alina";
        }

        String voiceId = VOICE_MAP.get(normalized);
        if (voiceId != null) {
            return voiceId;
        }

        // Language based fallback
        if (language != null) {
            String langNorm = language.trim().toLowerCase();
            if (langNorm.contains("hindi"))
                return "hi-IN-ayushi";
            if (langNorm.contains("spanish"))
                return "es-MX-alejandro";
            if (langNorm.contains("french"))
                return "fr-FR-adélie";
            if (langNorm.contains("german"))
                return "de-DE-josephine";
            if (langNorm.contains("italian"))
                return "it-IT-giorgio";
            if (langNorm.contains("japanese"))
                return "ja-JP-kenji";
            if (langNorm.contains("korean"))
                return "ko-KR-hwan";
            if (langNorm.contains("portuguese"))
                return "pt-BR-isadora";
            if (langNorm.contains("bangla"))
                return "bn-IN-anwesha";
            if (langNorm.contains("tamil"))
                return "ta-IN-sarvesh";
        }

        if (defaultVoiceId != null && !defaultVoiceId.trim().isEmpty()) {
            return defaultVoiceId.trim();
        }

        return "en-US-alina";
    }
}