package com.digitaltimeline.config;
import org.springframework.boot.context.properties.ConfigurationProperties;
@ConfigurationProperties("app.ai")
public record AiProperties(String geminiApiKey, String geminiModel) {}
