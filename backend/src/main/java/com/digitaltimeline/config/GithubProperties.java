package com.digitaltimeline.config;
import org.springframework.boot.context.properties.ConfigurationProperties;
@ConfigurationProperties("app.github")
public record GithubProperties(String clientId, String clientSecret, String scopes, String redirectUri, String frontendUrl) {}
