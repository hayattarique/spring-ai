# Spring AI with Ollama: Run AI Models on Your Own Machine 🦙

A beginner-friendly guide to using **Ollama** with **Spring AI**, written in simple English,
with a small working Spring Boot project that talks to a **local** AI model. No API key, no cloud, no bill.

**What you will learn:**
- What Ollama is, and why you would run an AI model locally
- How to install Ollama and download a model
- How Spring AI connects to Ollama (`OllamaChatModel`)
- How the **same ChatClient code** from day 1 works with a different vendor
- OpenAI vs Ollama: when to use which
- Common mistakes, errors, and **interview questions with answers**

**Before this:** read [day-1](../day-1/) first. It explains `ChatModel`, `ChatClient` and auto-configuration.

**Tech stack:** Spring Boot 4.1.2 · Spring AI 2.0.1 · Java 21 · Ollama (llama3.1)

---

## Table of Contents

1. [Basic words you must know](#1-basic-words-you-must-know)
2. [What is Ollama?](#2-what-is-ollama)
3. [Install Ollama and download a model](#3-install-ollama-and-download-a-model)
4. [How Spring AI connects to Ollama](#4-how-spring-ai-connects-to-ollama)
5. [Day 1 vs Day 2: what changed?](#5-day-1-vs-day-2-what-changed)
6. [How this project works, step by step](#6-how-this-project-works-step-by-step)
7. [How to run this project](#7-how-to-run-this-project)
8. [OpenAI vs Ollama: which one to use?](#8-openai-vs-ollama-which-one-to-use)
9. [Common mistakes and errors](#9-common-mistakes-and-errors)
10. [Interview questions and answers](#10-interview-questions-and-answers)
11. [One-minute summary](#11-one-minute-summary)
12. [What to learn next](#12-what-to-learn-next)

---

## 1. Basic words you must know

| Word | Simple meaning |
|---|---|
| **Local model** | An AI model that runs on **your own computer**, not on a company's server. |
| **Ollama** | A free tool that downloads and runs AI models locally, and gives them a simple HTTP API. |
| **Open-weight model** | A model whose files are public, so anyone can download and run it. Example: Llama, Mistral, Qwen, Gemma. |
| **Llama 3.1** | An open-weight model made by Meta. The `8b` version (8 billion parameters) runs on a normal laptop. |
| **Parameters (7b, 8b, 70b)** | The model's size. Bigger = smarter, but needs more RAM and is slower. |
| **Pull** | Download a model to your machine: `ollama pull llama3.1`. |
| **Port 11434** | The default port where Ollama listens: `http://localhost:11434`. |

---

## 2. What is Ollama?

Think of Ollama as **"Docker for AI models"**:

- `ollama pull llama3.1` downloads a model (like `docker pull`)
- `ollama run llama3.1` starts chatting with it in the terminal (like `docker run`)
- Ollama runs a small server on **port 11434**, so any app (like our Spring Boot app) can send it prompts over HTTP

```
┌─────────────────────┐   HTTP (localhost:11434)   ┌──────────────────────┐
│  Spring Boot app    │ ─────────────────────────▶ │  Ollama server       │
│  (ChatClient)       │ ◀───────────────────────── │  runs llama3.1 on    │
└─────────────────────┘        answer              │  YOUR CPU / GPU      │
                                                   └──────────────────────┘
        Everything stays on your machine. Nothing goes to the internet. 🔒
```

### Why run a model locally? ✅
| Benefit | Why it matters |
|---|---|
| **Free** | No per-token cost. Call it 10,000 times while learning, and pay nothing. |
| **Private** | Your data never leaves your machine. Good for company code, personal data, documents. |
| **Works offline** | No internet needed once the model is downloaded. |
| **No API key** | Nothing to leak on GitHub. |

### The downsides ⚠️
| Downside | Why it matters |
|---|---|
| **Slower** | Speed depends on your laptop. A cloud GPU is much faster. |
| **Less smart** | Small local models (8b) are weaker than GPT-4o or Claude for hard tasks. |
| **Needs RAM and disk** | `llama3.1` (8b) is about a 4.9 GB download and needs around 8 GB of free RAM. |

---

## 3. Install Ollama and download a model

**Step 1: Install Ollama** (macOS):
```bash
brew install ollama
```
Or download the app from [ollama.com/download](https://ollama.com/download) (macOS, Windows, Linux).

**Step 2: Start the Ollama server** (skip this if you installed the desktop app, it starts by itself):
```bash
ollama serve
```

**Step 3: Download the model** used in this project:
```bash
ollama pull llama3.1
```

**Step 4: Check it works** (type a question, `/bye` to exit):
```bash
ollama run llama3.1
```

### Useful Ollama commands 🧰
| Command | What it does |
|---|---|
| `ollama list` | Show downloaded models |
| `ollama pull <model>` | Download a model |
| `ollama run <model>` | Chat with a model in the terminal |
| `ollama ps` | Show models currently loaded in memory |
| `ollama rm <model>` | Delete a model to free disk space |

Check the server is up: open [http://localhost:11434](http://localhost:11434) in a browser. It should say **"Ollama is running"**.

---

## 4. How Spring AI connects to Ollama

The idea from day 1 stays the same: **ChatClient → ChatModel → vendor**.
Only the bottom layer changes.

```
        Your code (ChatController)
                 │
                 ▼
           ChatClient            ← same as day 1, no change
                 │
                 ▼
     ChatModel (interface)       ← same as day 1, no change
                 │
                 ▼
        OllamaChatModel          ← NEW: was OpenAiChatModel in day 1
                 │
                 ▼
     Ollama server :11434        ← NEW: was OpenAI's servers in day 1
```

### The dependency (`pom.xml`)
```xml
<dependency>
    <groupId>org.springframework.ai</groupId>
    <artifactId>spring-ai-starter-model-ollama</artifactId>
</dependency>
```
Spring Boot sees this starter and **auto-creates an `OllamaChatModel` bean**, plus the `ChatClient.Builder` connected to it.

### The properties (`application.properties`)
```properties
spring.ai.model.chat=ollama
spring.ai.ollama.base-url=http://localhost:11434
spring.ai.ollama.chat.model=llama3.1
```

| Property | Simple meaning |
|---|---|
| `spring.ai.model.chat=ollama` | "Use Ollama as the chat model." Important when more than one AI starter is on the classpath. |
| `spring.ai.ollama.base-url` | Where the Ollama server is. `http://localhost:11434` is the default, so this line is optional. |
| `spring.ai.ollama.chat.model` | Which model to use. Must match a name from `ollama list`. |

> 💡 You may see `spring.ai.ollama.chat.options.model` in older tutorials. In Spring AI 2.0 it is **deprecated**; use `spring.ai.ollama.chat.model`.

### Bonus: let Spring download the model for you
```properties
spring.ai.ollama.init.pull-model-strategy=when_missing
```
| Value | Meaning |
|---|---|
| `never` (default) | Don't download. You must run `ollama pull` yourself. |
| `when_missing` | Download at app startup **only if** the model is not there yet. |
| `always` | Download (update) every time the app starts. |

---

## 5. Day 1 vs Day 2: what changed?

This is the most important lesson of day 2: **Spring AI makes switching vendors cheap.**

| Part | Day 1 (OpenAI) | Day 2 (Ollama) |
|---|---|---|
| Starter in `pom.xml` | `spring-ai-starter-model-openai` | `spring-ai-starter-model-ollama` |
| Properties | `spring.ai.openai.api-key=${OPENAI_API_KEY}` | `spring.ai.ollama.base-url`, `spring.ai.ollama.chat.model` |
| Auto-created ChatModel | `OpenAiChatModel` | `OllamaChatModel` |
| API key | Required | **Not needed** |
| `SpringAIConfig.java` | `builder.build()` | **Same** ✅ |
| `ChatController.java` | `chatClient.prompt(message).call().content()` | **Same** ✅ |

👉 The Java code did **not change at all**. Only the dependency and the properties changed.
This is why we code against `ChatClient` / `ChatModel` interfaces, not vendor classes.

---

## 6. How this project works, step by step

### Project files
```
src/main/java/org/springai/day2/
├── Day2OllamaApplication.java     → Starts the Spring Boot app
├── config/SpringAIConfig.java     → Builds the ChatClient bean
└── controller/ChatController.java → REST endpoint /api/v1/chat
src/main/resources/
└── application.properties         → Ollama URL and model name
pom.xml                            → spring-ai-starter-model-ollama dependency
```

### The config
```java
@Configuration
public class SpringAIConfig {

    @Bean
    ChatClient chatClient(ChatClient.Builder builder) {  // Builder is auto-configured
        return builder.build();                         // ChatClient is not, so we build it
    }
}
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
3. ChatClient       →  creates a Prompt and passes it to the ChatModel
4. OllamaChatModel  →  converts it to Ollama's JSON and POSTs to http://localhost:11434/api/chat
5. Ollama server    →  llama3.1 runs on your CPU/GPU and writes the answer
6. OllamaChatModel  →  converts the reply into a ChatResponse
7. ChatClient       →  .content() takes out only the answer text
8. Controller       →  returns the String to the browser ✅
```

---

## 7. How to run this project

**Step 1:** Make sure Ollama is running and the model is downloaded:
```bash
ollama list
```
You should see `llama3.1` in the list. If not, run `ollama pull llama3.1`.

**Step 2:** Start the app (from the `day-2-ollama` folder):
```bash
mvn spring-boot:run
```

**Step 3:** Test it (in a new terminal, or open the URL in a browser):
```bash
curl "http://localhost:8080/api/v1/chat?message=What%20is%20Spring%20AI"
```
(`%20` means a space inside a URL.)

> ⏳ The **first** request can take 10–30 seconds, because Ollama loads the model into memory. Later requests are faster.

---

## 8. OpenAI vs Ollama: which one to use?

| Question | OpenAI (cloud) | Ollama (local) |
|---|---|---|
| Cost | Pay per token | Free |
| Speed | Fast (big cloud GPUs) | Depends on your machine |
| Quality | Very high | Good for simple tasks, weaker for hard ones |
| Privacy | Data goes to OpenAI | Data stays on your machine |
| Internet | Required | Not required |
| Setup | Just an API key | Install Ollama + download model |

**Simple rule:**
- Use **Ollama** for learning, local development, tests, and private/sensitive data.
- Use **OpenAI** (or another cloud vendor) when you need the best answers or fast production speed.
- Because the Java code is the same, you can **develop with Ollama and deploy with OpenAI** by switching the dependency and properties.

---

## 9. Common mistakes and errors

| ❌ Mistake | ✅ Correct |
|---|---|
| Starting the app before Ollama is running | Start Ollama first (`ollama serve` or the desktop app). |
| Model name in properties doesn't match `ollama list` | Use the exact name, e.g. `llama3.1`. Tags like `llama3.1:8b` also work. |
| Thinking you need an API key for Ollama | No key needed. Ollama is local. |
| Using `spring.ai.ollama.chat.options.model` | Deprecated in Spring AI 2.0. Use `spring.ai.ollama.chat.model`. |
| Pulling a huge model (`70b`) on a laptop | Start with `8b` or smaller (`llama3.2` is 3b). Bigger models need much more RAM. |
| Rewriting controller code to switch from OpenAI | Not needed. Change only the starter and the properties. |

### Errors you may see while running, and what they mean 🛠️
| What you see | Simple meaning | How to fix |
|---|---|---|
| **Connection refused** / `I/O error on POST request for "http://localhost:11434/api/chat"` | The Ollama server is not running | Run `ollama serve` or open the Ollama app, then check [http://localhost:11434](http://localhost:11434) |
| **404** / `model "llama3.1" not found, try pulling it first` | The model is not downloaded | Run `ollama pull llama3.1`, or set `spring.ai.ollama.init.pull-model-strategy=when_missing` |
| **Very slow answer / timeout** | The model is too big for your machine, or still loading | Wait for the first load, close other heavy apps, or use a smaller model like `llama3.2` |
| **"required a single bean, but 2 were found"** (about `ChatModel`) | Both OpenAI and Ollama starters are on the classpath | Set `spring.ai.model.chat=ollama` (or `openai`) to pick one |

---

## 10. Interview questions and answers

**How to use this section:** say the 🟢 **short answer** first. Add the 🔵 part only if the interviewer wants more detail.

---

**Q1. What is Ollama?**

🟢 **Short answer:** Ollama is a free tool that downloads and runs open-weight AI models (like Llama or Mistral) on your own machine, and exposes them through a local HTTP API on port 11434.

🔵 **If they ask more:** It works a bit like Docker for models: `ollama pull` downloads a model and `ollama run` starts it. Apps talk to it over REST, so any language or framework can use it.

---

**Q2. How do you use Ollama in a Spring Boot app?**

🟢 **Short answer:** Add `spring-ai-starter-model-ollama`, set `spring.ai.ollama.chat.model` in properties, and inject `ChatClient` as usual. Spring Boot auto-creates `OllamaChatModel`.

🔵 **If they ask more:** The base URL defaults to `http://localhost:11434`. You can also set `spring.ai.ollama.init.pull-model-strategy=when_missing` so the model is downloaded at startup.

---

**Q3. What did you change in code to move from OpenAI to Ollama?**

🟢 **Short answer:** Nothing in the Java code. Only the Maven starter and the properties changed.

🔵 **If they ask more:** Our code depends on `ChatClient` and `ChatModel`, which are interfaces. The vendor-specific class (`OpenAiChatModel` or `OllamaChatModel`) is picked by auto-configuration based on the classpath.

---

**Q4. Why would a company run a model locally instead of using OpenAI?**

🟢 **Short answer:** Privacy, cost, and offline use. Sensitive data never leaves their servers, and there is no per-token bill.

🔵 **If they ask more:** The trade-off is that they must manage hardware (GPUs, RAM), and local open-weight models are usually less capable than the best cloud models.

---

**Q5. What is `spring.ai.model.chat` used for?**

🟢 **Short answer:** It tells Spring AI which vendor's chat model to auto-configure, for example `ollama` or `openai`.

🔵 **If they ask more:** It is useful when more than one AI starter is on the classpath, so Spring doesn't create two `ChatModel` beans and fail with "expected single matching bean".

---

**Q6. What is the default Ollama URL and port?**

🟢 **Short answer:** `http://localhost:11434`.

🔵 **If they ask more:** If Ollama runs on another machine or in Docker, change `spring.ai.ollama.base-url` to that address.

---

**Q7. Why is the first request to Ollama slow?**

🟢 **Short answer:** Ollama loads the model from disk into memory on the first request. After that it stays loaded for a while, so later requests are faster.

🔵 **If they ask more:** You can control how long the model stays in memory with the `keep-alive` setting (`spring.ai.ollama.chat.keep-alive`).

---

## 11. One-minute summary

- **Ollama** = free tool to run open-weight AI models **locally**, on port **11434**.
- `ollama pull llama3.1` downloads a model, `ollama list` shows what you have.
- Add **`spring-ai-starter-model-ollama`** → Spring Boot auto-creates **`OllamaChatModel`**.
- Set **`spring.ai.ollama.chat.model=llama3.1`**. No API key needed.
- **`ChatClient` code is exactly the same as day 1.** Switching vendors = change dependency + properties.
- **Ollama** = free, private, offline, slower. **OpenAI** = paid, fast, smarter.
- Develop locally with Ollama, deploy with a cloud model if you need more power.

---

## 12. What to learn next

- [ ] **Use two vendors in one app:** OpenAI and Ollama side by side, one `ChatClient` each
- [ ] **Try other local models:** `mistral`, `qwen3`, `gemma3`, and compare answers
- [ ] **Chat options:** temperature and other settings per request
- [ ] **Streaming:** `.stream().content()` so the answer appears word by word
- [ ] **Embeddings with Ollama:** the first step toward RAG
