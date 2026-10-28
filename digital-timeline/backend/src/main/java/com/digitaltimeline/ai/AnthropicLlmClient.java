package com.digitaltimeline.ai;

import com.digitaltimeline.core.TextTools;
import com.digitaltimeline.exception.AiProviderException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/** Calls the Anthropic Messages API with plain java.net.http (no SDK needed). */
public class AnthropicLlmClient implements LlmClient {

    private static final URI ENDPOINT = URI.create("https://api.anthropic.com/v1/messages");

    private final HttpClient http;
    private final ObjectMapper mapper;
    private final AiProperties.Anthropic config;

    public AnthropicLlmClient(HttpClient http, ObjectMapper mapper, AiProperties.Anthropic config) {
        this.http = http;
        this.mapper = mapper;
        this.config = config;
    }

    @Override
    public String name() {
        return "anthropic:" + config.model();
    }

    @Override
    public boolean isGenerative() {
        return true;
    }

    @Override
    public String complete(String systemPrompt, String userPrompt) {
        try {
            ObjectNode body = mapper.createObjectNode();
            body.put("model", config.model());
            body.put("max_tokens", config.maxTokens());
            body.put("system", systemPrompt);
            ArrayNode messages = body.putArray("messages");
            messages.addObject().put("role", "user").put("content", userPrompt);

            HttpRequest request = HttpRequest.newBuilder(ENDPOINT)
                    .timeout(Duration.ofSeconds(60))
                    .header("content-type", "application/json")
                    .header("x-api-key", config.apiKey())
                    .header("anthropic-version", "2023-06-01")
                    .POST(HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(body)))
                    .build();

            HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() / 100 != 2) {
                throw new AiProviderException("Anthropic returned HTTP " + response.statusCode() + ": "
                        + TextTools.truncate(response.body(), 200));
            }
            StringBuilder text = new StringBuilder();
            for (JsonNode block : mapper.readTree(response.body()).path("content")) {
                if ("text".equals(block.path("type").asText())) text.append(block.path("text").asText());
            }
            return text.toString().trim();
        } catch (IOException e) {
            throw new AiProviderException("Could not reach Anthropic: " + e.getMessage(), e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new AiProviderException("Interrupted while calling Anthropic", e);
        }
    }
}
