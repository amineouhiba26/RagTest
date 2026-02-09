package com.example.demo.controller;

import com.example.demo.service.LangChain4jRagService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;

@RestController
@RequestMapping("/api/langchain4j")
@CrossOrigin(origins = "*")
public class LangChain4jController {

    private final LangChain4jRagService langChain4jService;

    @Autowired
    public LangChain4jController(LangChain4jRagService langChain4jService) {
        this.langChain4jService = langChain4jService;
    }

    @PostMapping("/ingest")
    public ResponseEntity<Map<String, String>> ingestDocument(@RequestParam("file") MultipartFile file) {
        try {
            if (file.isEmpty()) {
                return ResponseEntity.badRequest()
                    .body(Map.of("error", "Please select a file to upload"));
            }

            // Save file temporarily
            String fileName = file.getOriginalFilename();
            Path tempFile = Paths.get(System.getProperty("java.io.tmpdir"), fileName);
            Files.write(tempFile, file.getBytes());

            // Ingest with LangChain4j
            langChain4jService.ingestPdfDocument(tempFile.toString());

            // Clean up temp file
            Files.deleteIfExists(tempFile);

            return ResponseEntity.ok(Map.of(
                "message", "Document ingested successfully with LangChain4j",
                "filename", fileName
            ));

        } catch (IOException e) {
            return ResponseEntity.internalServerError()
                .body(Map.of("error", "File processing failed: " + e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                .body(Map.of("error", "Document ingestion failed: " + e.getMessage()));
        }
    }

    @PostMapping("/ask")
    public ResponseEntity<Map<String, String>> askQuestion(@RequestBody Map<String, String> request) {
        try {
            String question = request.get("question");
            if (question == null || question.trim().isEmpty()) {
                return ResponseEntity.badRequest()
                    .body(Map.of("error", "Question cannot be empty"));
            }

            String answer = langChain4jService.askQuestion(question);

            return ResponseEntity.ok(Map.of(
                "question", question,
                "answer", answer,
                "source", "LangChain4j RAG"
            ));

        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                .body(Map.of("error", "Failed to process question: " + e.getMessage()));
        }
    }

    @PostMapping("/chat")
    public ResponseEntity<Map<String, String>> simpleChat(@RequestBody Map<String, String> request) {
        try {
            String message = request.get("message");
            if (message == null || message.trim().isEmpty()) {
                return ResponseEntity.badRequest()
                    .body(Map.of("error", "Message cannot be empty"));
            }

            String response = langChain4jService.getSimpleResponse(message);

            return ResponseEntity.ok(Map.of(
                "message", message,
                "response", response,
                "source", "LangChain4j Chat"
            ));

        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                .body(Map.of("error", "Chat failed: " + e.getMessage()));
        }
    }

    @GetMapping("/status")
    public ResponseEntity<Map<String, String>> getStatus() {
        return ResponseEntity.ok(Map.of(
            "status", "LangChain4j service is running",
            "version", "0.34.0",
            "features", "PDF ingestion, RAG, Chat"
        ));
    }
}
