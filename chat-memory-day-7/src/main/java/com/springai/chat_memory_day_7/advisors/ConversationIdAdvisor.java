package com.springai.chat_memory_day_7.advisors;

import org.jspecify.annotations.NonNull;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.CallAdvisor;
import org.springframework.ai.chat.client.advisor.api.CallAdvisorChain;
import org.springframework.ai.chat.client.advisor.api.StreamAdvisor;
import org.springframework.ai.chat.client.advisor.api.StreamAdvisorChain;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import reactor.core.publisher.Flux;

@Component
public class ConversationIdAdvisor implements CallAdvisor, StreamAdvisor {
    private static final String HEADER = "X-Conversation-Id";

    @Override
    public @NonNull ChatClientResponse adviseCall(ChatClientRequest chatClientRequest, @NonNull CallAdvisorChain callAdvisorChain) {
        //respect an explicit id if caller already set
        if (!chatClientRequest.context().containsKey(ChatMemory.CONVERSATION_ID)) {
            String currentConversationId = getCurrentConversationId();
            if (currentConversationId == null || currentConversationId.isEmpty()) {
                throw new IllegalStateException("Missing " + HEADER + " header");
            }
            ChatClientRequest build = chatClientRequest.mutate().context(ChatMemory.CONVERSATION_ID, currentConversationId).build();

            return callAdvisorChain.nextCall(build);
        }
        return callAdvisorChain.nextCall(chatClientRequest);
    }

    private String getCurrentConversationId() {
        if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attributes) {
            return attributes.getRequest().getHeader(HEADER);

        }
        return null;
    }

    @Override
    public @NonNull String getName() {
        return "ConversationIdAdvisor";
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }

    @Override
    public @NonNull Flux<ChatClientResponse> adviseStream(ChatClientRequest chatClientRequest, StreamAdvisorChain streamAdvisorChain) {
        //respect earlier conversation id
        if (!chatClientRequest.context().containsKey(ChatMemory.CONVERSATION_ID)) {
            String currentConversationId = getCurrentConversationId();
            if (currentConversationId == null || currentConversationId.isEmpty()) {
                throw new IllegalStateException("Missing " + HEADER + " header");
            }

            return streamAdvisorChain.nextStream(chatClientRequest.mutate().context(ChatMemory.CONVERSATION_ID, currentConversationId).build());

        }
        return streamAdvisorChain.nextStream(chatClientRequest);
    }
}
