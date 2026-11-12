package com.ai.hrms.config;

import com.ai.hrms.advisors.ConversationIdAdvisor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;

@Configuration
public class AIConfig {

    @Value("classpath:/prompts/default-prompt.st")
    private Resource resource;


    @Bean
    ChatClient chatClient(ChatClient.Builder chatClientBuilder, ChatMemory chatMemory){
        return chatClientBuilder.
                defaultSystem(resource)
                .defaultAdvisors(new SimpleLoggerAdvisor(),new ConversationIdAdvisor(), MessageChatMemoryAdvisor.builder(chatMemory).build())
                .build();
    }
}
