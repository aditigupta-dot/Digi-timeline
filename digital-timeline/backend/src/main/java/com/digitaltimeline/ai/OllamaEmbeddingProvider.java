package com.digitaltimeline.ai;

import com.digitaltimeline.core.VectorMath;
import com.digitaltimeline.exception.AiProviderException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/** Real neural embeddings from a local Ollama model (default: nomic-embed-text, 768 dimensions). */
public class OllamaEmbeddingProvider implements EmbeddingProvider {

    private final HttpClient http;
    private final ObjectMapper mapper;
    private final AiProperties.Ollama config;
    private volatile int dims = 768;

    public OllamaEmbeddingProvider(HttpClient http, ObjectMapper mapper, AiProperties.Ollama config) {
        this.http = http;
        this.mapper = mapper;
        this.config = config;
    }

    @Override
    public String name() {
        return "ollama:" + config.embedModel();
    }

    @Override
    public int dimensions() {
        return dims;
    }

    @Override
    public float[] embed(String text) {
        try {
            ObjectNode body = mapper.createObjectNode();
            body.put("model", config.embedModel());
            body.put("prompt", text.length() > 6000 ? text.substring(0, 6000) : text);
            HttpRequest request = HttpRequest.newBuilder(URI.create(config.baseUrl() + "/api/embeddings"))
                    .timeout(Duration.ofSeconds(60))
                    .header("content-type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(body)))
                    .build();
            HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() / 100 != 2) {
                throw new AiProviderException("Ollama embeddings returned HTTP " + response.statusCode());
            }
            JsonNode arr = mapper.readTree(response.body()).path("embedding");
            if (!arr.isArray() || arr.isEmpty()) throw new AiProviderException("Ollama returned no embedding");
            float[] v = new float[arr.size()];
            for (int i = 0; i < v.length; i++) v[i] = (float) arr.get(i).asDouble();
            dims = v.length;
            return VectorMath.normalize(v);
        } catch (IOException e) {
            throw new AiProviderException("Could not reach Ollama embeddings: " + e.getMessage(), e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new AiProviderException("Interrupted while embedding", e);
        }
    }
}
