package com.example.demo.agents.controller;

import com.example.demo.agents.model.DemandeTraitement;
import com.example.demo.agents.orchestrator.OrchestratorMultiAgents;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Logger;

@RestController
@RequestMapping("/api/agents")
@CrossOrigin(origins = "*")
public class MultiAgentsController {

    private static final Logger logger = Logger.getLogger(MultiAgentsController.class.getName());
    
    private final OrchestratorMultiAgents orchestrateur;

    public MultiAgentsController(OrchestratorMultiAgents orchestrateur) {
        this.orchestrateur = orchestrateur;
    }

    @PostMapping("/traiter-sinistre")
    public CompletableFuture<ResponseEntity<Map<String, Object>>> traiterSinistre(
            @RequestBody Map<String, Object> requestData) {
        
        try {
            // Extraction des données de la requête
            String emailClient = (String) requestData.get("email");
            String contenuDemande = (String) requestData.get("description");
            @SuppressWarnings("unchecked")
            List<String> photosUrls = (List<String>) requestData.getOrDefault("photos", Arrays.asList());
            
            if (emailClient == null || contenuDemande == null) {
                return CompletableFuture.completedFuture(
                    ResponseEntity.badRequest().body(Map.of(
                        "error", "Email client et description sont obligatoires",
                        "success", false
                    ))
                );
            }

            // Création de la demande
            DemandeTraitement demande = new DemandeTraitement(emailClient, contenuDemande, photosUrls);
            
            logger.info(() -> "Réception nouvelle demande de sinistre: " + demande.getId());

            // Traitement par les agents
            return orchestrateur.traiterDemandeComplete(demande)
                .thenApply(demandeTraitee -> {
                    Map<String, Object> response = new java.util.HashMap<>();
                    response.put("success", true);
                    response.put("demandeId", demandeTraitee.getId());
                    response.put("statut", demandeTraitee.getStatut().name());
                    response.put("typeSinistre", demandeTraitee.getTypeSinistre().getDescription());
                    response.put("conforme", demandeTraitee.isConformite());
                    response.put("scoreConformite", demandeTraitee.getScoreConformite());
                    response.put("estimationCout", demandeTraitee.getEstimationCout() != null ?
                        demandeTraitee.getEstimationCout() : 0.0);
                    response.put("commentaires", demandeTraitee.getCommentairesEstimation() != null ?
                        demandeTraitee.getCommentairesEstimation() : "");
                    response.put("raisonRejet", demandeTraitee.getRaisonNonConformite() != null ?
                        demandeTraitee.getRaisonNonConformite() : "");
                    response.put("validationHumaineRequise", demandeTraitee.isNecessiteValidationHumaine());
                    response.put("raisonValidationHumaine", demandeTraitee.getRaisonValidationHumaine() != null ?
                        demandeTraitee.getRaisonValidationHumaine() : "");
                    response.put("anomaliesDetectees", demandeTraitee.getAnomaliesDetectees());
                    response.put("metadata", demandeTraitee.getMetadata());
                    response.put("auditLogs", demandeTraitee.getAuditLogs());

                    return ResponseEntity.ok(response);
                })
                .exceptionally(throwable -> {
                    logger.severe("Erreur lors du traitement: " + throwable.getMessage());
                    return ResponseEntity.internalServerError().body(Map.of(
                        "error", "Erreur lors du traitement: " + throwable.getMessage(),
                        "success", false
                    ));
                });

        } catch (Exception e) {
            logger.severe("Erreur lors de la création de la demande: " + e.getMessage());
            return CompletableFuture.completedFuture(
                ResponseEntity.internalServerError().body(Map.of(
                    "error", "Erreur lors de la création de la demande: " + e.getMessage(),
                    "success", false
                ))
            );
        }
    }

    @PostMapping("/traiter-express")
    public CompletableFuture<ResponseEntity<Map<String, Object>>> traiterExpresse(
            @RequestBody Map<String, Object> requestData) {
        
        try {
            String emailClient = (String) requestData.get("email");
            String contenuDemande = (String) requestData.get("description");
            @SuppressWarnings("unchecked")
            List<String> photosUrls = (List<String>) requestData.getOrDefault("photos", Arrays.asList());

            DemandeTraitement demande = new DemandeTraitement(emailClient, contenuDemande, photosUrls);
            
            logger.info(() -> "Traitement express pour la demande: " + demande.getId());

            return orchestrateur.traiterDemandeExpresse(demande)
                .thenApply(demandeTraitee -> ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Traitement express terminé",
                    "demandeId", demandeTraitee.getId(),
                    "statut", demandeTraitee.getStatut().name(),
                    "typeSinistre", demandeTraitee.getTypeSinistre().getDescription(),
                    "estimationCout", demandeTraitee.getEstimationCout() != null ? 
                        demandeTraitee.getEstimationCout() : 0.0
                )));

        } catch (Exception e) {
            return CompletableFuture.completedFuture(
                ResponseEntity.internalServerError().body(Map.of(
                    "error", "Erreur traitement express: " + e.getMessage(),
                    "success", false
                ))
            );
        }
    }

    @PostMapping("/generer-rapport/{demandeId}")
    public CompletableFuture<ResponseEntity<Map<String, String>>> genererRapport(
            @PathVariable String demandeId,
            @RequestBody DemandeTraitement demande) {
        
        try {
            logger.info(() -> "Génération de rapport pour la demande: " + demandeId);

            return orchestrateur.genererRapportComplet(demande)
                .thenApply(rapport -> ResponseEntity.ok(Map.of(
                    "success", "true",
                    "demandeId", demandeId,
                    "rapport", rapport
                )))
                .exceptionally(throwable -> ResponseEntity.internalServerError().body(Map.of(
                    "error", "Erreur génération rapport: " + throwable.getMessage(),
                    "success", "false"
                )));

        } catch (Exception e) {
            return CompletableFuture.completedFuture(
                ResponseEntity.internalServerError().body(Map.of(
                    "error", "Erreur lors de la génération du rapport: " + e.getMessage(),
                    "success", "false"
                ))
            );
        }
    }

    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getStatus() {
        return ResponseEntity.ok(Map.of(
            "service", "Multi-Agents Orchestrator",
            "version", "1.0.0",
            "agents", Arrays.asList("Routeur", "Validateur", "Estimateur"),
            "patterns", "Orchestrator-Workers",
            "status", "Opérationnel",
            "capabilities", Arrays.asList(
                "Classification automatique des sinistres",
                "Validation de conformité via RAG",
                "Estimation multimodale des coûts",
                "Rapports détaillés",
                "Traitement asynchrone"
            )
        ));
    }

    @PostMapping("/test-demo")
    public CompletableFuture<ResponseEntity<Map<String, Object>>> testDemo() {
        try {
            // Données de test pour démonstration
            Map<String, Object> testData = Map.of(
                "email", "client.test@example.com",
                "description", "Suite à un accident de voiture ce matin, mon pare-brise est complètement brisé avec des éclats partout. L'impact s'est produit sur l'autoroute suite à un caillou projeté par un camion. La fissure s'étend sur toute la surface et rend la conduite dangereuse.",
                "photos", Arrays.asList(
                    "https://example.com/photos/pare-brise-impact.jpg",
                    "https://example.com/photos/pare-brise-fissures.jpg"
                )
            );

            return traiterSinistre(testData);

        } catch (Exception e) {
            return CompletableFuture.completedFuture(
                ResponseEntity.internalServerError().body(Map.of(
                    "error", "Erreur test démo: " + e.getMessage(),
                    "success", false
                ))
            );
        }
    }
}
