package com.digitaltimeline.config;

import com.digitaltimeline.ai.AiProperties;
import com.digitaltimeline.ai.AnthropicLlmClient;
import com.digitaltimeline.ai.CachingEmbeddingProvider;
import com.digitaltimeline.ai.EmbeddingProvider;
import com.digitaltimeline.ai.HashingEmbeddingProvider;
import com.digitaltimeline.ai.LlmClient;
import com.digitaltimeline.ai.OfflineLlmClient;
import com.digitaltimeline.ai.OllamaEmbeddingProvider;
import com.digitaltimeline.ai.OllamaLlmClient;
import com.digitaltimeline.core.PiiRedactor;
import com.digitaltimeline.core.TimeRangeParser;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.http.HttpClient;
import java.time.Clock;
import java.time.Duration;
import java.util.Locale;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** FACTORY of strategies: picks the LLM / embedding implementation from configuration. */
@Configuration
public class AiConfig {
    private static final Logger log = LoggerFactory.getLogger(AiConfig.class);

    @Bean
    HttpClient httpClient() {
        return HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10))
                .followRedirects(HttpClient.Redirect.NORMAL).build();
    }

    @Bean
    Clock clock() {
        return Clock.systemUTC();
    }

    @Bean
    LlmClient llmClient(AiProperties props, HttpClient http, ObjectMapper mapper) {
        String provider = props.llmProvider().toLowerCase(Locale.ROOT);
        LlmClient client = switch (provider) {
            case "anthropic" -> {
                if (props.anthropic().apiKey().isBlank()) {
                    log.warn("dt.ai.llm-provider=anthropic but ANTHROPIC_API_KEY is empty -> using offline mode");
                    yield new OfflineLlmClient();
                }
                yield new AnthropicLlmClient(http, mapper, props.anthropic());
            }
            case "ollama" -> new OllamaLlmClient(http, mapper, props.ollama());
            default -> new OfflineLlmClient();
        };
        log.info("LLM provider: {}", client.name());
        return client;
    }

    @Bean
    EmbeddingProvider embeddingProvider(AiProperties props, HttpClient http, ObjectMapper mapper) {
        EmbeddingProvider base = "ollama".equalsIgnoreCase(props.embeddingProvider())
                ? new OllamaEmbeddingProvider(http, mapper, props.ollama())
                : new HashingEmbeddingProvider(256);
        EmbeddingProvider provider = new CachingEmbeddingProvider(base, 2000);
        log.info("Embedding provider: {}", provider.name());
        return provider;
    }

    @Bean
    PiiRedactor piiRedactor() {
        return PiiRedactor.withDefaults();
    }

    @Bean
    TimeRangeParser timeRangeParser(Clock clock) {
        return new TimeRangeParser(clock, java.time.ZoneOffset.UTC);
    }
}
