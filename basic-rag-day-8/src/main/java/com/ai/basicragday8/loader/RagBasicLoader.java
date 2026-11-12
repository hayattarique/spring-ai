package com.ai.basicragday8.loader;

import jakarta.annotation.PostConstruct;
import org.springframework.ai.document.Document;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;
import org.springframework.util.StreamUtils;

import java.nio.charset.StandardCharsets;
import java.util.List;

@Component

public class RagBasicLoader {
    private final VectorStore vectorStore;
    private final Resource resource;

    public RagBasicLoader(VectorStore vectorStore, @Value("classpath:/prompts/system-context.st") Resource resource) {
        this.vectorStore = vectorStore;
        this.resource = resource;
    }

    @PostConstruct
    public void loadContext() {

        String context = loadContext(resource);

        Document document = new Document(context);

        TokenTextSplitter splitter = TokenTextSplitter.builder()
                .withChunkSize(800)
                .withMinChunkSizeChars(350)
                .withMinChunkLengthToEmbed(5)
                .withMaxNumChunks(10000)
                .withKeepSeparator(true)
                .build();

        List<Document> chunks = splitter.split(document);

        vectorStore.add(chunks);
    }

    public static String loadContext(Resource resource) {
        String prompt;

        try {
            prompt = StreamUtils.copyToString(resource.getInputStream(), StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        return prompt;
    }
}
