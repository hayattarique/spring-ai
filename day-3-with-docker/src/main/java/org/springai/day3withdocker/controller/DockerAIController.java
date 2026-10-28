package org.springai.day3withdocker.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

@RequiredArgsConstructor
@RequestMapping("/api/chat/v1")
@RestController
public class DockerAIController {

    private final ChatClient chatClient;

    @GetMapping
    public Flux<String> getMessage(@RequestParam String message) {
        return chatClient.prompt().user(message).stream().content();
    }
}

