package com.payflow.assistant;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;

/** Stands in for the LLM: returns scripted answers and records every prompt it receives. */
class FakeChatModel implements ChatModel {

    final Deque<AssistantMessage> script = new ArrayDeque<>();
    final List<Prompt> received = new ArrayList<>();

    FakeChatModel reply(String text) {
        script.add(new AssistantMessage(text));
        return this;
    }

    FakeChatModel callTool(String name, String argumentsJson) {
        script.add(AssistantMessage.builder().content("")
                .toolCalls(List.of(new AssistantMessage.ToolCall("call-" + (script.size() + 1), "function", name, argumentsJson)))
                .build());
        return this;
    }

    @Override
    public ChatResponse call(Prompt prompt) {
        received.add(prompt);
        return new ChatResponse(List.of(new Generation(script.removeFirst())));
    }
}
