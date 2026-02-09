package com.example.demo.service;

import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.parser.apache.pdfbox.ApachePdfBoxDocumentParser;
import dev.langchain4j.data.document.splitter.DocumentSplitters;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore;
import org.springframework.stereotype.Service;

import java.io.FileInputStream;
import java.util.List;
import java.util.logging.Logger;

@Service
public class LangChain4jRagService {

    private static final Logger logger = Logger.getLogger(LangChain4jRagService.class.getName());

    private final ChatLanguageModel chatModel;
    private final EmbeddingModel embeddingModel;
    private final EmbeddingStore<TextSegment> embeddingStore;

    public LangChain4jRagService(ChatLanguageModel chatModel, EmbeddingModel embeddingModel) {
        this.chatModel = chatModel;
        this.embeddingModel = embeddingModel;
        this.embeddingStore = new InMemoryEmbeddingStore<>();
    }

    public void ingestPdfDocument(String filePath) {
        try {
            logger.info(() -> "Starting PDF ingestion with LangChain4j: " + filePath);
            
            // Parse PDF document
            ApachePdfBoxDocumentParser parser = new ApachePdfBoxDocumentParser();
            Document document = parser.parse(new FileInputStream(filePath));
            
            // Split document into segments
            List<TextSegment> segments = DocumentSplitters.recursive(
                800,  // max segment size
                100   // max overlap
            ).split(document);
            
            // Add segments to embedding store with embeddings
            for (TextSegment segment : segments) {
                embeddingStore.add(embeddingModel.embed(segment).content(), segment);
            }
            
            logger.info(() -> String.format("Successfully ingested %d segments using LangChain4j", segments.size()));
            
        } catch (Exception e) {
            logger.severe("Failed to ingest PDF with LangChain4j: " + e.getMessage());
            throw new RuntimeException("PDF ingestion failed", e);
        }
    }

    public String askQuestion(String question) {
        try {
            logger.info(() -> "Processing question with LangChain4j: " + question);
            
            // Find relevant segments
            List<dev.langchain4j.store.embedding.EmbeddingMatch<TextSegment>> relevantSegments = 
                embeddingStore.search(dev.langchain4j.store.embedding.EmbeddingSearchRequest.builder()
                    .queryEmbedding(embeddingModel.embed(question).content())
                    .maxResults(5)
                    .minScore(0.6)
                    .build()).matches();
            
            // Build context from retrieved segments
            StringBuilder contextBuilder = new StringBuilder();
            for (dev.langchain4j.store.embedding.EmbeddingMatch<TextSegment> match : relevantSegments) {
                contextBuilder.append(match.embedded().text()).append("\n\n");
            }
            
            String context = contextBuilder.toString();
            
            // Create prompt with context
            String prompt = """
                Based on the following context, please answer the question. 
                If the answer is not in the context, say 'I don't have enough information to answer this question.'
                
                Context:
                %s
                
                Question: %s
                
                Answer:
                """.formatted(context, question);
            
            // Generate response
            String response = chatModel.generate(prompt);
            
            logger.info("LangChain4j response generated successfully");
            return response;
            
        } catch (Exception e) {
            logger.severe("Error processing question with LangChain4j: " + e.getMessage());
            return "Sorry, I encountered an error while processing your question: " + e.getMessage();
        }
    }

    public String getSimpleResponse(String message) {
        try {
            return chatModel.generate("Please respond to this message: " + message);
        } catch (Exception e) {
            logger.severe("Error generating simple response: " + e.getMessage());
            return "Sorry, I couldn't generate a response at this time.";
        }
    }
}
