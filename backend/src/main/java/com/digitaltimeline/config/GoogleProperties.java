package com.digitaltimeline.config;
import org.springframework.boot.context.properties.ConfigurationProperties;
@ConfigurationProperties("app.google")
public record GoogleProperties(String clientId, String clientSecret, String redirectUriTemplate) {}
