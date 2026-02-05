package com.example.demo.service;

import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;


@Service
public class RagService {

    private final OllamaChatModel chatModel;
    private final VectorStore vectorStore;

    public RagService(OllamaChatModel chatModel, VectorStore vectorStore) {
        this.chatModel = chatModel;
        this.vectorStore = vectorStore;
    }

    public String askQuestion(String question) {

        List<Document> similarDocs = vectorStore.similaritySearch(
                SearchRequest.builder()
                        .query(question)
                        .topK(10)
                        .build()
        );

        if (similarDocs.isEmpty()) {
            return "The insurance document doesn't contain sufficient information to answer this question.";
        }

        String context = similarDocs.stream()
                .map(Document::getText)
                .filter(t -> t != null && !t.isBlank())
                .collect(Collectors.joining("\n\n"));

        String prompt =
                "Answer ONLY using the context below.\n\n" +
                        "CONTEXT:\n" + context + "\n\n" +
                        "QUESTION: " + question;

        return chatModel.call(new Prompt(new UserMessage(prompt)))
                .getResult()
                .getOutput()
                .getText();
    }
}
