package com.example.demo.agents.service;

import com.example.demo.agents.model.DemandeTraitement;
import com.example.demo.agents.model.SinistreType;
import dev.langchain4j.model.chat.ChatLanguageModel;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.logging.Logger;

@Service
public class AgentRouteur {
    
    private static final Logger logger = Logger.getLogger(AgentRouteur.class.getName());
    
    private final ChatLanguageModel chatModel;

    public AgentRouteur(ChatLanguageModel chatModel) {
        this.chatModel = chatModel;
    }

    public SinistreType classifierTypeSinistre(DemandeTraitement demande) {
        try {
            logger.info(() -> "Agent Routeur: Classification du type de sinistre pour la demande " + demande.getId());

            String typesDisponibles = Arrays.stream(SinistreType.values())
                .map(type -> "- " + type.name() + ": " + type.getDescription())
                .reduce((a, b) -> a + "\n" + b)
                .orElse("");

            String prompt = """
                En tant qu'agent routeur spécialisé en assurance, analysez cette demande et classifiez le type de sinistre:
                
                Email du client: "%s"
                Contenu de la demande: "%s"
                
                Types de sinistres disponibles:
                %s
                
                Analysez attentivement le contenu pour identifier:
                1. Les mots-clés indiquant le type d'incident
                2. Le contexte et les circonstances
                3. Les dommages décrits
                
                Répondez UNIQUEMENT avec le nom exact du type (ex: ACCIDENT_AUTOMOBILE, DEGATS_EAU, etc.)
                Si aucun type ne correspond parfaitement, utilisez: AUTRES
                """.formatted(
                    demande.getEmailClient(),
                    demande.getContenuDemande(),
                    typesDisponibles
                );

            String reponse = chatModel.generate(prompt).trim().toUpperCase();
            
            // Extraction du type de sinistre depuis la réponse
            SinistreType typeClassifie = extraireTypeSinistre(reponse);
            
            demande.setTypeSinistre(typeClassifie);
            
            logger.info(() -> String.format("Type classifié: %s (%s)", 
                typeClassifie.name(), typeClassifie.getDescription()));
            
            return typeClassifie;

        } catch (Exception e) {
            logger.severe("Erreur lors de la classification: " + e.getMessage());
            // Par défaut, classer comme "AUTRES" en cas d'erreur
            demande.setTypeSinistre(SinistreType.AUTRES);
            return SinistreType.AUTRES;
        }
    }

    private SinistreType extraireTypeSinistre(String reponse) {
        // Recherche directe du type exact
        for (SinistreType type : SinistreType.values()) {
            if (reponse.contains(type.name())) {
                return type;
            }
        }
        
        // Recherche par mots-clés si le type exact n'est pas trouvé
        String reponseMinuscule = reponse.toLowerCase();
        
        if (reponseMinuscule.contains("accident") && reponseMinuscule.contains("automobile")) {
            return SinistreType.ACCIDENT_AUTOMOBILE;
        }
        if (reponseMinuscule.contains("dégât") || reponseMinuscule.contains("dégat") || reponseMinuscule.contains("eau")) {
            return SinistreType.DEGATS_EAU;
        }
        if (reponseMinuscule.contains("incendie") || reponseMinuscule.contains("feu")) {
            return SinistreType.INCENDIE;
        }
        if (reponseMinuscule.contains("vol") || reponseMinuscule.contains("cambriolage")) {
            return SinistreType.VOL;
        }
        if (reponseMinuscule.contains("catastrophe") || reponseMinuscule.contains("naturelle")) {
            return SinistreType.CATASTROPHE_NATURELLE;
        }
        if (reponseMinuscule.contains("bris") || reponseMinuscule.contains("glace")) {
            return SinistreType.BRIS_DE_GLACE;
        }
        if (reponseMinuscule.contains("responsabilité") || reponseMinuscule.contains("civile")) {
            return SinistreType.RESPONSABILITE_CIVILE;
        }
        
        // Type par défaut
        return SinistreType.AUTRES;
    }

    public String genererAnalyseRoutage(DemandeTraitement demande) {
        try {
            String prompt = """
                Générez un rapport d'analyse de routage pour cette demande:
                
                ID: %s
                Email client: %s
                Type classifié: %s
                Contenu: "%s"
                
                Créez un rapport incluant:
                - Méthode de classification utilisée
                - Mots-clés identifiés
                - Niveau de confiance dans la classification
                - Recommandations pour les agents suivants
                
                Le rapport doit expliquer le processus de décision.
                """.formatted(
                    demande.getId(),
                    demande.getEmailClient(),
                    demande.getTypeSinistre().getDescription(),
                    demande.getContenuDemande()
                );

            return chatModel.generate(prompt);

        } catch (Exception e) {
            logger.severe("Erreur lors de la génération du rapport de routage: " + e.getMessage());
            return "Impossible de générer le rapport de routage.";
        }
    }

    public boolean necessiteTraitementPrioritaire(DemandeTraitement demande) {
        try {
            String prompt = """
                Évaluez si cette demande nécessite un traitement prioritaire:
                
                Type: %s
                Contenu: "%s"
                
                Considérez:
                - Gravité du sinistre
                - Urgence des réparations
                - Impact sur la sécurité
                - Risque d'aggravation
                
                Répondez par OUI ou NON suivi d'une justification.
                """.formatted(
                    demande.getTypeSinistre().getDescription(),
                    demande.getContenuDemande()
                );

            String reponse = chatModel.generate(prompt);
            return reponse.toUpperCase().startsWith("OUI");

        } catch (Exception e) {
            logger.severe("Erreur lors de l'évaluation de priorité: " + e.getMessage());
            return false;
        }
    }
}
