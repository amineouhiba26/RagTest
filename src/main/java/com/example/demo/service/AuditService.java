package com.example.demo.service;

import com.example.demo.model.DemandeTraitement;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

/**
 * Service d'audit et de traçabilité selon BF11 du cahier des charges
 * Audit et traçabilité : Journalisation complète des actions des agents et des humains
 */
@Service
public class AuditService {
    
    private static final Logger logger = Logger.getLogger(AuditService.class.getName());
    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    
    // Stockage en mémoire des événements d'audit (en production, utiliser une base de données)
    private final Map<String, List<AuditEvent>> auditLogs = new ConcurrentHashMap<>();
    
    /**
     * Enregistre un événement d'audit pour une demande
     */
    public void enregistrerEvenement(String demandeId, 
                                   TypeEvenement type, 
                                   String acteur, 
                                   String action, 
                                   Map<String, Object> metadonnees) {
        
        AuditEvent event = new AuditEvent(
            LocalDateTime.now(),
            type,
            acteur,
            action,
            metadonnees != null ? new HashMap<>(metadonnees) : new HashMap<>()
        );
        
        auditLogs.computeIfAbsent(demandeId, k -> new ArrayList<>()).add(event);
        
        if (logger.isLoggable(java.util.logging.Level.INFO)) {
            logger.info(String.format("Audit [%s] %s - %s: %s", 
                demandeId, type, acteur, action));
        }
    }
    
    /**
     * Enregistre une action d'agent IA
     */
    public void enregistrerActionAgent(DemandeTraitement demande, 
                                     String nomAgent, 
                                     String action, 
                                     Map<String, Object> resultats) {
        
        Map<String, Object> metadonnees = new HashMap<>();
        metadonnees.put("score_confiance", demande.getScoreConformite());
        metadonnees.put("statut", demande.getStatut().name());
        if (resultats != null) {
            metadonnees.putAll(resultats);
        }
        
        enregistrerEvenement(
            demande.getId(),
            TypeEvenement.ACTION_AGENT,
            nomAgent,
            action,
            metadonnees
        );
    }
    
    /**
     * Enregistre une validation humaine
     */
    public void enregistrerValidationHumaine(String demandeId, 
                                           String utilisateur, 
                                           String decision, 
                                           String justification) {
        
        Map<String, Object> metadonnees = new HashMap<>();
        metadonnees.put("decision", decision);
        metadonnees.put("justification", justification);
        metadonnees.put("timestamp_validation", LocalDateTime.now().format(formatter));
        
        enregistrerEvenement(
            demandeId,
            TypeEvenement.VALIDATION_HUMAINE,
            utilisateur,
            "Validation: " + decision,
            metadonnees
        );
    }
    
    /**
     * Enregistre un événement système
     */
    public void enregistrerEvenementSysteme(String demandeId, 
                                          String evenement, 
                                          Map<String, Object> details) {
        
        enregistrerEvenement(
            demandeId,
            TypeEvenement.SYSTEME,
            "SYSTEME",
            evenement,
            details
        );
    }
    
    /**
     * Récupère l'historique d'audit pour une demande
     */
    public List<AuditEvent> obtenirHistoriqueAudit(String demandeId) {
        return new ArrayList<>(auditLogs.getOrDefault(demandeId, new ArrayList<>()));
    }
    
    /**
     * Génère un rapport d'audit pour une demande
     */
    public String genererRapportAudit(String demandeId) {
        List<AuditEvent> events = obtenirHistoriqueAudit(demandeId);
        
        if (events.isEmpty()) {
            return "Aucun événement d'audit trouvé pour la demande: " + demandeId;
        }
        
        StringBuilder rapport = new StringBuilder();
        rapport.append("=== RAPPORT D'AUDIT ===\n");
        rapport.append("Demande ID: ").append(demandeId).append("\n");
        rapport.append("Nombre d'événements: ").append(events.size()).append("\n");
        rapport.append("Période: ").append(events.get(0).getTimestamp().format(formatter))
               .append(" - ").append(events.get(events.size()-1).getTimestamp().format(formatter)).append("\n\n");
        
        for (AuditEvent event : events) {
            rapport.append(formatEvenement(event)).append("\n");
        }
        
        return rapport.toString();
    }
    
    /**
     * Formate un événement d'audit pour affichage
     */
    private String formatEvenement(AuditEvent event) {
        StringBuilder sb = new StringBuilder();
        sb.append("[").append(event.getTimestamp().format(formatter)).append("] ");
        sb.append(event.getType()).append(" | ");
        sb.append(event.getActeur()).append(" | ");
        sb.append(event.getAction());
        
        if (!event.getMetadonnees().isEmpty()) {
            sb.append(" | Détails: ");
            event.getMetadonnees().forEach((k, v) -> 
                sb.append(k).append("=").append(v).append("; "));
        }
        
        return sb.toString();
    }
    
    /**
     * Vérifie la conformité d'audit pour une demande
     */
    public boolean verifierConformiteAudit(String demandeId) {
        List<AuditEvent> events = obtenirHistoriqueAudit(demandeId);
        
        // Vérifications de conformité
        boolean aCreation = events.stream()
            .anyMatch(e -> e.getAction().contains("créée") || e.getAction().contains("reçue"));
        
        boolean aClassification = events.stream()
            .anyMatch(e -> e.getType() == TypeEvenement.ACTION_AGENT && 
                          e.getActeur().contains("Routeur"));
        
        boolean aValidation = events.stream()
            .anyMatch(e -> e.getType() == TypeEvenement.ACTION_AGENT && 
                          e.getActeur().contains("Validateur"));
        
        return aCreation && aClassification && aValidation;
    }
    
    /**
     * Types d'événements d'audit
     */
    public enum TypeEvenement {
        CREATION,
        ACTION_AGENT,
        VALIDATION_HUMAINE,
        MODIFICATION,
        SYSTEME,
        ERREUR
    }
    
    /**
     * Classe représentant un événement d'audit
     */
    public static class AuditEvent {
        private final LocalDateTime timestamp;
        private final TypeEvenement type;
        private final String acteur;
        private final String action;
        private final Map<String, Object> metadonnees;
        
        public AuditEvent(LocalDateTime timestamp, 
                         TypeEvenement type, 
                         String acteur, 
                         String action, 
                         Map<String, Object> metadonnees) {
            this.timestamp = timestamp;
            this.type = type;
            this.acteur = acteur;
            this.action = action;
            this.metadonnees = metadonnees;
        }
        
        // Getters
        public LocalDateTime getTimestamp() { return timestamp; }
        public TypeEvenement getType() { return type; }
        public String getActeur() { return acteur; }
        public String getAction() { return action; }
        public Map<String, Object> getMetadonnees() { return metadonnees; }
    }
}
