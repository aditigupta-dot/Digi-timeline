package com.digitaltimeline.ai;

import com.digitaltimeline.core.TextTools;
import com.digitaltimeline.exception.AiProviderException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/** Local LLM through Ollama: nothing ever leaves the user's machine. */
public class OllamaLlmClient implements LlmClient {

    private final HttpClient http;
    private final ObjectMapper mapper;
    private final AiProperties.Ollama config;

    public OllamaLlmClient(HttpClient http, ObjectMapper mapper, AiProperties.Ollama config) {
        this.http = http;
        this.mapper = mapper;
        this.config = config;
    }

    @Override
    public String name() {
        return "ollama:" + config.chatModel();
    }

    @Override
    public boolean isGenerative() {
        return true;
    }

    @Override
    public String complete(String systemPrompt, String userPrompt) {
        try {
            ObjectNode body = mapper.createObjectNode();
            body.put("model", config.chatModel());
            body.put("stream", false);
            ArrayNode messages = body.putArray("messages");
            messages.addObject().put("role", "system").put("content", systemPrompt);
            messages.addObject().put("role", "user").put("content", userPrompt);

            HttpRequest request = HttpRequest.newBuilder(URI.create(config.baseUrl() + "/api/chat"))
                    .timeout(Duration.ofSeconds(120))
                    .header("content-type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(body)))
                    .build();
            HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() / 100 != 2) {
                throw new AiProviderException("Ollama returned HTTP " + response.statusCode() + ": "
                        + TextTools.truncate(response.body(), 200));
            }
            return mapper.readTree(response.body()).path("message").path("content").asText("").trim();
        } catch (IOException e) {
            throw new AiProviderException("Could not reach Ollama: " + e.getMessage(), e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new AiProviderException("Interrupted while calling Ollama", e);
        }
    }
}
