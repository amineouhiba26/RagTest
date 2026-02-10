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
            demande.addAuditLog("Agent Routeur: Début de classification");

            // Étape 1: Extraction et normalisation des métadonnées
            extraireMetadonnees(demande);

            // Étape 2: Détection des anomalies
            detecterAnomalies(demande);

            String typesDisponibles = Arrays.stream(SinistreType.values())
                .map(type -> "- " + type.name() + ": " + type.getDescription())
                .reduce((a, b) -> a + "\n" + b)
                .orElse("");

            String prompt = """
                En tant qu'agent routeur spécialisé en assurance, analysez cette demande et classifiez le type de sinistre:
                
                Email du client: "%s"
                Contenu de la demande: "%s"
                Métadonnées extraites: %s
                
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
                    demande.getMetadata().toString(),
                    typesDisponibles
                );

            String reponse = chatModel.generate(prompt).trim().toUpperCase();
            
            // Extraction du type de sinistre depuis la réponse
            SinistreType typeClassifie = extraireTypeSinistre(reponse);
            
            demande.setTypeSinistre(typeClassifie);
            demande.setStatut(DemandeTraitement.StatutTraitement.CLASSIFIE);
            demande.addAuditLog("Type classifié: " + typeClassifie.getDescription());

            logger.info(() -> String.format("Type classifié: %s (%s)", 
                typeClassifie.name(), typeClassifie.getDescription()));
            
            return typeClassifie;

        } catch (Exception e) {
            logger.severe("Erreur lors de la classification: " + e.getMessage());
            demande.addAuditLog("Erreur classification: " + e.getMessage());
            // Par défaut, classer comme "AUTRES" en cas d'erreur
            demande.setTypeSinistre(SinistreType.AUTRES);
            return SinistreType.AUTRES;
        }
    }

    /**
     * Extrait et normalise les métadonnées clés de la demande
     */
    private void extraireMetadonnees(DemandeTraitement demande) {
        try {
            String prompt = """
                Extrayez et normalisez les métadonnées de cette demande de sinistre:
                
                Email: "%s"
                Contenu: "%s"
                
                Identifiez et extrayez au format JSON:
                - nom du client (si mentionné)
                - date de l'incident (si mentionné)
                - montant estimé par le client (si mentionné)
                - localisation (si mentionné)
                - nombre de pièces jointes mentionnées
                - mots-clés principaux (liste de 3-5 mots)
                
                Répondez uniquement avec les informations trouvées dans le format:
                NOM: [nom ou N/A]
                DATE_INCIDENT: [date ou N/A]
                MONTANT_ESTIME: [montant ou N/A]
                LOCALISATION: [lieu ou N/A]
                PIECES_JOINTES: [nombre ou 0]
                MOTS_CLES: [mot1, mot2, mot3]
                """.formatted(
                    demande.getEmailClient(),
                    demande.getContenuDemande()
                );

            String reponse = chatModel.generate(prompt);

            // Parser la réponse et ajouter aux métadonnées
            parseMetadata(reponse, demande);

            demande.addAuditLog("Métadonnées extraites: " + demande.getMetadata().size() + " champs");

        } catch (Exception e) {
            logger.warning("Erreur extraction métadonnées: " + e.getMessage());
            demande.addAnomalie("Échec extraction métadonnées");
        }
    }

    private void parseMetadata(String reponse, DemandeTraitement demande) {
        for (String line : reponse.split("\n")) {
            if (line.contains(":")) {
                String[] parts = line.split(":", 2);
                String key = parts[0].trim();
                String value = parts[1].trim();
                if (!value.equalsIgnoreCase("N/A") && !value.isEmpty()) {
                    demande.addMetadata(key, value);
                }
            }
        }
    }

    /**
     * Détecte les anomalies ou données manquantes dans la demande
     */
    private void detecterAnomalies(DemandeTraitement demande) {
        // Vérification de l'email
        if (demande.getEmailClient() == null || demande.getEmailClient().trim().isEmpty()) {
            demande.addAnomalie("Email client manquant");
        } else if (!demande.getEmailClient().matches("^[A-Za-z0-9+_.-]+@(.+)$")) {
            demande.addAnomalie("Format email invalide");
        }

        // Vérification du contenu
        if (demande.getContenuDemande() == null || demande.getContenuDemande().trim().length() < 20) {
            demande.addAnomalie("Description du sinistre insuffisante (minimum 20 caractères)");
        }

        // Vérification des pièces jointes
        if (demande.getPhotosUrls() == null || demande.getPhotosUrls().isEmpty()) {
            demande.addAnomalie("Aucune pièce jointe fournie");
        }

        // Analyse sémantique des incohérences
        try {
            String prompt = """
                Analysez cette demande pour détecter des incohérences ou informations manquantes critiques:
                
                Contenu: "%s"
                
                Vérifiez:
                - Cohérence temporelle (dates contradictoires)
                - Informations essentielles manquantes pour ce type de sinistre
                - Contradictions dans la description
                - Gravité disproportionnée par rapport aux détails
                
                Listez uniquement les anomalies graves trouvées, une par ligne.
                Si aucune anomalie, répondez: AUCUNE
                """.formatted(demande.getContenuDemande());

            String reponse = chatModel.generate(prompt);

            if (!reponse.trim().equalsIgnoreCase("AUCUNE")) {
                for (String anomalie : reponse.split("\n")) {
                    if (!anomalie.trim().isEmpty()) {
                        demande.addAnomalie(anomalie.trim());
                    }
                }
            }
        } catch (Exception e) {
            logger.warning("Erreur détection anomalies sémantiques: " + e.getMessage());
        }

        // Si anomalies critiques, marquer pour validation humaine
        if (!demande.getAnomaliesDetectees().isEmpty()) {
            demande.addAuditLog("Anomalies détectées: " + demande.getAnomaliesDetectees().size());

            // Décision si validation humaine nécessaire
            if (demande.getAnomaliesDetectees().size() >= 2 ||
                demande.getAnomaliesDetectees().stream()
                    .anyMatch(a -> a.toLowerCase().contains("critique") ||
                                   a.toLowerCase().contains("manquant") ||
                                   a.toLowerCase().contains("contradiction"))) {
                demande.setNecessiteValidationHumaine(true);
                demande.setRaisonValidationHumaine("Anomalies critiques détectées: " +
                    String.join(", ", demande.getAnomaliesDetectees()));
            }
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
