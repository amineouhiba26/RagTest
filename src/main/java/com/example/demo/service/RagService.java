package com.example.demo.service;

import com.example.demo.repository.DocumentRepository;
import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class RagService {

    private final DocumentRepository repo;
    private final OllamaChatModel chatModel;
    private final VectorStore vectorStore;

    public RagService(DocumentRepository repo, OllamaChatModel chatModel, VectorStore vectorStore) {
        this.repo = repo;
        this.chatModel = chatModel;
        this.vectorStore = vectorStore;
    }

 public String askQuestion(String question) {
        try {
            // Only use vector similarity search for precise document-based responses
            List<Document> similarDocs = vectorStore.similaritySearch(question);
            
            if (similarDocs.isEmpty()) {
                return "I don't have information about that topic in the insurance document. Please ask questions related to insurance concepts that might be covered in the document.";
            }
            
            // Extract content from similar documents  
            String context = similarDocs.stream()
                    .map(Document::getText)
                    .collect(Collectors.joining("\n\n"));
            
            // Create enhanced prompt that strictly uses only document content
            String enhancedPrompt = "Answer the following question ONLY based on the provided context from the insurance document. " +
                    "Do not use any external knowledge. If the context doesn't contain enough information to answer the question, " +
                    "say 'The document doesn't contain sufficient information to answer this question.'\n\n" +
                    "Context from insurance document:\n" + context + "\n\n" +
                    "Question: " + question + "\n\n" +
                    "Answer based only on the document context:";
            
            // Get response from Ollama
            Prompt aiPrompt = new Prompt(new UserMessage(enhancedPrompt));
            return chatModel.call(aiPrompt).getResult().getOutput().getText();
            
        } catch (Exception e) {
            // Fallback to simple question answering if vector search fails
            String prompt = "Answer the following question: " + question;
            Prompt aiPrompt = new Prompt(new UserMessage(prompt));
            return chatModel.call(aiPrompt).getResult().getOutput().getText();
        }
    }
}

