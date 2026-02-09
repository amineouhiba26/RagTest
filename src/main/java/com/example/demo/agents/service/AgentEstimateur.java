package com.example.demo.agents.service;

import com.example.demo.agents.model.DemandeTraitement;
import dev.langchain4j.model.chat.ChatLanguageModel;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.logging.Logger;

@Service
public class AgentEstimateur {
    
    private static final Logger logger = Logger.getLogger(AgentEstimateur.class.getName());
    
    private final ChatLanguageModel chatModel;

    public AgentEstimateur(ChatLanguageModel chatModel) {
        this.chatModel = chatModel;
    }

    public double estimerCout(DemandeTraitement demande) {
        try {
            logger.info(() -> "Agent Estimateur: Estimation du coût pour la demande " + demande.getId());

            // Analyse des photos (simulation - dans un vrai système, on utiliserait un modèle multimodal)
            String analysePhotos = analyserPhotos(demande.getPhotosUrls());

            String prompt = """
                En tant qu'expert estimateur d'assurance, calculez une pré-estimation du coût de ce sinistre:
                
                Type de sinistre: %s
                Description: "%s"
                Analyse des photos: %s
                
                Basez votre estimation sur:
                1. Le type et l'étendue des dommages
                2. Les coûts moyens de réparation/remplacement
                3. La main d'œuvre nécessaire
                4. Les pièces et matériaux requis
                5. Les coûts indirects (expertise, location, etc.)
                
                Fournissez:
                - Une estimation en euros (montant numérique uniquement)
                - Une justification détaillée de votre calcul
                - Le niveau de confiance de votre estimation (Faible/Moyen/Élevé)
                
                Format: [MONTANT_EUROS] | [JUSTIFICATION] | [NIVEAU_CONFIANCE]
                Exemple: 2500.00 | Remplacement pare-brise + main d'œuvre | Élevé
                """.formatted(
                    demande.getTypeSinistre().getDescription(),
                    demande.getContenuDemande(),
                    analysePhotos
                );

            String reponse = chatModel.generate(prompt);
            
            // Parse de la réponse pour extraire le montant
            double montantEstime = extraireMontant(reponse);
            String justification = extraireJustification(reponse);
            
            demande.setEstimationCout(montantEstime);
            demande.setCommentairesEstimation(justification);
            demande.setStatut(DemandeTraitement.StatutTraitement.ESTIME);

            logger.info(() -> String.format("Estimation: %.2f€ - %s", montantEstime, justification));

            return montantEstime;

        } catch (Exception e) {
            logger.severe("Erreur lors de l'estimation: " + e.getMessage());
            demande.setEstimationCout(0.0);
            demande.setCommentairesEstimation("Erreur technique lors de l'estimation: " + e.getMessage());
            return 0.0;
        }
    }

    private String analyserPhotos(List<String> photosUrls) {
        if (photosUrls == null || photosUrls.isEmpty()) {
            return "Aucune photo fournie pour l'analyse visuelle.";
        }

        // Simulation d'analyse de photos
        // Dans un vrai système, on utiliserait un modèle multimodal comme GPT-4V ou Claude 3
        StringBuilder analyse = new StringBuilder();
        analyse.append("Analyse simulée de ").append(photosUrls.size()).append(" photo(s):\n");
        
        for (int i = 0; i < photosUrls.size(); i++) {
            String url = photosUrls.get(i);
            analyse.append("- Photo ").append(i + 1).append(" (").append(url).append("): ");
            
            // Simulation basée sur le nom du fichier ou URL
            String urlLower = url.toLowerCase();
            if (urlLower.contains("pare") || urlLower.contains("brise") || urlLower.contains("vitre")) {
                analyse.append("Dommage visible sur pare-brise avec impact et fissures radiaires.");
            } else if (urlLower.contains("carrosserie") || urlLower.contains("aile") || urlLower.contains("portiere")) {
                analyse.append("Déformation importante de la carrosserie, rayures profondes.");
            } else if (urlLower.contains("interieur") || urlLower.contains("tableau")) {
                analyse.append("Dégâts à l'habitacle, éléments du tableau de bord endommagés.");
            } else if (urlLower.contains("eau") || urlLower.contains("inondation")) {
                analyse.append("Traces d'eau visibles, infiltration importante détectée.");
            } else {
                analyse.append("Dommages généraux visibles nécessitant évaluation détaillée.");
            }
            analyse.append("\n");
        }
        
        return analyse.toString();
    }

    private double extraireMontant(String reponse) {
        try {
            // Recherche du montant avant le premier "|"
            String[] parties = reponse.split("\\|");
            if (parties.length > 0) {
                String montantStr = parties[0].trim()
                    .replaceAll("[^0-9.,]", "") // Garde seulement les chiffres et séparateurs
                    .replace(",", "."); // Normalise les décimales
                
                if (!montantStr.isEmpty()) {
                    return Double.parseDouble(montantStr);
                }
            }
        } catch (NumberFormatException e) {
            logger.warning("Impossible de parser le montant: " + reponse);
        }
        
        // Estimation par défaut basée sur le type de sinistre
        return 1000.0;
    }

    private String extraireJustification(String reponse) {
        try {
            String[] parties = reponse.split("\\|");
            if (parties.length >= 2) {
                return parties[1].trim();
            }
        } catch (Exception e) {
            logger.warning("Impossible d'extraire la justification: " + e.getMessage());
        }
        
        return "Estimation basée sur les données disponibles.";
    }

    public String genererRapportEstimation(DemandeTraitement demande) {
        try {
            String prompt = """
                Générez un rapport d'estimation détaillé pour ce sinistre:
                
                ID: %s
                Type: %s
                Estimation: %.2f€
                Justification: %s
                Photos analysées: %d
                
                Créez un rapport professionnel incluant:
                - Méthodologie d'estimation utilisée
                - Détail des coûts par poste
                - Facteurs de risque identifiés
                - Recommandations pour l'expertise
                - Comparaison avec les barèmes standards
                
                Le rapport doit être précis et justifier chaque élément de coût.
                """.formatted(
                    demande.getId(),
                    demande.getTypeSinistre().getDescription(),
                    demande.getEstimationCout() != null ? demande.getEstimationCout() : 0.0,
                    demande.getCommentairesEstimation() != null ? demande.getCommentairesEstimation() : "N/A",
                    demande.getPhotosUrls() != null ? demande.getPhotosUrls().size() : 0
                );

            return chatModel.generate(prompt);

        } catch (Exception e) {
            logger.severe("Erreur lors de la génération du rapport d'estimation: " + e.getMessage());
            return "Impossible de générer le rapport d'estimation.";
        }
    }

    public boolean necessiteExpertiseComplementaire(DemandeTraitement demande) {
        try {
            String prompt = """
                Déterminez si ce sinistre nécessite une expertise complémentaire:
                
                Type: %s
                Estimation: %.2f€
                Description: "%s"
                
                Considérez:
                - Complexité des dommages
                - Montant de l'estimation
                - Incertitudes techniques
                - Risques de sous-estimation
                
                Répondez par OUI ou NON avec justification.
                """.formatted(
                    demande.getTypeSinistre().getDescription(),
                    demande.getEstimationCout() != null ? demande.getEstimationCout() : 0.0,
                    demande.getContenuDemande()
                );

            String reponse = chatModel.generate(prompt);
            return reponse.toUpperCase().startsWith("OUI");

        } catch (Exception e) {
            logger.severe("Erreur lors de l'évaluation d'expertise: " + e.getMessage());
            return true; // Par sécurité, recommander une expertise en cas d'erreur
        }
    }
}
