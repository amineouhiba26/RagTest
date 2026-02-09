package com.example.demo.agents.model;

import java.time.LocalDateTime;
import java.util.List;

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

    public DemandeTraitement() {
        this.id = java.util.UUID.randomUUID().toString();
        this.dateReception = LocalDateTime.now();
        this.statut = StatutTraitement.EN_COURS;
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
    public void setStatut(StatutTraitement statut) { this.statut = statut; }

    public enum StatutTraitement {
        EN_COURS, VALIDE, REJETE, ESTIME, TERMINE
    }
}
