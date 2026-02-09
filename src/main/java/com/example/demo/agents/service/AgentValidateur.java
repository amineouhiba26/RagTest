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

            // Recherche des conditions de la police via RAG
            String questionPolice = String.format(
                "Quelles sont les conditions de couverture pour un sinistre de type %s? " +
                "Quels sont les exclusions et les franchises applicables?",
                demande.getTypeSinistre().getDescription()
            );

            String conditionsPolice = ragService.askQuestion(questionPolice);

            // Analyse de conformité avec l'IA
            String prompt = """
                En tant qu'agent validateur d'assurance, vérifiez la conformité de cette demande:
                
                Type de sinistre: %s
                Contenu de la demande: "%s"
                
                Conditions de la police d'assurance:
                %s
                
                Analysez si:
                1. Le type de sinistre est couvert par la police
                2. Les circonstances décrites respectent les conditions
                3. Aucune exclusion ne s'applique
                4. Les délais de déclaration sont respectés (si applicable)
                5. Les informations fournies sont suffisantes
                
                Répondez par:
                - CONFORME si la demande respecte toutes les conditions
                - NON_CONFORME si une ou plusieurs conditions ne sont pas respectées
                
                Puis expliquez brièvement la raison de votre décision.
                Format: [CONFORME/NON_CONFORME] - Explication
                """.formatted(
                    demande.getTypeSinistre().getDescription(),
                    demande.getContenuDemande(),
                    conditionsPolice
                );

            String reponse = chatModel.generate(prompt);
            
            // Parse de la réponse
            boolean conforme = reponse.toUpperCase().startsWith("CONFORME");
            String explication = reponse.contains(" - ") ? 
                reponse.substring(reponse.indexOf(" - ") + 3) : reponse;

            demande.setConformite(conforme);
            if (!conforme) {
                demande.setRaisonNonConformite(explication);
                demande.setStatut(DemandeTraitement.StatutTraitement.REJETE);
            } else {
                demande.setStatut(DemandeTraitement.StatutTraitement.VALIDE);
            }

            logger.info(() -> String.format("Résultat validation: %s - %s", 
                conforme ? "CONFORME" : "NON_CONFORME", explication));

            return conforme;

        } catch (Exception e) {
            logger.severe("Erreur lors de la validation de conformité: " + e.getMessage());
            demande.setConformite(false);
            demande.setRaisonNonConformite("Erreur technique lors de la validation: " + e.getMessage());
            demande.setStatut(DemandeTraitement.StatutTraitement.REJETE);
            return false;
        }
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
