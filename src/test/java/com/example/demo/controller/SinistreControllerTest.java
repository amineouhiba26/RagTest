package com.example.demo.controller;

import com.example.demo.model.DemandeTraitement;
import com.example.demo.agents.orchestrator.OrchestratorMultiAgents;
import com.example.demo.service.AuditService;
import com.example.demo.service.HumanValidationService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.example.demo.config.SecurityConfig;
import org.springframework.context.annotation.Import;
/**
 * Tests d'intégration pour l'API REST des sinistres
 */
@WebMvcTest(SinistreController.class)
@Import(SecurityConfig.class)
@DisplayName("Tests API REST - Contrôleur Sinistres")
class SinistreControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private OrchestratorMultiAgents orchestrator;

    @MockBean
    private HumanValidationService validationService;

    @MockBean
    private AuditService auditService;

    private SinistreController.SinistreRequest sinistreRequest;

    @BeforeEach
    void setUp() {
        sinistreRequest = new SinistreController.SinistreRequest(
            "client@example.com",
            "Dégât des eaux dans ma cuisine suite à une fuite de canalisation",
            List.of("photo1.jpg", "photo2.jpg")
        );
    }

    @Test
    @DisplayName("Test soumission de sinistre avec utilisateur CLIENT")
    @WithMockUser(roles = "CLIENT")
    void testSoumettreSinistreAvecRoleClient() throws Exception {
        // Given
        DemandeTraitement demande = new DemandeTraitement();
        demande.setId("test-id-123");
        when(orchestrator.traiterDemandeComplete(any(DemandeTraitement.class)))
            .thenReturn(CompletableFuture.completedFuture(demande));

        // When & Then
        mockMvc.perform(post("/api/sinistres/soumettre")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(sinistreRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Sinistre soumis avec succès. Traitement en cours."))
                .andExpect(jsonPath("$.data").exists());
    }

    @Test
    @DisplayName("Test soumission de sinistre avec utilisateur GESTIONNAIRE")
    @WithMockUser(roles = "GESTIONNAIRE")
    void testSoumettreSinistreAvecRoleGestionnaire() throws Exception {
        // Given
        DemandeTraitement demande = new DemandeTraitement();
        when(orchestrator.traiterDemandeComplete(any(DemandeTraitement.class)))
            .thenReturn(CompletableFuture.completedFuture(demande));

        // When & Then
        mockMvc.perform(post("/api/sinistres/soumettre")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(sinistreRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @DisplayName("Test consultation de statut")
    @WithMockUser(roles = "CLIENT")
    void testConsulterStatut() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/sinistres/test-id/statut"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value("test-id"));
    }

    @Test
    @DisplayName("Test accès audit avec rôle GESTIONNAIRE")
    @WithMockUser(roles = "GESTIONNAIRE")
    void testAccesAuditAvecRoleGestionnaire() throws Exception {
        // Given
        when(auditService.obtenirHistoriqueAudit("test-id"))
            .thenReturn(List.of());

        // When & Then
        mockMvc.perform(get("/api/sinistres/test-id/audit"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @DisplayName("Test refus d'accès audit avec rôle CLIENT")
    @WithMockUser(roles = "CLIENT")
    void testRefusAccesAuditAvecRoleClient() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/sinistres/test-id/audit"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Test validation des données d'entrée")
    @WithMockUser(roles = "CLIENT")
    void testValidationDonneesEntree() throws Exception {
        // Given - Requête avec données invalides
        SinistreController.SinistreRequest requeteInvalide = 
            new SinistreController.SinistreRequest("", "", null);

        // When & Then
        mockMvc.perform(post("/api/sinistres/soumettre")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requeteInvalide)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Test accès sans authentification")
    void testAccesSansAuthentification() throws Exception {
        // When & Then
        mockMvc.perform(post("/api/sinistres/soumettre")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(sinistreRequest)))
                .andExpect(status().isUnauthorized());
    }
}
