package com.ai.basicragday8.controller;


import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/chat")
public class RagBasicController {

    private final ChatClient chatClient;
    private final Resource resource;
    private final VectorStore vectorStore;
    private final Resource hrResource;

    public RagBasicController(ChatClient chatClient, @Value("classpath:/prompts/system-default.st") Resource resource,
                              VectorStore vectorStore, @Value("classpath:/prompts/hr-system.st") Resource hrResource) {
        this.chatClient = chatClient;
        this.resource = resource;
        this.vectorStore = vectorStore;
        this.hrResource = hrResource;
    }

    @GetMapping("/rag")
    public String ask(@RequestParam String message) {

        SearchRequest searchRequest = SearchRequest.builder().query(message).topK(3).similarityThreshold(0.75).build();
        List<Document> documents = vectorStore.similaritySearch(searchRequest);

        return chatClient.prompt().system(sys -> sys.text(resource).param("context", documents)).user(message).call().content();
    }

    @GetMapping
    public String askHR(@RequestParam String message) {

        return chatClient.prompt().system(hrResource).user(message).call().content();
    }

    @GetMapping("/web")
    public String webSearch(@RequestParam String query) {
        return chatClient.prompt().user(query).call().content();
    }
}
