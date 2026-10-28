# Spring AI with Docker Model Runner: Local Models + Streaming 🐳

A beginner-friendly guide to running AI models with **Docker Model Runner** and calling them from **Spring AI**,
written in simple English. This project also shows **streaming**: the answer arrives word by word instead of all at once.

**What you will learn:**
- What Docker Model Runner is, and how it compares to Ollama (day 2)
- How to enable it, pull a model, and check it works
- How **both** the OpenAI starter and the Anthropic starter can talk to Docker Model Runner, and **why the base URL must change**
- ⭐ **Why the OpenAI starter is the official choice** for Docker, as the Spring AI docs recommend (with interview follow-ups)
- How streaming works with `.stream().content()` and `Flux<String>`
- Common mistakes, errors, and **interview questions with answers**

**Before this:** read [day-1](../day-1/) (ChatClient basics) and [day-2-ollama](../day-2-ollama/) (local models).

**Tech stack:** Spring Boot 4.1.2 · Spring AI 2.0.1 · Java 21 · Docker Model Runner (gemma3:4b) · OpenAI or Anthropic API format

---

## Table of Contents

1. [Basic words you must know](#1-basic-words-you-must-know)
2. [What is Docker Model Runner?](#2-what-is-docker-model-runner)
3. [Enable Docker Model Runner and pull a model](#3-enable-docker-model-runner-and-pull-a-model)
4. [The trick: one local model, two API formats](#4-the-trick-one-local-model-two-api-formats)
5. [Connecting Spring AI: OpenAI starter or Anthropic starter](#5-connecting-spring-ai-openai-starter-or-anthropic-starter)
   - [Why do we need to change the URL?](#-why-do-we-need-to-change-the-url)
6. [Streaming: getting the answer word by word](#6-streaming-getting-the-answer-word-by-word)
7. [How this project works, step by step](#7-how-this-project-works-step-by-step)
8. [How to run this project](#8-how-to-run-this-project)
9. [Ollama vs Docker Model Runner](#9-ollama-vs-docker-model-runner)
10. [Common mistakes and errors](#10-common-mistakes-and-errors)
11. [Interview questions and answers](#11-interview-questions-and-answers)
12. [One-minute summary](#12-one-minute-summary)
13. [What to learn next](#13-what-to-learn-next)
14. [References](#references-)

---

## 1. Basic words you must know

| Word | Simple meaning |
|---|---|
| **Docker Model Runner (DMR)** | A feature of Docker Desktop that downloads and runs AI models on your machine. |
| **`ai/` models** | Ready-to-use models on Docker Hub, like `ai/gemma3`, `ai/llama3.2`, `ai/qwen3`. |
| **Gemma 3** | An open-weight model made by Google. `4b` = 4 billion parameters, small enough for a laptop. |
| **Port 12434** | The port where Docker Model Runner listens on your machine. |
| **API format** | The "shape" of the HTTP request and JSON. OpenAI and Anthropic each have their own format. |
| **OpenAI-compatible** | A server that accepts requests in **OpenAI's format**, even though it is not OpenAI. |
| **Base URL** | The start of the address the SDK sends requests to. The SDK adds the rest of the path itself. |
| **Streaming** | The server sends the answer in small pieces as it writes them, instead of waiting until it's finished. |
| **`Flux<String>`** | A Java type (from Project Reactor) meaning "a stream of many Strings that arrive over time". |

---

## 2. What is Docker Model Runner?

Docker Model Runner lets you run AI models with the **same Docker tool you already use for containers**.

- `docker model pull ai/gemma3:4b` downloads a model (just like `docker pull` for images)
- `docker model run ai/gemma3:4b` lets you chat with it in the terminal
- Docker runs a small server on **port 12434**, so any app can send it prompts over HTTP

```
┌─────────────────────┐   HTTP (localhost:12434)    ┌──────────────────────────┐
│  Spring Boot app    │ ──────────────────────────▶ │  Docker Model Runner     │
│  (ChatClient)       │ ◀────────────────────────── │  runs ai/gemma3:4b on    │
└─────────────────────┘   answer (streamed)         │  YOUR machine            │
                                                    └──────────────────────────┘
        Everything stays on your machine. No real API key, no bill. 🔒
```

It gives you the same benefits as Ollama (day 2): **free, private, works offline**.
The difference is that it's **built into Docker Desktop**, so teams who already use Docker don't need to install anything new.

---

## 3. Enable Docker Model Runner and pull a model

**Step 1: Enable it in Docker Desktop**
- Open **Docker Desktop → Settings → AI** (called "Features in development" in older versions)
- Turn on **Enable Docker Model Runner**
- Turn on **Enable host-side TCP support** (port `12434`). Without this, apps outside Docker can't reach it.
- Click **Apply & restart**

**Step 2: Pull the model** used in this project:
```bash
docker model pull ai/gemma3:4b
```

**Step 3: Check it works**:
```bash
docker model run ai/gemma3:4b "Say hi"
```

### Useful commands 🧰
| Command | What it does |
|---|---|
| `docker model status` | Is Docker Model Runner running? |
| `docker model ls` | Show downloaded models |
| `docker model pull <model>` | Download a model |
| `docker model run <model> "question"` | Ask a model one question in the terminal |
| `docker model ps` | Show models currently loaded in memory |
| `docker model rm <model>` | Delete a model to free disk space |

Check the server from the terminal:
```bash
curl http://localhost:12434/engines/v1/models
```
You should see JSON that lists `ai/gemma3:4b`.

---

## 4. The trick: one local model, two API formats

Every AI vendor has its own request format (URL path + JSON shape). Docker Model Runner understands **two** of them,
and sends both to the **same** model:

| URL on port 12434 | Format it understands | Which Spring AI starter sends this |
|---|---|---|
| `/engines/v1/chat/completions` | **OpenAI** format | `spring-ai-starter-model-openai` |
| `/engines/v1/messages` | **Anthropic** format | `spring-ai-starter-model-anthropic` |

```
                    ┌─ OpenAI starter ──── POST /engines/v1/chat/completions ──┐
ChatClient ─────────┤                       (OpenAI-format JSON)               ├──▶ Docker Model Runner ──▶ ai/gemma3:4b
(same code for both)└─ Anthropic starter ─ POST /engines/v1/messages ──────────┘     (localhost:12434)       (Google's model)
                                            (Anthropic-format JSON)
```

👉 **Key idea: the starter decides the *format* of the request, not *which model* answers.**
The OpenAI starter doesn't know it's talking to Docker, and the Anthropic starter doesn't know it's talking to Gemma.
They just send their normal requests to whatever `base-url` you give them. Docker accepts both formats and replies in the same format.

That's why **both starters work** with Docker Model Runner, and the Java code (`ChatClient`, controller) is the **same** for both.

> [!IMPORTANT]
> ### ⭐ OpenAI starter or Anthropic starter for Docker? (interview favourite)
>
> **One-line answer:** Docker Model Runner supports **both** OpenAI's and Anthropic's request formats, so **either starter works**.
> **Spring AI officially documents the OpenAI one.**
>
> These are **two separate facts**. Don't mix them up:
> 1. Docker speaks **OpenAI's format** (`/engines/v1/chat/completions`) → that's why the **OpenAI starter** works.
> 2. Docker **also** speaks **Anthropic's format** (`/engines/v1/messages`) → that's why the **Anthropic starter** works.
>
> The Anthropic starter does **not** work "because of" OpenAI compatibility. It works because Docker *separately* implements Anthropic's Messages API too.
> Both were tested in this project: each starter streamed real answers from `ai/gemma3:4b`.
>
> | | OpenAI starter | Anthropic starter |
> |---|---|---|
> | Works with Docker Model Runner? | ✅ Yes | ✅ Yes |
> | Documented by Spring AI for Docker? | ✅ Yes, the official DMR page uses it | ❌ No, not mentioned there |
> | Supported by other local tools? | Almost all (Groq, vLLM, LM Studio…) | Far fewer |
> | Risk | Low: the documented, tested path | A bit higher: Spring AI doesn't document this combination, so some features (tool calling, token usage details) might behave differently |
>
> **So the difference is not *whether* they work, but *how official* each one is.**
>
> **What the official Spring AI docs say** ([Docker Model Runner Chat](https://docs.spring.io/spring-ai/reference/api/chat/dmr-chat.html)):
> - Spring AI integrates with Docker Model Runner by *reusing the existing OpenAI-backed ChatClient*.
> - Set `spring.ai.openai.base-url` to `http://localhost:12434/engines/v1`.
> - `spring.ai.openai.api-key` can be **any string**, because Docker doesn't check it.
>
> **Why Spring AI picked the OpenAI starter for Docker:**
> 1. **OpenAI's API format is the industry standard.** Many tools (Docker Model Runner, Groq, vLLM, LM Studio and others) copy it, so any OpenAI client can talk to them.
> 2. **Less code to maintain.** Spring AI doesn't need a new starter for every OpenAI-compatible server. One well-tested client covers them all.
>    The docs do the same for **Groq**: "reusing the existing OpenAI client" with `base-url=https://api.groq.com/openai/v1`.
> 3. **Easy switching.** Local Docker in development, real OpenAI in production: **same code, same starter, different properties**.
>
> **The catch (good to mention in interviews):** "compatible" doesn't always mean *100% identical*.
> Some features may not work, depending on the server and the model. For example, tool calling works only if the model supports it,
> and the Spring AI docs sample turns off embeddings (`spring.ai.model.embedding=none`) for Docker Model Runner.
> Always test the features you need.
>
> **🎤 Say this in an interview:**
> *"The starter only decides the request format, not the model. Docker Model Runner understands both OpenAI's and Anthropic's formats,
> so both starters work. I'd pick the OpenAI starter because that's what Spring AI documents and what most local tools support,
> but I've also tested the Anthropic starter, and it works with the right base URL."*
>
> This shows you know the **official answer** *and* that you **tested it yourself**, which is a strong combination.

---

## 5. Connecting Spring AI: OpenAI starter or Anthropic starter

Both setups below were tested against Docker Model Runner with `ai/gemma3:4b`, and both stream answers.
**This project's code currently uses the Anthropic starter.** The OpenAI lines are kept, commented out, in `application.properties`.

| | **Option A: OpenAI starter** (official Spring AI way) | **Option B: Anthropic starter** (current code) |
|---|---|---|
| Dependency in `pom.xml` | `spring-ai-starter-model-openai` | `spring-ai-starter-model-anthropic` |
| Auto-created ChatModel | `OpenAiChatModel` | `AnthropicChatModel` |
| `api-key` | `spring.ai.openai.api-key=test` | `spring.ai.anthropic.api-key=test` |
| `base-url` | `spring.ai.openai.base-url=http://localhost:12434/engines/v1` | `spring.ai.anthropic.base-url=http://localhost:12434/engines` |
| `chat.model` | `spring.ai.openai.chat.model=ai/gemma3:4b` | `spring.ai.anthropic.chat.model=ai/gemma3:4b` |
| Java code | Same | Same |

### Option A: OpenAI starter
```xml
<dependency>
    <groupId>org.springframework.ai</groupId>
    <artifactId>spring-ai-starter-model-openai</artifactId>
</dependency>
```
```properties
spring.ai.openai.api-key=test
spring.ai.openai.base-url=http://localhost:12434/engines/v1
spring.ai.openai.chat.model=ai/gemma3:4b
```

### Option B: Anthropic starter (what this project uses now)
```xml
<dependency>
    <groupId>org.springframework.ai</groupId>
    <artifactId>spring-ai-starter-model-anthropic</artifactId>
</dependency>
```
```properties
spring.ai.anthropic.api-key=test
spring.ai.anthropic.base-url=http://localhost:12434/engines
spring.ai.anthropic.chat.model=ai/gemma3:4b
```

| Property | Simple meaning |
|---|---|
| `api-key` | Docker Model Runner **ignores** the key. Use a dummy value like `test`, so it's clear no real key is used. |
| `base-url` | **Where** to send requests. We change it to point at Docker instead of the vendor's cloud. See the next part for why. |
| `chat.model` | Which model to use. Must match a model from `docker model ls`. |

> ⚠️ **To switch between A and B, change the dependency in `pom.xml` too**, not just the properties.
> The properties only configure a starter. They don't add one. If both starters are on the classpath at the same time,
> Spring creates two `ChatModel` beans, and you must pick one with `spring.ai.model.chat=openai` (or `anthropic`).

---

### 🔗 Why do we need to change the URL?

There are **two** reasons. The first is about **where** the request goes. The second is about **why the two URLs look different**.

#### Reason 1: by default, the starters call the real cloud

Without `base-url`, each SDK sends requests to its company's servers:

| Starter | Default destination (no `base-url` set) | With our `base-url` |
|---|---|---|
| OpenAI | `https://api.openai.com/v1` (real OpenAI, needs a paid key) | `http://localhost:12434/engines/v1` (Docker on your laptop) |
| Anthropic | `https://api.anthropic.com` (real Claude, needs a paid key) | `http://localhost:12434/engines` (Docker on your laptop) |

So `base-url` is the switch that says: **"don't go to the cloud, go to Docker on my machine."**
With the fake key `test`, the real cloud would reject the request. Docker accepts it.

#### Reason 2: each SDK adds a different path to the base URL

The SDK builds the final URL like this:

```
final URL = base-url + path the SDK adds by itself
```

And the two SDKs add **different** paths:

| Starter | Path the SDK adds | Our `base-url` | Final URL | Result |
|---|---|---|---|---|
| OpenAI | `/chat/completions` | `http://localhost:12434/engines/v1` | `…/engines/v1/chat/completions` | ✅ works |
| Anthropic | `/v1/messages` | `http://localhost:12434/engines` | `…/engines/v1/messages` | ✅ works |

Both final URLs land under **`/engines/v1/...`**, which is where Docker listens.
The only difference is **who writes the `/v1`**:
- **OpenAI:** the `/v1` is part of OpenAI's normal base URL (`https://api.openai.com/v1`), so **you** write it.
- **Anthropic:** Anthropic's normal base URL has no `/v1` (`https://api.anthropic.com`), so **the SDK** adds it.

#### What happens if you get it wrong ❌

| Starter | Wrong `base-url` | Final URL | Result |
|---|---|---|---|
| OpenAI | `http://localhost:12434/engines` (missing `/v1`) | `…/engines/chat/completions` | **404** from Docker → **500** from your app |
| Anthropic | `http://localhost:12434/engines/v1` (extra `/v1`) | `…/engines/v1/v1/messages` | **404** from Docker → **500** from your app |

💡 **Easy way to remember:** copy the vendor's own default base URL, and replace only the **host part** with Docker's address:
- `https://api.openai.com`**`/v1`** → `http://localhost:12434/engines`**`/v1`**
- `https://api.anthropic.com` → `http://localhost:12434/engines`

The official Spring AI docs also say the OpenAI base URL "must be set to `localhost:12434/engines/v1`".

---

> 💡 **Note from the Spring AI docs:** their OpenAI sample also adds `spring.ai.model.embedding=none`, so Spring doesn't set up an
> OpenAI **embedding** model against Docker. This project only does chat, so it works without that line, but adding it is safe.

### Two other ways to enable Docker Model Runner (from the Spring AI docs)
| Way | How |
|---|---|
| **Command line** instead of the Docker Desktop settings screen | `docker desktop enable model-runner --tcp 12434` |
| **Testcontainers** (for integration tests) | Use `DockerModelRunnerContainer` and take the URL from `DMR.getOpenAIEndpoint()`, then build an `OpenAiChatModel` with that base URL and `apiKey("test")` |

### Changing the model for one request only
The model in `application.properties` is the **default**. With the OpenAI starter, you can override it per request with `OpenAiChatOptions`:
```java
chatClient.prompt()
        .user(message)
        .options(OpenAiChatOptions.builder().model("ai/llama3.2"))  // pass the builder, no .build()
        .stream()
        .content();
```
(The model must be pulled first: `docker model pull ai/llama3.2`.)

> 💡 In Spring AI 2.0, `ChatClient`'s `.options(...)` takes the options **builder**. Older 1.x examples call `.build()` first; that doesn't compile in 2.0.

### Switching to the real cloud later
Only the properties change. The Java code stays the same. Remove `base-url` so the SDK goes back to its default cloud URL, and use a real key from an environment variable:
```properties
# Option A: real OpenAI
spring.ai.openai.api-key=${OPENAI_API_KEY}
spring.ai.openai.chat.model=gpt-4o-mini

# Option B: real Claude
spring.ai.anthropic.api-key=${ANTHROPIC_API_KEY}
spring.ai.anthropic.chat.model=claude-sonnet-5-5
```

---

## 6. Streaming: getting the answer word by word

In day 1 and day 2 we used `.call()`. The app **waited** until the model finished, then returned the whole answer.
For long answers, the user stares at a blank screen for many seconds.

With `.stream()`, the app sends each piece **as soon as the model writes it**, the same way ChatGPT and Claude show text appearing.

| | `.call().content()` | `.stream().content()` |
|---|---|---|
| Return type | `String` | `Flux<String>` |
| When you get the answer | All at once, at the end | Piece by piece, while it's being written |
| User experience | Waits, then the full text appears | Text starts appearing almost immediately |
| Good for | Short answers, backend processing | Chat UIs, long answers |

### `Flux<String>` in simple words
- A `String` is **one** value.
- A `Flux<String>` is **a stream of many Strings** that arrive over time, like `"Docker"`, `" is"`, `" a"`, `" platform"`...
- `Flux` comes from **Project Reactor**. Spring AI already brings it in, so you don't add any extra dependency.

### Does Flux work in a normal Spring MVC app?
Yes. This project uses `spring-boot-starter-webmvc` (not WebFlux). When a controller returns `Flux<String>`,
Spring MVC **writes each piece to the response as it arrives** (as `text/plain`). You don't need WebFlux for this.

### `.prompt().user(message)` vs `.prompt(message)`
Both do the same thing: send `message` as the user's message.
`.prompt().user(...)` is the longer form. It's useful when you also want to add `.system(...)` instructions:
```java
chatClient.prompt()
        .system("You are a helpful Java teacher.")
        .user(message)
        .stream()
        .content();
```

---

## 7. How this project works, step by step

### Project files
```
src/main/java/org/springai/day3withdocker/
├── Day3WithDockerApplication.java     → Starts the Spring Boot app
├── config/SpringAIConfig.java         → Builds the ChatClient bean
└── controller/DockerAIController.java → Streaming REST endpoint /api/chat/v1
src/main/resources/
└── application.properties             → Docker Model Runner URL and model name
pom.xml                                → spring-ai-starter-model-anthropic dependency (or -openai, see section 5)
```

### The config (same as day 1 and day 2)
```java
@Configuration
public class SpringAIConfig {

    @Bean
    ChatClient chatClient(ChatClient.Builder chatClientBuilder) {  // Builder is auto-configured
        return chatClientBuilder.build();                          // ChatClient is not, so we build it
    }
}
```

### The controller (NEW: streaming)
```java
@RequiredArgsConstructor
@RequestMapping("/api/chat/v1")
@RestController
public class DockerAIController {

    private final ChatClient chatClient;

    @GetMapping
    public Flux<String> getMessage(@RequestParam String message) {
        return chatClient.prompt().user(message).stream().content();  // stream, not call
    }
}
```

### What happens when a request comes in 🔄
```
1. Browser/curl        →  GET /api/chat/v1?message=Hello
2. DockerAIController  →  chatClient.prompt().user("Hello").stream().content()
3. ChatClient          →  creates a Prompt and asks the ChatModel to STREAM
4. AnthropicChatModel  →  sends Anthropic-format JSON to localhost:12434/engines/v1/messages
                          (with the OpenAI starter: OpenAiChatModel → .../engines/v1/chat/completions)
5. Docker Model Runner →  gemma3:4b writes the answer, piece by piece
6. AnthropicChatModel  →  turns each piece into a Flux<String> item
7. Spring MVC          →  writes each piece to the HTTP response as it arrives ✅
```

---

## 8. How to run this project

**Step 1:** Make sure Docker Model Runner is on and the model is downloaded:
```bash
docker model ls
```
You should see `gemma3:4b`. If not, run `docker model pull ai/gemma3:4b`.

**Step 2:** Start the app (from the `day-3-with-docker` folder):
```bash
mvn spring-boot:run
```

**Step 3:** Test it. The `-N` flag tells curl **not to buffer**, so you can watch the answer stream in:
```bash
curl -N "http://localhost:8080/api/chat/v1?message=Explain%20Docker%20in%20one%20sentence"
```
(`%20` means a space inside a URL. If you see a `%` at the very end in zsh, it just means the answer had no final newline.)

---

## 9. Ollama vs Docker Model Runner

| Question | Ollama (day 2) | Docker Model Runner (day 3) |
|---|---|---|
| Install | Separate app (`brew install ollama`) | Built into Docker Desktop |
| Default port | `11434` | `12434` |
| Model names | `llama3.1`, `mistral` | `ai/gemma3:4b`, `ai/llama3.2` (from Docker Hub) |
| Download a model | `ollama pull llama3.1` | `docker model pull ai/gemma3:4b` |
| Spring AI starter | `spring-ai-starter-model-ollama` | OpenAI starter (official) or Anthropic starter (both formats work) |
| Cost / privacy | Free, local | Free, local |

**Simple rule:**
- Already using Docker for your project? → **Docker Model Runner** fits in nicely, nothing new to install.
- Want the simplest standalone tool with many models? → **Ollama**.
- Either way, your `ChatClient` code is the **same**.

---

## 10. Common mistakes and errors

| ❌ Mistake | ✅ Correct |
|---|---|
| OpenAI `base-url` without `/v1` | Use `http://localhost:12434/engines/v1`. The OpenAI SDK adds only `/chat/completions`. |
| Anthropic `base-url` **with** `/v1` | Use `http://localhost:12434/engines`. The Anthropic SDK adds `/v1/messages` itself. |
| Putting a real OpenAI or Anthropic key in this project | Not needed. Docker ignores the key, so use a dummy value like `test`. |
| Forgetting "host-side TCP support" in Docker Desktop | Turn it on, or your app can't reach port 12434. |
| Changing only the properties to switch starters | Also change the dependency in `pom.xml`. Properties configure a starter; they don't add one. |
| Testing streaming with plain `curl` and thinking it doesn't stream | Use `curl -N` so curl shows each piece immediately. |
| Thinking you need WebFlux for `Flux<String>` | Spring MVC can return `Flux<String>` and stream it too. |

### Errors you may see while running, and what they mean 🛠️
| What you see | Simple meaning | How to fix |
|---|---|---|
| **500** from your app, with a "not found" / 404 error in the log | Wrong URL. The path `/v1` is missing (OpenAI) or doubled (Anthropic) | Check the base URL against [Why do we need to change the URL?](#-why-do-we-need-to-change-the-url) |
| **Connection refused** on port 12434 | Docker Model Runner is off, or host TCP support is off | Enable both in Docker Desktop → Settings → AI, then `docker model status` |
| **Model not found** | The model isn't pulled, or the name doesn't match | `docker model pull ai/gemma3:4b`, and check `docker model ls` |
| **"Port 8080 was already in use"** | Another app (maybe an older copy of this one) is already running | Stop the other app, or run with `--server.port=8081` |

---

## 11. Interview questions and answers

**How to use this section:** say the 🟢 **short answer** first. Add the 🔵 part only if the interviewer wants more detail.

---

**Q1. What is Docker Model Runner?**

🟢 **Short answer:** It's a Docker Desktop feature that pulls and runs AI models locally, like containers, and exposes them through an HTTP API on port 12434.

🔵 **If they ask more:** Models come from Docker Hub under the `ai/` namespace, for example `ai/gemma3`. It offers OpenAI-compatible and Anthropic-compatible endpoints, so existing SDKs and Spring AI starters can use it without code changes.

---

**Q2. How can the OpenAI starter talk to a Gemma model?**

🟢 **Short answer:** Docker Model Runner accepts requests in OpenAI's API format. We point `spring.ai.openai.base-url` at Docker, so `OpenAiChatModel` sends its normal requests there.

🔵 **If they ask more:** The client only cares about the request/response format, not which model answers. This makes it easy to develop against a free local model and switch to the real OpenAI by changing only the properties.

---

**Q3. What is the difference between `.call()` and `.stream()` in ChatClient?**

🟢 **Short answer:** `.call()` waits for the full answer and returns it at once. `.stream()` returns a `Flux` that gives the answer piece by piece while the model is writing it.

🔵 **If they ask more:** `.call().content()` returns a `String`. `.stream().content()` returns `Flux<String>`. Streaming improves the user experience for long answers, because text starts appearing immediately.

---

**Q4. What is `Flux`?**

🟢 **Short answer:** `Flux` is a type from Project Reactor that represents a stream of 0 to N items arriving over time.

🔵 **If they ask more:** `Mono` is the 0-or-1 version. Spring AI uses `Flux` for streaming responses. You can transform it with operators like `map` and `filter` before returning it.

---

**Q5. Do you need Spring WebFlux to return a `Flux` from a controller?**

🟢 **Short answer:** No. Spring MVC can return `Flux<String>` and streams each item to the response as it arrives.

🔵 **If they ask more:** WebFlux is a fully non-blocking web stack. Spring MVC supports reactive return types too, as long as Reactor is on the classpath, which Spring AI already brings.

---

**Q6. Why does the OpenAI base URL need `/v1` but the Anthropic one doesn't?**

🟢 **Short answer:** Each SDK adds a different path. The OpenAI SDK adds `/chat/completions`, so `/v1` must be in the base URL. The Anthropic SDK adds `/v1/messages`, so `/v1` must not be.

🔵 **If they ask more:** Getting this wrong gives a 404, which shows up as a 500 from your app. A good habit is to check the real request path in the logs when you get a 404.

---

### ⭐ Follow-up questions: "OpenAI starter or Anthropic starter for Docker?"

Interviewers love to dig into this. Practise these together.

---

**Q7. You're running Docker, not OpenAI. Why is your dependency `spring-ai-starter-model-openai`?**

🟢 **Short answer:** Docker Model Runner exposes an OpenAI-compatible API. Spring AI's official approach is to reuse the OpenAI starter and point `spring.ai.openai.base-url` at `http://localhost:12434/engines/v1`.

🔵 **If they ask more:** The starter only knows how to speak the OpenAI HTTP format. It doesn't care who answers. That's also why the API key can be any string: Docker doesn't check it. Docker also supports Anthropic's format, so the Anthropic starter would work too, but the OpenAI starter is the documented way.

---

**Q8. Is there a separate Spring AI starter for Docker Model Runner?**

🟢 **Short answer:** No. The Spring AI reference docs integrate Docker Model Runner by reusing the existing OpenAI client.

🔵 **If they ask more:** Spring AI does the same for other OpenAI-compatible services, for example Groq, where you set `base-url=https://api.groq.com/openai/v1`. One client, many providers.

---

**Q9. What are the risks of using an "OpenAI-compatible" server?**

🟢 **Short answer:** Compatible doesn't mean identical. Some features may be missing or behave differently, so you must test the features you use.

🔵 **If they ask more:** Tool calling works only if the chosen model supports it. The Spring AI docs sample for Docker Model Runner turns off embeddings (`spring.ai.model.embedding=none`). The Groq docs also warn that Groq "is not fully compatible" with the OpenAI API.

---

**Q10. How would you move this app from Docker to the real OpenAI in production?**

🟢 **Short answer:** No Java changes. Remove `base-url` (or set it to OpenAI's), set a real key from an environment variable, and change the model name, e.g. with a Spring profile like `application-prod.properties`.

🔵 **If they ask more:** That's the main benefit of coding against `ChatClient` and the OpenAI-compatible format: dev and prod differ only in configuration.

---

**Q11. Your project works with the Anthropic starter too. How is that possible, and which would you choose?**

🟢 **Short answer:** Docker Model Runner implements both OpenAI's and Anthropic's request formats, so both starters work. I'd choose the OpenAI starter, because Spring AI documents it for Docker and most local tools support that format.

🔵 **If they ask more:** These are two separate features of Docker, not one: the Anthropic starter works because Docker also implements Anthropic's Messages API, not because of OpenAI compatibility. I tested both. The only setup difference is the base URL: OpenAI needs `/engines/v1` (the SDK adds `/chat/completions`), Anthropic needs `/engines` (the SDK adds `/v1/messages`). The Anthropic combination isn't documented by Spring AI, so I'd test features like tool calling before relying on it.

---

**Q12. How do you test code that uses Docker Model Runner?**

🟢 **Short answer:** With Testcontainers. The Spring AI docs show a `DockerModelRunnerContainer`, whose `getOpenAIEndpoint()` gives the base URL for an `OpenAiChatModel`.

🔵 **If they ask more:** That way the integration test starts what it needs by itself, and the test still uses the same OpenAI client as the real app.

---

## 12. One-minute summary

- **Docker Model Runner** = Docker Desktop feature to run AI models locally, on port **12434**.
- `docker model pull ai/gemma3:4b` downloads a model, `docker model ls` shows what you have.
- DMR speaks **OpenAI and Anthropic formats**, so **both starters work**. The starter picks the request format, not the model.
- ⭐ **Which starter?** Both work, because DMR supports both formats (two separate features). The **OpenAI starter is the official** Spring AI way; there's no separate Docker starter.
- We change `base-url` so requests go to Docker instead of the cloud. **Final URL = base-url + path the SDK adds.**
- OpenAI: `base-url=…/engines/v1` (SDK adds `/chat/completions`). Anthropic: `base-url=…/engines` (SDK adds `/v1/messages`).
- `api-key=test` (Docker ignores it), `chat.model=ai/gemma3:4b`.
- **`.stream().content()`** returns **`Flux<String>`**: the answer arrives word by word.
- Spring MVC can stream a `Flux<String>`; no WebFlux needed. Test with `curl -N`.
- `ChatClient` code is the same as day 1 and day 2. Only the starter and properties change.

---

## 13. What to learn next

- [ ] **Switch this project between the two starters** (section 5), and compare the requests in `docker model requests`
- [ ] **Server-Sent Events (SSE):** `produces = MediaType.TEXT_EVENT_STREAM_VALUE` for streaming to a browser UI
- [ ] **System prompts:** `.system("...")` to give the model a role
- [ ] **Chat memory:** let the model remember earlier messages
- [ ] **Run the whole app in Docker:** Docker Compose with your app and the model together

---

## References 📚

- [Spring AI: Docker Model Runner Chat](https://docs.spring.io/spring-ai/reference/api/chat/dmr-chat.html): the official setup with the OpenAI starter
- [Spring AI: Groq Chat](https://docs.spring.io/spring-ai/reference/api/chat/groq-chat.html): another provider that reuses the OpenAI client
- [Spring AI: ChatClient API](https://docs.spring.io/spring-ai/reference/api/chatclient.html): `.call()`, `.stream()`, options
