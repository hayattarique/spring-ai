package com.ai.basicragday8.config;

import com.ai.basicragday8.loader.WebSearchDocumentRetriever;
import org.springframework.ai.chat.cache.semantic.SemanticCacheAdvisor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.client.advisor.api.BaseAdvisor;
import org.springframework.ai.rag.advisor.RetrievalAugmentationAdvisor;
import org.springframework.ai.rag.preretrieval.query.transformation.TranslationQueryTransformer;
import org.springframework.ai.rag.retrieval.search.VectorStoreDocumentRetriever;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class AIConfig {


    @Bean
    public ChatClient chatClient(ChatClient.Builder chatClientBuilder, BaseAdvisor advisor, SemanticCacheAdvisor cacheAdvisor) {
        return chatClientBuilder.defaultAdvisors(advisor, cacheAdvisor,new SimpleLoggerAdvisor()).build();
    }

    @Bean
    BaseAdvisor advisor(VectorStore store, WebSearchDocumentRetriever retriever, ChatClient.Builder chatBuilder) {

//        ContextualQueryAugmenter.builder().promptTemplate(new SystemPromptTemplate())
        return RetrievalAugmentationAdvisor

                .builder()
                .queryTransformers(TranslationQueryTransformer.builder()
                        .chatClientBuilder(chatBuilder).targetLanguage("English").build())
                .documentRetriever(retriever)
                .build();
    }


}
