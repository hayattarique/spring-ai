package com.ai.demo.config;

import com.ai.demo.advisors.TokenAuditAdvisor;
import com.ai.demo.constants.PromptConstants;
import com.ai.demo.util.PromptUtil;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class AiConfig {

    @Bean
    ChatClient chatClient(ChatClient.Builder chatClientBuilder) {

        return chatClientBuilder.defaultSystem(PromptUtil.getPrompt(PromptConstants.AI_DEFAULT_PROMPT))
                .defaultAdvisors(List.of(new SimpleLoggerAdvisor(), new TokenAuditAdvisor()))
                .defaultOptions(
                        ChatOptions.builder().maxTokens(100).model("gpt-5").frequencyPenalty(0.5).presencePenalty(0.5)

                )
                .build();
    }
}
