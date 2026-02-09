package com.example.demo.agents.orchestrator;

import com.example.demo.agents.model.DemandeTraitement;
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
        
        return CompletableFuture
            // Étape 1: Classification par l'Agent Routeur
            .supplyAsync(() -> {
                logger.info("Phase 1: Classification du type de sinistre");
                agentRouteur.classifierTypeSinistre(demande);
                return demande;
            }, executorService)
            
            // Étape 2: Validation par l'Agent Validateur
            .thenCompose(d -> CompletableFuture.supplyAsync(() -> {
                logger.info("Phase 2: Validation de la conformité");
                boolean conforme = agentValidateur.validerConformite(d);
                
                if (!conforme) {
                    logger.warning("Demande non conforme - arrêt du traitement");
                    d.setStatut(DemandeTraitement.StatutTraitement.REJETE);
                    return d;
                }
                
                return d;
            }, executorService))
            
            // Étape 3: Estimation (si conforme) par l'Agent Estimateur
            .thenCompose(d -> {
                if (d.getStatut() == DemandeTraitement.StatutTraitement.REJETE) {
                    return CompletableFuture.completedFuture(d);
                }
                
                return CompletableFuture.supplyAsync(() -> {
                    logger.info("Phase 3: Estimation du coût");
                    agentEstimateur.estimerCout(d);
                    d.setStatut(DemandeTraitement.StatutTraitement.TERMINE);
                    return d;
                }, executorService);
            })
            
            // Gestion des erreurs
            .exceptionally(throwable -> {
                logger.severe("Erreur lors du traitement orchestré: " + throwable.getMessage());
                demande.setStatut(DemandeTraitement.StatutTraitement.REJETE);
                demande.setRaisonNonConformite("Erreur technique: " + throwable.getMessage());
                return demande;
            });
    }

    public CompletableFuture<String> genererRapportComplet(DemandeTraitement demande) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                StringBuilder rapport = new StringBuilder();
                
                rapport.append("=== RAPPORT DE TRAITEMENT MULTI-AGENTS ===\n\n");
                rapport.append("ID Demande: ").append(demande.getId()).append("\n");
                rapport.append("Email Client: ").append(demande.getEmailClient()).append("\n");
                rapport.append("Date de réception: ").append(demande.getDateReception()).append("\n\n");
                
                // Rapport de routage
                rapport.append("1. ANALYSE DE ROUTAGE:\n");
                rapport.append(agentRouteur.genererAnalyseRoutage(demande)).append("\n\n");
                
                // Rapport de validation
                rapport.append("2. VALIDATION DE CONFORMITÉ:\n");
                rapport.append(agentValidateur.genererRapportValidation(demande)).append("\n\n");
                
                // Rapport d'estimation (si applicable)
                if (demande.getStatut() != DemandeTraitement.StatutTraitement.REJETE) {
                    rapport.append("3. ESTIMATION DU COÛT:\n");
                    rapport.append(agentEstimateur.genererRapportEstimation(demande)).append("\n\n");
                }
                
                // Synthèse finale
                rapport.append("4. SYNTHÈSE FINALE:\n");
                rapport.append("Statut: ").append(demande.getStatut()).append("\n");
                rapport.append("Type de sinistre: ").append(demande.getTypeSinistre().getDescription()).append("\n");
                rapport.append("Conformité: ").append(demande.isConformite() ? "OUI" : "NON").append("\n");
                
                if (demande.getEstimationCout() != null) {
                    rapport.append("Estimation: ").append(String.format("%.2f€", demande.getEstimationCout())).append("\n");
                }
                
                if (demande.getRaisonNonConformite() != null) {
                    rapport.append("Raison non-conformité: ").append(demande.getRaisonNonConformite()).append("\n");
                }
                
                return rapport.toString();
                
            } catch (Exception e) {
                logger.severe("Erreur lors de la génération du rapport complet: " + e.getMessage());
                return "Erreur lors de la génération du rapport complet.";
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
