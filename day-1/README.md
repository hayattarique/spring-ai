# Spring AI Basics: ChatModel, ChatClient and OpenAI 📘

A beginner-friendly guide to the core building blocks of **Spring AI**, written in simple English,
with a small working Spring Boot project that talks to OpenAI.

**What you will learn:**
- What `ChatModel`, `OpenAiChatModel`, `ChatClient` and `DefaultChatClient` are, and how they connect
- Why `ChatClient` exists, and when (rarely) to use `ChatModel` directly
- How Spring Boot auto-configures everything for you
- How to read the AI's answer and its metadata (`content()` vs `chatResponse()`)
- Common mistakes, errors, and **17 interview questions with answers**

**Who is this for:** Java and Spring Boot developers who are new to Spring AI.
Read from top to bottom. Each part builds on the one before it.

**Tech stack:** Spring Boot 4.1.2 · Spring AI 2.0.1 · Java 21 · OpenAI

---

## Table of Contents

1. [Basic words you must know](#1-basic-words-you-must-know)
2. [The big picture](#2-the-big-picture)
3. [ChatModel](#3-chatmodel)
4. [OpenAiChatModel](#4-openaichatmodel)
5. [Prompt and ChatResponse](#5-prompt-and-chatresponse)
6. [ChatClient](#6-chatclient)
7. [DefaultChatClient](#7-defaultchatclient)
8. [ChatClient.Builder and auto-configuration](#8-chatclientbuilder-and-auto-configuration)
9. [ChatModel vs ChatClient: which one to use?](#9-chatmodel-vs-chatclient-which-one-to-use)
10. [`content()` vs `chatResponse()`](#10-content-vs-chatresponse)
11. [How this project works, step by step](#11-how-this-project-works-step-by-step)
12. [How to run this project](#12-how-to-run-this-project)
13. [Common mistakes and errors](#13-common-mistakes-and-errors)
14. [Interview questions and answers](#14-interview-questions-and-answers)
15. [One-minute summary](#15-one-minute-summary)
16. [Self-test](#16-self-test)
17. [What to learn next](#17-what-to-learn-next)

---

## 1. Basic words you must know

| Word | Simple meaning |
|---|---|
| **AI model / LLM** | The "brain" that reads your text and writes an answer. Example: GPT-4o, Claude. LLM = Large Language Model. |
| **Vendor / Provider** | The company that runs the AI model. Example: OpenAI, Anthropic, Google, Ollama (runs on your own machine). |
| **Prompt** | The message (question or instruction) you send to the AI. |
| **Response** | The answer the AI sends back. |
| **Token** | A small piece of text (around ¾ of a word). AI vendors charge money **per token**, for both the input and the output. |
| **API key** | A secret password that lets your app use the vendor's AI. **Never** put it directly in code or push it to GitHub. |
| **Spring AI** | A Spring project that makes it easy to use AI models inside a Spring Boot app. |

---

## 2. The big picture

Every AI vendor has its **own API**: different URLs, different JSON formats, different rules.
If you wrote code directly for OpenAI, switching to Anthropic later would mean **rewriting everything**.

**Spring AI solves this problem.** It gives you **one common set of interfaces**, and each vendor plugs in behind them.

```
   Your Controller / Service
             │
             ▼
   ┌──────────────────────┐
   │      ChatClient      │   ← Easy, high-level, fluent API (you use this most)
   │  (DefaultChatClient) │
   └──────────────────────┘
             │  uses
             ▼
   ┌──────────────────────┐
   │      ChatModel       │   ← Low-level interface (common for all vendors)
   └──────────────────────┘
             │  implemented by
     ┌───────┼───────────┬──────────────┐
     ▼       ▼           ▼              ▼
  OpenAi   Anthropic   Ollama       ...others
ChatModel  ChatModel   ChatModel
     │
     ▼
 OpenAI servers (GPT models) over the internet
```

**Simple example from real life 🚗** (we will use this same example in the whole document)
Think of a **car**.
- **ChatModel = the engine slot.** It is a standard shape. Any engine that fits this shape can go into the car.
- **OpenAiChatModel, AnthropicChatModel, ... = different brand engines.** Each company builds its own engine, but all of them fit the same slot.
- **ChatClient = the full car** (steering wheel, pedals, dashboard). The engine is inside. You just drive.

So if you want to change from OpenAI to Anthropic, you **change the engine**, not the whole car. Your driving (your code) stays the same.

---

## 3. ChatModel

### What is it?
`ChatModel` is an **interface** in Spring AI.
It describes the basic job: **"send a prompt to an AI and get a response back."**

It does **not** know how to talk to OpenAI or Anthropic itself. It only defines **what** must be done.
The vendor-specific classes define **how** it is done.

### Main method (simplified)
```java
public interface ChatModel {
    ChatResponse call(Prompt prompt);   // main method
    String call(String message);        // shortcut for quick use
}
```

### Why does it exist?
- So that your code depends on **one common interface**, not on one vendor.
- You can **switch vendors** (OpenAI → Anthropic) by changing the dependency and properties, with little or no code change.

### Using ChatModel directly (low-level style)
```java
ChatResponse response = chatModel.call(new Prompt("Tell me a joke"));
String answer = response.getResult().getOutput().getText();
```
It works, but notice: you create a `Prompt` object yourself, and you dig through
`getResult().getOutput().getText()` to get the text. This feels **verbose (long)**.
That is the reason `ChatClient` was created.

> 💡 **Remember:** ChatModel is not hard to *configure* (configuration is just properties).
> It is more work to *use*, because it is low-level.

---

## 4. OpenAiChatModel

### What is it?
`OpenAiChatModel` is a **class** that **implements** the `ChatModel` interface **for OpenAI**.

It knows how to:
- Turn your `Prompt` into OpenAI's JSON format
- Call OpenAI's servers over HTTP, using your API key
- Turn OpenAI's JSON reply back into a Spring AI `ChatResponse`

### Who creates it?
**Spring Boot creates it automatically** (auto-configuration) when:
1. The dependency `spring-ai-starter-model-openai` is in `pom.xml` ✅ (this project has it)
2. The API key property is set ✅ (this project has it)

```properties
spring.ai.openai.api-key=${OPENAI_API_KEY}
```

`${OPENAI_API_KEY}` means: *"read the value from an environment variable named OPENAI_API_KEY."*
This keeps the secret key **out of the code**. 👍

### Choosing the model and settings (optional)
If you don't set a model, Spring AI uses a default one. You can choose it yourself in `application.properties`:

```properties
spring.ai.openai.chat.options.model=gpt-4o-mini
spring.ai.openai.chat.options.temperature=0.7
```

| Property | Simple meaning |
|---|---|
| `model` | Which GPT model to use. Smaller models are cheaper and faster. Bigger models are smarter. |
| `temperature` | How "creative" the answer is. Low (like `0.2`) = same, safe answers every time. High (like `1.0`) = more creative and random answers. |

### Other implementations (same idea, different vendor)
| Vendor | Class | Starter dependency |
|---|---|---|
| OpenAI | `OpenAiChatModel` | `spring-ai-starter-model-openai` |
| Anthropic (Claude) | `AnthropicChatModel` | `spring-ai-starter-model-anthropic` |
| Ollama (local models) | `OllamaChatModel` | `spring-ai-starter-model-ollama` |

---

## 5. Prompt and ChatResponse

### Prompt 📝 (what you SEND)
A `Prompt` is an object that holds:
- One or more **messages** (for example, a system message and a user message)
- Optional **settings** (called "options"), such as which model to use and temperature

```java
Prompt prompt = new Prompt("What is Spring Boot?");
```

**Message types:**
| Type | Who writes it | Purpose |
|---|---|---|
| **System message** | You (the developer) | Sets the AI's behaviour. Example: "You are a helpful Java teacher. Answer simply." |
| **User message** | The user | The actual question |
| **Assistant message** | The AI | The AI's previous answers (used for memory, later topic) |

### ChatResponse 📦 (what you GET BACK)
A `ChatResponse` is the **full reply object**. It holds:
- The **answer text**
- **Metadata** (extra information about the call)

```java
String answer = response.getResult().getOutput().getText();   // the answer
Usage usage   = response.getMetadata().getUsage();            // token usage
```

More details in [section 10](#10-content-vs-chatresponse).

---

## 6. ChatClient

### What is it?
`ChatClient` is an **interface** that sits **on top of** `ChatModel`.
It is a **fluent API**: you write calls as a **chain of methods**, which reads almost like English.

```java
String answer = chatClient.prompt()
        .user("What is Spring Boot?")
        .call()
        .content();
```

Read it like a sentence:
> "Start a **prompt**, the **user** says 'What is Spring Boot?', **call** the AI, give me the **content**."

Short form (used in this project):
```java
String answer = chatClient.prompt("What is Spring Boot?").call().content();
```

### Why do we use ChatClient? (very important ⭐)
1. **Less code.** You can pass a plain `String`. No need to create `Prompt` objects yourself.
2. **Easy to read.** Method chaining is clean and clear.
3. **Easy result.** `.content()` gives the answer as a `String` directly.
4. **Default settings.** Set a system prompt once, and it is used for every call:
   ```java
   builder.defaultSystem("You are a helpful Java teacher").build();
   ```
5. **Structured output.** Convert the AI's answer directly into a Java object:
   ```java
   record Movie(String title, int year) {}
   Movie movie = chatClient.prompt("Give me one famous movie").call().entity(Movie.class);
   ```
6. **Streaming.** Get the answer word-by-word, like ChatGPT typing:
   ```java
   Flux<String> stream = chatClient.prompt("Tell a story").stream().content();
   ```
7. **Advisors.** Add **chat memory** and **RAG** (chatting with your own documents). This is the next big topic.

> 💡 **Remember:** `ChatClient` does **not** talk to OpenAI itself.
> Inside, it still uses a `ChatModel` (here, `OpenAiChatModel`) to do the real work.

---

## 7. DefaultChatClient

### What is it?
`ChatClient` is an **interface**. Interfaces cannot do work by themselves.
`DefaultChatClient` is the **class that implements** `ChatClient`. It is provided by Spring AI.

```
ChatClient (interface)  ──implemented by──▶  DefaultChatClient (class)
```

### Do I create it myself?
**No.** You never write `new DefaultChatClient(...)`.
When you call `builder.build()`, Spring AI creates a `DefaultChatClient` for you and returns it as the `ChatClient` type.

You only need to know that it exists, which is useful for interviews and when debugging.

---

## 8. ChatClient.Builder and auto-configuration

### What does Spring Boot create automatically?
| Thing | Auto-created by Spring Boot? |
|---|---|
| `OpenAiChatModel` (the `ChatModel` bean) | ✅ Yes |
| `ChatClient.Builder` | ✅ Yes |
| `ChatClient` itself | ❌ **No.** You must build it yourself. |

So you **inject the Builder** and **call `.build()`**.

### Way 1: Create a ChatClient bean in a config class (used in this project ✅)
`src/main/java/com/spring_ai/day_1/config/SpringAIConfig.java`
```java
@Configuration
public class SpringAIConfig {

    @Bean
    ChatClient chatClient(ChatClient.Builder builder) {   // Spring gives us the auto-configured Builder
        return builder.build();                           // we build the ChatClient (a DefaultChatClient)
    }
}
```
Now any class can inject `ChatClient` directly.

### Way 2: Build it inside the class that uses it
```java
@RestController
public class ChatController {

    private final ChatClient chatClient;

    public ChatController(ChatClient.Builder builder) {
        this.chatClient = builder.build();
    }
}
```

### Which way should I choose?
| Way | Choose it when |
|---|---|
| **Way 1** (a `@Bean` in a config class) | Many classes need the **same** ChatClient with the **same** settings. Build it once, use it everywhere. |
| **Way 2** (build in the constructor) | One class needs its **own** ChatClient with **different** settings. Example: a "Java teacher" bot and a "customer support" bot with different system prompts. |

> ⚠️ **Build the ChatClient once and reuse it.** Do **not** call `.build()` inside a method that runs on every request.
> Building it once (in a bean or in a constructor) is the correct way.

### With default settings
```java
@Bean
ChatClient chatClient(ChatClient.Builder builder) {
    return builder
            .defaultSystem("You are a friendly Java teacher. Use simple English.")
            .build();
}
```

### Two things that look similar but are different ⚠️
| Code | Meaning |
|---|---|
| `ChatClient.Builder` (injected) | Auto-configured by Spring. **Already connected** to the auto-configured ChatModel. |
| `ChatClient.builder(chatModel)` (static method) | You create a builder yourself and **pass the ChatModel you want**. Used when you have many vendors. |

### Bonus: using two vendors at once (OpenAI + Anthropic)
If both starters are added, Spring does not know **which** ChatModel the auto-configured Builder should use.
Solution:
1. Turn off the automatic builder:
   ```properties
   spring.ai.chat.client.enabled=false
   ```
2. Build each client yourself:
   ```java
   @Bean
   ChatClient openAiClient(OpenAiChatModel model) {
       return ChatClient.builder(model).build();
   }

   @Bean
   ChatClient anthropicClient(AnthropicChatModel model) {
       return ChatClient.builder(model).build();
   }
   ```

### Bonus fact
The auto-configured `ChatClient.Builder` is **prototype-scoped**.
That means **every place that injects it gets a new, fresh Builder**, so different ChatClients don't share settings by mistake.

---

## 9. ChatModel vs ChatClient: which one to use?

### First, the simple idea 🚗

Think of a **car**:

- **ChatModel = the engine** (in this project, the OpenAI engine: `OpenAiChatModel`). It does the real work (talks to OpenAI). But you can't easily drive an engine by itself. You would need to connect the wheels, steering and brakes yourself.
- **ChatClient = the full car.** The engine is **inside** it. You get a steering wheel, pedals and a dashboard, so driving is easy.

Most people just **drive the car** (use ChatClient).
Only a **mechanic**, or someone **building their own car**, touches the engine directly (uses ChatModel).

> 💡 Important: when you use ChatClient, you are **still using ChatModel**, because it is inside.
> It is not "ChatModel OR ChatClient". It is "ChatClient on top of ChatModel".

---

### Let's see the difference with real code 👀

We will do the **same 3 tasks** both ways. Look at how much work each one needs.

#### Task 1: Ask a simple question

**With ChatModel:**
```java
ChatResponse response = chatModel.call(new Prompt("What is Java?"));
String answer = response.getResult().getOutput().getText();
```

**With ChatClient:**
```java
String answer = chatClient.prompt("What is Java?").call().content();
```

👉 Both work. ChatClient is shorter: no `Prompt` object, no digging through `getResult().getOutput().getText()`.

#### Task 2: Tell the AI how to behave (a system message) + ask a question

**With ChatModel:** you must create each message yourself and put them in a list.
```java
SystemMessage systemMessage = new SystemMessage("You are a Java teacher. Use simple English.");
UserMessage userMessage = new UserMessage("What is Java?");

Prompt prompt = new Prompt(List.of(systemMessage, userMessage));

ChatResponse response = chatModel.call(prompt);
String answer = response.getResult().getOutput().getText();
```

**With ChatClient:**
```java
String answer = chatClient.prompt()
        .system("You are a Java teacher. Use simple English.")
        .user("What is Java?")
        .call()
        .content();
```

👉 ChatClient reads like a sentence. The difference is getting bigger.

#### Task 3: Get the answer as a Java object (not plain text)

Say we want this:
```java
record Movie(String title, int year) {}
```

**With ChatModel:** you must do every step yourself.
```java
// 1. Create a converter for the Movie class
BeanOutputConverter<Movie> converter = new BeanOutputConverter<>(Movie.class);

// 2. Add instructions to the prompt, telling the AI to reply in Movie's JSON format
String text = "Give me one famous movie. " + converter.getFormat();

// 3. Call the AI
ChatResponse response = chatModel.call(new Prompt(text));

// 4. Take out the text and convert it into a Movie object
Movie movie = converter.convert(response.getResult().getOutput().getText());
```

**With ChatClient:**
```java
Movie movie = chatClient.prompt("Give me one famous movie")
        .call()
        .entity(Movie.class);
```

👉 **4 steps vs 1 line.** ChatClient does all of those steps for you, inside `.entity()`.

**This is the main reason ChatClient exists:** the more features you need, the more work ChatClient saves you.

---

### Simple comparison table

| Question | **ChatModel** | **ChatClient** |
|---|---|---|
| What is it? | The engine (low-level) | The full car (high-level) |
| Is it an interface? | Yes | Yes |
| Who implements it? | Each AI company has its own class: `OpenAiChatModel`, `AnthropicChatModel`, ... | Only one class for everyone: `DefaultChatClient` |
| What do I send? | A `Prompt` object (you build it) | A plain String, with easy methods like `.system()` and `.user()` |
| What do I get back? | A `ChatResponse` (you dig the text out yourself) | Whatever you ask for: `.content()` → String, `.entity()` → Java object, `.chatResponse()` → full response |
| Extra features (memory, Java objects, streaming) | You build them yourself | Ready to use |
| Does Spring Boot create it for me? | ✅ Yes, automatically | ❌ No. Spring gives you a Builder, you call `.build()` |

---

### ✅ When to use ChatClient (almost always)

Use ChatClient for **any normal feature in your app**. For example:

- 💬 A chatbot endpoint, like the `/api/v1/chat` endpoint in this project
- 📄 "Summarize this text for me"
- 🛒 "Write a product description for this item"
- 🎬 "Give me movie details as a `Movie` object"
- 🧠 A chatbot that **remembers** earlier messages (memory, a later topic)
- 📚 A chatbot that answers from **your own documents** (RAG, a later topic)

**Simple rule: if you are writing code for a feature in your app, use ChatClient.**

---

### 🔧 When to use ChatModel directly (rare cases)

#### Case 1: You use two AI companies in the same app

ChatClient needs to know **which engine** to use. So you take each ChatModel and **give it to** a ChatClient.
(First turn off the automatic Builder with `spring.ai.chat.client.enabled=false`. See [section 8](#8-chatclientbuilder-and-auto-configuration).)

```java
@Bean
ChatClient openAiClient(OpenAiChatModel openAiChatModel) {
    return ChatClient.builder(openAiChatModel).build();    // car with the OpenAI engine
}

@Bean
ChatClient claudeClient(AnthropicChatModel anthropicChatModel) {
    return ChatClient.builder(anthropicChatModel).build(); // car with the Claude engine
}
```

👉 Here you **touch** ChatModel, but only to build the ChatClients.
In your controllers and services you **still use ChatClient** to chat.

#### Case 2: You are building your own tool on top of the AI

Example: your company wants its own `AiHelper` class that every team uses, with company rules inside (logging, security checks, and so on).

```java
@Slf4j      // Lombok: gives us the "log" object
@Service
public class AiHelper {

    private final ChatModel chatModel;   // only needs the basic engine

    public AiHelper(ChatModel chatModel) {
        this.chatModel = chatModel;
    }

    public String ask(String question) {
        log.info("AI request: {}", question);   // company rule: log every request
        return chatModel.call(question);
    }
}
```

👉 You are **building your own car**, so you start from the engine.
This is advanced work. If you are new to Spring AI, you won't need it for a while.

#### Case 3: You want to learn what happens underneath

Using ChatModel once or twice is a good way to **understand** what ChatClient does for you
(creating the `Prompt`, digging out the text, converting to objects).
After that, go back to ChatClient.

---

### ❌ Wrong reason to use ChatModel

> "I need the token usage, so I must use ChatModel."

**Not true!** ChatClient can also give you the full response with all the details:
```java
ChatResponse response = chatClient.prompt("What is Java?").call().chatResponse();
response.getMetadata().getUsage();   // token usage ✅
```

---

### 🧭 Quick decision guide

```
Are you building a normal feature in your app?
   │
   ├── YES → Use ChatClient ✅
   │
   └── NO → Are you connecting two or more AI companies?
              │
              ├── YES → Use ChatModel to BUILD each ChatClient,
              │         then chat using ChatClient ✅
              │
              └── NO → Are you building your own reusable AI tool?
                         │
                         ├── YES → ChatModel is fine 🔧
                         └── NO  → Use ChatClient ✅
```

### 📌 Remember in one line

> **ChatModel is the engine. ChatClient is the car. Drive the car. Touch the engine only when you are building or fixing the car.**

---

## 10. `content()` vs `chatResponse()`

| Method | Returns | Use when |
|---|---|---|
| `.call().content()` | `String`, just the answer text | You only want to show the answer (most of the time) |
| `.call().chatResponse()` | `ChatResponse`, answer **plus metadata** | You need extra information about the call |
| `.call().entity(MyClass.class)` | Your Java object | You want structured data, not plain text |

### What metadata is inside ChatResponse?
| Information | How to get it | Why it is useful |
|---|---|---|
| **Token usage** | `response.getMetadata().getUsage()` | 💰 **Cost tracking.** Vendors charge per token. |
| **Model name** | `response.getMetadata().getModel()` | Confirm which model actually answered |
| **Finish reason** | `response.getResult().getMetadata().getFinishReason()` | Why the AI stopped: `STOP` = finished normally, `LENGTH` = hit the token limit (**answer may be cut off!**), content filter = blocked |
| **Rate limit** | `response.getMetadata().getRateLimit()` | How many requests or tokens are left before the vendor slows you down |

### Example
```java
ChatResponse response = chatClient.prompt()
        .user("Explain Spring AI")
        .call()
        .chatResponse();

String answer = response.getResult().getOutput().getText();
Usage usage   = response.getMetadata().getUsage();

System.out.println("Answer: " + answer);
System.out.println("Total tokens used: " + usage.getTotalTokens());
```

---

## 11. How this project works, step by step

### Project files
```
src/main/java/com/spring_ai/day_1/
├── OpenAIApplication.java        → Starts the Spring Boot app
├── config/SpringAIConfig.java    → Builds the ChatClient bean
└── controller/ChatController.java→ REST endpoint /api/v1/chat
src/main/resources/
└── application.properties        → OpenAI API key (from environment variable)
pom.xml                           → spring-ai-starter-model-openai dependency
```

### The controller
```java
@RestController
@RequestMapping("/api/v1/chat")
@RequiredArgsConstructor                 // Lombok: creates a constructor for final fields
public class ChatController {

    private final ChatClient chatClient; // injected from SpringAIConfig

    @GetMapping
    public String chat(@RequestParam String message) {
        return chatClient.prompt(message).call().content();
    }
}
```

### What happens when a request comes in 🔄
```
1. Browser/Postman  →  GET /api/v1/chat?message=Hello
2. ChatController   →  chatClient.prompt("Hello").call().content()
3. ChatClient (DefaultChatClient) → creates a Prompt and passes it to the ChatModel
4. OpenAiChatModel  →  converts it to OpenAI JSON and sends an HTTP request with the API key
5. OpenAI servers   →  GPT model thinks and replies
6. OpenAiChatModel  →  converts the reply into a ChatResponse
7. ChatClient       →  .content() takes out only the answer text
8. Controller       →  returns the String to the browser ✅
```

### What happens when the app STARTS 🚀
```
1. Spring Boot sees spring-ai-starter-model-openai in pom.xml
2. It reads spring.ai.openai.api-key
3. It auto-creates the OpenAiChatModel bean
4. It auto-creates the ChatClient.Builder bean (connected to OpenAiChatModel)
5. SpringAIConfig gets the Builder and calls .build() → ChatClient bean is ready
6. ChatController gets the ChatClient injected
```

---

## 12. How to run this project

**Step 1:** Set your OpenAI API key as an environment variable (macOS/Linux):
```bash
export OPENAI_API_KEY=your-key-here
```

**Step 2:** Start the app:
```bash
./mvnw spring-boot:run
```

**Step 3:** Test it (in a new terminal, or open the URL in a browser):
```bash
curl "http://localhost:8080/api/v1/chat?message=What%20is%20Spring%20AI"
```
(`%20` means a space inside a URL.)

---

## 13. Common mistakes and errors

| ❌ Mistake | ✅ Correct |
|---|---|
| Saying **"OpenAPI"** for the AI company | **OpenAI** = the AI company (GPT). **OpenAPI** = a spec for describing REST APIs (Swagger). Totally different! |
| Thinking `ChatClient` is auto-configured | Only `ChatClient.Builder` is auto-configured. You must call `.build()`. |
| Thinking ChatClient talks to OpenAI directly | ChatClient uses a ChatModel inside. The ChatModel does the real HTTP call. |
| Writing the API key directly in `application.properties` | Use `${OPENAI_API_KEY}` and an environment variable. Never push keys to GitHub. |
| "ChatModel is hard to configure" | Configuration is easy (just properties). It is more verbose to **use**. |
| Using ChatModel just to get token usage | Use `chatClient...call().chatResponse()` instead. |
| Adding two vendor starters and getting an error | Set `spring.ai.chat.client.enabled=false` and build each client with `ChatClient.builder(model)`. |
| Calling `.build()` inside the request method | Build the ChatClient **once** (in a bean or constructor) and reuse it. |

### Errors you may see while running, and what they mean 🛠️
| What you see | Simple meaning | How to fix |
|---|---|---|
| **401 Unauthorized** (or the app fails to start because of the API key) | OpenAI didn't accept your key, or the key is missing | Check that `OPENAI_API_KEY` is set **in the same terminal** where you start the app |
| **429 Too Many Requests** | You sent too many requests too fast, or your OpenAI account has no credit left | Wait and try again, or check billing on your OpenAI account |
| **"required a single bean, but 2 were found"** (about `ChatModel`) | Two AI starters are added, and Spring doesn't know which ChatModel the Builder should use | See [section 8](#8-chatclientbuilder-and-auto-configuration): turn off the auto Builder and build one ChatClient per vendor |
| **Answer is cut off in the middle** | The model hit its token limit (finish reason = `LENGTH`) | Ask for a shorter answer, or allow more output tokens |

---

## 14. Interview questions and answers

**How to use this section:** each question has a **short answer** (say this first, in 1–2 sentences)
and a **"if they ask more"** part (say this only if the interviewer wants more detail).
Practice saying the short answers out loud.

---

**Q1. What is ChatModel in Spring AI?**

🟢 **Short answer:** ChatModel is a low-level interface for sending a prompt to an AI model and getting a response back. Each vendor gives its own implementation, like `OpenAiChatModel` or `AnthropicChatModel`.

🔵 **If they ask more:** Its main method is `ChatResponse call(Prompt prompt)`. Because our code depends on the interface and not on one vendor, we can switch from OpenAI to Anthropic mostly by changing the dependency and properties.

---

**Q2. What is ChatClient?**

🟢 **Short answer:** ChatClient is a high-level, fluent API built on top of ChatModel. It makes AI calls short and easy, like `chatClient.prompt("Hi").call().content()`.

🔵 **If they ask more:** It also gives ready-made features: a default system prompt, structured output with `.entity()`, streaming with `.stream()`, and advisors for chat memory and RAG. Inside, it still uses ChatModel to make the real call.

---

**Q3. What is DefaultChatClient?**

🟢 **Short answer:** It is the class that implements the ChatClient interface.

🔵 **If they ask more:** We never create it with `new`. When we call `builder.build()`, Spring AI returns a `DefaultChatClient`, but we use it through the `ChatClient` type.

---

**Q4. How do you create a ChatClient in Spring Boot?**

🟢 **Short answer:** Spring Boot auto-configures a `ChatClient.Builder`. I inject it and call `.build()`. ChatClient itself is not auto-configured.

🔵 **If they ask more:** I usually build it once in a `@Bean` method so many classes can share it. If one class needs different settings (like its own system prompt), I build it in that class's constructor. The Builder is prototype-scoped, so each injection gets a fresh one.

---

**Q5. When would you use ChatModel directly instead of ChatClient?**

🟢 **Short answer:** In normal application code I always use ChatClient. I use ChatModel directly only in special cases, like connecting multiple AI vendors or building my own AI library.

🔵 **If they ask more:** With multiple vendors, I pass each model into `ChatClient.builder(chatModel)` to create one ChatClient per vendor. For my own library, the basic ChatModel interface is enough. Needing token usage is **not** a reason, because ChatClient gives it through `.chatResponse()`.

---

**Q6. What is the difference between `.content()` and `.chatResponse()`?**

🟢 **Short answer:** `.content()` returns only the answer text as a String. `.chatResponse()` returns the full response, including metadata.

🔵 **If they ask more:** The metadata includes token usage, the model name and the finish reason. I use it for cost tracking, monitoring, and to detect answers that were cut off because the token limit was reached.

---

**Q7. How do you use two AI vendors in one app?**

🟢 **Short answer:** I turn off the auto-configured Builder and create one ChatClient bean for each vendor.

🔵 **If they ask more:** I set `spring.ai.chat.client.enabled=false`, then create beans with `ChatClient.builder(openAiChatModel).build()` and `ChatClient.builder(anthropicChatModel).build()`. Otherwise Spring finds two ChatModel beans and doesn't know which one to use for the Builder.

---

**Q8. Why does Spring AI use interfaces like ChatModel?**

🟢 **Short answer:** For loose coupling. Our code depends on the interface, not on one vendor, so changing the AI provider needs very little code change.

🔵 **If they ask more:** It is the same idea as JDBC: we write code against the JDBC interfaces, and the MySQL or PostgreSQL driver gives the implementation.

---

**Q9. What is the difference between a system message and a user message?**

🟢 **Short answer:** A system message tells the AI **how to behave**. A user message is the **actual question**.

🔵 **If they ask more:** The developer writes the system message, for example "You are a Java teacher. Answer in simple English." The user writes the user message, for example "What is a HashMap?" In ChatClient I use `.system(...)` and `.user(...)`, or `defaultSystem(...)` on the Builder to apply it to every call.

---

**Q10. What happens when you call `chatClient.prompt("Hi").call().content()`?**

🟢 **Short answer:** ChatClient builds a Prompt, gives it to the ChatModel, the ChatModel calls the AI vendor's API, and `.content()` returns only the answer text.

🔵 **If they ask more:** In this project, the ChatModel is `OpenAiChatModel`. It converts the Prompt into OpenAI's JSON format, sends an HTTP request with the API key, and converts the reply into a `ChatResponse`. Then `.content()` takes the text out of that ChatResponse.

---

**Q11. How does Spring Boot know to create `OpenAiChatModel`?**

🟢 **Short answer:** Through auto-configuration. When the `spring-ai-starter-model-openai` dependency is present, Spring Boot creates the `OpenAiChatModel` bean using the properties in `application.properties`.

🔵 **If they ask more:** The API key comes from `spring.ai.openai.api-key`, which I read from an environment variable so it is never in the code. The model and temperature can be set with `spring.ai.openai.chat.options.*` properties.

---

**Q12. Should you create a new ChatClient for every request?**

🟢 **Short answer:** No. Build it once and reuse it.

🔵 **If they ask more:** I build it in a `@Bean` method or in a constructor, so it is created once at startup. Calling `.build()` inside a request method would create a new client every time for no reason.

---

**Q13. Why use Spring AI instead of calling the OpenAI REST API directly?**

🟢 **Short answer:** Spring AI gives one common API for many AI vendors, so my code is not locked to one company. It also removes the boring work of building JSON and HTTP calls myself.

🔵 **If they ask more:** If I call OpenAI directly, I must write the HTTP request, build the JSON, read the JSON reply, and handle errors myself. And if we later move to Anthropic, I must rewrite all of it. With Spring AI, I get auto-configuration, simple properties, `ChatClient`, structured output, streaming, and advisors out of the box.

---

**Q14. What is a fluent API?**

🟢 **Short answer:** A fluent API is a style where you **chain methods one after another**, and the code reads almost like an English sentence.

🔵 **If they ask more:** Each method returns an object, so you can call the next method on it right away. Example: `chatClient.prompt().user("Hi").call().content()`. Java Streams and Spring's `RestClient` work in the same style.

---

**Q15. What is a token, and why should a developer care?**

🟢 **Short answer:** A token is a small piece of text, around ¾ of a word. AI vendors charge **per token**, for both the input and the output.

🔵 **If they ask more:** Tokens affect **cost** (more tokens = more money), **limits** (every model has a maximum number of tokens), and **cut-off answers** (if the limit is reached, the finish reason is `LENGTH`). I track token usage with `chatResponse().getMetadata().getUsage()`.

---

**Q16. What is temperature?**

🟢 **Short answer:** Temperature controls how creative or random the answer is. Low = safe and similar answers. High = more creative and different answers.

🔵 **If they ask more:** I'd use a low temperature (like `0.2`) for facts, code and data extraction, and a higher one (like `0.8`) for stories or marketing text. I set it with `spring.ai.openai.chat.options.temperature`.

---

**Q17. Besides ChatModel, what other model types does Spring AI have?**

🟢 **Short answer:** ChatModel is for text chat. Spring AI also has `EmbeddingModel`, `ImageModel`, `TranscriptionModel` (speech to text), `TextToSpeechModel` and `ModerationModel`.

🔵 **If they ask more:** They all follow the same idea as ChatModel: one common interface, with a separate implementation for each vendor. `EmbeddingModel` is important later for RAG, because it turns text into numbers (vectors) so we can search documents by meaning.

---

## 15. One-minute summary

- **ChatModel** = interface, low-level, the common "engine slot" for all AI vendors.
- **OpenAiChatModel** = OpenAI's implementation of ChatModel. Auto-created by Spring Boot.
- **Prompt** = what you send. **ChatResponse** = what you get back (answer + metadata).
- **ChatClient** = interface, high-level, fluent, easy. Uses ChatModel inside.
- **DefaultChatClient** = the implementation of ChatClient. Created by `builder.build()`.
- **ChatClient.Builder** = auto-configured by Spring Boot. **ChatClient is not.** Inject the Builder and call `.build()`.
- **`.content()`** = just text. **`.chatResponse()`** = text + metadata (tokens, finish reason, model).
- **ChatModel = engine, ChatClient = car.** Drive the car (ChatClient) in normal code. Touch the engine (ChatModel) only to connect multiple vendors or to build your own AI tool.

---

## 16. Self-test

Try to answer **before** you click to see the answer. If you get all 10 right, you are ready for basic Spring AI interview questions.

<details>
<summary><b>1.</b> Is ChatModel a class or an interface? Who gives its implementation?</summary>

An **interface**. Each AI vendor gives the implementation, for example `OpenAiChatModel` and `AnthropicChatModel`.
</details>

<details>
<summary><b>2.</b> What is the implementation class of ChatClient?</summary>

`DefaultChatClient`. You get it by calling `builder.build()`. You never create it with `new`.
</details>

<details>
<summary><b>3.</b> Which one does Spring Boot auto-configure: ChatClient or ChatClient.Builder?</summary>

Only **ChatClient.Builder**. You must call `.build()` yourself to get a ChatClient.
</details>

<details>
<summary><b>4.</b> Fill the blank: <code>chatClient.prompt("Hi").call().______()</code> to get the answer as a String.</summary>

`content()`
</details>

<details>
<summary><b>5.</b> Which method gives you token usage: <code>.content()</code> or <code>.chatResponse()</code>?</summary>

`.chatResponse()`. Then use `response.getMetadata().getUsage()`.
</details>

<details>
<summary><b>6.</b> Fill the blank to get a <code>Movie</code> object: <code>chatClient.prompt("Give me a movie").call().______(Movie.class)</code></summary>

`entity`
</details>

<details>
<summary><b>7.</b> What is the difference between a system message and a user message?</summary>

The **system message** tells the AI how to behave (written by the developer).
The **user message** is the actual question (written by the user).
</details>

<details>
<summary><b>8.</b> You added both the OpenAI and Anthropic starters, and the app fails to start. Why, and how do you fix it?</summary>

Spring finds **two ChatModel beans** and doesn't know which one to use for the auto-configured Builder.
Fix: set `spring.ai.chat.client.enabled=false`, then build one ChatClient per vendor with `ChatClient.builder(model).build()`.
</details>

<details>
<summary><b>9.</b> The finish reason is <code>LENGTH</code>. What does it mean?</summary>

The model hit its **token limit**, so the answer may be **cut off**.
</details>

<details>
<summary><b>10.</b> In the car example, what is ChatModel and what is ChatClient?</summary>

**ChatModel = the engine** (does the real work). **ChatClient = the full car** (easy to drive, engine inside).
Drive the car in normal code. Touch the engine only when building or fixing the car.
</details>

---

## 17. What to learn next

- [ ] **System prompts and prompt templates:** reusable prompts with `{placeholders}`
- [ ] **Structured output:** `.entity(MyRecord.class)`
- [ ] **Streaming:** `.stream().content()` with `Flux<String>`
- [ ] **Advisors:** chat memory (remember previous messages)
- [ ] **RAG:** chat with your own documents (vector stores, embeddings)
- [ ] **Tool calling:** let the AI call your Java methods
