package com.example.demo.agents.service;

import com.example.demo.model.DemandeTraitement;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Service de calcul de score de confiance selon BF9 du cahier des charges
 * Gestion de la confiance : Calcul d'un score de confiance pour chaque décision automatisée
 */
@Service
public class ConfidenceScoreService {
    
    private static final Logger logger = Logger.getLogger(ConfidenceScoreService.class.getName());
    
    // Seuils de confiance
    private static final double SEUIL_VALIDATION_HUMAINE = 0.7;
    private static final double SEUIL_CONFIANCE_ELEVEE = 0.85;
    
    // Constantes pour les clés de scores
    private static final String SCORE_CLASSIFICATION = "classification";
    private static final String SCORE_COMPLETUDE = "completude";
    private static final String SCORE_CONFORMITE = "conformite";
    private static final String SCORE_COHERENCE = "coherence";
    
    /**
     * Calcule un score de confiance global pour une demande de traitement
     */
    public double calculerScoreConfiance(DemandeTraitement demande) {
        try {
            Map<String, Double> scores = new HashMap<>();
            
            // Score de classification (basé sur la clarté du type de sinistre)
            scores.put(SCORE_CLASSIFICATION, calculerScoreClassification(demande));
            
            // Score de complétude des données
            scores.put(SCORE_COMPLETUDE, calculerScoreCompletude(demande));
            
            // Score de conformité contractuelle
            scores.put(SCORE_CONFORMITE, calculerScoreConformite(demande));
            
            // Score de cohérence des informations
            scores.put(SCORE_COHERENCE, calculerScoreCoherence(demande));
            
            // Calcul du score global pondéré
            double scoreGlobal = calculerScorePondere(scores);
            
            // Détermination de la nécessité de validation humaine
            evaluerNecessiteValidationHumaine(demande, scoreGlobal, scores);
            
            demande.setScoreConformite(scoreGlobal);
            demande.addAuditLog(String.format("Score de confiance calculé: %.2f", scoreGlobal));
            
            if (logger.isLoggable(java.util.logging.Level.INFO)) {
                logger.info(String.format("Score de confiance pour demande %s: %.2f", 
                    demande.getId(), scoreGlobal));
            }
            
            return scoreGlobal;
            
        } catch (Exception e) {
            logger.severe("Erreur lors du calcul du score de confiance: " + e.getMessage());
            demande.addAuditLog("Erreur calcul score: " + e.getMessage());
            return 0.0; // Score minimum en cas d'erreur
        }
    }
    
    /**
     * Calcule le score de classification basé sur la clarté du type de sinistre
     */
    private double calculerScoreClassification(DemandeTraitement demande) {
        if (demande.getTypeSinistre() == null) {
            return 0.0;
        }
        
        // Score plus élevé pour les types bien définis
        return switch (demande.getTypeSinistre()) {
            case ACCIDENT_AUTOMOBILE, INCENDIE -> 0.9;
            case DEGATS_EAU, VOL, BRIS_DE_GLACE -> 0.8;
            case CATASTROPHE_NATURELLE, RESPONSABILITE_CIVILE -> 0.7;
            case AUTRES -> 0.3;
        };
    }
    
    /**
     * Calcule le score de complétude des données fournies
     */
    private double calculerScoreCompletude(DemandeTraitement demande) {
        double score = 0.0;
        
        // Vérification des champs obligatoires
        if (demande.getEmailClient() != null && !demande.getEmailClient().trim().isEmpty()) {
            score += 0.2;
        }
        
        if (demande.getContenuDemande() != null && demande.getContenuDemande().length() > 50) {
            score += 0.3;
        }
        
        if (demande.getPhotosUrls() != null && !demande.getPhotosUrls().isEmpty()) {
            score += 0.2;
        }
        
        // Vérification des métadonnées extraites
        if (demande.getMetadata().containsKey("date_incident")) {
            score += 0.15;
        }
        
        if (demande.getMetadata().containsKey("montant_estime")) {
            score += 0.15;
        }
        
        return Math.min(score, 1.0);
    }
    
    /**
     * Calcule le score de conformité contractuelle
     */
    private double calculerScoreConformite(DemandeTraitement demande) {
        // Si conformité déjà validée
        if (demande.isConformite()) {
            return 0.95;
        }
        
        // Si non conformité détectée (raison présente)
        if (demande.getRaisonNonConformite() != null && !demande.getRaisonNonConformite().isEmpty()) {
            return 0.1;
        }
        
        // Score neutre si pas encore vérifié
        return 0.7;
    }
    
    /**
     * Calcule le score de cohérence des informations
     */
    private double calculerScoreCoherence(DemandeTraitement demande) {
        double score = 0.8; // Score de base
        
        // Réduction du score en fonction des anomalies détectées
        int nombreAnomalies = demande.getAnomaliesDetectees().size();
        score -= (nombreAnomalies * 0.1);
        
        return Math.max(score, 0.0);
    }
    
    /**
     * Calcule le score pondéré final
     */
    private double calculerScorePondere(Map<String, Double> scores) {
        double scoreClassification = scores.get(SCORE_CLASSIFICATION) * 0.3;
        double scoreCompletude = scores.get(SCORE_COMPLETUDE) * 0.25;
        double scoreConformite = scores.get(SCORE_CONFORMITE) * 0.3;
        double scoreCoherence = scores.get(SCORE_COHERENCE) * 0.15;
        
        return scoreClassification + scoreCompletude + scoreConformite + scoreCoherence;
    }
    
    /**
     * Détermine si une validation humaine est nécessaire
     */
    private void evaluerNecessiteValidationHumaine(DemandeTraitement demande, 
                                                  double scoreGlobal, 
                                                  Map<String, Double> scores) {
        
        if (scoreGlobal < SEUIL_VALIDATION_HUMAINE) {
            demande.setNecessiteValidationHumaine(true);
            demande.setRaisonValidationHumaine(
                String.format("Score de confiance insuffisant: %.2f < %.2f", 
                    scoreGlobal, SEUIL_VALIDATION_HUMAINE));
            return;
        }
        
        // Vérification de scores critiques individuels
        if (scores.get(SCORE_CONFORMITE) < 0.6) {
            demande.setNecessiteValidationHumaine(true);
            demande.setRaisonValidationHumaine("Score de conformité contractuelle trop faible");
            return;
        }
        
        if (scores.get(SCORE_CLASSIFICATION) < 0.5) {
            demande.setNecessiteValidationHumaine(true);
            demande.setRaisonValidationHumaine("Classification du sinistre incertaine");
            return;
        }
        
        // Vérification des anomalies
        if (demande.getAnomaliesDetectees() != null && !demande.getAnomaliesDetectees().isEmpty()) {
            demande.setNecessiteValidationHumaine(true);
            demande.setRaisonValidationHumaine("Anomalies détectées nécessitant validation");
            return;
        }
        
        // Cas spéciaux nécessitant toujours une validation humaine
        if (demande.getEstimationCout() != null && demande.getEstimationCout() > 10000) {
            demande.setNecessiteValidationHumaine(true);
            demande.setRaisonValidationHumaine("Montant élevé nécessitant validation humaine");
            return;
        }
        
        demande.setNecessiteValidationHumaine(false);
        demande.setRaisonValidationHumaine(null);
    }
    
    /**
     * Vérifie si le score de confiance est suffisamment élevé pour une automatisation complète
     */
    public boolean isConfianceElevee(double score) {
        return score >= SEUIL_CONFIANCE_ELEVEE;
    }
    
    /**
     * Fournit une explication textuelle du score de confiance
     */
    public String expliquerScore(double score) {
        if (score >= SEUIL_CONFIANCE_ELEVEE) {
            return "Confiance élevée - Traitement automatique recommandé";
        } else if (score >= SEUIL_VALIDATION_HUMAINE) {
            return "Confiance modérée - Traitement automatique possible avec supervision";
        } else {
            return "Confiance faible - Validation humaine requise";
        }
    }
}
