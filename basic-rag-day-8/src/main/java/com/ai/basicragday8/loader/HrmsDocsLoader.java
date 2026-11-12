package com.ai.basicragday8.loader;

import jakarta.annotation.PostConstruct;
import org.springframework.ai.document.Document;
import org.springframework.ai.reader.tika.TikaDocumentReader;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class HrmsDocsLoader {

    private final Resource resource;
    private final VectorStore vectorStore;

    public HrmsDocsLoader(@Value("classpath:/Esparks_HR.pdf") Resource resource, VectorStore vectorStore) {
        this.resource = resource;
        this.vectorStore = vectorStore;
    }

    @PostConstruct
    public void load() {
        TikaDocumentReader reader = new TikaDocumentReader(resource);

        List<Document> documentList = reader.read();
        List<Document> split = TokenTextSplitter.builder().withMinChunkSizeChars(200).withMaxNumChunks(500).build().split(documentList);

        vectorStore.add(split);
    }
}
