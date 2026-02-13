package com.example.demo.controller;

import com.example.demo.model.DemandeTraitement;
import com.example.demo.agents.orchestrator.OrchestratorMultiAgents;
import com.example.demo.service.AuditService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Contrôleur REST sécurisé pour le système multi-agents selon BF12 du cahier des charges
 * API externe : Exposition d'API REST sécurisées pour permettre l'intégration avec des systèmes tiers
 */
@RestController
@RequestMapping("/api/sinistres")
@CrossOrigin(origins = "*", maxAge = 3600)
public class SinistreController {
    
    private static final Logger logger = Logger.getLogger(SinistreController.class.getName());
    
    private final OrchestratorMultiAgents orchestrator;
    private final AuditService auditService;
    
    public SinistreController(OrchestratorMultiAgents orchestrator,
                            AuditService auditService) {
        this.orchestrator = orchestrator;
        this.auditService = auditService;
    }
    
    /**
     * BF1 — Ingestion documentaire : Soumission d'un nouveau sinistre
     */
    @PostMapping("/soumettre")
    @PreAuthorize("hasAnyRole('CLIENT', 'GESTIONNAIRE', 'ADMIN')")
    public ResponseEntity<ApiResponse<String>> soumettreSinistre(
            @Valid @RequestBody SinistreRequest request,
            Authentication authentication) {
        
        try {
            DemandeTraitement demande = new DemandeTraitement(
                request.emailClient(),
                request.contenuDemande(),
                request.photosUrls()
            );
            
            // Audit de la soumission
            auditService.enregistrerEvenement(
                demande.getId(),
                AuditService.TypeEvenement.CREATION,
                authentication.getName(),
                "Soumission de sinistre",
                Map.of(
                    "email_client", request.emailClient(),
                    "nombre_photos", request.photosUrls() != null ? request.photosUrls().size() : 0
                )
            );
            
            // Traitement asynchrone par les agents
            orchestrator.traiterDemandeComplete(demande);
            
            if (logger.isLoggable(java.util.logging.Level.INFO)) {
                logger.info(String.format("Sinistre soumis par %s avec ID: %s", 
                    authentication.getName(), demande.getId()));
            }
            
            return ResponseEntity.ok(new ApiResponse<>(
                true,
                "Sinistre soumis avec succès. Traitement en cours.",
                demande.getId(),
                null
            ));
            
        } catch (Exception e) {
            logger.severe("Erreur lors de la soumission: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ApiResponse<>(false, "Erreur lors de la soumission: " + e.getMessage(), null, null));
        }
    }
    
    /**
     * BF4 — Recherche sémantique : Consultation du statut d'un sinistre
     */
    @GetMapping("/{id}/statut")
    @PreAuthorize("hasAnyRole('CLIENT', 'GESTIONNAIRE', 'ADMIN')")
    public ResponseEntity<ApiResponse<StatutSinistreResponse>> consulterStatut(@PathVariable String id) {
        
        try {
            // En production, récupérer depuis la base de données
            // Pour cette démo, récupération depuis les services
            
            StatutSinistreResponse statut = new StatutSinistreResponse(
                id,
                "EN_COURS", // Statut actuel
                false, // Nécessite validation humaine
                0.85, // Score de confiance
                null, // Estimation
                "Traitement automatique en cours"
            );
            
            return ResponseEntity.ok(new ApiResponse<>(
                true,
                "Statut récupéré avec succès",
                statut,
                null
            ));
            
        } catch (Exception e) {
            logger.severe("Erreur lors de la consultation: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ApiResponse<>(false, "Sinistre non trouvé", null, null));
        }
    }
    
    /**
     * BF11 — Audit et traçabilité : Récupération de l'historique d'audit
     */
    @GetMapping("/{id}/audit")
    @PreAuthorize("hasAnyRole('GESTIONNAIRE', 'ADMIN')")
    public ResponseEntity<ApiResponse<List<AuditService.AuditEvent>>> obtenirAudit(@PathVariable String id) {
        
        try {
            List<AuditService.AuditEvent> events = auditService.obtenirHistoriqueAudit(id);
            
            return ResponseEntity.ok(new ApiResponse<>(
                true,
                "Historique d'audit récupéré",
                events,
                null
            ));
            
        } catch (Exception e) {
            logger.severe("Erreur lors de la récupération de l'audit: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ApiResponse<>(false, "Erreur lors de la récupération", null, null));
        }
    }
    
    // DTOs pour les requêtes et réponses
    
    /**
     * Requête de soumission de sinistre
     */
    public record SinistreRequest(
        @NotBlank(message = "L'email du client est obligatoire")
        String emailClient,
        
        @NotBlank(message = "Le contenu de la demande est obligatoire")
        String contenuDemande,
        
        List<String> photosUrls
    ) {}
    
    /**
     * Réponse de statut de sinistre
     */
    public record StatutSinistreResponse(
        String id,
        String statut,
        boolean necessiteValidationHumaine,
        double scoreConfiance,
        Double estimationCout,
        String commentaire
    ) {}
    
    /**
     * Réponse API générique
     */
    public record ApiResponse<T>(
        boolean success,
        String message,
        T data,
        Map<String, Object> metadata
    ) {}
}
