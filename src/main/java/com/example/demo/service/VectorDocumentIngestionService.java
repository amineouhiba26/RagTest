package com.example.demo.service;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class VectorDocumentIngestionService {

    @Value("${app.pdf.chunk-size:800}")
    private int chunkSize;

    private final VectorStore vectorStore;

    public VectorDocumentIngestionService(VectorStore vectorStore) {
        this.vectorStore = vectorStore;
    }


    private void addChunk(List<Document> chunks, StringBuilder buffer, String fileName, int index) {

        String content = buffer.toString().trim();

        if (content.length() < 50) return;

        chunks.add(new Document(
                content,
                Map.of(
                        "source", fileName,
                        "chunk_index", index
                )
        ));
    }

    private List<Document> createChunks(String text, String fileName) {

        List<Document> chunks = new ArrayList<>();
        String[] sentences = text.split("(?<=[.!?])\\s+");

        StringBuilder buffer = new StringBuilder();
        int index = 0;

        for (String sentence : sentences) {

            if (buffer.length() + sentence.length() > chunkSize) {
                addChunk(chunks, buffer, fileName, index++);
                buffer.setLength(0);
            }

            buffer.append(sentence).append(" ");
        }

        addChunk(chunks, buffer, fileName, index);

        return chunks;
    }

    public void ingestPdfToVectorStore(String filePath) {

        try (PDDocument pdf = PDDocument.load(new File(filePath))) {

            String fullText = new PDFTextStripper().getText(pdf);

            List<Document> chunks = createChunks(fullText, new File(filePath).getName());
            vectorStore.add(chunks);

        } catch (Exception e) {
            throw new RuntimeException("PDF ingestion failed", e);
        }
    }

}
