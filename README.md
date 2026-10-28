# Spring AI POCs

Daily proof-of-concept projects for learning **Spring AI**, one small Spring Boot app per day.
Each `day-N/` folder is a standalone Maven project with its own README explaining the concepts.

**Tech stack:** Java 21 · Spring Boot 4.1.2 · Spring AI 2.0.1 · OpenAI · Ollama

## Projects

| Day | Topic |
|-----|-------|
| [day-1](day-1/) | ChatModel, ChatClient and OpenAI basics |
| [day-2-ollama](day-2-ollama/) | Running local AI models with Ollama |

## Running a project

1. Set your OpenAI API key:

   ```bash
   export OPENAI_API_KEY=your-key-here
   ```

2. Go into a day's folder and start the app (requires Maven installed):

   ```bash
   cd day-1
   mvn spring-boot:run
   ```
