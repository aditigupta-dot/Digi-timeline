package com.digitaltimeline.ai;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

@ConfigurationProperties(prefix = "dt.ai")
public record AiProperties(
        @DefaultValue("offline") String llmProvider,
        @DefaultValue("hashing") String embeddingProvider,
        @DefaultValue("8") int topK,
        @DefaultValue("true") boolean redactPii,
        @DefaultValue Anthropic anthropic,
        @DefaultValue Ollama ollama) {

    public record Anthropic(@DefaultValue("") String apiKey,
                            @DefaultValue("claude-sonnet-5-5") String model,
                            @DefaultValue("1200") int maxTokens) {}

    public record Ollama(@DefaultValue("http://localhost:11434") String baseUrl,
                         @DefaultValue("llama3.1") String chatModel,
                         @DefaultValue("nomic-embed-text") String embedModel) {}
}
