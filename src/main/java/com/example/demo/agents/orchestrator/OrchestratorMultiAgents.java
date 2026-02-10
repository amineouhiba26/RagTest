package com.example.demo.agents.orchestrator;

import com.example.demo.agents.model.DemandeTraitement;
import com.example.demo.agents.model.SinistreType;
import com.example.demo.agents.service.AgentEstimateur;
import com.example.demo.agents.service.AgentRouteur;
import com.example.demo.agents.service.AgentValidateur;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Logger;

@Service
public class OrchestratorMultiAgents {
    
    private static final Logger logger = Logger.getLogger(OrchestratorMultiAgents.class.getName());
    
    private final AgentRouteur agentRouteur;
    private final AgentValidateur agentValidateur;
    private final AgentEstimateur agentEstimateur;
    private final ExecutorService executorService;

    public OrchestratorMultiAgents(AgentRouteur agentRouteur, 
                                  AgentValidateur agentValidateur,
                                  AgentEstimateur agentEstimateur) {
        this.agentRouteur = agentRouteur;
        this.agentValidateur = agentValidateur;
        this.agentEstimateur = agentEstimateur;
        this.executorService = Executors.newFixedThreadPool(5);
    }

    public CompletableFuture<DemandeTraitement> traiterDemandeComplete(DemandeTraitement demande) {
        logger.info(() -> "Orchestrateur: Démarrage du traitement de la demande " + demande.getId());
        demande.addAuditLog("Orchestrateur: Démarrage du workflow multi-agents");

        return CompletableFuture
            // Étape 1: Classification et extraction par l'Agent Routeur
            .supplyAsync(() -> {
                logger.info("Phase 1: Classification et extraction des métadonnées");
                demande.addAuditLog("Phase 1: Classification - Agent Routeur");
                agentRouteur.classifierTypeSinistre(demande);

                // Vérification si validation humaine nécessaire dès la classification
                if (demande.isNecessiteValidationHumaine()) {
                    logger.warning("Validation humaine requise après classification: " +
                        demande.getRaisonValidationHumaine());
                    demande.setStatut(DemandeTraitement.StatutTraitement.EN_ATTENTE_VALIDATION_HUMAINE);
                    return demande;
                }

                return demande;
            }, executorService)
            
            // Étape 2: Validation contractuelle par l'Agent Validateur
            .thenCompose(d -> {
                if (d.getStatut() == DemandeTraitement.StatutTraitement.EN_ATTENTE_VALIDATION_HUMAINE) {
                    return CompletableFuture.completedFuture(d);
                }

                return CompletableFuture.supplyAsync(() -> {
                    logger.info("Phase 2: Validation de la conformité contractuelle");
                    d.addAuditLog("Phase 2: Validation - Agent Validateur");
                    boolean conforme = agentValidateur.validerConformite(d);

                    if (!conforme && !d.isNecessiteValidationHumaine()) {
                        logger.warning("Demande non conforme - arrêt du traitement");
                        d.setStatut(DemandeTraitement.StatutTraitement.REJETE);
                        d.addAuditLog("Demande rejetée - non conforme");
                        return d;
                    }

                    if (d.isNecessiteValidationHumaine()) {
                        logger.info("Validation humaine requise: " + d.getRaisonValidationHumaine());
                        d.setStatut(DemandeTraitement.StatutTraitement.EN_ATTENTE_VALIDATION_HUMAINE);
                    }

                    return d;
                }, executorService);
            })

            // Étape 3: Estimation financière par l'Agent Estimateur (si conforme)
            .thenCompose(d -> {
                if (d.getStatut() == DemandeTraitement.StatutTraitement.REJETE ||
                    d.getStatut() == DemandeTraitement.StatutTraitement.EN_ATTENTE_VALIDATION_HUMAINE) {
                    return CompletableFuture.completedFuture(d);
                }
                
                return CompletableFuture.supplyAsync(() -> {
                    logger.info("Phase 3: Estimation financière");
                    d.addAuditLog("Phase 3: Estimation - Agent Estimateur");
                    agentEstimateur.estimerCout(d);

                    // Vérification finale pour validation humaine
                    if (!d.isNecessiteValidationHumaine()) {
                        d.setStatut(DemandeTraitement.StatutTraitement.TERMINE);
                        d.addAuditLog("Traitement terminé avec succès");
                    }

                    return d;
                }, executorService);
            })
            
            // Étape 4: Agrégation finale et décision
            .thenApply(d -> {
                logger.info("Phase 4: Agrégation des résultats et décision finale");
                d.addAuditLog("Phase 4: Agrégation finale");

                // Calcul de métriques globales
                d.addMetadata("workflow_complete", true);
                d.addMetadata("nombre_anomalies", d.getAnomaliesDetectees().size());
                d.addMetadata("logs_count", d.getAuditLogs().size());

                // Détermination automatique de la validation humaine si pas déjà définie
                if (!d.isNecessiteValidationHumaine()) {
                    boolean requiresHumanValidation = determinerValidationHumaine(d);
                    if (requiresHumanValidation) {
                        d.setNecessiteValidationHumaine(true);
                        d.setRaisonValidationHumaine("Décision automatique basée sur l'analyse globale");
                        d.setStatut(DemandeTraitement.StatutTraitement.EN_ATTENTE_VALIDATION_HUMAINE);
                    }
                }

                logger.info(() -> String.format("Traitement terminé - Statut: %s, Score: %.2f, Validation humaine: %s",
                    d.getStatut(), d.getScoreConformite(), d.isNecessiteValidationHumaine()));

                return d;
            })

            // Gestion des erreurs
            .exceptionally(throwable -> {
                logger.severe("Erreur lors du traitement orchestré: " + throwable.getMessage());
                demande.setStatut(DemandeTraitement.StatutTraitement.REJETE);
                demande.setRaisonNonConformite("Erreur technique: " + throwable.getMessage());
                demande.addAuditLog("Erreur fatale: " + throwable.getMessage());
                return demande;
            });
    }

    /**
     * Détermine si une intervention humaine est nécessaire basée sur plusieurs critères
     */
    private boolean determinerValidationHumaine(DemandeTraitement demande) {
        // Critères pour validation humaine:
        // 1. Score de conformité faible (< 80)
        if (demande.getScoreConformite() < 80 && demande.getScoreConformite() > 0) {
            return true;
        }

        // 2. Estimation élevée (> 10000€)
        if (demande.getEstimationCout() != null && demande.getEstimationCout() > 10000) {
            return true;
        }

        // 3. Nombreuses anomalies (>= 3)
        if (demande.getAnomaliesDetectees().size() >= 3) {
            return true;
        }

        // 4. Type de sinistre complexe
        if (demande.getTypeSinistre() == SinistreType.CATASTROPHE_NATURELLE ||
            demande.getTypeSinistre() == SinistreType.RESPONSABILITE_CIVILE) {
            return true;
        }

        return false;
    }

    public CompletableFuture<String> genererRapportComplet(DemandeTraitement demande) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                StringBuilder rapport = new StringBuilder();
                
                rapport.append("═══════════════════════════════════════════════════════════\n");
                rapport.append("     RAPPORT DE TRAITEMENT MULTI-AGENTS - SYSTÈME AGENTIQUE\n");
                rapport.append("═══════════════════════════════════════════════════════════\n\n");

                // Informations générales
                rapport.append("📋 INFORMATIONS GÉNÉRALES\n");
                rapport.append("─────────────────────────────────────────────────────────\n");
                rapport.append("ID Demande: ").append(demande.getId()).append("\n");
                rapport.append("Email Client: ").append(demande.getEmailClient()).append("\n");
                rapport.append("Date de réception: ").append(demande.getDateReception()).append("\n");
                rapport.append("Statut final: ").append(demande.getStatut()).append("\n");
                rapport.append("Validation humaine requise: ")
                    .append(demande.isNecessiteValidationHumaine() ? "OUI" : "NON").append("\n");
                if (demande.isNecessiteValidationHumaine()) {
                    rapport.append("Raison validation humaine: ")
                        .append(demande.getRaisonValidationHumaine()).append("\n");
                }
                rapport.append("\n");

                // Métadonnées extraites
                rapport.append("🔍 MÉTADONNÉES EXTRAITES\n");
                rapport.append("─────────────────────────────────────────────────────────\n");
                if (!demande.getMetadata().isEmpty()) {
                    demande.getMetadata().forEach((key, value) ->
                        rapport.append("- ").append(key).append(": ").append(value).append("\n"));
                } else {
                    rapport.append("Aucune métadonnée extraite\n");
                }
                rapport.append("\n");

                // Anomalies détectées
                rapport.append("⚠️  ANOMALIES DÉTECTÉES\n");
                rapport.append("─────────────────────────────────────────────────────────\n");
                if (!demande.getAnomaliesDetectees().isEmpty()) {
                    demande.getAnomaliesDetectees().forEach(anomalie ->
                        rapport.append("- ").append(anomalie).append("\n"));
                } else {
                    rapport.append("Aucune anomalie détectée\n");
                }
                rapport.append("\n");

                // Rapport de routage (Agent 1)
                rapport.append("🤖 AGENT 1: ROUTEUR - ANALYSE ET CLASSIFICATION\n");
                rapport.append("─────────────────────────────────────────────────────────\n");
                rapport.append(agentRouteur.genererAnalyseRoutage(demande)).append("\n\n");
                
                // Rapport de validation (Agent 2)
                rapport.append("🤖 AGENT 2: VALIDATEUR - CONFORMITÉ CONTRACTUELLE\n");
                rapport.append("─────────────────────────────────────────────────────────\n");
                rapport.append(agentValidateur.genererRapportValidation(demande)).append("\n\n");
                
                // Rapport d'estimation (Agent 3) - si applicable
                if (demande.getStatut() != DemandeTraitement.StatutTraitement.REJETE &&
                    demande.getEstimationCout() != null) {
                    rapport.append("🤖 AGENT 3: ESTIMATEUR - ÉVALUATION FINANCIÈRE\n");
                    rapport.append("─────────────────────────────────────────────────────────\n");
                    rapport.append(agentEstimateur.genererRapportEstimation(demande)).append("\n\n");
                }
                
                // Synthèse finale
                rapport.append("📊 SYNTHÈSE FINALE\n");
                rapport.append("─────────────────────────────────────────────────────────\n");
                rapport.append("Type de sinistre: ").append(demande.getTypeSinistre().getDescription()).append("\n");
                rapport.append("Score de conformité: ")
                    .append(String.format("%.2f/100", demande.getScoreConformite())).append("\n");
                rapport.append("Conformité: ").append(demande.isConformite() ? "✓ CONFORME" : "✗ NON CONFORME").append("\n");

                if (demande.getEstimationCout() != null) {
                    rapport.append("Estimation financière: ")
                        .append(String.format("%.2f €", demande.getEstimationCout())).append("\n");
                }
                
                if (demande.getRaisonNonConformite() != null) {
                    rapport.append("Raison non-conformité: ").append(demande.getRaisonNonConformite()).append("\n");
                }
                rapport.append("\n");

                // Piste d'audit
                rapport.append("📝 PISTE D'AUDIT (LOGS)\n");
                rapport.append("─────────────────────────────────────────────────────────\n");
                demande.getAuditLogs().forEach(log -> rapport.append(log).append("\n"));
                rapport.append("\n");

                // Recommandations
                rapport.append("💡 RECOMMANDATIONS\n");
                rapport.append("─────────────────────────────────────────────────────────\n");
                if (demande.isNecessiteValidationHumaine()) {
                    rapport.append("🔴 VALIDATION HUMAINE REQUISE\n");
                    rapport.append("   Raison: ").append(demande.getRaisonValidationHumaine()).append("\n");
                } else if (demande.isConformite()) {
                    rapport.append("🟢 TRAITEMENT AUTOMATIQUE COMPLET\n");
                    rapport.append("   Le dossier peut être traité automatiquement.\n");
                } else {
                    rapport.append("🔴 DOSSIER REJETÉ\n");
                    rapport.append("   Raison: ").append(demande.getRaisonNonConformite()).append("\n");
                }
                rapport.append("\n");

                rapport.append("═══════════════════════════════════════════════════════════\n");
                rapport.append("                  FIN DU RAPPORT\n");
                rapport.append("═══════════════════════════════════════════════════════════\n");

                return rapport.toString();
                
            } catch (Exception e) {
                logger.severe("Erreur lors de la génération du rapport complet: " + e.getMessage());
                return "Erreur lors de la génération du rapport complet: " + e.getMessage();
            }
        }, executorService);
    }

    public CompletableFuture<DemandeTraitement> traiterDemandeExpresse(DemandeTraitement demande) {
        // Version accélérée pour les cas urgents
        logger.info(() -> "Orchestrateur: Traitement express de la demande " + demande.getId());
        
        return CompletableFuture.supplyAsync(() -> {
            // Classification rapide
            agentRouteur.classifierTypeSinistre(demande);
            
            // Validation simplifiée
            boolean conforme = agentValidateur.validerConformite(demande);
            if (!conforme) {
                demande.setStatut(DemandeTraitement.StatutTraitement.REJETE);
                return demande;
            }
            
            // Estimation rapide
            agentEstimateur.estimerCout(demande);
            demande.setStatut(DemandeTraitement.StatutTraitement.TERMINE);
            
            return demande;
        }, executorService);
    }

    public void arreterOrchestrator() {
        logger.info("Arrêt de l'orchestrateur multi-agents");
        executorService.shutdown();
    }
}
