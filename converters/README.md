# Spring AI Structured Output: Turning AI Text into Java Objects 🔄

A beginner-friendly guide to **output converters** in Spring AI, written in simple English.
Instead of getting a plain `String` back from the AI, you get a `List`, a `Map`, or your own Java class.

**What you will learn:**
- Why `.content()` (a plain `String`) is not enough for real apps
- What **structured output** and **output converters** are
- The three converters: `BeanOutputConverter`, `MapOutputConverter`, `ListOutputConverter`
- How `.entity(...)` works behind the scenes (format instructions + JSON Schema)
- How to get a `List<YourRecord>` with `ParameterizedTypeReference`
- Spring AI 2.0 extras: native (provider) structured output and schema validation
- Common mistakes, errors, and **interview questions with answers**

**Before this:** read [day-1](../day-1/) (ChatClient basics) and [spring-ai-prompt-template](../spring-ai-prompt-template/) (prompts).

**Tech stack:** Spring Boot 4.1.2 · Spring AI 2.0.1 · Java 21 · OpenAI

### What is in this project and what is not

| Topic | Status |
|---|---|
| `.entity(List.class)`: get a raw `List` back from the AI | ✅ In the code |
| `POST` endpoint that takes the prompt as the request body | ✅ In the code |
| `.entity(MyRecord.class)` with your own class | 📘 Explained here with examples, not in the code yet |
| `List<MyRecord>` with `ParameterizedTypeReference` | 📘 Explained here, not in the code yet |
| `ListOutputConverter` / `MapOutputConverter` | 📘 Explained here, not in the code yet |
| Native structured output and `validateSchema()` | 📘 Explained here, not in the code yet |

[Section 7](#7-upgrade-this-project-step-by-step) shows how to add the 📘 parts yourself.

---

## Table of Contents

1. [Basic words you must know](#1-basic-words-you-must-know)
2. [Why do we need structured output?](#2-why-do-we-need-structured-output)
3. [How structured output works](#3-how-structured-output-works)
4. [The three output converters](#4-the-three-output-converters)
5. [How this project works](#5-how-this-project-works)
6. [All the ways to call `.entity(...)`](#6-all-the-ways-to-call-entity)
7. [Upgrade this project step by step](#7-upgrade-this-project-step-by-step)
8. [Spring AI 2.0 extras: native output and validation](#8-spring-ai-20-extras-native-output-and-validation)
9. [How to run this project](#9-how-to-run-this-project)
10. [Common mistakes and errors](#10-common-mistakes-and-errors)
11. [Interview questions and answers](#11-interview-questions-and-answers)
12. [One-minute summary](#12-one-minute-summary)
13. [Self-test](#13-self-test)
14. [What to learn next](#14-what-to-learn-next)
15. [References](#references-)

---

## 1. Basic words you must know

| Word | Simple meaning |
|---|---|
| **Structured output** | Asking the AI to reply in a fixed shape (JSON) so your code can read it, instead of free text. |
| **Output converter** | A Spring AI class that (1) tells the AI what format to use and (2) turns the reply text into a Java object. |
| **`StructuredOutputConverter<T>`** | The interface all converters implement. It has `getFormat()` and `convert(String)`. |
| **Format instructions** | Extra text Spring AI adds to your prompt, like "reply only in JSON matching this schema". |
| **JSON Schema** | A description of a JSON shape: which fields, which types. Spring AI generates it from your Java class. |
| **`.entity(...)`** | The `ChatClient` method that uses a converter and returns a Java object instead of a `String`. |
| **Record** | A short Java class for holding data: `record Country(String name, String capital) {}`. Perfect for AI output. |
| **`ParameterizedTypeReference`** | A Spring trick to keep generic types like `List<Country>` at runtime (Java normally forgets the `<Country>` part). |
| **Deserialize** | Turning JSON text into a Java object. Spring AI uses Jackson for this. |

---

## 2. Why do we need structured output?

With `.content()` you get a `String`:

```
Here are 3 countries in Asia:
1. India - capital New Delhi
2. Japan - capital Tokyo
3. Nepal - capital Kathmandu
Let me know if you need more!
```

This is nice for a human, but **bad for code**:

| Problem with plain text | What structured output gives you |
|---|---|
| You must parse the text yourself with `split()` or regex | You get a ready `List<Country>` |
| The AI changes the format from run to run ("1." vs "-") | The format is fixed by a JSON Schema |
| Extra chatty lines ("Let me know if...") break parsing | The prompt tells the AI: JSON only, no extra text |
| You can't easily return it as JSON from your REST API | Spring turns the Java object into clean JSON for you |

**Simple rule:** if a **human** reads the answer, use `.content()`. If **code** reads the answer, use `.entity(...)`.

---

## 3. How structured output works

The AI model itself only returns **text**. Spring AI does two jobs around the call:

```
Your code:  chatClient.prompt().user("List 3 countries in Asia").call().entity(Country[].class)
                │
                ▼
① BEFORE the call: the converter adds FORMAT INSTRUCTIONS to the prompt
   "Your response should be in JSON format ... Here is the JSON Schema
    instance your output must adhere to: { ...schema made from Country... }"
                │
                ▼
② The model replies with JSON TEXT
   [{"name":"India","capital":"New Delhi"}, {"name":"Japan","capital":"Tokyo"}, ...]
                │
                ▼
③ AFTER the call: the converter DESERIALIZES the text (Jackson)
   → Country[] { Country[name=India, capital=New Delhi], ... }  ✅
```

So a converter is two things in one:

| Method | When it runs | What it does |
|---|---|---|
| `getFormat()` | Before the call | Returns the instructions that are added to your prompt |
| `convert(String text)` | After the call | Turns the reply text into the Java type |

> 💡 The model is only **asked** to follow the format. It is not forced (unless you use native structured output, see [section 8](#8-spring-ai-20-extras-native-output-and-validation)). That's why a bad reply can still cause a parsing error.

---

## 4. The three output converters

| Converter | Gives you | Format it asks for | Use it when |
|---|---|---|---|
| **`BeanOutputConverter<T>`** | Your class / record (or `List<YourClass>`) | JSON that matches a JSON Schema made from your class | You know the exact fields. **The one you'll use most.** |
| **`MapOutputConverter`** | `Map<String, Object>` | Any JSON object | The fields are dynamic or unknown |
| **`ListOutputConverter`** | `List<String>` | Comma-separated values: `foo, bar, baz` | You only need a simple list of words |

You often don't create them yourself. `.entity(...)` picks the right one:

| You write | Spring AI uses |
|---|---|
| `.entity(Country.class)` | `BeanOutputConverter<Country>` |
| `.entity(new ParameterizedTypeReference<List<Country>>() {})` | `BeanOutputConverter<List<Country>>` |
| `.entity(List.class)` (this project) | `BeanOutputConverter<List>`: a raw list, see below |
| `.entity(new ListOutputConverter())` | `ListOutputConverter` (you pass it yourself) |
| `.entity(new MapOutputConverter())` | `MapOutputConverter` (you pass it yourself) |

> 💡 In Spring AI 2.0.1, `new ListOutputConverter()` works. Older tutorials write `new ListOutputConverter(new DefaultConversionService())`, which does the same thing.

---

## 5. How this project works

The app has one endpoint. You send any text as the request body, and the AI's reply comes back as a JSON **array**.

```
POST /api/v1/convert        body: "List 5 countries in Asia"
          │
          ▼
ConverterController         chatClient.prompt().user(message).call().entity(List.class)
          │
          ▼
ChatClient (from AiConfig)  plain ChatClient, no defaults
          │                 + format instructions: "reply in JSON, schema: { type: array }"
          ▼
OpenAI (gpt-5-mini, the default model)
          │                 replies: ["India","Japan","China","Nepal","Bhutan"]
          ▼
BeanOutputConverter         Jackson reads the text into a java.util.List
          │
          ▼
ResponseEntity.ok(list)     Spring MVC writes the list back as JSON
```

### Files

| File | What it does |
|---|---|
| [`AiConfig.java`](src/main/java/com/ai/converters/config/AiConfig.java) | Builds a plain `ChatClient` bean (no system prompt, no advisors) |
| [`ConverterController.java`](src/main/java/com/ai/converters/controller/ConverterController.java) | The `POST /api/v1/convert` endpoint |
| [`application.properties`](src/main/resources/application.properties) | Reads the API key from the `OPEN_AI_KEY` environment variable |

### The controller

```java
@PostMapping
public ResponseEntity<Object> getCountryList(@RequestBody String message) {
    return ResponseEntity.ok(chatClient.prompt().user(message).call().entity(
            List.class
    ));
}
```

| Part | Meaning |
|---|---|
| `@PostMapping` + `@RequestBody String` | The **whole request body** is the prompt. No JSON wrapping needed. |
| `.entity(List.class)` | "Give me a `java.util.List` instead of a `String`." |
| `ResponseEntity<Object>` | Spring's HTTP response. Spring MVC turns the list into JSON. |

### What does `List.class` (a raw list) really give you? 🤔

Java **erases** generic types, so `List.class` has no idea what is inside the list. Spring AI generates a very loose schema (just "an array"), and Jackson fills the list with whatever JSON it finds:

| The AI replies with | Each item in your `List` becomes |
|---|---|
| `["India", "Japan"]` | `String` |
| `[{"name":"India","capital":"New Delhi"}]` | `LinkedHashMap<String, Object>` |
| `[1, 2, 3]` | `Integer` |

So the shape of the items depends on **how you word the prompt**. That's fine for a first demo, but in real code you want `List<Country>`, so the schema tells the AI the exact fields. See [section 6](#6-all-the-ways-to-call-entity).

### Which model is used?

`application.properties` doesn't set a model, so the OpenAI starter's default is used. In Spring AI 2.0.1 that's **`gpt-5-mini`** (`OpenAiChatOptions.DEFAULT_CHAT_MODEL`). To change it, add:

```properties
spring.ai.openai.chat.model=gpt-5
```

---

## 6. All the ways to call `.entity(...)`

Use a **record** for the data. Records are short, immutable and work well with Jackson.

```java
public record Country(String name, String capital, long population) {}
```

### A. One object

```java
Country country = chatClient.prompt()
        .user("Give me details about India")
        .call()
        .entity(Country.class);
// Country[name=India, capital=New Delhi, population=1428000000]
```

### B. A list of objects (the right way to get a list)

```java
List<Country> countries = chatClient.prompt()
        .user("List 5 countries in Asia with capital and population")
        .call()
        .entity(new ParameterizedTypeReference<List<Country>>() {});
```

The `{}` at the end creates a tiny anonymous class. That's how Java keeps the `<Country>` part at runtime.

### C. A wrapper record (often the cleanest) ⭐

```java
public record CountryList(List<Country> countries) {}

CountryList result = chatClient.prompt()
        .user("List 5 countries in Asia")
        .call()
        .entity(CountryList.class);
```

No `ParameterizedTypeReference` needed, and the JSON is an **object**, not a top-level array. This matters for native structured output (see [section 8](#8-spring-ai-20-extras-native-output-and-validation)).

### D. A simple list of words

```java
List<String> colors = chatClient.prompt()
        .user("List 5 colors of the rainbow")
        .call()
        .entity(new ListOutputConverter());
// [red, orange, yellow, green, blue]
```

### E. A map with dynamic keys

```java
Map<String, Object> capitals = chatClient.prompt()
        .user("Give me the capitals of India, Japan and France as a JSON object")
        .call()
        .entity(new MapOutputConverter());
// {India=New Delhi, Japan=Tokyo, France=Paris}
```

### F. The object AND the metadata (tokens, model)

`.entity(...)` throws away the metadata. `.responseEntity(...)` keeps both:

```java
var result = chatClient.prompt()
        .user("Give me details about India")
        .call()
        .responseEntity(Country.class);

Country country = result.entity();
ChatResponse response = result.response();
Integer tokens = response.getMetadata().getUsage().getTotalTokens();
```

> ⚠️ This `ResponseEntity` is **Spring AI's** class (`org.springframework.ai.chat.client.ResponseEntity`), not Spring MVC's `org.springframework.http.ResponseEntity`. Both have the same name. In the controller, use `var` or a full package name so you don't import the wrong one.

### Field descriptions help the AI 💡

Add Jackson annotations to guide the AI and control field names:

```java
public record Country(
        @JsonPropertyDescription("Official country name in English") String name,
        @JsonPropertyDescription("Capital city") String capital,
        @JsonProperty(required = true) long population) {}
```

The descriptions go into the JSON Schema, so the AI sees them.

---

## 7. Upgrade this project step by step

These steps are **not in the code yet**.

### Step 1: Add a record

`src/main/java/com/ai/converters/model/Country.java`

```java
package com.ai.converters.model;

public record Country(String name, String capital, long population) {}
```

### Step 2: Add a typed endpoint

Add this to `ConverterController`:

```java
@PostMapping("/countries")
public List<Country> getCountries(@RequestBody String message) {
    return chatClient.prompt()
            .user(message)
            .call()
            .entity(new ParameterizedTypeReference<List<Country>>() {});
}
```

Imports needed: `org.springframework.core.ParameterizedTypeReference`, `com.ai.converters.model.Country`.

### Step 3: Try it

```bash
curl -X POST "http://localhost:8080/api/v1/convert/countries" -H "Content-Type: text/plain" -d "List 3 countries in Europe"
```

You should get clean, typed JSON every time:

```json
[
  {"name": "France", "capital": "Paris", "population": 68000000},
  {"name": "Germany", "capital": "Berlin", "population": 84000000},
  {"name": "Italy", "capital": "Rome", "population": 59000000}
]
```

Compare it with the old endpoint, where the item shape changed depending on your wording.

---

## 8. Spring AI 2.0 extras: native output and validation

By default, Spring AI puts the format instructions **in the prompt text** and hopes the model follows them. Spring AI 2.0 adds two options through a second argument to `.entity(...)`:

```java
CountryList result = chatClient.prompt()
        .user("List 5 countries in Asia")
        .call()
        .entity(CountryList.class, spec -> spec.useProviderStructuredOutput());
```

| Option | What it does | Watch out |
|---|---|---|
| `useProviderStructuredOutput()` | Sends the JSON Schema to the provider's **API** (e.g. OpenAI Structured Outputs), so the model is constrained to it, not just asked | **OpenAI rejects a top-level array schema.** `List<T>` fails with this option. Wrap it in a record (option C above). Off by default because support differs between providers. |
| `validateSchema()` | Checks the reply against the schema. If it doesn't match, it **retries** with the error message (up to 3 times by default) | Not supported for streaming. Each retry is another paid call. |

You can use both together: `spec -> spec.useProviderStructuredOutput().validateSchema()`.

---

## 9. How to run this project

**You need:** Java 21, Maven and an OpenAI API key. Calls use your OpenAI account and may cost money.

1. Set your API key. ⚠️ This project reads **`OPEN_AI_KEY`** (not `OPENAI_API_KEY` like day-1):

   ```bash
   export OPEN_AI_KEY=your-key-here
   ```

2. Start the app:

   ```bash
   cd converters
   mvn spring-boot:run
   ```

3. Ask for a simple list:

   ```bash
   curl -X POST "http://localhost:8080/api/v1/convert" -H "Content-Type: text/plain" -d "List 5 countries in Asia"
   ```

   Example reply: `["India","China","Japan","Nepal","Bhutan"]`

4. Ask for a list of objects, and see how the item shape changes:

   ```bash
   curl -X POST "http://localhost:8080/api/v1/convert" -H "Content-Type: text/plain" -d "List 3 countries in Asia with their capital"
   ```

   Example reply: `[{"country":"India","capital":"New Delhi"}, ...]`. The field names may change from run to run, because the raw `List.class` schema doesn't fix them.

---

## 10. Common mistakes and errors

| Problem | Why it happens | Fix |
|---|---|---|
| App fails to start with an API key error | `OPEN_AI_KEY` isn't set in this terminal | `export OPEN_AI_KEY=...` in the same terminal before `mvn spring-boot:run` |
| Jackson error like `Unrecognized token` or `Cannot deserialize` | The model added extra text, or used ```` ```json ```` fences, or returned a different shape | Make the prompt clearer, use a typed record, or add `validateSchema()` |
| `ClassCastException` when reading list items | `List.class` is raw: items are `String` or `LinkedHashMap`, not your class | Use `ParameterizedTypeReference<List<Country>>` or a wrapper record |
| Field names change from run to run | Raw `List`/`Map` gives no schema with field names | Use a record, so the schema has fixed fields |
| `400` error from OpenAI with `useProviderStructuredOutput()` and a `List<T>` | OpenAI Structured Outputs doesn't accept a top-level array | Wrap the list: `record CountryList(List<Country> countries)` |
| Wrong `ResponseEntity` import / compile error | Spring AI and Spring MVC both have a class called `ResponseEntity` | Use the full package name for one of them, or `var` |
| `415 Unsupported Media Type` | Request sent without a body or with an odd content type | Send `-H "Content-Type: text/plain" -d "..."` |
| Answer cut off, then JSON parse error | The reply hit the token limit in the middle of the JSON | Raise `maxTokens` or ask for fewer items |
| `.stream().entity(...)` doesn't exist | Structured output needs the **full** reply to parse | Use `.call()` for `.entity(...)` |

---

## 11. Interview questions and answers

**Q1. What is structured output in Spring AI?**
Getting the model's reply as a Java object (record, list, map) instead of a plain string, so code can use it directly.

**Q2. What does an output converter do?**
Two things: `getFormat()` adds format instructions (often a JSON Schema) to the prompt, and `convert()` turns the reply text into the Java type.

**Q3. Name the three built-in converters.**
`BeanOutputConverter` (your class, from a JSON Schema), `MapOutputConverter` (`Map<String, Object>`), and `ListOutputConverter` (`List<String>` from comma-separated text).

**Q4. Which converter does `.entity(Country.class)` use?**
`BeanOutputConverter<Country>`. It creates a JSON Schema from the `Country` class.

**Q5. How do you get `List<Country>`? Why not `List.class`?**
Use `new ParameterizedTypeReference<List<Country>>() {}`. Because of type erasure, `List.class` doesn't know the item type, so the items come back as maps or strings.

**Q6. Does the model always follow the format?**
No. By default the format is only text in the prompt. The model can still add extra words or change the shape. Native structured output and `validateSchema()` make it much more reliable.

**Q7. What is `useProviderStructuredOutput()`?**
A Spring AI 2.0 option that sends the schema to the provider's API so the model is constrained to it. It's off by default. OpenAI doesn't accept a top-level array schema, so wrap lists in a record.

**Q8. What does `validateSchema()` do?**
It validates the reply against the schema and, if it fails, retries with the error as feedback (up to 3 times by default). It doesn't work with streaming.

**Q9. `.entity()` vs `.responseEntity()`?**
`.entity()` gives only the object. `.responseEntity()` gives both the object and the `ChatResponse` (token usage, model name, finish reason).

**Q10. Why are records a good choice for AI output?**
They are short, immutable and Jackson maps them to JSON easily. The record components become the schema fields.

**Q11. How can you help the model fill fields correctly?**
Add `@JsonPropertyDescription` to fields. The descriptions go into the JSON Schema that the model sees.

**Q12. Can you use `.entity()` with streaming?**
No. The converter needs the complete reply to parse JSON. Use `.call()`.

---

## 12. One-minute summary

- The model only returns **text**. Converters turn it into **Java objects**.
- A converter = **`getFormat()`** (instructions added to the prompt) + **`convert()`** (text → object).
- **`BeanOutputConverter`** for your classes, **`MapOutputConverter`** for maps, **`ListOutputConverter`** for simple word lists.
- `.entity(Country.class)` for one object, **`ParameterizedTypeReference<List<Country>>`** or a **wrapper record** for lists.
- This project uses a **raw `List.class`**: it works, but the item shape depends on the prompt.
- **`.responseEntity()`** keeps token usage and other metadata.
- Spring AI 2.0: **`useProviderStructuredOutput()`** (API-level schema, no top-level arrays on OpenAI) and **`validateSchema()`** (auto retry).
- This project reads the key from **`OPEN_AI_KEY`**.

---

## 13. Self-test

<details>
<summary>1. You call <code>.entity(List.class)</code> and ask for "3 countries with capitals". What type is each item?</summary>

A `LinkedHashMap<String, Object>`, because the raw list has no item type and the AI replied with JSON objects.
</details>

<details>
<summary>2. Which two methods does every output converter have?</summary>

`getFormat()` (before the call, adds instructions) and `convert(String)` (after the call, parses the reply).
</details>

<details>
<summary>3. Why does <code>List&lt;Country&gt;</code> fail with <code>useProviderStructuredOutput()</code> on OpenAI, and how do you fix it?</summary>

OpenAI's Structured Outputs API doesn't accept a top-level array schema. Wrap it: `record CountryList(List<Country> countries)`.
</details>

<details>
<summary>4. You need the parsed object and the token count. Which method?</summary>

`.responseEntity(Country.class)`: `entity()` gives the object, `response().getMetadata().getUsage()` gives the tokens.
</details>

<details>
<summary>5. Why is the curly-brace <code>{}</code> needed in <code>new ParameterizedTypeReference&lt;List&lt;Country&gt;&gt;() {}</code>?</summary>

It creates an anonymous subclass. A subclass keeps its generic type information at runtime, so Spring can see `List<Country>`.
</details>

---

## 14. What to learn next

- **Chat memory:** let the AI remember earlier messages, see [chat-memory-day-7](../chat-memory-day-7/)
- **RAG:** give the AI your own documents, see [basic-rag-day-8](../basic-rag-day-8/)
- **Tool calling:** let the AI call your Java methods, which also uses JSON Schemas

---

## References 📚

- [Spring AI: Structured Output Converter](https://docs.spring.io/spring-ai/reference/api/structured-output-converter.html)
- [Spring AI: ChatClient (entity / responseEntity)](https://docs.spring.io/spring-ai/reference/api/chatclient.html)
- [OpenAI: Structured Outputs](https://platform.openai.com/docs/guides/structured-outputs)
- [JSON Schema](https://json-schema.org/)
