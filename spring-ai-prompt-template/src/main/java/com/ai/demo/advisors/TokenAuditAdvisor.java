package com.ai.demo.advisors;

import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.CallAdvisor;
import org.springframework.ai.chat.client.advisor.api.CallAdvisorChain;
import org.springframework.ai.chat.metadata.Usage;

@Slf4j
public class TokenAuditAdvisor implements CallAdvisor {
    @Override
    public ChatClientResponse adviseCall(ChatClientRequest chatClientRequest, CallAdvisorChain callAdvisorChain) {

        ChatClientResponse chatClientResponse = callAdvisorChain.nextCall(chatClientRequest);


        assert chatClientResponse.chatResponse() != null;
        Usage usage = chatClientResponse.chatResponse().getMetadata().getUsage();
        log.info("Token usage: {}",usage.getTotalTokens());
        return chatClientResponse;
    }

    @Override
    public @NonNull String getName() {
        return "TokenAuditAdvisor";
    }

    @Override
    public int getOrder() {
        return 1;
    }
}
