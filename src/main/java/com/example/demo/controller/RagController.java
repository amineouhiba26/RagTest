package com.example.demo.controller;

import com.example.demo.service.RagService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Contrôleur pour la gestion du RAG et de l'ingestion documentaire (BF1, BF4)
 */
@RestController
@RequestMapping("/api/rag")
@CrossOrigin(origins = "*")
public class RagController {

    private static final Logger logger = Logger.getLogger(RagController.class.getName());

    private final RagService ragService;

    public RagController(RagService ragService) {
        this.ragService = ragService;
    }

    /**
     * BF1 — Ingestion documentaire : Upload et ingestion d'un contrat PDF
     */
    @PostMapping("/ingest")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, String>> ingestDocument(@RequestParam("file") MultipartFile file) {
        try {
            if (file.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "Fichier vide"));
            }

            String fileName = file.getOriginalFilename();
            Path tempFile = Paths.get(System.getProperty("java.io.tmpdir"), fileName);
            Files.write(tempFile, file.getBytes());

            ragService.ingestPdf(tempFile.toString());

            Files.deleteIfExists(tempFile);

            return ResponseEntity.ok(Map.of(
                "message", "Document ingéré avec succès dans la base vectorielle",
                "filename", fileName
            ));

        } catch (IOException e) {
            logger.severe("Erreur E/S lors de l'ingestion: " + e.getMessage());
            return ResponseEntity.internalServerError().body(Map.of("error", "Erreur E/S: " + e.getMessage()));
        } catch (Exception e) {
            logger.severe("Échec critique de l'ingestion: " + e.getMessage());
            return ResponseEntity.internalServerError().body(Map.of("error", "Échec de l'ingestion: " + e.getMessage()));
        }
    }

    /**
     * BF4 — Recherche sémantique : Test de recherche dans la base
     */
    @PostMapping("/search")
    @PreAuthorize("hasAnyRole('GESTIONNAIRE', 'ADMIN')")
    public ResponseEntity<Map<String, String>> searchInContracts(@RequestBody Map<String, String> request) {
        String query = request.get("query");
        if (query == null || query.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Requête vide"));
        }

        String context = ragService.searchContext(query);

        return ResponseEntity.ok(Map.of(
            "query", query,
            "context", context
        ));
    }
}
