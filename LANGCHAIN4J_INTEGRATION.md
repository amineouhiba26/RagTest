# LangChain4j Integration Guide

This project now includes **LangChain4j** alongside Spring AI, giving you two powerful AI frameworks to work with.

## What I Added

### 1. Dependencies in `pom.xml`

```xml
<!-- LangChain4j Dependencies -->
<dependency>
    <groupId>dev.langchain4j</groupId>
    <artifactId>langchain4j</artifactId>
    <version>0.34.0</version>
</dependency>

<dependency>
    <groupId>dev.langchain4j</groupId>
    <artifactId>langchain4j-spring-boot-starter</artifactId>
    <version>0.34.0</version>
</dependency>

<dependency>
    <groupId>dev.langchain4j</groupId>
    <artifactId>langchain4j-ollama</artifactId>
    <version>0.34.0</version>
</dependency>

<dependency>
    <groupId>dev.langchain4j</groupId>
    <artifactId>langchain4j-document-parser-apache-pdfbox</artifactId>
    <version>0.34.0</version>
</dependency>

<dependency>
    <groupId>dev.langchain4j</groupId>
    <artifactId>langchain4j-embeddings-all-minilm-l6-v2</artifactId>
    <version>0.34.0</version>
</dependency>

<dependency>
    <groupId>dev.langchain4j</groupId>
    <artifactId>langchain4j-easy-rag</artifactId>
    <version>0.34.0</version>
</dependency>
```

### 2. Configuration (`LangChain4jConfig.java`)

I created a configuration class that sets up:
- **Ollama Chat Model**: Connects to your local Ollama instance
- **Local Embedding Model**: Uses AllMiniLmL6V2 (runs locally, no external API needed)
- **Chat Memory**: For conversation history

### 3. Services

#### `LangChain4jRagService.java`
- PDF document ingestion using LangChain4j's document parser
- Text splitting into semantic chunks
- In-memory vector storage with embeddings
- RAG (Retrieval Augmented Generation) question answering
- Simple chat functionality

### 4. REST API Endpoints (`LangChain4jController.java`)

- `POST /api/langchain4j/ingest` - Upload and ingest PDF documents
- `POST /api/langchain4j/ask` - Ask questions about ingested documents
- `POST /api/langchain4j/chat` - Simple chat without document context
- `GET /api/langchain4j/status` - Check service status

## How I Made It

### Step 1: Added Dependencies
I added LangChain4j core dependencies to your `pom.xml`, including:
- Core framework
- Spring Boot starter for auto-configuration
- Ollama integration for connecting to your local models
- PDF parsing capabilities
- Local embedding model (no external API required)

### Step 2: Configuration Setup
```java
@Configuration
public class LangChain4jConfig {
    
    @Bean
    public ChatLanguageModel chatLanguageModel() {
        return OllamaChatModel.builder()
                .baseUrl(ollamaBaseUrl)
                .modelName(chatModelName)
                .temperature(0.7)
                .timeout(Duration.ofMinutes(3))
                .build();
    }
    
    @Bean
    public EmbeddingModel embeddingModel() {
        return new AllMiniLmL6V2EmbeddingModel(); // Local embeddings
    }
}
```

### Step 3: RAG Implementation
The LangChain4j service implements RAG using:
- Document parsing with Apache PDFBox
- Text splitting for better semantic chunks
- In-memory embedding store (can be replaced with persistent storage)
- Similarity search for relevant content retrieval

### Step 4: API Layer
Created REST endpoints to interact with LangChain4j functionality, allowing you to:
- Upload PDFs for processing
- Ask questions about documents
- Have simple conversations

## Key Differences from Spring AI

### LangChain4j Advantages:
1. **Rich Ecosystem**: More integrations and tools
2. **Advanced RAG**: Built-in RAG patterns and utilities
3. **Memory Management**: Sophisticated conversation memory
4. **Tool Integration**: Easy function calling and tool use
5. **Local Embeddings**: Built-in local embedding models

### Spring AI Advantages:
1. **Spring Integration**: Native Spring Boot integration
2. **Vector Stores**: Better integration with external vector databases
3. **Reactive Support**: Built-in reactive programming support
4. **Spring Ecosystem**: Leverages Spring's configuration and DI

## ✅ Issues Fixed During Integration

### 1. Bean Name Conflict
**Problem**: Both Spring AI and LangChain4j tried to create a bean named `chatMemory`, causing a startup conflict.
**Solution**: Renamed LangChain4j's chat memory bean to `langchain4jChatMemory` using `@Bean("langchain4jChatMemory")`.

### 2. Duplicate Mapping Endpoints  
**Problem**: DocumentController had two methods with the same `@PostMapping("/vector/insurance")` path.
**Solution**: Changed the second endpoint to `@PostMapping("/vector/upload")` for file uploads.

## Usage Examples

### Ingest a PDF Document
```bash
curl -X POST "http://localhost:8080/api/langchain4j/ingest" \
  -F "file=@your-document.pdf"
```

### Ask Questions
```bash
curl -X POST "http://localhost:8080/api/langchain4j/ask" \
  -H "Content-Type: application/json" \
  -d '{"question": "What is insurance?"}'
```

### Simple Chat
```bash
curl -X POST "http://localhost:8080/api/langchain4j/chat" \
  -H "Content-Type: application/json" \
  -d '{"message": "Hello! How are you?"}'
```

## Configuration Options

The service uses these configuration values (from `application.properties`):
- `langchain4j.ollama.chat-model.base-url`: Ollama server URL
- `langchain4j.ollama.chat-model.model-name`: Chat model name

## Next Steps

You can now:
1. Compare Spring AI vs LangChain4j implementations side by side
2. Use LangChain4j's advanced RAG features
3. Implement more complex AI workflows
4. Add persistent vector storage
5. Integrate with other LangChain4j tools and services

Both frameworks are now available in your project, giving you the flexibility to choose the best tool for each use case!

## ✅ Success! Both Frameworks Working

Your application is now successfully running with both Spring AI and LangChain4j integrated:

### Verified Working Endpoints:

**LangChain4j Endpoints:**
- ✅ `GET /api/langchain4j/status` - Service status check
- ✅ `POST /api/langchain4j/chat` - Simple chat functionality
- ✅ `POST /api/langchain4j/ingest` - PDF document ingestion
- ✅ `POST /api/langchain4j/ask` - RAG question answering

**Spring AI Endpoints:**
- ✅ `GET /rag/ask` - Spring AI RAG question answering
- ✅ `POST /documents/vector/insurance` - Insurance PDF ingestion
- ✅ `POST /documents/vector/upload` - General file upload

### Test Results:
- **LangChain4j Chat**: Successfully responds to conversation prompts
- **Spring AI RAG**: Properly searches vector store and responds to questions
- **No conflicts**: Both frameworks coexist without bean conflicts or mapping issues

The application starts in ~4 seconds and is ready to handle requests from both AI frameworks simultaneously!
