package com.example.demo.controller;

import com.example.demo.model.DemandeTraitement;
import com.example.demo.service.HumanValidationService;
import com.example.demo.service.AuditService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Contrôleur pour la validation humaine selon BF10 du cahier des charges
 */
@RestController
@RequestMapping("/api/validation")
public class ValidationController {
    
    private static final Logger logger = Logger.getLogger(ValidationController.class.getName());
    
    private final HumanValidationService validationService;
    private final AuditService auditService;
    
    public ValidationController(HumanValidationService validationService, AuditService auditService) {
        this.validationService = validationService;
        this.auditService = auditService;
    }
    
    /**
     * Récupère toutes les demandes en attente de validation
     */
    @GetMapping("/en-attente")
    @PreAuthorize("hasRole('GESTIONNAIRE')")
    public ResponseEntity<ApiResponse<Map<String, DemandeTraitement>>> getDemandesEnAttente() {
        
        try {
            Map<String, DemandeTraitement> demandes = validationService.getDemandesEnAttente();
            
            return ResponseEntity.ok(new ApiResponse<>(
                true,
                "Demandes en attente récupérées",
                demandes,
                Map.of("count", demandes.size())
            ));
            
        } catch (Exception e) {
            logger.severe("Erreur lors de la récupération des demandes: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ApiResponse<>(false, "Erreur système", null, null));
        }
    }
    
    /**
     * Valide une demande spécifique
     */
    @PostMapping("/{id}/valider")
    @PreAuthorize("hasRole('GESTIONNAIRE')")
    public ResponseEntity<ApiResponse<String>> validerDemande(
            @PathVariable String id,
            @Valid @RequestBody ValidationRequest request,
            Authentication authentication) {
        
        try {
            HumanValidationService.ValidationResult result = validationService.validerDemande(
                id,
                authentication.getName(),
                request.decision(),
                request.justification(),
                request.corrections()
            );
            
            if (result.isSuccess()) {
                return ResponseEntity.ok(new ApiResponse<>(
                    true,
                    result.getMessage(),
                    "Validation effectuée",
                    null
                ));
            } else {
                return ResponseEntity.badRequest()
                    .body(new ApiResponse<>(false, result.getMessage(), null, null));
            }
            
        } catch (Exception e) {
            logger.severe("Erreur lors de la validation: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ApiResponse<>(false, "Erreur lors de la validation", null, null));
        }
    }
    
    /**
     * Génère un rapport d'audit pour une demande
     */
    @GetMapping("/{id}/rapport")
    @PreAuthorize("hasAnyRole('GESTIONNAIRE', 'ADMIN')")
    public ResponseEntity<ApiResponse<String>> genererRapportAudit(@PathVariable String id) {
        
        try {
            String rapport = auditService.genererRapportAudit(id);
            
            return ResponseEntity.ok(new ApiResponse<>(
                true,
                "Rapport généré",
                rapport,
                null
            ));
            
        } catch (Exception e) {
            logger.severe("Erreur lors de la génération du rapport: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ApiResponse<>(false, "Erreur lors de la génération", null, null));
        }
    }
    
    // DTOs
    
    /**
     * Requête de validation
     */
    public record ValidationRequest(
        @NotNull(message = "La décision est obligatoire")
        HumanValidationService.DecisionValidation decision,
        
        @NotBlank(message = "La justification est obligatoire")
        String justification,
        
        Map<String, Object> corrections
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
