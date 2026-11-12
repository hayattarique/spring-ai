package com.springai.chat_memory_day_7.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

@RestController
@RequestMapping("/api/v1/chat")
@RequiredArgsConstructor
public class ChatController {

    private final ChatClient chatClient;


    @GetMapping
    public Flux<String> ask(@RequestParam String message) {
        return chatClient.prompt().user(message).stream().content();
    }

}
