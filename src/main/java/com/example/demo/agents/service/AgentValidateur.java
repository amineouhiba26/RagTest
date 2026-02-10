package com.example.demo.agents.service;

import com.example.demo.agents.model.DemandeTraitement;
import com.example.demo.service.LangChain4jRagService;
import dev.langchain4j.model.chat.ChatLanguageModel;
import org.springframework.stereotype.Service;

import java.util.logging.Logger;

@Service
public class AgentValidateur {
    
    private static final Logger logger = Logger.getLogger(AgentValidateur.class.getName());
    
    private final ChatLanguageModel chatModel;
    private final LangChain4jRagService ragService;

    public AgentValidateur(ChatLanguageModel chatModel, LangChain4jRagService ragService) {
        this.chatModel = chatModel;
        this.ragService = ragService;
    }

    public boolean validerConformite(DemandeTraitement demande) {
        try {
            logger.info(() -> "Agent Validateur: Vérification de conformité pour la demande " + demande.getId());
            demande.addAuditLog("Agent Validateur: Début de validation");

            // Recherche des conditions de la police via RAG
            String questionPolice = String.format(
                "Quelles sont les conditions de couverture pour un sinistre de type %s? " +
                "Quels sont les exclusions, franchises et plafonds applicables?",
                demande.getTypeSinistre().getDescription()
            );

            String conditionsPolice = ragService.askQuestion(questionPolice);
            demande.addAuditLog("Conditions contractuelles récupérées via RAG");

            // Analyse détaillée de conformité avec scoring
            String prompt = """
                En tant qu'agent validateur d'assurance, vérifiez la conformité de cette demande:
                
                Type de sinistre: %s
                Contenu de la demande: "%s"
                Métadonnées: %s
                Anomalies détectées: %s
                
                Conditions de la police d'assurance:
                %s
                
                Analysez et notez chaque critère sur 20 points:
                1. Couverture du type de sinistre (0-20 points)
                2. Respect des conditions contractuelles (0-20 points)
                3. Absence d'exclusions applicables (0-20 points)
                4. Respect des délais de déclaration (0-20 points)
                5. Suffisance des informations fournies (0-20 points)
                
                Pour chaque critère, indiquez:
                - Le score
                - Une justification courte
                
                Puis calculez le score total sur 100.
                
                Format de réponse:
                CRITERE_1: [score]/20 - [justification]
                CRITERE_2: [score]/20 - [justification]
                CRITERE_3: [score]/20 - [justification]
                CRITERE_4: [score]/20 - [justification]
                CRITERE_5: [score]/20 - [justification]
                SCORE_TOTAL: [total]/100
                DECISION: [CONFORME/NON_CONFORME/DOUTEUX]
                RAISON: [explication de la décision]
                """.formatted(
                    demande.getTypeSinistre().getDescription(),
                    demande.getContenuDemande(),
                    demande.getMetadata().toString(),
                    demande.getAnomaliesDetectees().isEmpty() ? "Aucune" : demande.getAnomaliesDetectees().toString(),
                    conditionsPolice
                );

            String reponse = chatModel.generate(prompt);
            
            // Parse de la réponse et extraction du score
            double score = extraireScore(reponse);
            String decision = extraireDecision(reponse);
            String explication = extraireRaison(reponse);

            demande.setScoreConformite(score);
            demande.addMetadata("rapport_validation", reponse);
            demande.addAuditLog(String.format("Score conformité: %.2f/100", score));

            // Décision basée sur le score et la décision de l'IA
            boolean conforme;
            if (decision.contains("DOUTEUX") || score < 70) {
                // Cas douteux : validation humaine requise
                demande.setNecessiteValidationHumaine(true);
                demande.setRaisonValidationHumaine("Score de conformité faible ou cas complexe");
                demande.setStatut(DemandeTraitement.StatutTraitement.EN_ATTENTE_VALIDATION_HUMAINE);
                conforme = false;
                demande.addAuditLog("Validation humaine requise - cas complexe");
            } else {
                conforme = decision.contains("CONFORME") && score >= 70;
            }

            demande.setConformite(conforme);
            if (!conforme && !demande.isNecessiteValidationHumaine()) {
                demande.setRaisonNonConformite(explication);
                demande.setStatut(DemandeTraitement.StatutTraitement.REJETE);
                demande.addAuditLog("Demande rejetée: " + explication);
            } else if (conforme) {
                demande.setStatut(DemandeTraitement.StatutTraitement.VALIDE);
                demande.addAuditLog("Demande validée");
            }

            logger.info(() -> String.format("Résultat validation: %s (score: %.2f/100) - %s",
                conforme ? "CONFORME" : "NON_CONFORME", score, explication));

            return conforme;

        } catch (Exception e) {
            logger.severe("Erreur lors de la validation de conformité: " + e.getMessage());
            demande.setConformite(false);
            demande.setRaisonNonConformite("Erreur technique lors de la validation: " + e.getMessage());
            demande.setStatut(DemandeTraitement.StatutTraitement.REJETE);
            demande.addAuditLog("Erreur validation: " + e.getMessage());
            return false;
        }
    }

    private double extraireScore(String reponse) {
        try {
            // Recherche de "SCORE_TOTAL: XX/100"
            String[] lines = reponse.split("\n");
            for (String line : lines) {
                if (line.toUpperCase().contains("SCORE_TOTAL")) {
                    String[] parts = line.split(":");
                    if (parts.length > 1) {
                        String scoreStr = parts[1].trim().split("/")[0].trim();
                        return Double.parseDouble(scoreStr);
                    }
                }
            }
        } catch (Exception e) {
            logger.warning("Impossible d'extraire le score: " + e.getMessage());
        }
        return 50.0; // Score par défaut moyen
    }

    private String extraireDecision(String reponse) {
        String[] lines = reponse.split("\n");
        for (String line : lines) {
            if (line.toUpperCase().contains("DECISION")) {
                return line.toUpperCase();
            }
        }
        return "DOUTEUX";
    }

    private String extraireRaison(String reponse) {
        String[] lines = reponse.split("\n");
        for (String line : lines) {
            if (line.toUpperCase().contains("RAISON")) {
                String[] parts = line.split(":", 2);
                if (parts.length > 1) {
                    return parts[1].trim();
                }
            }
        }
        return "Analyse complète disponible dans les métadonnées";
    }

    public String genererRapportValidation(DemandeTraitement demande) {
        try {
            String prompt = """
                Générez un rapport de validation détaillé pour cette demande de sinistre:
                
                ID Demande: %s
                Type: %s
                Conformité: %s
                Raison: %s
                
                Créez un rapport professionnel incluant:
                - Résumé de l'analyse
                - Points vérifiés
                - Décision finale
                - Recommandations pour la suite du traitement
                
                Le rapport doit être clair et précis pour l'équipe de traitement.
                """.formatted(
                    demande.getId(),
                    demande.getTypeSinistre().getDescription(),
                    demande.isConformite() ? "CONFORME" : "NON_CONFORME",
                    demande.getRaisonNonConformite() != null ? demande.getRaisonNonConformite() : "Conforme aux conditions"
                );

            return chatModel.generate(prompt);

        } catch (Exception e) {
            logger.severe("Erreur lors de la génération du rapport de validation: " + e.getMessage());
            return "Impossible de générer le rapport de validation.";
        }
    }
}
