package com.ai.basicragday8.loader;

import com.ai.basicragday8.config.TavilyApiConfig;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.ai.document.Document;
import org.springframework.ai.rag.Query;
import org.springframework.ai.rag.retrieval.search.DocumentRetriever;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class WebSearchDocumentRetriever implements DocumentRetriever {

    private final TavilyApiConfig apiConfig;
    private final RestClient restClient;

    @Override
    public @NonNull List<Document> retrieve(@NonNull Query query) {
        String q = query.text();

        TavilyResponsePayload response = restClient.post()
                .body(new TavilyRequestPayload(q, "advanced", apiConfig.getLimit()))
                .retrieve()
                .body(TavilyResponsePayload.class);

        if (response == null || CollectionUtils.isEmpty(response.results())) {
            return List.of();
        }

        List<Document> docs = new ArrayList<>(response.results().size());
        for (TavilyResponsePayload.Hit hit : response.results()) {
            // Map each Tavily hit into a Spring AI Document with metadata and score.
            Document doc = Document.builder()
                    .text(hit.content())
                    .metadata("title", hit.title())
                    .metadata("url", hit.url())
                    .score(hit.score())
                    .build();
            docs.add(doc);
        }
        return docs;
    }



    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    record TavilyRequestPayload(String query, @JsonProperty("search_depth")
    String searchDepth, @JsonProperty("max_results") int maxResults) {}

    record TavilyResponsePayload(List<Hit> results) {
        record Hit(String title, String url, String content, Double score) {}
    }
}
