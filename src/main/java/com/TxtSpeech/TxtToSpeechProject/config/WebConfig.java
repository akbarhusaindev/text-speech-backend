package com.TxtSpeech.TxtToSpeechProject.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    private static final Logger log = LoggerFactory.getLogger(WebConfig.class);

    @Value("${tts.audio.storage-path:/tmp/audio/}")
    private String storagePath;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        try {
            Path path = Paths.get(storagePath).toAbsolutePath().normalize();
            if (!Files.exists(path)) {
                Files.createDirectories(path);
            }
            String uriString = path.toUri().toString();
            if (!uriString.endsWith("/")) {
                uriString += "/";
            }

            log.info("Mapping /audio/** to resource location: {}", uriString);

            registry.addResourceHandler("/audio/**")
                    .addResourceLocations(uriString, "classpath:/static/audio/");
        } catch (Exception e) {
            log.error("Failed to initialize audio static resource handler", e);
        }
    }
}