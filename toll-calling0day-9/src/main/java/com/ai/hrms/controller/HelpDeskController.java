package com.ai.hrms.controller;

import com.ai.hrms.tools.HelpDeskTool;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RequestMapping("/api/v1/help")
@RestController
@RequiredArgsConstructor
public class HelpDeskController {

    private final ChatClient chatClient;
    private final HelpDeskTool deskTool;


    @PostMapping("/query")
    public ResponseEntity<Object> ask(@RequestHeader("X-USER-ACCOUNT-ID") Long userAccountId, @RequestBody String message) {

        return ResponseEntity.ok(  chatClient.prompt()
                .user(message)
                .tools(deskTool)
                .toolContext(Map.of("userAccountId", userAccountId))
                .call()
                .content());



    }
}
