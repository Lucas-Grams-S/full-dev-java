package com.payflow.assistant;

import com.anthropic.models.messages.OutputConfig;
import org.springframework.ai.anthropic.AnthropicChatOptions;

/**
 * Compiled in the lab, NEVER called (no API key). Shows the options as code; in a Boot app the same goes in
 * application.yml: spring.ai.anthropic.api-key / spring.ai.anthropic.chat.model / spring.ai.anthropic.chat.max-tokens.
 * No temperature: Claude Opus 5.5 rejects sampling parameters (400). Effort defaults to MEDIUM on this model.
 */
public final class AnthropicConfigExample {

    private AnthropicConfigExample() {}

    public static AnthropicChatOptions supportDraftOptions() {
        return AnthropicChatOptions.builder()
                .model("claude-opus-5-5")
                .maxTokens(2000)
                .effort(OutputConfig.Effort.LOW)       // short drafts: a lower effort is the first cost lever to test
                .build();
    }
}
