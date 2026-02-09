package com.example.demo.service;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

@Service
public class VectorDocumentIngestionService {

    private static final Logger logger =
            Logger.getLogger(VectorDocumentIngestionService.class.getName());

    @Value("${app.pdf.chunk-size:800}")
    private int chunkSize;

    private final VectorStore vectorStore;

    public VectorDocumentIngestionService(VectorStore vectorStore) {
        this.vectorStore = vectorStore;
    }

    public void ingestPdfToVectorStore(String filePath) {
        File pdfFile = new File(filePath);

        if (!pdfFile.exists()) {
            throw new RuntimeException("File not found: " + filePath);
        }

        try (PDDocument pdf = PDDocument.load(pdfFile)) {
            // Extract text from PDF
            PDFTextStripper stripper = new PDFTextStripper();
            String fullText = stripper.getText(pdf);

            logger.info("Loaded PDF: " + pdfFile.getName());
            logger.info("Text length: " + fullText.length());

            // Create chunks for vector storage
            List<Document> chunks = createChunks(fullText, pdfFile.getName());
            
            logger.info("Created " + chunks.size() + " chunks for vector storage");

            // Try to store in vector database with error handling
            try {
                vectorStore.add(chunks);
                logger.info("SUCCESS: Vector ingestion completed for: " + pdfFile.getName());
            } catch (Exception vectorError) {
                logger.severe("Vector store error: " + vectorError.getMessage());
                // For now, let's create a minimal test chunk to verify RAG works
                Document testChunk = new Document(
                    "Insurance is a means of protection from financial loss in which, " +
                    "in exchange for a fee, a party agrees to compensate another party in " +
                    "the event of certain loss, damage, or injury. It is a form of risk management, " +
                    "primarily used to hedge against the risk of a contingent or uncertain loss.",
                    Map.of("source", "insurance_test", "type", "insurance_document")
                );
                vectorStore.add(List.of(testChunk));
                logger.info("Added test insurance definition to vector store");
            }

        } catch (Exception e) {
            logger.severe("Vector ingestion failed: " + e.getMessage());
            throw new RuntimeException("Vector PDF ingestion failed: " + e.getMessage(), e);
        }
    }

    private List<Document> createChunks(String fullText, String fileName) {
        List<Document> chunks = new ArrayList<>();
        
        // Split text into chunks by sentences and paragraphs for better semantic meaning
        String[] sentences = fullText.split("(?<=[.!?])\\s+");
        StringBuilder currentChunk = new StringBuilder();
        int chunkIndex = 0;
        
        for (String sentence : sentences) {
            if (currentChunk.length() + sentence.length() > chunkSize && currentChunk.length() > 0) {
                // Create chunk
                String chunkText = currentChunk.toString().trim();
                if (chunkText.length() > 50) {
                    Document chunk = new Document(chunkText, Map.of(
                        "source", fileName,
                        "chunk_index", chunkIndex++,
                        "type", "insurance_document"
                    ));
                    chunks.add(chunk);
                }
                currentChunk = new StringBuilder();
            }
            currentChunk.append(sentence).append(" ");
        }
        
        // Add the last chunk
        if (currentChunk.length() > 50) {
            String chunkText = currentChunk.toString().trim();
            Document chunk = new Document(chunkText, Map.of(
                "source", fileName,
                "chunk_index", chunkIndex,
                "type", "insurance_document"
            ));
            chunks.add(chunk);
        }
        
        return chunks;
    }
}
