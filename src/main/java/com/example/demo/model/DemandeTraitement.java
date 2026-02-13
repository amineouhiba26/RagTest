package com.example.demo.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DemandeTraitement {
    private String id;
    private String emailClient;
    private String contenuDemande;
    private List<String> photosUrls;
    private LocalDateTime dateReception;
    private SinistreType typeSinistre;
    private boolean conformite;
    private String raisonNonConformite;
    private Double estimationCout;
    private String commentairesEstimation;
    private StatutTraitement statut;

    // Enhanced metadata
    private Map<String, Object> metadata;
    private List<String> anomaliesDetectees;
    private boolean necessiteValidationHumaine;
    private String raisonValidationHumaine;
    private double scoreConformite;
    private List<String> auditLogs;

    public DemandeTraitement() {
        this.id = java.util.UUID.randomUUID().toString();
        this.dateReception = LocalDateTime.now();
        this.statut = StatutTraitement.EN_COURS;
        this.metadata = new HashMap<>();
        this.anomaliesDetectees = new ArrayList<>();
        this.auditLogs = new ArrayList<>();
        this.scoreConformite = 0.0;
        this.necessiteValidationHumaine = false;
        addAuditLog("Demande créée");
    }

    public DemandeTraitement(String emailClient, String contenuDemande, List<String> photosUrls) {
        this();
        this.emailClient = emailClient;
        this.contenuDemande = contenuDemande;
        this.photosUrls = photosUrls;
    }

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getEmailClient() { return emailClient; }
    public void setEmailClient(String emailClient) { this.emailClient = emailClient; }

    public String getContenuDemande() { return contenuDemande; }
    public void setContenuDemande(String contenuDemande) { this.contenuDemande = contenuDemande; }

    public List<String> getPhotosUrls() { return photosUrls; }
    public void setPhotosUrls(List<String> photosUrls) { this.photosUrls = photosUrls; }

    public LocalDateTime getDateReception() { return dateReception; }
    public void setDateReception(LocalDateTime dateReception) { this.dateReception = dateReception; }

    public SinistreType getTypeSinistre() { return typeSinistre; }
    public void setTypeSinistre(SinistreType typeSinistre) { this.typeSinistre = typeSinistre; }

    public boolean isConformite() { return conformite; }
    public void setConformite(boolean conformite) { this.conformite = conformite; }

    public String getRaisonNonConformite() { return raisonNonConformite; }
    public void setRaisonNonConformite(String raisonNonConformite) { this.raisonNonConformite = raisonNonConformite; }

    public Double getEstimationCout() { return estimationCout; }
    public void setEstimationCout(Double estimationCout) { this.estimationCout = estimationCout; }

    public String getCommentairesEstimation() { return commentairesEstimation; }
    public void setCommentairesEstimation(String commentairesEstimation) { this.commentairesEstimation = commentairesEstimation; }

    public StatutTraitement getStatut() { return statut; }
    public void setStatut(StatutTraitement statut) {
        this.statut = statut;
        addAuditLog("Statut changé à: " + statut.name());
    }

    public Map<String, Object> getMetadata() { return metadata; }
    public void setMetadata(Map<String, Object> metadata) { this.metadata = metadata; }
    public void addMetadata(String key, Object value) { this.metadata.put(key, value); }

    public List<String> getAnomaliesDetectees() { return anomaliesDetectees; }
    public void setAnomaliesDetectees(List<String> anomaliesDetectees) { this.anomaliesDetectees = anomaliesDetectees; }
    public void addAnomalie(String anomalie) { this.anomaliesDetectees.add(anomalie); }

    public boolean isNecessiteValidationHumaine() { return necessiteValidationHumaine; }
    public void setNecessiteValidationHumaine(boolean necessiteValidationHumaine) {
        this.necessiteValidationHumaine = necessiteValidationHumaine;
        if (necessiteValidationHumaine) {
            addAuditLog("Validation humaine requise");
        }
    }

    public String getRaisonValidationHumaine() { return raisonValidationHumaine; }
    public void setRaisonValidationHumaine(String raisonValidationHumaine) { this.raisonValidationHumaine = raisonValidationHumaine; }

    public double getScoreConformite() { return scoreConformite; }
    public void setScoreConformite(double scoreConformite) { this.scoreConformite = scoreConformite; }

    public List<String> getAuditLogs() { return auditLogs; }
    public void addAuditLog(String log) {
        this.auditLogs.add(LocalDateTime.now() + " - " + log);
    }

    public enum StatutTraitement {
        EN_COURS,
        CLASSIFIE,
        VALIDE,
        REJETE,
        ESTIME,
        TERMINE,
        EN_ATTENTE_VALIDATION_HUMAINE
    }
}
