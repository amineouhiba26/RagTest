package com.example.demo.service;

import com.example.demo.model.DemandeTraitement;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

/**
 * Service de validation humaine selon BF10 du cahier des charges
 * Validation humaine : Permet à un employé de vérifier, corriger ou valider les décisions proposées par les agents IA
 */
@Service
public class HumanValidationService {
    
    private static final Logger logger = Logger.getLogger(HumanValidationService.class.getName());
    
    // Constantes pour les métadonnées
    private static final String VALIDATION_HUMAINE_KEY = "validation_humaine";
    private static final String DECISION_KEY = "decision";
    private static final String GESTIONNAIRE_KEY = "gestionnaire";
    private static final String JUSTIFICATION_KEY = "justification";
    private static final String TIMESTAMP_KEY = "timestamp";
    
    private final AuditService auditService;
    
    // Stockage temporaire des demandes en attente de validation
    private final Map<String, DemandeTraitement> demandesEnAttente = new ConcurrentHashMap<>();
    
    public HumanValidationService(AuditService auditService) {
        this.auditService = auditService;
    }
    
    /**
     * Soumet une demande pour validation humaine
     */
    public void soumettreValidation(DemandeTraitement demande, String raisonValidation) {
        demande.setStatut(DemandeTraitement.StatutTraitement.EN_ATTENTE_VALIDATION_HUMAINE);
        demande.setRaisonValidationHumaine(raisonValidation);
        demande.addAuditLog("Soumise pour validation humaine: " + raisonValidation);
        
        demandesEnAttente.put(demande.getId(), demande);
        
        // Audit de la soumission
        Map<String, Object> metadonnees = new HashMap<>();
        metadonnees.put("raison", raisonValidation);
        metadonnees.put("score_confiance", demande.getScoreConformite());
        
        auditService.enregistrerEvenementSysteme(
            demande.getId(),
            "Soumission pour validation humaine",
            metadonnees
        );
        
        if (logger.isLoggable(java.util.logging.Level.INFO)) {
            logger.info(String.format("Demande %s soumise pour validation humaine: %s", 
                demande.getId(), raisonValidation));
        }
    }
    
    /**
     * Valide une demande par un gestionnaire humain
     */
    public ValidationResult validerDemande(String demandeId, 
                                         String gestionnaire, 
                                         DecisionValidation decision, 
                                         String justification, 
                                         Map<String, Object> corrections) {
        
        DemandeTraitement demande = demandesEnAttente.get(demandeId);
        if (demande == null) {
            return new ValidationResult(false, "Demande non trouvée ou non en attente de validation");
        }
        
        try {
            // Application de la décision
            switch (decision) {
                case APPROUVER -> approuverDemande(demande, gestionnaire, justification);
                case REJETER -> rejeterDemande(demande, gestionnaire, justification);
                case CORRIGER -> corrigerDemande(demande, gestionnaire, justification, corrections);
                case DEMANDER_INFORMATION -> demanderInformationSupplement(demande, gestionnaire, justification);
            }
            
            // Enregistrement de l'audit
            auditService.enregistrerValidationHumaine(
                demandeId, 
                gestionnaire, 
                decision.toString(), 
                justification
            );
            
            demande.addAuditLog(String.format("Validation par %s: %s - %s", 
                gestionnaire, decision, justification));
            
            // Retirer de la file d'attente si traitement terminé
            if (decision != DecisionValidation.DEMANDER_INFORMATION) {
                demandesEnAttente.remove(demandeId);
            }
            
            return new ValidationResult(true, "Validation effectuée avec succès");
            
        } catch (Exception e) {
            logger.severe("Erreur lors de la validation: " + e.getMessage());
            return new ValidationResult(false, "Erreur lors de la validation: " + e.getMessage());
        }
    }
    
    /**
     * Approuve la demande
     */
    private void approuverDemande(DemandeTraitement demande, String gestionnaire, String justification) {
        demande.setStatut(DemandeTraitement.StatutTraitement.VALIDE);
        demande.setNecessiteValidationHumaine(false);
        demande.addMetadata(VALIDATION_HUMAINE_KEY, Map.of(
            DECISION_KEY, "APPROUVE",
            GESTIONNAIRE_KEY, gestionnaire,
            JUSTIFICATION_KEY, justification,
            TIMESTAMP_KEY, LocalDateTime.now()
        ));
        
        if (logger.isLoggable(java.util.logging.Level.INFO)) {
            logger.info(String.format("Demande %s approuvée par %s", demande.getId(), gestionnaire));
        }
    }
    
    /**
     * Rejette la demande
     */
    private void rejeterDemande(DemandeTraitement demande, String gestionnaire, String justification) {
        demande.setStatut(DemandeTraitement.StatutTraitement.REJETE);
        demande.setRaisonNonConformite(justification);
        demande.addMetadata(VALIDATION_HUMAINE_KEY, Map.of(
            DECISION_KEY, "REJETE",
            GESTIONNAIRE_KEY, gestionnaire,
            JUSTIFICATION_KEY, justification,
            TIMESTAMP_KEY, LocalDateTime.now()
        ));
        
        if (logger.isLoggable(java.util.logging.Level.INFO)) {
            logger.info(String.format("Demande %s rejetée par %s: %s", 
                demande.getId(), gestionnaire, justification));
        }
    }
    
    /**
     * Corrige la demande avec des modifications
     */
    private void corrigerDemande(DemandeTraitement demande, 
                               String gestionnaire, 
                               String justification, 
                               Map<String, Object> corrections) {
        
        if (corrections != null && !corrections.isEmpty()) {
            corrections.forEach((key, value) -> {
                switch (key) {
                    case "type_sinistre" -> {
                        if (value instanceof String) {
                            try {
                                demande.setTypeSinistre(com.example.demo.model.SinistreType.valueOf((String) value));
                            } catch (IllegalArgumentException e) {
                                logger.warning("Type de sinistre invalide: " + value);
                            }
                        }
                    }
                    case "estimation_cout" -> {
                        if (value instanceof Number) {
                            demande.setEstimationCout(((Number) value).doubleValue());
                        }
                    }
                    case "conformite" -> {
                        if (value instanceof Boolean) {
                            demande.setConformite((Boolean) value);
                        }
                    }
                    default -> demande.addMetadata("correction_" + key, value);
                }
            });
        }
        
        demande.setStatut(DemandeTraitement.StatutTraitement.VALIDE);
        demande.setNecessiteValidationHumaine(false);
        demande.addMetadata(VALIDATION_HUMAINE_KEY, Map.of(
            DECISION_KEY, "CORRIGE",
            GESTIONNAIRE_KEY, gestionnaire,
            JUSTIFICATION_KEY, justification,
            "corrections", corrections != null ? corrections : Map.of(),
            TIMESTAMP_KEY, LocalDateTime.now()
        ));
        
        if (logger.isLoggable(java.util.logging.Level.INFO)) {
            logger.info(String.format("Demande %s corrigée par %s avec %d corrections", 
                demande.getId(), gestionnaire, corrections != null ? corrections.size() : 0));
        }
    }
    
    /**
     * Demande des informations supplémentaires
     */
    private void demanderInformationSupplement(DemandeTraitement demande, String gestionnaire, String justification) {
        demande.addMetadata("information_supplementaire_requise", Map.of(
            GESTIONNAIRE_KEY, gestionnaire,
            "demande", justification,
            TIMESTAMP_KEY, LocalDateTime.now()
        ));
        
        // La demande reste en attente de validation
        demande.addAuditLog("Informations supplémentaires demandées: " + justification);
        
        if (logger.isLoggable(java.util.logging.Level.INFO)) {
            logger.info(String.format("Informations supplémentaires demandées pour %s par %s", 
                demande.getId(), gestionnaire));
        }
    }
    
    /**
     * Récupère toutes les demandes en attente de validation
     */
    public Map<String, DemandeTraitement> getDemandesEnAttente() {
        return new HashMap<>(demandesEnAttente);
    }
    
    /**
     * Vérifie si une demande est en attente de validation
     */
    public boolean estEnAttenteValidation(String demandeId) {
        return demandesEnAttente.containsKey(demandeId);
    }
    
    /**
     * Énumérations pour les décisions de validation
     */
    public enum DecisionValidation {
        APPROUVER,
        REJETER, 
        CORRIGER,
        DEMANDER_INFORMATION
    }
    
    /**
     * Classe de résultat de validation
     */
    public static class ValidationResult {
        private final boolean success;
        private final String message;
        
        public ValidationResult(boolean success, String message) {
            this.success = success;
            this.message = message;
        }
        
        public boolean isSuccess() { return success; }
        public String getMessage() { return message; }
    }
}
