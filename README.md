# Spring AI POCs

Daily proof-of-concept projects for learning **Spring AI**, one small Spring Boot app per day.
Each folder is a standalone Maven project with its own README explaining the concepts, with interview questions, a one-minute summary and (in most) a self-test for revision.

**Tech stack:** Java 21 (17 for the RAG project) · Spring Boot 4.1.2 · Spring AI 2.0.1 · OpenAI · Ollama · Docker Model Runner · Amazon Bedrock · PostgreSQL · Qdrant · Redis · Tavily

## Projects

Read them in this order. Each one builds on the one before it.

| # | Project | Topic | Needs |
|---|---------|-------|-------|
| 1 | [day-1](day-1/) | ChatModel, ChatClient and OpenAI basics | `OPENAI_API_KEY` |
| 2 | [day-2-ollama](day-2-ollama/) | Running local AI models with Ollama | Ollama |
| 3 | [day-3-with-docker](day-3-with-docker/) | Docker Model Runner, OpenAI-compatible API and streaming | Docker Desktop |
| 4 | [day-4-with-aws-bedrock](day-4-with-aws-bedrock/) | Amazon Bedrock (Converse API), AWS credentials and model IDs | AWS keys |
| 5 | [spring-ai-prompt-template](spring-ai-prompt-template/) | Prompt templates, `.st` files, prompt stuffing and token logging | `OPEN_AI_KEY` |
| 6 | [converters](converters/) | Structured output: `.entity(...)`, Bean/Map/List output converters | `OPEN_AI_KEY` |
| 7 | [chat-memory-day-7](chat-memory-day-7/) | Chat memory in PostgreSQL, conversation ID advisor, advisor order | `OPEN_AI_KEY`, PostgreSQL |
| 8 | [basic-rag-day-8](basic-rag-day-8/) | RAG with Qdrant, Tavily web search, semantic cache in Redis, PII masking | `OPEN_AI_KEY`, `TAVILY_API_KEY`, Docker |

## Quick revision map

| Concept | Where to revise it |
|---|---|
| `ChatModel` vs `ChatClient`, `content()` vs `chatResponse()` | [day-1](day-1/) |
| Switching providers by changing the starter + properties | [day-2](day-2-ollama/), [day-3](day-3-with-docker/), [day-4](day-4-with-aws-bedrock/) |
| Streaming with `Flux<String>` | [day-3](day-3-with-docker/), [chat-memory-day-7](chat-memory-day-7/) |
| `defaultSystem`, `defaultOptions`, `.st` files, `{variables}` | [spring-ai-prompt-template](spring-ai-prompt-template/) |
| Advisors: custom `CallAdvisor` / `StreamAdvisor`, ordering | [spring-ai-prompt-template](spring-ai-prompt-template/), [chat-memory-day-7](chat-memory-day-7/) |
| Getting Java objects from the AI | [converters](converters/) |
| Conversation memory, `ChatMemory`, JDBC repository | [chat-memory-day-7](chat-memory-day-7/) |
| Embeddings, vector store, `RetrievalAugmentationAdvisor`, semantic cache | [basic-rag-day-8](basic-rag-day-8/) |

## Running a project

1. Set the environment variables the project needs (see the **Needs** column). ⚠️ The API key name is **not the same** in every project:

   | Projects | Variable |
   |---|---|
   | `day-1` | `OPENAI_API_KEY` |
   | `spring-ai-prompt-template`, `converters`, `chat-memory-day-7`, `basic-rag-day-8` | `OPEN_AI_KEY` |

   ```bash
   export OPENAI_API_KEY=your-key-here
   ```

   ```bash
   export OPEN_AI_KEY=your-key-here
   ```

2. Go into a project's folder and start the app (requires Maven installed):

   ```bash
   cd day-1
   mvn spring-boot:run
   ```

Each project's README has a **How to run this project** section with the exact steps and `curl` commands.

> 🔐 Never commit API keys. Read them from environment variables with `${VARIABLE_NAME}` in `application.properties`.
