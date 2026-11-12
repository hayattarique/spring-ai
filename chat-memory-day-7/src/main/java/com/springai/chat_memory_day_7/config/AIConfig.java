package com.springai.chat_memory_day_7.config;

import com.springai.chat_memory_day_7.advisors.ConversationIdAdvisor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AIConfig {


    @Bean
    ChatClient chatClient(ChatClient.Builder chatClientBuilder, ChatMemory chatMemory, ConversationIdAdvisor advisor) {

        return chatClientBuilder.defaultAdvisors(MessageChatMemoryAdvisor.builder(chatMemory).build(), advisor).build();
    }


}
