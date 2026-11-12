package com.ai.demo.controller;


import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1/prompt")
public class PromptTemplateController {

    private final ChatClient chatClient;


    @GetMapping
    public String ask(@RequestParam String message) {

        return chatClient.prompt().user(message).call().content();
    }
}
