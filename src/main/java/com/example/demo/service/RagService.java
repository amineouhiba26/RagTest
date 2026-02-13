package com.example.demo.service;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * Service de RAG (Retrieval Augmented Generation) unifié
 * Conforme aux besoins BF1, BF3 et BF4 du cahier des charges
 */
@Service
public class RagService {

    private static final Logger logger = Logger.getLogger(RagService.class.getName());

    @Value("${app.pdf.chunk-size:800}")
    private int chunkSize;

    private final VectorStore vectorStore;

    public RagService(VectorStore vectorStore) {
        this.vectorStore = vectorStore;
    }

    /**
     * BF1 — Ingestion documentaire : Ingestion d'un PDF dans la base vectorielle
     */
    public void ingestPdf(String filePath) {
        File pdfFile = new File(filePath);
        if (!pdfFile.exists()) {
            throw new RuntimeException("Fichier non trouvé: " + filePath);
        }

        try (PDDocument pdf = PDDocument.load(pdfFile)) {
            PDFTextStripper stripper = new PDFTextStripper();
            String fullText = stripper.getText(pdf);

            logger.info("Chargement du PDF: " + pdfFile.getName());
            
            List<Document> chunks = createChunks(fullText, pdfFile.getName());
            
            vectorStore.add(chunks);
            logger.info("Ingestion réussie de " + chunks.size() + " segments pour: " + pdfFile.getName());

        } catch (Exception e) {
            logger.severe("Échec de l'ingestion: " + e.getMessage());
            throw new RuntimeException("Échec de l'ingestion vectorielle: " + e.getMessage(), e);
        }
    }

    /**
     * BF4 — Recherche sémantique : Recherche de contexte pertinent
     */
    public String searchContext(String query) {
        try {
            logger.info("Recherche sémantique pour: " + query);
            
            SearchRequest searchRequest = SearchRequest.builder()
                .query(query)
                .topK(5)
                .similarityThreshold(0.7)
                .build();
                
            List<Document> results = vectorStore.similaritySearch(searchRequest);

            if (results.isEmpty()) {
                logger.warning("Aucun résultat pertinent trouvé pour la requête");
                return "";
            }

            return results.stream()
                .map(Document::getText)
                .collect(Collectors.joining("\n\n"));

        } catch (Exception e) {
            logger.severe("Erreur lors de la recherche sémantique: " + e.getMessage());
            return "";
        }
    }

    private List<Document> createChunks(String fullText, String fileName) {
        List<Document> chunks = new ArrayList<>();
        String[] sentences = fullText.split("(?<=[.!?])\\s+");
        StringBuilder currentChunk = new StringBuilder();
        int chunkIndex = 0;
        
        for (String sentence : sentences) {
            if (currentChunk.length() + sentence.length() > chunkSize && currentChunk.length() > 0) {
                String chunkText = currentChunk.toString().trim();
                if (chunkText.length() > 50) {
                    chunks.add(new Document(chunkText, Map.of(
                        "source", fileName,
                        "chunk_index", chunkIndex++,
                        "type", "contract_clause"
                    )));
                }
                currentChunk = new StringBuilder();
            }
            currentChunk.append(sentence).append(" ");
        }
        
        if (currentChunk.length() > 50) {
            chunks.add(new Document(currentChunk.toString().trim(), Map.of(
                "source", fileName,
                "chunk_index", chunkIndex,
                "type", "contract_clause"
            )));
        }
        
        return chunks;
    }
}
