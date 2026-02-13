package com.example.demo.agents.service;

import com.example.demo.model.DemandeTraitement;
import com.example.demo.model.SinistreType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Tests du Service de Score de Confiance")
class ConfidenceScoreServiceTest {

    private ConfidenceScoreService confidenceScoreService;
    private DemandeTraitement demande;

    @BeforeEach
    void setUp() {
        confidenceScoreService = new ConfidenceScoreService();
        
        // Création d'une demande de test
        demande = new DemandeTraitement(
            "client@example.com",
            "Dégât des eaux dans ma cuisine suite à une fuite de canalisation. " +
            "L'eau a endommagé le parquet et les meubles bas. Photos en pièce jointe.",
            List.of("photo1.jpg", "photo2.jpg")
        );
        
        demande.setTypeSinistre(SinistreType.DEGATS_EAU);
    }

    @Test
    @DisplayName("Test du calcul de score de confiance pour une demande complète")
    void testCalculerScoreConfianceDemandeComplete() {
        // Given - Demande bien remplie
        demande.setConformite(true);
        demande.addMetadata("date_incident", "2026-02-10");
        demande.addMetadata("montant_estime", "2500");

        // When
        double score = confidenceScoreService.calculerScoreConfiance(demande);

        // Then
        assertTrue(score > 0.7, "Le score devrait être supérieur à 0.7 pour une demande complète");
        assertFalse(demande.isNecessiteValidationHumaine(), "Une demande avec un bon score ne devrait pas nécessiter de validation humaine");
        assertEquals(score, demande.getScoreConformite(), "Le score devrait être sauvegardé dans la demande");
    }

    @Test
    @DisplayName("Test du calcul de score pour une demande incomplète")
    void testCalculerScoreConfianceDemandeIncomplete() {
        // Given - Demande incomplète
        demande.setContenuDemande("Dégât");  // Contenu très court
        demande.setPhotosUrls(null);  // Pas de photos
        // Pas de métadonnées

        // When
        double score = confidenceScoreService.calculerScoreConfiance(demande);

        // Then
        assertTrue(score < 0.7, "Le score devrait être faible pour une demande incomplète");
        assertTrue(demande.isNecessiteValidationHumaine(), "Une demande incomplète devrait nécessiter une validation humaine");
        assertNotNull(demande.getRaisonValidationHumaine(), "La raison de validation humaine devrait être définie");
    }

    @Test
    @DisplayName("Test du calcul de score avec anomalies détectées")
    void testCalculerScoreConfianceAvecAnomalies() {
        // Given - Demande avec anomalies
        demande.addAnomalie("Date d'incident manquante");
        demande.addAnomalie("Montant incohérent");

        // When
        double score = confidenceScoreService.calculerScoreConfiance(demande);

        // Then
        assertTrue(score < 0.8, "Les anomalies devraient réduire le score");
        assertTrue(demande.isNecessiteValidationHumaine(), "Les anomalies devraient déclencher une validation humaine");
    }

    @Test
    @DisplayName("Test du seuil de montant élevé")
    void testSeuilMontantEleve() {
        // Given - Demande avec montant élevé
        demande.setEstimationCout(15000.0);  // Montant > 10k
        demande.setConformite(true);
        demande.addMetadata("date_incident", "2026-02-10");

        // When
        confidenceScoreService.calculerScoreConfiance(demande);

        // Then
        assertTrue(demande.isNecessiteValidationHumaine(), "Un montant élevé devrait toujours déclencher une validation humaine");
        assertEquals("Montant élevé nécessitant validation humaine", demande.getRaisonValidationHumaine());
    }

    @Test
    @DisplayName("Test de la méthode isConfianceElevee")
    void testIsConfianceElevee() {
        // Test des seuils
        assertTrue(confidenceScoreService.isConfianceElevee(0.85), "Score >= 0.85 devrait être considéré comme élevé");
        assertTrue(confidenceScoreService.isConfianceElevee(0.90), "Score >= 0.85 devrait être considéré comme élevé");
        assertFalse(confidenceScoreService.isConfianceElevee(0.80), "Score < 0.85 ne devrait pas être considéré comme élevé");
    }

    @Test
    @DisplayName("Test de l'explication du score")
    void testExpliquerScore() {
        // Test des différentes explications
        String explicationElevee = confidenceScoreService.expliquerScore(0.90);
        assertTrue(explicationElevee.contains("Confiance élevée"));
        
        String explicationModeree = confidenceScoreService.expliquerScore(0.75);
        assertTrue(explicationModeree.contains("Confiance modérée"));
        
        String explicationFaible = confidenceScoreService.expliquerScore(0.60);
        assertTrue(explicationFaible.contains("Confiance faible"));
    }

    @Test
    @DisplayName("Test de robustesse avec données nulles")
    void testRobustesseAvecDonneesNulles() {
        // Given - Demande avec données manquantes
        DemandeTraitement demandeVide = new DemandeTraitement();
        demandeVide.setTypeSinistre(null);
        demandeVide.setContenuDemande(null);

        // When
        double score = confidenceScoreService.calculerScoreConfiance(demandeVide);

        // Then
        assertNotNull(score, "Le service devrait gérer les données nulles");
        assertTrue(score >= 0.0, "Le score ne devrait jamais être négatif");
        assertTrue(demandeVide.isNecessiteValidationHumaine(), "Une demande vide devrait nécessiter une validation humaine");
    }
}
