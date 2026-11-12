# Spring AI Chat Memory: Making the AI Remember the Conversation 🧠

A beginner-friendly guide to **chat memory** in Spring AI, written in simple English, with a working project that saves the conversation in **PostgreSQL** and streams the answer.

**What you will learn:**
- Why AI models **forget** everything between two requests
- `ChatMemory`, `ChatMemoryRepository` and `MessageWindowChatMemory`: who does what
- How `MessageChatMemoryAdvisor` adds old messages to every request
- What a **conversation ID** is, and how this project reads it from an HTTP header with a **custom advisor**
- Why **advisor order** matters
- How the JDBC repository stores messages in PostgreSQL
- Common mistakes, errors, and **interview questions with answers**

**Before this:** read [day-1](../day-1/) (ChatClient), [day-3](../day-3-with-docker/) (streaming with `Flux`) and [spring-ai-prompt-template](../spring-ai-prompt-template/) (advisors).

**Tech stack:** Spring Boot 4.1.2 · Spring AI 2.0.1 · Java 21 · OpenAI (`gpt-5-mini`) · PostgreSQL

### What is in this project and what is not

| Topic | Status |
|---|---|
| `MessageChatMemoryAdvisor` with the auto-configured `ChatMemory` | ✅ In the code |
| JDBC chat memory in PostgreSQL (table created automatically) | ✅ In the code |
| Custom `ConversationIdAdvisor` that reads `X-Conversation-Id` | ✅ In the code |
| Streaming answers (`Flux<String>`) with memory | ✅ In the code |
| Passing the conversation ID with `.advisors(a -> a.param(...))` | 📘 Explained here, not in the code |
| `PromptChatMemoryAdvisor`, `VectorStoreChatMemoryAdvisor` | 📘 Explained here, not in the code |
| Docker Compose file for PostgreSQL | ❌ Not in the project: you run PostgreSQL yourself (see [section 9](#9-how-to-run-this-project)) |

---

## Table of Contents

1. [Basic words you must know](#1-basic-words-you-must-know)
2. [Why does the AI forget?](#2-why-does-the-ai-forget)
3. [The building blocks of chat memory](#3-the-building-blocks-of-chat-memory)
4. [The three memory advisors](#4-the-three-memory-advisors)
5. [Conversation ID: whose chat is this?](#5-conversation-id-whose-chat-is-this)
6. [How this project works](#6-how-this-project-works)
7. [Advisor order: why `HIGHEST_PRECEDENCE`?](#7-advisor-order-why-highest_precedence)
8. [The database side](#8-the-database-side)
9. [How to run this project](#9-how-to-run-this-project)
10. [Code check: things to improve](#10-code-check-things-to-improve)
11. [Common mistakes and errors](#11-common-mistakes-and-errors)
12. [Interview questions and answers](#12-interview-questions-and-answers)
13. [One-minute summary](#13-one-minute-summary)
14. [Self-test](#14-self-test)
15. [What to learn next](#15-what-to-learn-next)
16. [References](#references-)

---

## 1. Basic words you must know

| Word | Simple meaning |
|---|---|
| **Stateless** | The model keeps nothing between requests. Every call starts from zero. |
| **Chat memory** | Saving earlier messages and sending them again with each new request, so the AI "remembers". |
| **Conversation ID** | A key that says which chat a message belongs to. One user/chat = one ID. |
| **`ChatMemory`** | Decides **which** old messages to keep and send (for example, the last 20). |
| **`ChatMemoryRepository`** | Decides **where** messages are stored (memory, PostgreSQL, Cassandra, Neo4j...). |
| **`MessageWindowChatMemory`** | The default `ChatMemory`: keeps a sliding window of the last N messages (default **20**). |
| **Memory advisor** | An advisor that loads history before the call and saves the new messages after it. |
| **Advisor order** | A number. Lower number = runs earlier on the way in. |
| **`RequestContextHolder`** | A Spring helper that gives you the current HTTP request from anywhere, using a `ThreadLocal`. |

---

## 2. Why does the AI forget?

Try this with a plain `ChatClient` (day-1 style):

```
Request 1:  "My name is Aman."         → AI: "Nice to meet you, Aman!"
Request 2:  "What is my name?"          → AI: "I don't know your name."   😕
```

The model is **stateless**. Request 2 doesn't contain request 1, so the model never sees it.

**Chat memory fixes this by sending the history again, every time:**

```
Request 2 actually sent to the model:
   USER:      My name is Aman.            ← loaded from memory
   ASSISTANT: Nice to meet you, Aman!     ← loaded from memory
   USER:      What is my name?             ← the new question
                                           → AI: "Your name is Aman." ✅
```

> 💡 **Memory is not free.** Every old message is sent again and you pay tokens for it, on every request. That's why we keep only a **window** of recent messages.

---

## 3. The building blocks of chat memory

```
          ChatClient
              │  (advisor)
              ▼
  MessageChatMemoryAdvisor        "add history before, save new messages after"
              │
              ▼
      ChatMemory                  WHICH messages?  → MessageWindowChatMemory (last 20)
              │
              ▼
  ChatMemoryRepository            WHERE are they stored?
              │
     ┌────────┴──────────┬────────────────────┐
     ▼                   ▼                    ▼
 InMemory (default)   JDBC (this project)   Cassandra / Neo4j / ...
 lost on restart      PostgreSQL, MySQL...  other starters
```

| Piece | Interface / class | Who creates it in this project |
|---|---|---|
| What to keep | `ChatMemory` → `MessageWindowChatMemory` | Spring Boot auto-configuration |
| Where to store | `ChatMemoryRepository` → `JdbcChatMemoryRepository` | The `spring-ai-starter-model-chat-memory-repository-jdbc` starter |
| When to load/save | `MessageChatMemoryAdvisor` | You, in `AIConfig` |

**Auto-configuration rule:** if no `ChatMemoryRepository` bean exists, Spring AI creates an `InMemoryChatMemoryRepository`. Adding the JDBC starter + a `DataSource` gives you a `JdbcChatMemoryRepository` instead. So **you didn't write any repository code**: the starter in `pom.xml` decided it.

### Changing the window size

The default window is 20 messages. To change it, define your own `ChatMemory` bean:

```java
@Bean
ChatMemory chatMemory(ChatMemoryRepository repository) {
    return MessageWindowChatMemory.builder()
            .chatMemoryRepository(repository)
            .maxMessages(10)
            .build();
}
```

When the window is full, the **oldest** messages are dropped. System messages are kept.

---

## 4. The three memory advisors

| Advisor | How it adds history | Good for |
|---|---|---|
| **`MessageChatMemoryAdvisor`** (this project) | As real `USER` / `ASSISTANT` messages in the prompt | Most chat apps. The model sees a normal conversation. |
| **`PromptChatMemoryAdvisor`** | As plain text pasted into the **system** prompt | Models that don't handle a list of messages well |
| **`VectorStoreChatMemoryAdvisor`** | Searches a vector store for the most **relevant** old messages | Very long histories ("long-term memory") |

---

## 5. Conversation ID: whose chat is this?

Without an ID, every user would share **one** history. User A would see what user B said. 😬

The memory advisor reads the ID from the **advisor context**, under the key `ChatMemory.CONVERSATION_ID` (the string `"chat_memory_conversation_id"`). If it's missing, it fails with `conversationId cannot be null`.

### Way 1: The usual way (pass it per request) 📘

```java
@GetMapping
public Flux<String> ask(@RequestParam String message,
                        @RequestHeader("X-Conversation-Id") String conversationId) {
    return chatClient.prompt()
            .user(message)
            .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, conversationId))
            .stream()
            .content();
}
```

### Way 2: A custom advisor reads it for you (this project ✅)

The controller stays clean, and **every** call through this `ChatClient` gets the ID automatically from the `X-Conversation-Id` header. See the next section.

| | Way 1: `.advisors(a -> a.param(...))` | Way 2: `ConversationIdAdvisor` |
|---|---|---|
| Where the ID is set | In each controller method | Once, in an advisor |
| Missing header | Spring returns **400 Bad Request** automatically | Throws `IllegalStateException` → **500** (see [section 10](#10-code-check-things-to-improve)) |
| Works outside an HTTP request (scheduler, test) | ✅ Yes | ❌ No request → no header → error |
| Easy to forget | Yes, in every method | No |

---

## 6. How this project works

```
GET /api/v1/chat?message=...        header: X-Conversation-Id: user-1
          │
          ▼
ChatController            chatClient.prompt().user(message).stream().content()
          │
          ▼
ConversationIdAdvisor     order = HIGHEST_PRECEDENCE (runs first)
          │               reads the header → puts it in the context as CONVERSATION_ID
          ▼
MessageChatMemoryAdvisor  order = HIGHEST_PRECEDENCE + 200
          │   BEFORE: loads the last 20 messages of "user-1" from PostgreSQL,
          │           adds them to the prompt, saves the new USER message
          ▼
OpenAI (gpt-5-mini)       streams the answer piece by piece
          │
          ▼
MessageChatMemoryAdvisor  AFTER: when the stream is finished, joins the pieces
          │                     and saves the full ASSISTANT message
          ▼
Flux<String>              the browser / curl gets the words as they arrive
```

### Files

| File | What it does |
|---|---|
| [`AIConfig.java`](src/main/java/com/springai/chat_memory_day_7/config/AIConfig.java) | Builds the `ChatClient` with the memory advisor and the conversation-ID advisor |
| [`ConversationIdAdvisor.java`](src/main/java/com/springai/chat_memory_day_7/advisors/ConversationIdAdvisor.java) | Custom advisor: copies the `X-Conversation-Id` header into the advisor context |
| [`ChatController.java`](src/main/java/com/springai/chat_memory_day_7/controller/ChatController.java) | The streaming `GET /api/v1/chat` endpoint |
| [`application.properties`](src/main/resources/application.properties) | OpenAI key and model, PostgreSQL connection, schema setting |

### The config

```java
@Bean
ChatClient chatClient(ChatClient.Builder chatClientBuilder, ChatMemory chatMemory, ConversationIdAdvisor advisor) {
    return chatClientBuilder
            .defaultAdvisors(MessageChatMemoryAdvisor.builder(chatMemory).build(), advisor)
            .build();
}
```

- `ChatMemory chatMemory`: injected by Spring Boot (a `MessageWindowChatMemory` backed by JDBC).
- `ConversationIdAdvisor advisor`: a `@Component`, so Spring creates it and injects it here.
- The **list order doesn't matter**. Advisors are sorted by `getOrder()`, see [section 7](#7-advisor-order-why-highest_precedence).

### The custom advisor (the interesting part ⭐)

```java
@Component
public class ConversationIdAdvisor implements CallAdvisor, StreamAdvisor {
    private static final String HEADER = "X-Conversation-Id";

    @Override
    public ChatClientResponse adviseCall(ChatClientRequest request, CallAdvisorChain chain) {
        // respect an explicit id if the caller already set one
        if (!request.context().containsKey(ChatMemory.CONVERSATION_ID)) {
            String id = getCurrentConversationId();
            if (id == null || id.isEmpty()) {
                throw new IllegalStateException("Missing " + HEADER + " header");
            }
            return chain.nextCall(request.mutate().context(ChatMemory.CONVERSATION_ID, id).build());
        }
        return chain.nextCall(request);
    }

    private String getCurrentConversationId() {
        if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attributes) {
            return attributes.getRequest().getHeader(HEADER);
        }
        return null;
    }

    @Override public int getOrder() { return Ordered.HIGHEST_PRECEDENCE; }
    // adviseStream(...) does the same thing for streaming
}
```

| Line | Meaning |
|---|---|
| `implements CallAdvisor, StreamAdvisor` | Works for both `.call()` and `.stream()`. The controller uses `.stream()`, so `adviseStream` is the one that runs. |
| `context().containsKey(...)` | If someone already passed the ID with `.advisors(a -> a.param(...))`, keep it. Explicit wins over the header. |
| `request.mutate().context(key, id).build()` | Requests are **immutable**. `mutate()` makes a copy with the new context value. |
| `RequestContextHolder` | Gets the current HTTP request without passing it as a parameter. It uses a `ThreadLocal`. |
| `chain.nextCall(...)` / `nextStream(...)` | Passes the request to the next advisor. |

### The controller

```java
@GetMapping
public Flux<String> ask(@RequestParam String message) {
    return chatClient.prompt().user(message).stream().content();
}
```

No ID here at all. The advisor takes care of it.

### Memory + streaming: when is the answer saved?

With `.stream()`, the answer comes in many small pieces. `MessageChatMemoryAdvisor` **joins** the pieces and saves the full `ASSISTANT` message **after the stream finishes**. If the client disconnects in the middle, the answer may not be saved.

---

## 7. Advisor order: why `HIGHEST_PRECEDENCE`?

Advisors run like **nested layers**. Lower order = outer layer = runs **first** on the way in and **last** on the way out.

```
            ┌───────────────────────────────────────────────┐
request ──▶ │ ConversationIdAdvisor   order: HIGHEST_PRECEDENCE  │  sets the ID
            │  ┌──────────────────────────────────────────┐ │
            │  │ MessageChatMemoryAdvisor  order: +200      │ │  needs the ID!
            │  │        ┌───────────────┐                  │ │
            │  │        │  OpenAI call  │                  │ │
            │  │        └───────────────┘                  │ │
            │  └──────────────────────────────────────────┘ │
            └───────────────────────────────────────────────┘
```

| Advisor | Order value |
|---|---|
| `ConversationIdAdvisor` | `Ordered.HIGHEST_PRECEDENCE` (`Integer.MIN_VALUE`) |
| `MessageChatMemoryAdvisor` (default) | `Advisor.DEFAULT_CHAT_MEMORY_PRECEDENCE_ORDER` = `HIGHEST_PRECEDENCE + 200` |

The memory advisor **reads** the conversation ID, so the advisor that **sets** it must run first. If you gave `ConversationIdAdvisor` a higher order (like `0`), the memory advisor would run first and fail with `conversationId cannot be null`.

### Why `RequestContextHolder` works here (and when it wouldn't)

`RequestContextHolder` uses a `ThreadLocal`: it only works on the **HTTP request thread**. Here it works because `ConversationIdAdvisor` is the **first** advisor, so it runs when Spring MVC starts (subscribes to) the `Flux`, which happens on the request thread. Some advisors move work to other threads (Reactor schedulers). If an advisor like that ran **before** this one, the header would no longer be reachable. One more reason to keep it at `HIGHEST_PRECEDENCE`.

---

## 8. The database side

### The properties

```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/ai_service
spring.datasource.username=postgres
spring.datasource.password=root

spring.ai.chat.memory.repository.jdbc.initialize-schema=always
```

| `initialize-schema` value | Meaning |
|---|---|
| `embedded` (default) | Create the table only for embedded databases (H2, HSQL). **PostgreSQL is not embedded**, so nothing would be created. |
| `always` (this project) | Run the schema script on every start. Safe, because it uses `CREATE TABLE IF NOT EXISTS`. |
| `never` | Never create it. Use this with Flyway/Liquibase in real projects. |

### The table Spring AI creates (PostgreSQL)

```sql
CREATE TABLE IF NOT EXISTS SPRING_AI_CHAT_MEMORY (
    conversation_id VARCHAR(36) NOT NULL,
    content TEXT NOT NULL,
    type VARCHAR(10) NOT NULL CHECK (type IN ('USER', 'ASSISTANT', 'SYSTEM', 'TOOL')),
    "timestamp" TIMESTAMP NOT NULL,
    sequence_id BIGINT NOT NULL
);
```

> ⚠️ **`conversation_id` is `VARCHAR(36)`**: exactly the length of a UUID. A conversation ID longer than 36 characters fails to save. Use a UUID (e.g. `uuidgen`) or a short ID.

### Look at the saved messages

```bash
psql -h localhost -U postgres -d ai_service -c "SELECT conversation_id, type, left(content, 60) FROM spring_ai_chat_memory ORDER BY conversation_id, sequence_id;"
```

---

## 9. How to run this project

**You need:** Java 21, Maven, an OpenAI API key, and PostgreSQL running on `localhost:5432`.

1. Start PostgreSQL. The project has no `compose.yml`, so the easiest way is one Docker command (user `postgres`, password `root`, database `ai_service`, to match `application.properties`):

   ```bash
   docker run -d --name chat-memory-db -p 5432:5432 -e POSTGRES_PASSWORD=root -e POSTGRES_DB=ai_service postgres:17
   ```

2. Set your API key. ⚠️ This project reads **`OPEN_AI_KEY`**:

   ```bash
   export OPEN_AI_KEY=your-key-here
   ```

3. Start the app:

   ```bash
   cd chat-memory-day-7
   mvn spring-boot:run
   ```

4. Tell the AI your name (conversation `user-1`):

   ```bash
   curl -N --get "http://localhost:8080/api/v1/chat" -H "X-Conversation-Id: user-1" --data-urlencode "message=My name is Aman"
   ```

5. Ask again in the **same** conversation. It remembers:

   ```bash
   curl -N --get "http://localhost:8080/api/v1/chat" -H "X-Conversation-Id: user-1" --data-urlencode "message=What is my name?"
   ```

6. Ask in a **different** conversation. It doesn't know:

   ```bash
   curl -N --get "http://localhost:8080/api/v1/chat" -H "X-Conversation-Id: user-2" --data-urlencode "message=What is my name?"
   ```

7. Restart the app and repeat step 5. It **still** remembers, because the history is in PostgreSQL, not in RAM.

`-N` turns off curl's buffering so you see the streamed words as they arrive.

---

## 10. Code check: things to improve

The project works. These are small things worth knowing and fixing later:

| # | What | Why it matters | Suggested fix |
|---|---|---|---|
| 1 | Missing header throws `IllegalStateException` | The client gets a **500 Internal Server Error**, but it's really the client's mistake (should be **400**) | Use `@RequestHeader("X-Conversation-Id")` in the controller (Spring returns 400), or add an `@ExceptionHandler` that maps it to 400 |
| 2 | In `adviseStream`, the exception is thrown **directly**, not inside the `Flux` | Reactive code usually reports errors as `Flux.error(...)`, so error handlers and logs see them the normal way | `return Flux.error(new IllegalStateException(...))` |
| 3 | No check on the ID length | IDs over 36 characters fail when saving (the column is `VARCHAR(36)`) | Validate the header, or use UUIDs |
| 4 | Anyone can send any ID | A user who guesses `user-1` can read user-1's chat history through the AI | In a real app, derive the ID from the logged-in user (Spring Security), not from a free header |
| 5 | Password `root` is in `application.properties` | Fine for local practice, not for real servers | `spring.datasource.password=${DB_PASSWORD}` |
| 6 | No system prompt | The AI has no role or limits | Add `.defaultSystem(...)` in `AIConfig` (see [spring-ai-prompt-template](../spring-ai-prompt-template/)) |

---

## 11. Common mistakes and errors

| Problem | Why it happens | Fix |
|---|---|---|
| `IllegalStateException: Missing X-Conversation-Id header` (HTTP 500) | The request has no `X-Conversation-Id` header | Add `-H "X-Conversation-Id: user-1"` to curl |
| `conversationId cannot be null` | The memory advisor ran without an ID: the ID advisor is missing or runs **after** it | Keep `ConversationIdAdvisor` at `HIGHEST_PRECEDENCE`, or pass the ID with `.advisors(a -> a.param(...))` |
| `Connection to localhost:5432 refused` at startup | PostgreSQL isn't running | Start it (section 9, step 1) |
| `database "ai_service" does not exist` | The DB wasn't created | Create it, or use `-e POSTGRES_DB=ai_service` with Docker |
| `relation "spring_ai_chat_memory" does not exist` | `initialize-schema` is `embedded` (default) and PostgreSQL isn't embedded | Set `initialize-schema=always` (already done here) |
| `value too long for type character varying(36)` | Conversation ID longer than 36 characters | Use a UUID or a shorter ID |
| AI still forgets | Different ID each time, or the memory advisor isn't registered | Use the same ID; check `defaultAdvisors(...)` |
| Users see each other's history | Everyone uses the same ID | One ID per user/chat |
| Token cost grows over time | History is sent again on every request | Lower `maxMessages`, or summarize old messages |
| Answer appears all at once in curl | curl buffers the output | Use `curl -N` |
| App fails with an API key error | `OPEN_AI_KEY` isn't set in this terminal | `export OPEN_AI_KEY=...` |

---

## 12. Interview questions and answers

**Q1. Why does an LLM forget previous messages?**
LLMs are stateless. Each API call is independent. To "remember", the app must send the earlier messages again with each new request.

**Q2. `ChatMemory` vs `ChatMemoryRepository`?**
`ChatMemory` decides **which** messages to keep (e.g. the last 20). `ChatMemoryRepository` decides **where** they are stored (memory, JDBC, Cassandra...).

**Q3. What is `MessageWindowChatMemory`?**
The default `ChatMemory`. It keeps a sliding window of the last N messages (20 by default) and drops the oldest ones, keeping system messages.

**Q4. What is the default repository if you add nothing?**
`InMemoryChatMemoryRepository`. History is lost when the app restarts.

**Q5. `MessageChatMemoryAdvisor` vs `PromptChatMemoryAdvisor`?**
`MessageChatMemoryAdvisor` adds history as real user/assistant messages. `PromptChatMemoryAdvisor` adds it as plain text inside the system prompt.

**Q6. What is `VectorStoreChatMemoryAdvisor` for?**
Long-term memory: it finds the most **relevant** old messages with similarity search, instead of just the latest ones.

**Q7. What is a conversation ID and how do you pass it?**
A key that separates one chat from another. Normally: `.advisors(a -> a.param(ChatMemory.CONVERSATION_ID, id))`. This project sets it in a custom advisor from the `X-Conversation-Id` header.

**Q8. Why must `ConversationIdAdvisor` have `HIGHEST_PRECEDENCE`?**
Because the memory advisor (order `HIGHEST_PRECEDENCE + 200`) needs the ID. The advisor that sets the ID must run before the one that reads it. Lower order runs first.

**Q9. Why does the advisor use `request.mutate()`?**
`ChatClientRequest` is immutable. `mutate()` returns a builder with a copy, so you can add the context value and build a new request.

**Q10. How does memory work with streaming?**
The memory advisor saves the user message before the call, then collects the streamed pieces and saves the full assistant message when the stream completes.

**Q11. What does `spring.ai.chat.memory.repository.jdbc.initialize-schema=always` do?**
It runs the schema script on every startup, creating `SPRING_AI_CHAT_MEMORY` if it doesn't exist. The default (`embedded`) only does this for embedded databases like H2.

**Q12. What is a security risk of taking the conversation ID from a header?**
Anyone can send someone else's ID and read their history. In production, tie the ID to the authenticated user.

**Q13. What is the cost impact of chat memory?**
All remembered messages are sent on every request, so input tokens grow with the window size. Keep the window small or summarize old turns.

---

## 13. One-minute summary

- LLMs are **stateless**. Memory = **sending the history again** each time.
- **`ChatMemory`** = which messages (default: **`MessageWindowChatMemory`, last 20**). **`ChatMemoryRepository`** = where (default **in-memory**, here **JDBC/PostgreSQL**).
- **`MessageChatMemoryAdvisor`** loads history before the call and saves messages after it.
- Every chat needs a **conversation ID** under `ChatMemory.CONVERSATION_ID`.
- This project's **`ConversationIdAdvisor`** reads it from the **`X-Conversation-Id`** header, at **`HIGHEST_PRECEDENCE`** so it runs before the memory advisor.
- **`initialize-schema=always`** creates the `SPRING_AI_CHAT_MEMORY` table in PostgreSQL. `conversation_id` is **max 36 characters**.
- With streaming, the full answer is saved **after the stream ends**.
- Memory costs tokens on **every** request.

---

## 14. Self-test

<details>
<summary>1. You remove the JDBC starter from <code>pom.xml</code>. Does memory still work? What changes?</summary>

Yes, it still works, but Spring AI falls back to `InMemoryChatMemoryRepository`. The history is lost when the app restarts.
</details>

<details>
<summary>2. You change <code>ConversationIdAdvisor.getOrder()</code> to return <code>0</code>. What happens?</summary>

The memory advisor (`HIGHEST_PRECEDENCE + 200`) now runs first, finds no conversation ID, and fails with `conversationId cannot be null`.
</details>

<details>
<summary>3. A user sends <code>X-Conversation-Id: my-very-long-conversation-identifier-123</code>. What happens?</summary>

The ID is longer than 36 characters, so saving to `SPRING_AI_CHAT_MEMORY.conversation_id VARCHAR(36)` fails.
</details>

<details>
<summary>4. Why does the advisor check <code>context().containsKey(ChatMemory.CONVERSATION_ID)</code> first?</summary>

So an ID passed explicitly with `.advisors(a -> a.param(...))` is respected and not overwritten by the header.
</details>

<details>
<summary>5. With <code>initialize-schema</code> left at its default, would this project create the table in PostgreSQL?</summary>

No. The default is `embedded`, which only applies to embedded databases like H2.
</details>

---

## 15. What to learn next

- **RAG:** give the AI your own documents, see [basic-rag-day-8](../basic-rag-day-8/)
- **Memory + RAG together:** a chatbot that remembers the conversation and answers from your documents
- **Spring Security:** take the conversation ID from the logged-in user

---

## References 📚

- [Spring AI: Chat Memory](https://docs.spring.io/spring-ai/reference/api/chat-memory.html)
- [Spring AI: Advisors](https://docs.spring.io/spring-ai/reference/api/advisors.html)
- [Spring AI: ChatClient](https://docs.spring.io/spring-ai/reference/api/chatclient.html)
