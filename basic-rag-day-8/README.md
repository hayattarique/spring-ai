# Spring AI RAG: Chat With Your Own Documents (Qdrant + Web Search + Semantic Cache) 📚

A beginner-friendly guide to **Retrieval-Augmented Generation (RAG)** in Spring AI, written in simple English.
The project loads a **product knowledge base** and an **HR policy PDF** into the **Qdrant** vector database, searches the **web with Tavily**, and caches answers in **Redis** with a **semantic cache**.

**What you will learn:**
- What RAG is, and its two phases: **ingestion** (load documents) and **retrieval + generation** (answer questions)
- **Embeddings**, **vector stores** and **similarity search** (`topK`, `similarityThreshold`)
- The **ETL pipeline**: `TikaDocumentReader` → `TokenTextSplitter` → `VectorStore.add()`
- **Manual RAG** (search yourself, put the results in the prompt) vs **`RetrievalAugmentationAdvisor`** (modular RAG)
- Query **transformers**, a **custom `DocumentRetriever`** (web search) and the **query augmenter**
- **Semantic caching** with Redis: skip the AI call when a similar question was already answered
- Masking personal data (PII) with regex
- A **code check** of this project: what works, what surprises beginners, and what to fix
- Common mistakes, errors, and **interview questions with answers**

**Before this:** read [spring-ai-prompt-template](../spring-ai-prompt-template/) (prompt stuffing, `.st` files, advisors) and [chat-memory-day-7](../chat-memory-day-7/) (advisor order).

**Tech stack:** Spring Boot 4.1.2 · Spring AI 2.0.1 · Java 17 (set in `pom.xml`) · OpenAI (chat + embeddings) · Qdrant · Redis · Tavily · Apache Tika

### What is in this project and what is not

| Topic | Status |
|---|---|
| Loading a text knowledge base and a PDF into Qdrant at startup | ✅ In the code |
| Manual RAG: `vectorStore.similaritySearch()` + `{context}` in a system prompt | ✅ In the code (`/rag`) |
| `RetrievalAugmentationAdvisor` with a translation step | ✅ In the code (runs on **every** endpoint) |
| Custom `DocumentRetriever` that searches the web with Tavily | ✅ In the code |
| Semantic cache in Redis (`SemanticCacheAdvisor`) | ✅ In the code |
| `PIIMaskingUtil` (mask emails and phone numbers) | ⚠️ Written, but **not used** anywhere yet |
| `VectorStoreDocumentRetriever` (advisor searches Qdrant) | 📘 Imported but not used. Explained in [section 9](#9-the-code-check-what-really-happens-) |
| `AIService` | ⚠️ Empty class, not used |

---

## Table of Contents

1. [Basic words you must know](#1-basic-words-you-must-know)
2. [What is RAG?](#2-what-is-rag)
3. [Embeddings and vector stores in simple words](#3-embeddings-and-vector-stores-in-simple-words)
4. [Phase 1: Ingestion (the ETL pipeline)](#4-phase-1-ingestion-the-etl-pipeline)
5. [Phase 2a: Manual RAG (the `/rag` endpoint)](#5-phase-2a-manual-rag-the-rag-endpoint)
6. [Phase 2b: Modular RAG with `RetrievalAugmentationAdvisor`](#6-phase-2b-modular-rag-with-retrievalaugmentationadvisor)
7. [Web search as a retriever (Tavily)](#7-web-search-as-a-retriever-tavily)
8. [Semantic cache with Redis](#8-semantic-cache-with-redis)
9. [The code check: what really happens ⚠️](#9-the-code-check-what-really-happens-)
10. [PII masking](#10-pii-masking)
11. [Project files](#11-project-files)
12. [How to run this project](#12-how-to-run-this-project)
13. [Common mistakes and errors](#13-common-mistakes-and-errors)
14. [Interview questions and answers](#14-interview-questions-and-answers)
15. [One-minute summary](#15-one-minute-summary)
16. [Self-test](#16-self-test)
17. [What to learn next](#17-what-to-learn-next)
18. [References](#references-)

---

## 1. Basic words you must know

| Word | Simple meaning |
|---|---|
| **RAG** | Retrieval-Augmented Generation: **search** your data first, then give the matching parts to the AI so it answers from them. |
| **Document** | Spring AI's class for a piece of text + metadata (title, url, page...) + an optional score. |
| **Chunk** | A small piece of a big document. We split documents so we can send only the useful parts. |
| **Embedding** | A list of numbers (a vector) that represents the **meaning** of a text. Similar meaning → similar numbers. |
| **Embedding model** | The model that turns text into an embedding. Here: OpenAI `text-embedding-ada-002` (the default), 1536 numbers per text. |
| **Vector store** | A database that stores embeddings and finds the closest ones fast. Here: **Qdrant**. |
| **Similarity search** | "Find the chunks whose meaning is closest to this question." |
| **`topK`** | How many results to return at most (e.g. the best 3). |
| **`similarityThreshold`** | Minimum similarity score (0 to 1). Results below it are dropped. |
| **ETL** | Extract (read files) → Transform (split into chunks) → Load (save in the vector store). |
| **Retriever** | Code that finds documents for a query. Can search a vector store, the web, a database... |
| **Augmentation** | Adding the found documents to the prompt. |
| **Semantic cache** | A cache that matches questions by **meaning**, not exact text. |
| **PII** | Personally Identifiable Information: email, phone, Aadhaar, PAN... |

---

## 2. What is RAG?

The model only knows its training data. It does not know **TechNova's** refund policy or **your company's** leave policy.

In [spring-ai-prompt-template](../spring-ai-prompt-template/) we used **prompt stuffing**: put the whole policy in the prompt. That breaks when the data is big. **RAG = search first, then stuff only what matches.**

```
           PHASE 1: INGESTION (once, at startup)
 ┌──────────┐   ┌──────────┐   ┌───────────┐   ┌─────────────────┐
 │ PDF/text │──▶│  Reader  │──▶│ Splitter  │──▶│  Vector store   │
 │  files   │   │ (Tika)   │   │ (chunks)  │   │ (Qdrant)        │
 └──────────┘   └──────────┘   └───────────┘   │ text + embedding│
                                               └────────▲────────┘
           PHASE 2: RETRIEVAL + GENERATION (every question)│
 ┌──────────┐   ┌────────────────────┐                  │
 │ Question │──▶│ similarity search  │──────────────────┘
 └──────────┘   └─────────┬──────────┘
                          │ top matching chunks
                          ▼
               ┌─────────────────────────┐      ┌──────────┐
               │ Prompt = rules + chunks │─────▶│   LLM    │──▶ answer
               │          + question     │      └──────────┘
               └─────────────────────────┘
```

| | Prompt stuffing | RAG |
|---|---|---|
| What is sent | All the data | Only the matching chunks |
| Extra setup | None | Embedding model + vector store + ingestion |
| Data size | A few pages | Thousands of pages |
| Cost per question | Grows with the data | Stays small |

---

## 3. Embeddings and vector stores in simple words

Think of a **map of meanings**. Every text becomes a point on the map. Texts with similar meaning are **close together**:

```
   "refund takes 5–7 days" ●   ● "How long for my money back?"
                                          (close → similar meaning)

                         ● "NovaPhone X has 12GB RAM"   (far away)
```

- `vectorStore.add(docs)`: Spring AI calls the **embedding model** for each chunk, then saves *text + vector* in Qdrant. You never call the embedding model yourself.
- `vectorStore.similaritySearch(request)`: Spring AI embeds the **question** and asks Qdrant for the closest vectors.

> 💡 The same embedding model must be used for loading **and** searching. Vectors from different models are not comparable.

### Qdrant in this project

```properties
spring.ai.vectorstore.qdrant.initialize-schema=true      # create the collection if it doesn't exist
spring.ai.vectorstore.qdrant.host=localhost
spring.ai.vectorstore.qdrant.port=6334                    # gRPC port (Spring AI uses gRPC)
spring.ai.vectorstore.qdrant.collection-name=hayat
```

| Qdrant port | Used for |
|---|---|
| `6333` | REST API and the **web dashboard**: http://localhost:6333/dashboard |
| `6334` | **gRPC**: this is what Spring AI talks to |

---

## 4. Phase 1: Ingestion (the ETL pipeline)

Two `@Component` classes load data with `@PostConstruct`, which runs **once when the app starts**.

### Loader 1: TechNova product knowledge base (text)

[`RagBasicLoader.java`](src/main/java/com/ai/basicragday8/loader/RagBasicLoader.java) reads [`prompts/system-context.st`](src/main/resources/prompts/system-context.st): company info, 3 products, shipping, returns, payments, cancellation and loyalty rules.

```java
@PostConstruct
public void loadContext() {
    String context = loadContext(resource);              // E: read the file as a String
    Document document = new Document(context);

    TokenTextSplitter splitter = TokenTextSplitter.builder()   // T: split into chunks
            .withChunkSize(800)
            .withMinChunkSizeChars(350)
            .withMinChunkLengthToEmbed(5)
            .withMaxNumChunks(10000)
            .withKeepSeparator(true)
            .build();
    List<Document> chunks = splitter.split(document);

    vectorStore.add(chunks);                              // L: embed + save in Qdrant
}
```

> 💡 These five values are **exactly the defaults** of `TokenTextSplitter`. `TokenTextSplitter.builder().build()` would do the same. Writing them out is still useful for learning.

### Loader 2: HR policy PDF (Apache Tika)

[`HrmsDocsLoader.java`](src/main/java/com/ai/basicragday8/loader/HrmsDocsLoader.java) reads [`Esparks_HR.pdf`](src/main/resources/Esparks_HR.pdf):

```java
TikaDocumentReader reader = new TikaDocumentReader(resource);   // PDF → text
List<Document> documentList = reader.read();
List<Document> split = TokenTextSplitter.builder()
        .withMinChunkSizeChars(200).withMaxNumChunks(500).build()
        .split(documentList);
vectorStore.add(split);
```

**Tika** reads almost any format: PDF, DOCX, PPTX, HTML, TXT... That's why it's a good first choice. (Spring AI also has `PagePdfDocumentReader` for PDFs page by page.)

### `TokenTextSplitter` settings explained

| Setting | Default | Meaning |
|---|---|---|
| `chunkSize` | 800 | Target chunk size in **tokens** |
| `minChunkSizeChars` | 350 | When cutting, look for a sentence end (`.` `?` `!` newline) after at least this many **characters**, so chunks end at a natural place |
| `minChunkLengthToEmbed` | 5 | Throw away tiny chunks shorter than this |
| `maxNumChunks` | 10000 | Safety limit on how many chunks one document can create |
| `keepSeparator` | true | Keep newlines in the chunk text |

**Chunk size trade-off:** small chunks = precise search but less context in each. Big chunks = more context but less precise and more tokens.

---

## 5. Phase 2a: Manual RAG (the `/rag` endpoint)

You do the search yourself, then put the results in the system prompt.

```java
@GetMapping("/rag")
public String ask(@RequestParam String message) {
    SearchRequest searchRequest = SearchRequest.builder()
            .query(message)
            .topK(3)                       // best 3 chunks at most
            .similarityThreshold(0.75)     // drop weak matches
            .build();
    List<Document> documents = vectorStore.similaritySearch(searchRequest);

    return chatClient.prompt()
            .system(sys -> sys.text(resource).param("context", documents))
            .user(message)
            .call()
            .content();
}
```

The system prompt [`system-default.st`](src/main/resources/prompts/system-default.st) tells the AI to answer **only** from `{context}`, and to reply with a fixed sentence when the answer isn't there:

```
5. If the provided context does not contain sufficient information to answer the question, respond with exactly:
   "Please ask related to shopping orders payment and refund"
...
Context:
{context}
```

| `SearchRequest` default | Value |
|---|---|
| `topK` | 4 |
| `similarityThreshold` | 0.0 (accept everything) |

> ⚠️ `.param("context", documents)` puts a `List<Document>` into the template, so the prompt gets each document's `toString()`: `Document{id='...', text='...', media='...', metadata={...}, score=...}`. It works, but sends IDs and metadata the AI doesn't need. Better:
> ```java
> String context = documents.stream().map(Document::getText).collect(Collectors.joining("\n\n"));
> ```

---

## 6. Phase 2b: Modular RAG with `RetrievalAugmentationAdvisor`

Instead of writing search code in every controller method, an **advisor** does RAG for you. Spring AI splits RAG into small, replaceable steps:

```
User question
     │
     ▼
① Pre-retrieval   QueryTransformer(s)   rewrite / translate / compress the question
     │
     ▼
② Retrieval       DocumentRetriever     find documents (vector store, web, DB...)
     │
     ▼
③ (optional)      DocumentJoiner        merge results from several retrievers
     │
     ▼
④ Augmentation    QueryAugmenter        put the documents into the user message
     │
     ▼
    LLM
```

### This project's advisor ([`AIConfig.java`](src/main/java/com/ai/basicragday8/config/AIConfig.java))

```java
@Bean
BaseAdvisor advisor(VectorStore store, WebSearchDocumentRetriever retriever, ChatClient.Builder chatBuilder) {
    return RetrievalAugmentationAdvisor.builder()
            .queryTransformers(TranslationQueryTransformer.builder()
                    .chatClientBuilder(chatBuilder).targetLanguage("English").build())
            .documentRetriever(retriever)      // ← the WEB search retriever, not Qdrant
            .build();
}

@Bean
public ChatClient chatClient(ChatClient.Builder chatClientBuilder, BaseAdvisor advisor, SemanticCacheAdvisor cacheAdvisor) {
    return chatClientBuilder.defaultAdvisors(advisor, cacheAdvisor, new SimpleLoggerAdvisor()).build();
}
```

| Step | Used here | What it does |
|---|---|---|
| ① Transformer | `TranslationQueryTransformer` → English | Asks the LLM to translate the question to English (unchanged if already English). **One extra LLM call per question.** |
| ② Retriever | `WebSearchDocumentRetriever` (custom) | Calls the Tavily web search API. See [section 7](#7-web-search-as-a-retriever-tavily). |
| ④ Augmenter | `ContextualQueryAugmenter` (default, not set explicitly) | Rewrites the user message to include the documents |

### What the default augmenter sends

When documents are found, the user message becomes:

```
Context information is below.
---------------------
{the documents' text}
---------------------
Given the context information and no prior knowledge, answer the query.
Follow these rules:
1. If the answer is not in the context, just say that you don't know.
2. Avoid statements like "Based on the context..." or "The provided information...".
Query: {the question}
Answer:
```

When **no** documents are found (and `allowEmptyContext` is `false`, the default), it becomes:

```
The user query is outside your knowledge base.
Politely inform the user that you can't answer it.
```

Use `ContextualQueryAugmenter.builder().allowEmptyContext(true).build()` if the AI should answer normally when nothing is found.

### Other building blocks you can plug in

| Type | Class | Use |
|---|---|---|
| Transformer | `RewriteQueryTransformer` | Turn a messy question into a clean search query |
| Transformer | `CompressionQueryTransformer` | Turn a follow-up ("and the price?") + chat history into a full question |
| Expander | `MultiQueryExpander` | Create several versions of the question to find more documents |
| Retriever | `VectorStoreDocumentRetriever` | Search a vector store (Qdrant here) |
| Joiner | `ConcatenationDocumentJoiner` | Merge results from many queries/retrievers |

### Manual vs advisor RAG

| | Manual (`/rag`) | `RetrievalAugmentationAdvisor` |
|---|---|---|
| Where the search code lives | In the controller | In one advisor, used by every call |
| Where the context goes | Your own system prompt `{context}` | The **user** message (augmenter template) |
| Query rewriting / translation | You write it | Plug in a transformer |
| Good for | Learning, full control | Real apps |

---

## 7. Web search as a retriever (Tavily)

A `DocumentRetriever` is just an interface: **query in → `List<Document>` out**. Nothing says it must use a vector store. [`WebSearchDocumentRetriever.java`](src/main/java/com/ai/basicragday8/loader/WebSearchDocumentRetriever.java) uses the **Tavily** search API:

```java
@Override
public List<Document> retrieve(Query query) {
    TavilyResponsePayload response = restClient.post()
            .body(new TavilyRequestPayload(query.text(), "advanced", apiConfig.getLimit()))
            .retrieve()
            .body(TavilyResponsePayload.class);

    if (response == null || CollectionUtils.isEmpty(response.results())) {
        return List.of();
    }
    // each search hit → a Spring AI Document
    return response.results().stream()
            .map(hit -> Document.builder()
                    .text(hit.content())
                    .metadata("title", hit.title())
                    .metadata("url", hit.url())
                    .score(hit.score())
                    .build())
            .toList();
}
```

(The real code uses a `for` loop; this is the same logic, shorter.)

**The HTTP call:**

```
POST https://api.tavily.com/search
Authorization: Bearer <tavily key>
{ "query": "...", "search_depth": "advanced", "max_results": 5 }
→ { "results": [ { "title": "...", "url": "...", "content": "...", "score": 0.83 }, ... ] }
```

- `TavilyApiConfig` binds `tavily.api.*` properties with `@ConfigurationProperties` and builds a `RestClient` with the base URL and the `Authorization` header.
- The request/response are Java **records**. `@JsonProperty("search_depth")` and `@JsonProperty("max_results")` map Java names to Tavily's snake_case names.
- `search_depth: "advanced"` gives better results than `"basic"`, but Tavily charges more credits for it.

**Why web search for RAG?** Your vector store only knows what you loaded. Web search gives **fresh, public** information (news, prices, docs). Many real apps use **both**: company data from the vector store, and the web for everything else.

---

## 8. Semantic cache with Redis

LLM calls are slow and cost money. Many users ask the **same thing in different words**:

```
"How long does a refund take?"       ─┐
"When will I get my money back?"      ├─ similar meaning → same cached answer, no LLM call ✅
"Refund time?"                       ─┘
```

A normal cache needs the **exact** same key. A **semantic cache** embeds the question and looks for a **similar** one.

### The config ([`SemanticCacheConfig.java`](src/main/java/com/ai/basicragday8/config/SemanticCacheConfig.java))

```java
@Bean
RedisClient redisClient() {
    return RedisClient.builder().hostAndPort("localhost", 6379).build();
}

@Bean
SemanticCache semanticCache(RedisClient redisClient, EmbeddingModel embeddingModel) {
    return DefaultSemanticCache.builder()
            .prefix("rag:")                  // Redis key prefix (default "semantic-cache:")
            .indexName("hayat")              // Redis search index (default "semantic-cache-index")
            .embeddingModel(embeddingModel)  // same OpenAI embedding model
            .similarityThreshold(0.9)        // how similar a question must be (default 0.8)
            .jedisClient(redisClient)
            .build();
}

@Bean
SemanticCacheAdvisor semanticCacheAdvisor(SemanticCache semanticCache) {
    return SemanticCacheAdvisor.builder().cache(semanticCache).build();
}
```

### How `SemanticCacheAdvisor` decides

1. Take the **user message text** and a **SHA-256 hash of the system prompt**.
2. Search Redis for a cached question with similarity ≥ 0.9 **and** the same system-prompt hash.
3. **Hit** → return the cached `ChatResponse` immediately. **No translation, no web search, no LLM call.**
4. **Miss** → continue the chain, then save *question + response + hash* in Redis.

The system-prompt hash keeps answers separate when the rules are different. A question asked on `/web` (no system prompt) won't reuse an answer from `/` (HR system prompt).

### Where it runs in the chain

| Advisor | Order | Runs |
|---|---|---|
| `SemanticCacheAdvisor` | `HIGHEST_PRECEDENCE + 200` | **1st**: checks the cache before anything expensive |
| `RetrievalAugmentationAdvisor` | `0` | 2nd (on a cache miss) |
| `SimpleLoggerAdvisor` | `0` | 2nd/3rd (same order as RAG) |

The order in `defaultAdvisors(advisor, cacheAdvisor, ...)` doesn't matter; `getOrder()` decides.

### Things to know

| Point | Why it matters |
|---|---|
| **No expiry (TTL) is set** in this project | Cached answers stay forever. For **web search** this means stale answers ("today's weather", "latest version"). Clear Redis or add a TTL. |
| Each request does **one extra embedding call** for the lookup | Cheap compared to a chat call, but not free |
| Threshold too low (e.g. 0.7) | Different questions get the same answer ("refund time" vs "return time") |
| Threshold too high (e.g. 0.99) | Almost never a hit |
| Needs Redis with **vector search** | `redis:latest` (Redis 8+) has it built in. On Redis 7 use the `redis/redis-stack` image. |

---

## 9. The code check: what really happens ⚠️

This is the most important section for revision. The code runs, but some behaviour is **not what it looks like**.

### Finding 1: Web search runs on **every** endpoint

`RetrievalAugmentationAdvisor` is a **default advisor**, so it wraps **every** call through this `ChatClient`, not just `/web`:

| Endpoint | What you might expect | What actually happens |
|---|---|---|
| `GET /api/v1/chat/rag?message=` | Answer from Qdrant | Qdrant chunks in the **system** prompt **+** Tavily web results in the **user** message. The AI sees both. |
| `GET /api/v1/chat?message=` | Answer from the HR PDF | **Web search only** (see finding 2) |
| `GET /api/v1/chat/web?query=` | Web search | ✅ Web search |

Every request also pays for the **translation** LLM call and a **Tavily** search (unless the semantic cache hits).

**Fix:** keep the RAG advisor off the default list and add it only where needed:

```java
// AIConfig: no RAG advisor in the defaults
return chatClientBuilder.defaultAdvisors(cacheAdvisor, new SimpleLoggerAdvisor()).build();

// RagBasicController: only the web endpoint uses it
return chatClient.prompt().user(query).advisors(webRagAdvisor).call().content();
```

### Finding 2: The HR endpoint never searches the HR PDF

```java
@GetMapping
public String askHR(@RequestParam String message) {
    return chatClient.prompt().system(hrResource).user(message).call().content();
}
```

- `hr-system.st` ends with `{context}`, but **no `.param("context", ...)` is passed**. Spring AI only renders a system template **when params are given**, so the AI literally receives the text `{context}`. No error is thrown.
- Nothing calls `vectorStore.similaritySearch(...)` here. The PDF chunks are in Qdrant, but this endpoint never reads them.
- The only context the AI gets is the **web search** from finding 1. So HR answers come from the internet, not from `Esparks_HR.pdf`. 😬

**Fix (option A, manual, like `/rag`):**

```java
List<Document> docs = vectorStore.similaritySearch(
        SearchRequest.builder().query(message).topK(4).similarityThreshold(0.7).build());
String context = docs.stream().map(Document::getText).collect(Collectors.joining("\n\n"));

return chatClient.prompt()
        .system(s -> s.text(hrResource).param("context", context))
        .user(message)
        .call()
        .content();
```

**Fix (option B, advisor, the "modular RAG" way):**

```java
RetrievalAugmentationAdvisor hrRagAdvisor = RetrievalAugmentationAdvisor.builder()
        .documentRetriever(VectorStoreDocumentRetriever.builder()
                .vectorStore(vectorStore)
                .topK(4)
                .similarityThreshold(0.7)
                .build())
        .build();
```

(Then remove `{context}` from `hr-system.st`, because the augmenter puts the context in the user message.)

### Finding 3: Both data sets share one collection

Both loaders write to the same Qdrant collection `hayat`. A shopping question on `/rag` can retrieve **HR** chunks and the other way round.

**Fix:** add metadata when loading, and filter when searching:

```java
// when loading
chunks.forEach(c -> c.getMetadata().put("source", "hr"));

// when searching
SearchRequest.builder().query(message).filterExpression("source == 'hr'").build();
```

### Finding 4: Duplicates on every restart

`@PostConstruct` runs on every start, and every `vectorStore.add(...)` creates new chunks with new random IDs. Restart 5 times → every chunk is in Qdrant 5 times, and search returns the same text several times. It also pays for embeddings again each time.

**Fix (simple):** check first, for example search for something you know is there and skip loading if it's found, or delete the collection in the Qdrant dashboard before restarting. **Real apps** run ingestion as a separate job, not on every start.

### Finding 5: Smaller things

| # | What | Fix |
|---|---|---|
| 5a | 🔐 ✅ **Fixed:** the Tavily API key used to be written directly in `application.properties` | Now it's `tavily.api.api-key=${TAVILY_API_KEY}`, read from an environment variable. If a key is ever pushed or shared, **create a new key** in the Tavily dashboard. Never commit keys. |
| 5b | `system-context.st` is **data** (a knowledge base) kept in the `prompts/` folder | Move it to `src/main/resources/docs/technova-kb.md` so prompts and data aren't mixed |
| 5c | `List<Document>` passed as `{context}` (section 5) | Join `Document::getText` |
| 5d | `PIIMaskingUtil` is not called anywhere, and its Aadhaar/PAN patterns are defined but never applied | See [section 10](#10-pii-masking) |
| 5e | `AIService` is empty, in a package spelled `serrvice` | Delete it, or rename the package to `service` and move the logic out of the controller |
| 5f | `VectorStore store` is injected into the `advisor` bean but not used; `RestClient` is imported but not used in `AIConfig` | Remove, or use it for `VectorStoreDocumentRetriever` |
| 5g | `java.version` is 17 here, but 21 in the other projects | Fine either way, but keep projects the same to avoid surprises |
| 5h | Redis host/port are hardcoded in `SemanticCacheConfig` | Read them from properties |
| 5i | `/web` uses `?query=`, the other endpoints use `?message=` | Use one name for all |

---

## 10. PII masking

[`PIIMaskingUtil.java`](src/main/java/com/ai/basicragday8/util/PIIMaskingUtil.java) hides personal data with regex **before** it goes to the AI or into logs.

| Pattern | Example input | Masked as | Applied? |
|---|---|---|---|
| Email | `rahul.sharma@gmail.com` | `r***********@gmail.com` | ✅ Yes |
| Indian mobile (`+91` optional, starts with 6–9) | `9876543210` | `XXXXXX3210` | ✅ Yes |
| Aadhaar (12 digits) | `123412341234` | — | ❌ Pattern defined but not used |
| PAN (`ABCDE1234F`) | `ABCDE1234F` | — | ❌ Pattern defined but not used |

```java
String safe = PIIMaskingUtil.maskSensitiveData("Call me at 9876543210 or mail rahul@gmail.com");
// "Call me at XXXXXX3210 or mail r****@gmail.com"
```

**Where to use it (not done yet):**
- On the **user message** before it's sent: inside a small custom advisor, or in the controller.
- On **documents** before `vectorStore.add(...)`, so PII never gets into the vector store.
- On **logs**: `SimpleLoggerAdvisor` logs the full prompt at DEBUG.

> 💡 Regex masking is a good first layer, but it's not perfect (names, addresses and unusual formats slip through). It also changes the text the AI sees, so the AI can't, for example, repeat the user's phone number back.

---

## 11. Project files

| File | What it does |
|---|---|
| [`compose.yml`](compose.yml) | Starts **Qdrant** (6333, 6334) and **Redis** (6379) |
| [`config/AIConfig.java`](src/main/java/com/ai/basicragday8/config/AIConfig.java) | `ChatClient` with default advisors + the `RetrievalAugmentationAdvisor` bean |
| [`config/SemanticCacheConfig.java`](src/main/java/com/ai/basicragday8/config/SemanticCacheConfig.java) | Redis client, semantic cache and its advisor |
| [`config/TavilyApiConfig.java`](src/main/java/com/ai/basicragday8/config/TavilyApiConfig.java) | `tavily.api.*` properties + a `RestClient` for Tavily |
| [`controller/RagBasicController.java`](src/main/java/com/ai/basicragday8/controller/RagBasicController.java) | The three endpoints |
| [`loader/RagBasicLoader.java`](src/main/java/com/ai/basicragday8/loader/RagBasicLoader.java) | Loads the TechNova knowledge base into Qdrant at startup |
| [`loader/HrmsDocsLoader.java`](src/main/java/com/ai/basicragday8/loader/HrmsDocsLoader.java) | Loads the HR PDF into Qdrant at startup (Tika) |
| [`loader/WebSearchDocumentRetriever.java`](src/main/java/com/ai/basicragday8/loader/WebSearchDocumentRetriever.java) | Custom `DocumentRetriever` that calls Tavily |
| [`util/PIIMaskingUtil.java`](src/main/java/com/ai/basicragday8/util/PIIMaskingUtil.java) | Regex masking for emails and phone numbers (not wired in yet) |
| [`prompts/system-default.st`](src/main/resources/prompts/system-default.st) | System prompt for `/rag` (answer only from `{context}`) |
| [`prompts/hr-system.st`](src/main/resources/prompts/hr-system.st) | System prompt for the HR endpoint |
| [`prompts/system-context.st`](src/main/resources/prompts/system-context.st) | The TechNova knowledge base (**data**, not a prompt) |
| [`Esparks_HR.pdf`](src/main/resources/Esparks_HR.pdf) | The HR policy document |

### Dependencies (`pom.xml`)

| Artifact | Why |
|---|---|
| `spring-ai-starter-model-openai` | Chat model + **embedding model** |
| `spring-ai-starter-vector-store-qdrant` | `VectorStore` backed by Qdrant |
| `spring-ai-rag` | `RetrievalAugmentationAdvisor`, transformers, retrievers, augmenters |
| `spring-ai-tika-document-reader` | `TikaDocumentReader` for PDF and other files |
| `spring-ai-redis-semantic-cache` | `DefaultSemanticCache`, `SemanticCacheAdvisor` |

### The full request flow (cache miss) 🔄

```
GET /api/v1/chat/web?query=...
  │
  ▼
SemanticCacheAdvisor ── embed question → Redis lookup ── HIT? → return cached answer ✋
  │ miss
  ▼
RetrievalAugmentationAdvisor
  ├─ ① TranslationQueryTransformer → LLM call #1 (translate to English)
  ├─ ② WebSearchDocumentRetriever  → Tavily HTTP call → 5 Documents
  └─ ④ ContextualQueryAugmenter    → user message = context + question
  │
  ▼
SimpleLoggerAdvisor (logs at DEBUG)
  │
  ▼
OpenAI → LLM call #2 (the real answer)
  │
  ▼
SemanticCacheAdvisor saves question + answer in Redis
  │
  ▼
String answer
```

---

## 12. How to run this project

**You need:** Java 17+, Maven, Docker, an OpenAI API key and a Tavily API key (free tier at tavily.com). Calls use your OpenAI and Tavily accounts and may cost money.

1. Start Qdrant and Redis. The project has no Docker Compose starter dependency, so start them yourself:

   ```bash
   cd basic-rag-day-8
   docker compose up -d
   ```

2. Set your OpenAI key. ⚠️ This project reads **`OPEN_AI_KEY`**:

   ```bash
   export OPEN_AI_KEY=your-key-here
   ```

3. Set your Tavily key. `application.properties` reads it with `tavily.api.api-key=${TAVILY_API_KEY}`:

   ```bash
   export TAVILY_API_KEY=your-tavily-key
   ```

4. Start the app. Watch the logs: both loaders embed their documents at startup.

   ```bash
   mvn spring-boot:run
   ```

5. Open the Qdrant dashboard and look at the `hayat` collection and its points: http://localhost:6333/dashboard

6. Ask a shopping question (manual RAG + web):

   ```bash
   curl --get "http://localhost:8080/api/v1/chat/rag" --data-urlencode "message=What is the price of NovaPhone X?"
   ```

7. Ask the **same thing in other words** and check the logs: the semantic cache should answer without calling the LLM.

   ```bash
   curl --get "http://localhost:8080/api/v1/chat/rag" --data-urlencode "message=How much does the NovaPhone X cost?"
   ```

   A hit isn't guaranteed: the threshold is 0.9, and on `/rag` the system prompt includes the retrieved chunks, so the hash only matches if the same chunks come back.

8. Web search:

   ```bash
   curl --get "http://localhost:8080/api/v1/chat/web" --data-urlencode "query=What is new in Spring AI 2.0?"
   ```

9. Ask in Hindi, and see the translation step at work:

   ```bash
   curl --get "http://localhost:8080/api/v1/chat/web" --data-urlencode "query=Spring AI kya hai?"
   ```

10. HR question (remember finding 2: this one actually uses web search):

    ```bash
    curl --get "http://localhost:8080/api/v1/chat" --data-urlencode "message=How many casual leaves do I get?"
    ```

**Reset everything** (removes Qdrant data and the Redis cache):

```bash
docker compose down -v
```

---

## 13. Common mistakes and errors

| Problem | Why it happens | Fix |
|---|---|---|
| App fails at startup with a Qdrant `UNAVAILABLE` / connection error | Qdrant isn't running, or port 6333 is used instead of 6334 | `docker compose up -d`; Spring AI uses the **gRPC** port **6334** |
| `JedisConnectionException` | Redis isn't running | `docker compose up -d` |
| Redis error like `unknown command 'FT.CREATE'` | The Redis image has no search module | Use `redis:latest` (8+) or `redis/redis-stack` |
| API key error at startup | `OPEN_AI_KEY` isn't set | `export OPEN_AI_KEY=...` in the same terminal |
| App fails at startup: `Could not resolve placeholder 'TAVILY_API_KEY'` | The environment variable isn't set in this terminal | `export TAVILY_API_KEY=...` before `mvn spring-boot:run` |
| `401` from Tavily | Wrong Tavily key | Check the value of `TAVILY_API_KEY` |
| Collection dimension error | Collection was created with another embedding model (different vector size) | Delete the collection, or use a new `collection-name` |
| Same chunk returned several times | Data loaded again on each restart (finding 4) | Delete the collection and load once |
| "I can't answer that" for everything | Retriever returned nothing → empty-context template | Check Tavily results / lower the threshold / `allowEmptyContext(true)` |
| `/rag` always says "Please ask related to shopping orders payment and refund" | No chunk passed `similarityThreshold(0.75)` | Lower to ~0.6 and test |
| HR answers don't match the PDF | The HR endpoint never searches Qdrant (finding 2) | Apply the fix in finding 2 |
| Old / wrong answer comes back | Semantic cache hit, no TTL | Clear Redis (`docker compose down -v`) or add a TTL; tune the threshold |
| Every request is slow | Translation LLM call + Tavily call + answer call on every request | Remove the RAG advisor from the defaults (finding 1) |
| Startup is slow and costs money each time | Both loaders embed everything on every start | Load once, outside of startup |

---

## 14. Interview questions and answers

**Q1. What is RAG and why use it?**
Retrieval-Augmented Generation: search for relevant data first, then give it to the LLM to answer from. It lets the AI use private or fresh data without retraining, and lowers hallucinations.

**Q2. What are the two phases of RAG?**
Ingestion (read → split → embed → store) and retrieval + generation (embed the question → similarity search → put results in the prompt → call the LLM).

**Q3. What is an embedding?**
A vector of numbers that represents the meaning of a text. Texts with similar meaning have vectors that are close together.

**Q4. Why split documents into chunks?**
Embeddings work best on focused text, the context window is limited, and you only want to send the relevant parts. Chunks that are too small lose context; chunks that are too large are less precise and cost more.

**Q5. What do `topK` and `similarityThreshold` do?**
`topK` is the maximum number of results. `similarityThreshold` drops results below a minimum score. Defaults: `topK` 4, threshold 0.0 (accept all).

**Q6. What does `vectorStore.add()` do behind the scenes?**
It calls the embedding model for each document, then stores the text, metadata and vector in the vector store.

**Q7. What is `RetrievalAugmentationAdvisor`?**
An advisor that runs a modular RAG flow on each call: query transformers → document retriever → (joiner) → query augmenter, then the LLM call.

**Q8. What does `ContextualQueryAugmenter` do when no documents are found?**
By default (`allowEmptyContext=false`) it replaces the query with an instruction to politely say the question is outside the knowledge base. With `allowEmptyContext(true)` it sends the original question.

**Q9. What is a `DocumentRetriever`, and can it search the web?**
An interface: query in, list of documents out. Yes. This project's `WebSearchDocumentRetriever` calls Tavily and maps each hit into a `Document`.

**Q10. What does `TranslationQueryTransformer` do, and what does it cost?**
It uses the LLM to translate the question into a target language (English here) so it matches English documents. It adds one LLM call per request.

**Q11. What is a semantic cache?**
A cache that finds earlier questions with similar **meaning** using embeddings, and returns their saved answer instead of calling the LLM.

**Q12. Why does `SemanticCacheAdvisor` run before the RAG advisor?**
Its order is `HIGHEST_PRECEDENCE + 200`, lower than the RAG advisor's `0`. Checking the cache first means a hit skips retrieval and the LLM call entirely.

**Q13. Risks of a semantic cache?**
Wrong answers if the threshold is too low, stale answers without a TTL (bad for web data), and users possibly seeing answers generated for someone else's question.

**Q14. Manual RAG vs advisor RAG?**
Manual: you call `similaritySearch` and put the results in the prompt yourself; full control, more code. Advisor: one reusable component, pluggable steps, context added to the user message automatically.

**Q15. What happens if a system template has `{context}` but you pass no params?**
Spring AI only renders the template when params are given, so the AI receives the literal text `{context}`. No error is thrown, which makes this bug easy to miss.

**Q16. Why must the same embedding model be used for ingestion and search?**
Different models produce vectors of different sizes and meanings, so their vectors can't be compared.

**Q17. Why is loading documents in `@PostConstruct` a problem?**
It runs on every startup, creating duplicate chunks and paying for embeddings each time. Real apps run ingestion as a separate, idempotent job.

**Q18. How do you keep two document sets apart in one collection?**
Add metadata (e.g. `source=hr`) when loading and use a `filterExpression` when searching. Or use separate collections.

---

## 15. One-minute summary

- **RAG = search, then stuff only what matches** into the prompt.
- **Ingestion:** `TikaDocumentReader` → `TokenTextSplitter` (default 800 tokens) → `vectorStore.add()` (embeds automatically, OpenAI `text-embedding-ada-002`).
- **Qdrant**: Spring AI uses gRPC port **6334**; dashboard on **6333**.
- **Manual RAG** (`/rag`): `similaritySearch(topK 3, threshold 0.75)` → `{context}` in the system prompt.
- **`RetrievalAugmentationAdvisor`**: transformer → retriever → augmenter. Here: translate to English → **Tavily web search** → context in the user message.
- **Semantic cache** (Redis, threshold 0.9) runs **first** and skips everything on a hit. **No TTL** here.
- ⚠️ The RAG advisor is a **default**, so web search runs on **every** endpoint.
- ⚠️ The HR endpoint **never searches the PDF** and sends a literal `{context}`.
- ⚠️ Loaders add **duplicates on every restart**; both data sets share **one collection**.
- 🔐 The **Tavily key** comes from the `TAVILY_API_KEY` environment variable. Never write keys in `application.properties`.

---

## 16. Self-test

<details>
<summary>1. You call <code>/api/v1/chat/rag</code>. How many LLM calls and external calls happen on a cache miss?</summary>

Embedding calls: one for the Qdrant search, one for the cache lookup. Then one LLM call for translation, one Tavily search, and one LLM call for the answer. Then the answer is saved in Redis (another embedding).
</details>

<details>
<summary>2. Why does the HR endpoint answer from the internet instead of the PDF?</summary>

It never calls the vector store, `{context}` is never filled (no params → template not rendered), and the default `RetrievalAugmentationAdvisor` adds Tavily web results.
</details>

<details>
<summary>3. You restart the app 3 times. How many copies of each chunk are in Qdrant?</summary>

Three (one per startup), because `@PostConstruct` adds them again with new IDs each time.
</details>

<details>
<summary>4. Retrieval finds nothing and <code>allowEmptyContext</code> is the default. What does the AI receive?</summary>

"The user query is outside your knowledge base. Politely inform the user that you can't answer it."
</details>

<details>
<summary>5. Why can the semantic cache return a wrong answer for "What is the return time?" after "What is the refund time?"?</summary>

The two questions are close in meaning. If their similarity is above the threshold (0.9), the cached refund answer is returned without calling the LLM.
</details>

<details>
<summary>6. Which advisor runs first, and why: <code>SemanticCacheAdvisor</code> or <code>RetrievalAugmentationAdvisor</code>?</summary>

`SemanticCacheAdvisor` (order `HIGHEST_PRECEDENCE + 200`) runs before `RetrievalAugmentationAdvisor` (order `0`). Lower order runs first, so a cache hit skips the retrieval.
</details>

---

## 17. What to learn next

- **Fix the code-check findings**: separate advisors per endpoint, real HR RAG from Qdrant, metadata filters
- **Hybrid retrieval**: combine `VectorStoreDocumentRetriever` (company data) and web search with `ConcatenationDocumentJoiner`
- **RAG + chat memory**: add `MessageChatMemoryAdvisor` (see [chat-memory-day-7](../chat-memory-day-7/)) and `CompressionQueryTransformer` for follow-up questions
- **Evaluation**: check answers with `RelevancyEvaluator` / `FactCheckingEvaluator`
- **Tool calling**: let the AI decide **when** to search

---

## References 📚

- [Spring AI: Retrieval Augmented Generation](https://docs.spring.io/spring-ai/reference/api/retrieval-augmented-generation.html)
- [Spring AI: ETL Pipeline (readers, splitters)](https://docs.spring.io/spring-ai/reference/api/etl-pipeline.html)
- [Spring AI: Vector Databases](https://docs.spring.io/spring-ai/reference/api/vectordbs.html)
- [Spring AI: Qdrant](https://docs.spring.io/spring-ai/reference/api/vectordbs/qdrant.html)
- [Spring AI: Embeddings](https://docs.spring.io/spring-ai/reference/api/embeddings.html)
- [Qdrant documentation](https://qdrant.tech/documentation/)
- [Tavily Search API](https://docs.tavily.com/)
- [Redis vector search](https://redis.io/docs/latest/develop/interact/search-and-query/)
