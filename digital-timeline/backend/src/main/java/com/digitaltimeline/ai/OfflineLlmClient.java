package com.digitaltimeline.ai;

import com.digitaltimeline.exception.AiProviderException;

/** Null-object LLM. The app stays fully usable without any key: answers become extractive (see OfflineNarrator). */
public class OfflineLlmClient implements LlmClient {

    @Override
    public String name() {
        return "offline";
    }

    @Override
    public boolean isGenerative() {
        return false;
    }

    @Override
    public String complete(String systemPrompt, String userPrompt) {
        throw new AiProviderException("No LLM configured (offline mode)");
    }
}
