# 🧠 Detailed RAG Implementation Guide: From PDF to AI Response

This guide explains exactly how our Retrieval-Augmented Generation (RAG) system works. It breaks down the architecture, the code, and the data flow that allows an AI to answer questions using only our local PDF documents.

---

## 🏗️ 1. The Big Picture (Architecture)

The system is built on four pillars:
1.  **Spring AI**: The bridge between Java and AI models.
2.  **Ollama**: Runs our LLM (`llama3.2`) and Embedding model (`nomic-embed-text`) locally.
3.  **PostgreSQL + pgvector**: A database that can store and find mathematical "meaning" (vectors).
4.  **Retrieval-Augmented Generation (RAG)**: The technique of feeding document relevant snippets into the AI's prompt to ensure accuracy and prevent "hallucinations."

---

## 🛠️ 2. Core Configuration

All the magic starts in `application.properties`. We tell Spring Boot where our database is, which AI models to use, and how big our document "chunks" should be.

```properties
# 1. Database & Vector Store
spring.datasource.url=jdbc:postgresql://localhost:5432/testrag
spring.ai.vectorstore.pgvector.initialize-schema=true  # Auto-creates the vector table!

# 2. AI Connection (Ollama)
spring.ai.ollama.base-url=http://localhost:11434
spring.ai.ollama.chat.options.model=llama3.2:latest   # The "Brain" (LLM)
spring.ai.ollama.embedding.options.model=nomic-embed-text:latest # The "Translator" (Embeddings)

# 3. Processing Settings
app.pdf.chunk-size=500    # Each chunk is ~500 chars (approx 100 words)
app.pdf.chunk-overlap=100 # Chunks overlap to keep context across splits
```

---

## 📂 3. The Data Structure (The Model)

We use a specific table format to store embeddings. In `Document.java`, we define a `vector` column with **768** dimensions to match our `nomic-embed-text` model.

```java
@Entity
public class Document {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(columnDefinition = "TEXT")
    private String content; // The actual text from the PDF

    @Column(columnDefinition = "vector(768)")
    private float[] embedding; // The mathematical "meaning" of the text

    private String fileName;
}
```

---

## 🚀 4. Phase 1: Ingestion (How the PDF is "Learned")

Handled by `VectorDocumentIngestionService.java`. This process happens once per document.

### Step A: Extraction
We use **Apache PDFBox** to read the PDF and convert it into a giant string of text.

### Step B: Chunking
An LLM can't read a 50-page PDF all at once. We must split it into "chunks." Our service splits the text by sentences to preserve semantic meaning.

```java
// Inside VectorDocumentIngestionService.java
private List<Document> createChunks(String fullText, String fileName) {
    // Split text by sentences using Regex
    String[] sentences = fullText.split("(?<=[.!?])\\s+");
    // Group sentences until they reach ~500 characters (chunkSize)
    // ...
    return chunks;
}
```

### Step C: Embedding & Storage
For every chunk, Spring AI calls Ollama's embedding model. It turns `"Insurance is a contract..."` into a list of 768 numbers like `[0.12, -0.05, 0.88, ...]`. This is then saved into the PostgreSQL `vector_store` table.

```java
// We send chunks to the vector store in batches
vectorStore.add(cleanedBatch); 
```

---

## 🔍 5. Phase 2: Retrieval (Finding the Answer)

When you ask a question, the `RagService.java` performs a search.

### Step A: Similarity Search
We don't search for exact words (like Ctrl+F). We convert your **question** into a vector and find the **top 10 closest vectors** in the database.

```java
// Inside RagService.java
SearchRequest request = SearchRequest.builder()
        .query(question) // e.g. "What is casualty insurance?"
        .topK(10)        // Get the 10 most relevant chunks
        .build();

List<Document> similarDocs = vectorStore.similaritySearch(request);
```

### Step B: Prompt Engineering
This is the most critical part. We take those 10 chunks and "stuff" them into a prompt for the LLM. We tell the AI **not to use its own memory**, but only the chunks we found.

```java
String enhancedPrompt = 
    "Answer the following question ONLY based on the provided context.\n" +
    "Do not use external knowledge.\n\n" +
    "CONTEXT:\n" + context + "\n\n" +
    "QUESTION: " + question;

Prompt aiPrompt = new Prompt(new UserMessage(enhancedPrompt));
return chatModel.call(aiPrompt).getResult().getOutput().getText();
```

---

## 🧪 6. Comprehensive Testing

### To Ingest the Document:
This triggers the extraction, chunking, and vectorization.
```bash
curl -X POST http://localhost:8080/documents/vector/insurance
```

### To Query the Document (The RAG Step):
This retrieves the relevant chunks and generates the AI response.
```bash
curl -G "http://localhost:8080/rag/ask" --data-urlencode "question=What is casualty insurance?"
```

### Why it's "Secure":
If you ask `curl -G "http://localhost:8080/rag/ask" --data-urlencode "question=How do I steal a car?"`, the similarity search will find nothing relevant in the insurance PDF, the context will be empty, and the AI will (correctly) say: *"The insurance document doesn't contain sufficient information to answer this question."*

---

## 📊 Summary of SQL Fixes Made

During development, we resolved these critical SQL issues:
1.  **Dimension Mismatch**: Fixed the database to use `vector(768)` (correct for Nomic model) instead of `vector(1536)` (OpenAI default).
2.  **Table Names**: Corrected native queries to use `document` (singular) instead of `documents` (plural) to match Hibernate's default naming.
3.  **Schema Initialization**: Enabled automatic table creation in Spring AI to handle `jsonb` metadata and `vector` columns properly.
