# 🦙 Super-Easy RAG (Retrieval-Augmented Generation) with Spring Boot, Ollama & PGVector

Welcome! This project is a simple yet powerful AI system that lets you "chat" with your own PDF documents. Imagine having a robot friend that reads a huge insurance file for you and answers your questions about it instantly. 

This guide is written so that **anyone** (even a "dumb donkey" 🫏, as you put it!) can set it up. Just follow these steps one by one.

---

## 🛠 Step 0: What Do You Need? (The Tools)

Before we start, make sure you have these 3 things installed on your computer:
1.  **Docker Desktop**: To run our database. ([Download here](https://www.docker.com/products/docker-desktop/))
2.  **Ollama**: To run the AI model offline. ([Download here](https://ollama.com/))
3.  **Java 17**: To run our code.

---

## 🚀 Step 1: Start the Database (The Memory)

We use a database called **PostgreSQL** with a special "AI brain" called **PGVector**. This is where the AI stores the bits of information it reads from your PDF.

1.  Open your terminal (Command Prompt or Terminal).
2.  Stop and remove any old database if you had one:
    ```bash
    docker stop pgvector
    docker rm pgvector
    ```
3.  Run this magic command to start a fresh database:
    ```bash
    docker run --name pgvector -e POSTGRES_USER=postgres -e POSTGRES_PASSWORD=postgres -e POSTGRES_DB=testrag -p 5432:5432 -d pgvector/pgvector:pg16
    ```
    *Wait, what did that do?* It created a database named `testrag` with the password `postgres`. Easy!

---

## 🧠 Step 2: Set Up the AI (The Brain)

Now we need to download the AI models that will do the thinking and the reading.

1.  Open your terminal again.
2.  **Download the Thinker (Llama 3.2)**: This one talks to you.
    ```bash
    ollama run llama3.2
    ```
    *(Once it's done, you can type `/bye` to exit the chat and go back to the terminal)*.
3.  **Download the Reader (Nomic Embed Text)**: This one helps the AI understand the text in your PDF.
    ```bash
    ollama pull nomic-embed-text
    ```

---

## 🏗 Step 3: Configure the Project

Make sure your `src/main/resources/application.properties` file looks like this (it tells the code where the database and AI are):

```properties
spring.application.name=demo

# Database Settings
spring.datasource.url=jdbc:postgresql://localhost:5432/testrag
spring.datasource.username=postgres
spring.datasource.password=postgres
spring.jpa.hibernate.ddl-auto=update

# AI Settings (Ollama)
spring.ai.ollama.base-url=http://localhost:11434
spring.ai.ollama.chat.options.model=llama3.2:latest
spring.ai.ollama.embedding.options.model=nomic-embed-text:latest

# Automatically create the AI storage tables
spring.ai.vectorstore.pgvector.initialize-schema=true
```

---

## 🏃 Step 4: Run the Application

1.  Open the project in your favorite editor (like IntelliJ or VS Code).
2.  Run the `DemoApplication.java` file.
3.  Alternatively, use the terminal:
    ```bash
    ./mvnw spring-boot:run
    ```
    *Wait until you see something like `Started DemoApplication...`!*

---

## 📖 Step 5: How to Use It (The Fun Part!)

Now that everything is running, let's make it work!

### A. Feed the AI the PDF (Ingestion)
We have a file in the project folder called `Insurance - Wikipedia.pdf`. We need the AI to read it.
Open your browser or a tool like Postman and call this URL:
- **POST URL**: `http://localhost:8080/documents/vector/insurance`
- **Result**: You should see: *"Insurance PDF successfully ingested to vector database for precise RAG!"*

### B. Ask a Question!
Now you can ask anything about insurance.
- **GET URL**: `http://localhost:8080/rag/ask?question=What is the purpose of insurance?`
- **AI Response**: The AI will search the PDF and give you an answer **only** based on what it read in that file!

---

## 💡 How It Works (For Human Geeks)

1.  **Ingestion**: The `VectorDocumentIngestionService` reads the PDF, cleans the text (removing weird characters), breaks it into small pieces (chunks), and saves them into PostgreSQL as vectors.
2.  **Retrieval**: When you ask a question, the `RagService` searches the database for the pieces of text that are most relevant to your question.
3.  **Generation**: We send those relevant pieces + your question to **Llama 3.2** with a strict instruction: *"Only answer using this text!"* 

This prevents the AI from lying (hallucinating)!

---

## 🆘 Troubleshooting

- **Error: Connection Refused?** Make sure Docker is running and you started the `pgvector` container.
- **Error: 404 Not Found?** Make sure you ran the Spring Boot app and are using the correct URLs (`/documents/...` or `/rag/...`).
- **Slow Response?** AI takes power. Make sure your computer isn't busy doing other heavy things!

---
Enjoy your personal AI researcher! 🤖📄
