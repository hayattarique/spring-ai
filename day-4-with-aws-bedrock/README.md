# Spring AI with Amazon Bedrock: Claude, Llama & More Through AWS ☁️

A beginner-friendly guide to calling AI models through **Amazon Bedrock** with **Spring AI**, written in simple English.
Bedrock lets you use models from many companies (Anthropic Claude, Amazon Nova, Meta Llama, Mistral and others)
through **one AWS service**, with your **AWS account, AWS bill and AWS security**.

**What you will learn:**
- What Amazon Bedrock is, and why companies use it instead of calling OpenAI/Anthropic directly
- The **Converse API**, and why Spring AI uses it
- How to set up AWS credentials, region and model access
- **Model IDs vs inference profiles vs ARNs** (the most confusing part of Bedrock)
- How the **same `ChatClient` code** from day 1–3 works with Bedrock
- Common mistakes, errors, and **interview questions with answers**

**Before this:** read [day-1](../day-1/) (ChatClient basics). Days 2–3 (local models) are optional.

**Tech stack:** Spring Boot 4.1.2 · Spring AI 2.0.1 · Java 21 · Amazon Bedrock (Converse API)

---

## Table of Contents

1. [Basic words you must know](#1-basic-words-you-must-know)
2. [What is Amazon Bedrock?](#2-what-is-amazon-bedrock)
3. [Converse API vs InvokeModel API](#3-converse-api-vs-invokemodel-api)
4. [AWS setup: account, model access, IAM user](#4-aws-setup-account-model-access-iam-user)
5. [How Spring AI finds your AWS credentials](#5-how-spring-ai-finds-your-aws-credentials)
6. [Region and model ID (the confusing part)](#6-region-and-model-id-the-confusing-part)
7. [Connecting Spring AI to Bedrock](#7-connecting-spring-ai-to-bedrock)
   - ⭐ [Which Bedrock starter do I need? (3 starters, 3 different jobs)](#-which-bedrock-starter-do-i-need-3-starters-3-different-jobs)
8. [The code](#8-the-code)
9. [How to run this project](#9-how-to-run-this-project)
10. [OpenAI direct vs Bedrock](#10-openai-direct-vs-bedrock)
11. [Common mistakes and errors](#11-common-mistakes-and-errors)
12. [Interview questions and answers](#12-interview-questions-and-answers)
13. [One-minute summary](#13-one-minute-summary)
14. [What to learn next](#14-what-to-learn-next)
15. [References](#references-)

---

## 1. Basic words you must know

| Word | Simple meaning |
|---|---|
| **AWS** | Amazon Web Services: Amazon's cloud platform. |
| **Amazon Bedrock** | An AWS service that gives you AI models from many companies through one API. You pay on your AWS bill. |
| **Foundation model (FM)** | A big pre-trained AI model, e.g. Claude, Llama, Nova. Bedrock calls them "foundation models". |
| **Converse API** | Bedrock's **one common chat API** that works the same way for all chat models. |
| **IAM** | Identity and Access Management: AWS's system of users, roles and permissions. |
| **Access key / secret key** | A username/password pair that lets a program (like your Spring app) call AWS. **Never** put them in code or Git. |
| **Region** | The AWS data-centre location, e.g. `us-east-1` (Virginia), `ap-south-1` (Mumbai). Models are available per region. |
| **Model ID** | The name of a model in Bedrock, e.g. `anthropic.claude-...`. |
| **Inference profile** | A model ID with a prefix like `us.`, `eu.`, `apac.` or `global.` that lets AWS route your request across several regions. |
| **ARN** | Amazon Resource Name: a full AWS "address" like `arn:aws:bedrock:<region>:<account-id>:...`. |
| **Embedding** | Text turned into a list of numbers that captures its meaning, so similar texts can be found. |
| **Vector store** | A database that stores embeddings and finds the most similar ones. |
| **RAG** | Retrieval-Augmented Generation: find relevant document parts first, then let the AI answer using them. |

---

## 2. What is Amazon Bedrock?

Think of Bedrock as an **AI model shop inside AWS**:

```
                                   ┌──────────────── Amazon Bedrock (your AWS account) ────────────────┐
┌─────────────────────┐   HTTPS    │                                                                   │
│  Spring Boot app    │ ─────────▶ │   Converse API  ──▶  Anthropic Claude  /  Amazon Nova  /          │
│  (ChatClient)       │ ◀───────── │   (one format)       Meta Llama  /  Mistral  /  Cohere  / ...     │
└─────────────────────┘   answer   │                                                                   │
   signs requests with             │   IAM permissions · CloudWatch logs · AWS bill                    │
   your AWS credentials            └───────────────────────────────────────────────────────────────────┘
```

### Why do companies use Bedrock? ✅
| Reason | Why it matters |
|---|---|
| **Many models, one place** | Claude, Llama, Nova, Mistral… switch by changing the model ID. |
| **Already on AWS** | No new vendor contract. Usage shows up on the existing AWS bill. |
| **Security & compliance** | Uses IAM permissions, stays inside AWS, works with VPCs and AWS logging. Big companies like this. |
| **No API key per vendor** | One set of AWS credentials instead of separate OpenAI/Anthropic keys. |

### The downsides ⚠️
| Downside | Why it matters |
|---|---|
| **More setup** | AWS account, IAM user, region, model access. More steps than "paste an API key". |
| **It costs money** | You pay per token, like OpenAI. Set an AWS budget alert while learning. |
| **New features arrive later** | A brand-new model may appear on the vendor's own API before Bedrock. |

---

## 3. Converse API vs InvokeModel API

Bedrock has **two** ways to call a chat model:

| | **InvokeModel** (older) | **Converse** (newer) ✅ |
|---|---|---|
| Request format | **Different JSON for each model family** (Claude's format, Llama's format…) | **One common format** for all chat models |
| Switching models | Change code, because the JSON changes | Change only the model ID |
| Tool calling, streaming, system messages | Depends on the model | Built in, same way for all |
| Embeddings | ✅ Supported | ❌ Not supported |

**What the Spring AI docs say** ([Bedrock Converse API](https://docs.spring.io/spring-ai/reference/api/chat/bedrock-converse.html)):
- Following Bedrock's recommendation, Spring AI is moving **all chat** to the Converse API, and strongly recommends it.
- The Converse API **doesn't support embeddings**, so embeddings still use the older InvokeModel API.

👉 That's why this project uses **`spring-ai-starter-model-bedrock-converse`**.

> [!IMPORTANT]
> ### ⭐ Why the Converse API? (interview favourite)
>
> **One-line answer:** Converse gives **one request format for every chat model** on Bedrock, so switching from Claude to Llama to Nova is just a **model ID change**, not a code change.
>
> This is the same idea as Spring AI's `ChatClient`, but one level lower:
> - **Spring AI** hides the differences between **vendors** (OpenAI vs Anthropic vs Bedrock).
> - **Bedrock Converse** hides the differences between **models inside Bedrock** (Claude vs Llama vs Nova).
>
> **Follow-up trap:** *"Can I use Converse for embeddings (RAG)?"* → **No.** Converse is chat only. Embeddings use the InvokeModel-based embedding models.

---

## 4. AWS setup: account, model access, IAM user

You need an AWS account. Then do these three things once.

### Step 1: Turn on access to the model
- Open the **AWS Console → Amazon Bedrock**.
- Pick your **region** in the top-right corner (e.g. `us-east-1` or `ap-south-1`). Access is **per region**.
- Open **Model access** (or the model catalog) and make sure the model you want is **enabled** for your account.
  Anthropic models may ask you to fill in a short use-case form the first time.

### Step 2: Create an IAM user (or role) with Bedrock permission
Your app needs permission to call the model. The two actions it uses are:

| IAM action | Used for |
|---|---|
| `bedrock:InvokeModel` | `.call()`: normal request (Converse) |
| `bedrock:InvokeModelWithResponseStream` | `.stream()`: streaming request (ConverseStream) |

For learning, you can attach the AWS-managed policy **`AmazonBedrockFullAccess`** to an IAM user.
In real projects, give **only** the two actions above, and only for the models you use (**least privilege**).

### Step 3: Create an access key for that IAM user
**IAM → Users → your user → Security credentials → Create access key.**
You get an **access key ID** (starts with `AKIA…`) and a **secret access key** (shown only once). Treat them like a password.

### Step 4 (recommended): Set a budget alert 💰
**AWS Billing → Budgets → Create budget**, e.g. alert me at $5. Bedrock charges per token, just like OpenAI.

---

## 5. How Spring AI finds your AWS credentials

Spring AI's Bedrock auto-configuration checks these **in order**, and uses the **first** one it finds:

| Order | Where the credentials come from | When it's used |
|---|---|---|
| 1 | **Properties:** `spring.ai.bedrock.aws.access-key` + `secret-key` (+ `session-token` for temporary credentials) | You set them in `application.properties` |
| 2 | **AWS profile:** `spring.ai.bedrock.aws.profile.name` | You set a profile name from `~/.aws/credentials` |
| 3 | **AWS SDK default chain** | Neither of the above is set. AWS looks in env variables (`AWS_ACCESS_KEY_ID`, `AWS_SECRET_ACCESS_KEY`), `~/.aws/credentials`, and IAM roles (on EC2, ECS, Lambda) |

### Option A: properties that read environment variables (this project)
```properties
spring.ai.bedrock.aws.access-key=${AWS_ACCESS_KEY_ID}
spring.ai.bedrock.aws.secret-key=${AWS_SECRET_ACCESS_KEY}
```
```bash
export AWS_ACCESS_KEY_ID=AKIA...
export AWS_SECRET_ACCESS_KEY=...
```
The `${...}` means "read this from an environment variable", so the real key **never** goes into Git.

### Option B: no credential properties at all (the cleanest)
If you **don't** set `access-key`/`secret-key`, Spring AI falls back to the AWS default chain.
With the **standard** env variable names (`AWS_ACCESS_KEY_ID`, `AWS_SECRET_ACCESS_KEY`), or after `aws configure`, it just works.
On AWS servers (EC2/ECS/Lambda), an **IAM role** is used automatically, with no keys at all. That's the **best practice in production**.

> ⚠️ **Use the standard AWS variable names.** `${AWS_ACCESS_KEY_ID}` and `${AWS_SECRET_ACCESS_KEY}` work with every AWS tool.
> A made-up name (or one with a typo or a space, like `${AWS ACCESS.KEY}`) is never set by AWS tools.
>
> **Watch out: the app still starts.** If the variable isn't set, Spring passes the text `${...}` through **literally** as the key.
> You only find out on the **first request**, which fails with **403** (tested):
> - variable not set → *"The security token included in the request is invalid"*
> - name with a space, like `${AWS ACCESS.KEY}` → *"Invalid key=value pair (missing equal-sign) in Authorization header"*

---

## 6. Region and model ID (the confusing part)

### Region
```properties
spring.ai.bedrock.aws.region=us-east-1
```
- The default is **`us-east-1`**. If your model access, inference profile or ARN is in another region (e.g. `ap-south-1`), **set the region to match**.
- A model ID that works in one region can fail in another.

### Three ways to name a model 🏷️

| Form | Example | When to use |
|---|---|---|
| **Base model ID** | `anthropic.claude-haiku-4-5-20251001-v1:0` | Only for models that support direct ("on-demand") calls in your region |
| **Inference profile ID** (has a prefix) | `us.anthropic.claude-haiku-4-5-20251001-v1:0` | ✅ Most common. Required by many newer models. AWS can route the request across regions |
| **ARN** | `arn:aws:bedrock:<region>:<account-id>:inference-profile/<profile-id>` | Works too, but long, tied to one region, and contains your **AWS account ID** |

### What the prefix means
| Prefix | Meaning |
|---|---|
| `us.` | Route across **US** regions |
| `eu.` | Route across **Europe** regions |
| `apac.` | Route across **Asia-Pacific** regions |
| `global.` | Route across regions **worldwide** |
| `au.` | Route across **Australia** regions (e.g. from `ap-southeast-2`) |

AWS adds new prefixes over time. The Bedrock console shows the ones available for your model and region.

👉 **Simple rule:** in the Bedrock console, open your model and copy its **inference profile ID**. Put that in `spring.ai.bedrock.converse.chat.model`.
Prefer the **ID** over the full **ARN**: it's shorter and doesn't expose your account ID in Git.

> 💡 If you see *"Invocation of model ID … with on-demand throughput isn't supported"*, you used a **base model ID** for a model
> that needs an **inference profile**. Add the prefix (e.g. `us.` or `global.`).

---

## 7. Connecting Spring AI to Bedrock

### The dependency (`pom.xml`)
```xml
<dependency>
    <groupId>org.springframework.ai</groupId>
    <artifactId>spring-ai-starter-model-bedrock-converse</artifactId>
</dependency>
```
Spring Boot sees this starter and **auto-creates a `BedrockProxyChatModel` bean** (Spring AI's Bedrock Converse chat model),
plus a `ChatClient.Builder` connected to it.

### ⭐ Which Bedrock starter do I need? (3 starters, 3 different jobs)

When you search Maven for "Spring AI Bedrock", you'll find **three** starters. Their names look alike, but they do **completely different jobs**.
They don't replace each other. Think of them as three different tools in a toolbox:

| Starter | Simple name | What it does | Main class inside |
|---|---|---|---|
| `spring-ai-starter-model-bedrock-converse` | 💬 **The talker** | **Chat**: you send a question, the AI writes an answer (Claude, Nova, Llama…) | `BedrockProxyChatModel` |
| `spring-ai-starter-model-bedrock` | 🔢 **The translator** | **Embeddings only**: turns text into a list of numbers so similar texts can be found later (Titan, Cohere) | `BedrockTitanEmbeddingModel`, `BedrockCohereEmbeddingModel` |
| `spring-ai-starter-vector-store-bedrock-knowledgebase` | 📚 **The librarian** | **Searches your documents** stored in an AWS **Bedrock Knowledge Base** | `BedrockKnowledgeBaseVectorStore` |

(Checked against the Spring AI **2.0.1** jars on Maven Central.)

#### 💬 1. `bedrock-converse`: the talker (this project uses it)
- Uses the **Converse API** ([section 3](#3-converse-api-vs-invokemodel-api)), Bedrock's single chat format for all models.
- This is what makes `chatClient.prompt("Hi").call()` work.
- ✅ **For day 4 (chat only), this is the only starter you need.**

#### 🔢 2. `model-bedrock`: the translator (embeddings, NOT chat)
- ⚠️ **Trap:** the name sounds like "the main Bedrock starter", but in Spring AI 2.0 it has **no chat model at all**, only **embedding** models.
- **What is an embedding?** It turns text into numbers that capture its *meaning*:
  ```
  "How do I reset my password?"   →  [0.12, -0.83, 0.44, ...]
  "I forgot my login password"    →  [0.11, -0.80, 0.47, ...]   ← very similar numbers = similar meaning
  "What's the weather today?"     →  [-0.65, 0.22, -0.31, ...]  ← very different numbers
  ```
- **Why is it a separate starter?** Because the Converse API **can't do embeddings**. So embeddings still use Bedrock's older **InvokeModel** API, which lives in this starter.

#### 📚 3. `vector-store-bedrock-knowledgebase`: the librarian (search your documents)
- A **Bedrock Knowledge Base** is AWS's managed "document search" service. You put your files (e.g. PDFs in an S3 bucket) into it,
  and AWS **splits them into chunks, creates embeddings, and indexes them** for you.
- This starter lets Spring AI **search** that knowledge base through Spring AI's normal `VectorStore` interface (e.g. `similaritySearch(...)`).
- Usually AWS handles loading the documents (syncing from S3), and your app mainly **asks questions and searches**.

#### 🧩 How the three work together: RAG ("chat with my documents")
**RAG** = *Retrieval-Augmented Generation*: first **find** the relevant parts of your documents, then let the AI **answer using them**.
```
User: "What is our leave policy?"
        │
        ▼
📚 Knowledge Base vector store   →  finds the matching chunks from your HR PDFs        (starter 3)
        │   (AWS uses embeddings behind the scenes; starter 2 is only needed
        │    if YOU create embeddings yourself, e.g. for another vector store like PGVector)
        ▼
💬 Converse chat model           →  writes an answer using those chunks                (starter 1)
        │
        ▼
Answer: "Employees get 24 days of paid leave per year…"
```

| What you're building | Starters you need |
|---|---|
| A chatbot (this project, day 4) | `bedrock-converse` only |
| "Chat with my documents" using a Bedrock Knowledge Base | `bedrock-converse` + `vector-store-bedrock-knowledgebase` |
| "Chat with my documents" using your own vector store (PGVector, Redis…) with Bedrock embeddings | `bedrock-converse` + `model-bedrock` + that vector store's starter |

> [!IMPORTANT]
> **🎤 Say this in an interview:**
> *"`bedrock-converse` is for **chat** through the Converse API. `model-bedrock` is now only for **embeddings** (Titan, Cohere),
> because Converse doesn't support embeddings. The knowledge base starter is a **VectorStore** backed by a Bedrock Knowledge Base,
> used for **RAG**. For a plain chatbot, only `bedrock-converse` is needed."*

### The properties (`application.properties`)
```properties
spring.ai.model.chat=bedrock-converse

# AWS connection
spring.ai.bedrock.aws.region=us-east-1
spring.ai.bedrock.aws.access-key=${AWS_ACCESS_KEY_ID}
spring.ai.bedrock.aws.secret-key=${AWS_SECRET_ACCESS_KEY}

# Which model
spring.ai.bedrock.converse.chat.model=us.anthropic.claude-haiku-4-5-20251001-v1:0
```

| Property | Simple meaning |
|---|---|
| `spring.ai.model.chat=bedrock-converse` | "Use Bedrock Converse as the chat model." This is the default anyway; it matters when you have several AI starters. |
| `spring.ai.bedrock.aws.region` | AWS region to call. Default `us-east-1`. Must match where your model access is. |
| `spring.ai.bedrock.aws.access-key` / `secret-key` | AWS credentials. Read them from env variables, never hard-code them. |
| `spring.ai.bedrock.aws.session-token` | Only for **temporary** credentials (e.g. from AWS SSO). |
| `spring.ai.bedrock.aws.profile.name` | Use a named profile from `~/.aws/credentials` instead of keys. |
| `spring.ai.bedrock.converse.chat.model` | The model ID or inference profile ID ([section 6](#6-region-and-model-id-the-confusing-part)). **There is no default; you must set it.** |
| `spring.ai.bedrock.converse.chat.temperature` | Creativity: lower = more focused, higher = more random. |
| `spring.ai.bedrock.converse.chat.max-tokens` | Maximum length of the answer. Also limits your cost per request. |
| `spring.ai.bedrock.aws.timeout` | Max time for the whole call (default `5m`). |

---

## 8. The code

Good news: the Java code is the **same pattern as day 1–3**. Only the dependency and the properties changed.

### Project files
```
src/main/java/com/example/demo/
├── BedrockApplication.java        → Starts the Spring Boot app
├── config/SpringAIConfig.java     → Builds the ChatClient bean
└── controller/ChatController.java → REST endpoint /api/v1/chat
src/main/resources/
└── application.properties         → AWS region, credentials (from env variables) and model ID
pom.xml                            → spring-ai-starter-model-bedrock-converse dependency
```

### The config
```java
@Configuration
public class SpringAIConfig {

    @Bean
    ChatClient chatClient(ChatClient.Builder builder) {  // Builder is auto-configured (connected to Bedrock)
        return builder.build();
    }
}
```

### The controller
```java
@RestController
@RequestMapping("/api/v1/chat")
@RequiredArgsConstructor
public class ChatController {

    private final ChatClient chatClient;

    @GetMapping
    public String chat(@RequestParam String message) {
        return chatClient.prompt(message).call().content();
    }
}
```

### Optional: add streaming (see day 3)
Bedrock Converse supports streaming too. Add this method to `ChatController` to get the answer piece by piece:
```java
@GetMapping("/stream")
public Flux<String> stream(@RequestParam String message) {
    return chatClient.prompt(message).stream().content();
}
```
Streaming needs the extra IAM permission `bedrock:InvokeModelWithResponseStream`.

### Changing the model or settings for one request
Use `BedrockChatOptions`. In Spring AI 2.0, `.options(...)` takes the **builder** (no `.build()`):
```java
chatClient.prompt()
        .user(message)
        .options(BedrockChatOptions.builder()
                .model("us.anthropic.claude-haiku-4-5-20251001-v1:0")
                .temperature(0.3)
                .maxTokens(300))
        .call()
        .content();
```
> 💡 The Spring AI docs page still shows `.options(options)` with an already-built object. That's the 1.x style and doesn't compile in 2.0.1.
> Pass the builder, as above.

### What happens when a request comes in 🔄
```
1. Browser/curl          →  GET /api/v1/chat?message=Hello
2. Controller            →  chatClient.prompt("Hello").call().content()
3. ChatClient            →  creates a Prompt and passes it to the ChatModel
4. BedrockProxyChatModel →  builds a Converse request, signs it with your AWS credentials,
                            and sends it to bedrock-runtime in your region
5. Amazon Bedrock        →  checks IAM permission + model access, runs the model (e.g. Claude)
6. BedrockProxyChatModel →  converts the reply into a ChatResponse
7. ChatClient            →  .content() takes out only the answer text ✅
```

---

## 9. How to run this project

**Step 1:** Set your AWS credentials (same terminal where you start the app):
```bash
export AWS_ACCESS_KEY_ID=your-access-key-id
export AWS_SECRET_ACCESS_KEY=your-secret-access-key
```

**Step 2:** Check `application.properties`: the **region** matches your model access, and **`chat.model`** is a model or inference profile ID you have access to.

**Step 3:** Start the app (from the `day-4-with-aws-bedrock` folder, in the **same terminal** as step 1):
```bash
mvn spring-boot:run
```

**Step 4:** Test it:
```bash
curl "http://localhost:8080/api/v1/chat?message=What%20is%20Amazon%20Bedrock"
```
If you added the optional streaming method:
```bash
curl -N "http://localhost:8080/api/v1/chat/stream?message=Explain%20IAM%20in%20one%20sentence"
```

> 💡 **The app starting does NOT mean AWS is set up correctly.** Spring AI doesn't contact AWS at startup.
> Wrong keys, region or model ID only show up on the **first request**. Always test with `curl`.

---

## 10. OpenAI direct vs Bedrock

| Question | OpenAI / Anthropic directly | Amazon Bedrock |
|---|---|---|
| Models | One vendor's models | Many vendors (Claude, Llama, Nova, Mistral…) |
| Login | Vendor API key | AWS credentials / IAM role |
| Billing | Vendor's bill | Your AWS bill |
| Security | Vendor's controls | IAM, VPC, AWS logging and compliance |
| Setup | Very easy | More steps (IAM, region, model access) |
| Spring AI starter | `spring-ai-starter-model-openai` / `-anthropic` | `spring-ai-starter-model-bedrock-converse` |
| `ChatClient` code | Same | **Same** ✅ |

**Simple rule:**
- Startup or side project, one model? → call the vendor directly (simpler).
- Company already on AWS, needs IAM, compliance, or several model vendors? → **Bedrock**.

---

## 11. Common mistakes and errors

| ❌ Mistake | ✅ Correct |
|---|---|
| Real AWS keys in `application.properties` | Use `${AWS_ACCESS_KEY_ID}` / `${AWS_SECRET_ACCESS_KEY}`, or no key properties at all (default chain). |
| Made-up or misspelled env variable names | Use the standard names: `AWS_ACCESS_KEY_ID`, `AWS_SECRET_ACCESS_KEY`, `AWS_SESSION_TOKEN`. |
| Not setting the region | Default is `us-east-1`. Set it to where your model access / inference profile is. |
| Using a full ARN in Git | Prefer the inference profile **ID**. The ARN contains your AWS account ID. |
| Forgetting `spring.ai.bedrock.converse.chat.model` | It has no default. Set it to a model you have access to. |
| Thinking Converse supports embeddings | It doesn't. Converse is chat only. |
| No budget alert | Bedrock is pay-per-token. Set an AWS budget while learning. |

### Errors you may see, and what they mean 🛠️
| What you see | Simple meaning | How to fix |
|---|---|---|
| App starts, but every request returns **500**, with **403 "The security token included in the request is invalid"** in the log | The env variable isn't set in this terminal (Spring sent the literal text `${...}` as the key), or the key is wrong/deleted | `export` the variables in the **same terminal** before starting the app; check the keys; for temporary credentials also set `session-token` |
| **403 "Invalid key=value pair (missing equal-sign) in Authorization header"** | The access key value contains invalid characters, usually a broken placeholder like `${AWS ACCESS.KEY}` | Use `${AWS_ACCESS_KEY_ID}` exactly |
| **AccessDeniedException** | Your IAM user has no Bedrock permission, or model access isn't enabled | Add `bedrock:InvokeModel` (+ `InvokeModelWithResponseStream`), and enable the model in the Bedrock console |
| **ValidationException: "…on-demand throughput isn't supported…"** | This model needs an **inference profile** | Use the ID with a prefix, e.g. `us.anthropic...` |
| **ValidationException / ResourceNotFound** about the model ID | Wrong model ID, or the model/profile isn't in your configured region | Copy the ID from the console, and set `spring.ai.bedrock.aws.region` to match |
| **ThrottlingException** | Too many requests for your account's quota | Slow down, or request a quota increase in AWS Service Quotas |

---

## 12. Interview questions and answers

**How to use this section:** say the 🟢 **short answer** first. Add the 🔵 part only if the interviewer wants more detail.

---

**Q1. What is Amazon Bedrock?**

🟢 **Short answer:** A fully managed AWS service that gives access to foundation models from several companies (Anthropic, Amazon, Meta, Mistral and others) through one API, billed to your AWS account.

🔵 **If they ask more:** Because it's inside AWS, you get IAM permissions, regional control, AWS logging and compliance, which is why enterprises already on AWS prefer it over calling each vendor directly.

---

**Q2. What is the Converse API, and why does Spring AI use it?**

🟢 **Short answer:** Converse is Bedrock's single, model-independent chat API. Spring AI uses it because one request format covers all chat models, plus tool calling, streaming and system messages.

🔵 **If they ask more:** The older InvokeModel API needs different JSON per model family. Following AWS's recommendation, Spring AI is moving all Bedrock chat to Converse. Embeddings aren't supported by Converse, so they stay on InvokeModel.

---

**Q3. Which Spring AI starter do you use for Bedrock, and what bean does it create?**

🟢 **Short answer:** `spring-ai-starter-model-bedrock-converse`. It auto-creates `BedrockProxyChatModel` and a `ChatClient.Builder` connected to it.

🔵 **If they ask more:** It also creates the AWS `credentialsProvider` and `regionProvider` beans from the `spring.ai.bedrock.aws.*` properties. My `ChatClient` code is identical to the OpenAI version.

---

**Q4. How does your app authenticate with Bedrock?**

🟢 **Short answer:** With AWS credentials. Spring AI uses access/secret keys from properties if set, otherwise a named profile, otherwise the AWS SDK default chain.

🔵 **If they ask more:** The default chain checks environment variables, `~/.aws/credentials` and IAM roles. In production on AWS I'd use an **IAM role** with no keys at all. Locally I read keys from env variables, never from Git.

---

**Q5. What's the difference between a model ID and an inference profile?**

🟢 **Short answer:** A model ID names a model in one region. An inference profile (prefix `us.`, `eu.`, `apac.`, `global.`) lets Bedrock route requests across multiple regions for better availability.

🔵 **If they ask more:** Many newer models can only be called through an inference profile. Using the plain model ID then fails with "on-demand throughput isn't supported".

---

**Q6. What IAM permissions does the app need?**

🟢 **Short answer:** `bedrock:InvokeModel` for normal calls and `bedrock:InvokeModelWithResponseStream` for streaming.

🔵 **If they ask more:** Follow least privilege: allow only those actions, only on the models you use. Also make sure model access is enabled in that region.

---

**Q7. Why use Bedrock instead of calling Anthropic's API directly for Claude?**

🟢 **Short answer:** Enterprise reasons: one AWS bill, IAM security, data staying inside AWS, compliance, and access to many model vendors behind one API.

🔵 **If they ask more:** The trade-off is more setup, and sometimes new features arrive later than on the vendor's own API. With Spring AI, switching between them is mostly a dependency and properties change.

---

**Q8. Your app returns AccessDeniedException from Bedrock. How do you debug it?**

🟢 **Short answer:** Check three things: the IAM user/role has `bedrock:InvokeModel`, model access is enabled for that model, and the app calls the **same region** where access is enabled.

🔵 **If they ask more:** Then check the model ID: it must be a valid ID or inference profile for that region. AWS CloudTrail shows the denied call and which identity made it.

---

**Q9. What's the difference between `spring-ai-starter-model-bedrock`, `-model-bedrock-converse` and `-vector-store-bedrock-knowledgebase`?**

🟢 **Short answer:** `bedrock-converse` is for chat. `model-bedrock` is only for embeddings (Titan, Cohere). The knowledge base starter is a `VectorStore` that searches a Bedrock Knowledge Base, used for RAG.

🔵 **If they ask more:** `model-bedrock` still exists because the Converse API doesn't support embeddings, so embeddings use the older InvokeModel API. For a simple chatbot only `bedrock-converse` is needed. For RAG with a Bedrock Knowledge Base you add the knowledge base starter. You add `model-bedrock` only if you create embeddings yourself, e.g. for PGVector.

---

## 13. One-minute summary

- **Amazon Bedrock** = AWS service for many AI models (Claude, Nova, Llama, Mistral…) behind one API, on your AWS bill.
- **Converse API** = one chat format for all Bedrock models. Spring AI uses it. No embeddings.
- Starter: **`spring-ai-starter-model-bedrock-converse`** → auto-creates **`BedrockProxyChatModel`**.
- ⭐ **3 Bedrock starters, 3 jobs:** `bedrock-converse` = chat 💬, `model-bedrock` = embeddings only 🔢, `vector-store-bedrock-knowledgebase` = search documents 📚 (RAG).
- Credentials: properties → profile → **AWS default chain** (env vars, `~/.aws`, IAM role). Never commit keys.
- **Region** defaults to `us-east-1`; set it to match your model access.
- **`chat.model`** has no default. Prefer an **inference profile ID** (`us.`, `global.`…) over a full ARN.
- IAM: `bedrock:InvokeModel` + `bedrock:InvokeModelWithResponseStream`.
- **`ChatClient` code is the same as day 1–3.** Only the starter and properties change.

---

## 14. What to learn next

- [ ] **Switch models** by only changing `chat.model` (e.g. Claude → Amazon Nova → Llama) and compare answers
- [ ] **Tool calling** with Bedrock: let the model call your Java methods
- [ ] **Prompt caching** (`BedrockCacheOptions`): save cost when you repeat a large system prompt
- [ ] **Bedrock embeddings** (Titan / Cohere, `spring-ai-starter-model-bedrock`): the first step toward RAG on AWS
- [ ] **RAG with a Bedrock Knowledge Base** (`spring-ai-starter-vector-store-bedrock-knowledgebase`): chat with your own documents
- [ ] **Run on AWS with an IAM role**: no access keys at all

---

## References 📚

- [Spring AI: Bedrock Converse API](https://docs.spring.io/spring-ai/reference/api/chat/bedrock-converse.html): official setup, properties, options, prompt caching
- [Amazon Bedrock User Guide](https://docs.aws.amazon.com/bedrock/latest/userguide/what-is-bedrock.html): AWS's own documentation
- [Spring AI: ChatClient API](https://docs.spring.io/spring-ai/reference/api/chatclient.html): `.call()`, `.stream()`, options
