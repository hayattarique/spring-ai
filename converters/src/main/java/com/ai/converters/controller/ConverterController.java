package com.ai.converters.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/convert")
@RequiredArgsConstructor
public class ConverterController {

    private final ChatClient chatClient;

    @PostMapping
    public ResponseEntity<Object> getCountryList(@RequestBody String message) {

        return ResponseEntity.ok(chatClient.prompt().user(message).call().entity(
                List.class
        ));

    }
}
