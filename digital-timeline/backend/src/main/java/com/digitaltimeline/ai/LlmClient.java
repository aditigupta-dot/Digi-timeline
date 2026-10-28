package com.digitaltimeline.ai;

/**
 * STRATEGY pattern: the rest of the app only sees this interface.
 * Implementations: Anthropic (cloud), Ollama (local, private) and Offline (no model at all).
 */
public interface LlmClient {

    String name();

    /** False for the offline client: services then build a deterministic, extractive answer instead. */
    boolean isGenerative();

    String complete(String systemPrompt, String userPrompt);
}
