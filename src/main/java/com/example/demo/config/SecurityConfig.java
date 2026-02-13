package com.example.demo.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Configuration de sécurité selon les spécifications du cahier des charges
 * BNF4 — Sécurité : Protection des données et des API via OAuth2/JWT et gestion des rôles
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(authz -> authz
                // Endpoints publics pour la documentation et la santé
                .requestMatchers("/actuator/health", "/actuator/info", "/v3/api-docs/**", "/swagger-ui/**").permitAll()
                // Endpoints d'administration (métriques, audit) - accès restreint
                .requestMatchers("/actuator/**").hasRole("ADMIN")
                // Endpoints de validation humaine - accès gestionnaire
                .requestMatchers("/api/validation/**").hasRole("GESTIONNAIRE")
                // Endpoints RAG - ingestion réservée à l'admin, recherche pour gestionnaire/admin
                .requestMatchers("/api/rag/ingest").hasRole("ADMIN")
                .requestMatchers("/api/rag/search").hasAnyRole("GESTIONNAIRE", "ADMIN")
                // Endpoints clients - soumission et statut (accessible au client)
                .requestMatchers("/api/sinistres/soumettre").hasAnyRole("CLIENT", "GESTIONNAIRE", "ADMIN")
                .requestMatchers("/api/sinistres/*/statut").hasAnyRole("CLIENT", "GESTIONNAIRE", "ADMIN")
                // Endpoints d'audit - accès restreint (non accessible au client)
                .requestMatchers("/api/sinistres/*/audit").hasAnyRole("GESTIONNAIRE", "ADMIN")
                // Tous les autres endpoints nécessitent une authentification
                .anyRequest().authenticated()
            )
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            )
            .httpBasic(basic -> {})
            .csrf(csrf -> csrf.disable())
            .headers(headers -> headers
                .frameOptions(frame -> frame.deny())
                .contentTypeOptions(contentType -> {})
                .httpStrictTransportSecurity(hstsConfig -> hstsConfig
                    .maxAgeInSeconds(31536000)
                )
            );

        return http.build();
    }

    @Bean
    public UserDetailsService userDetailsService() {
        // Configuration temporaire pour le développement
        // En production, utiliser une base de données ou Keycloak
        UserDetails client = User.builder()
            .username("client")
            .password(passwordEncoder().encode("client123"))
            .roles("CLIENT")
            .build();

        UserDetails gestionnaire = User.builder()
            .username("gestionnaire")
            .password(passwordEncoder().encode("gestionnaire123"))
            .roles("GESTIONNAIRE")
            .build();

        UserDetails admin = User.builder()
            .username("admin")
            .password(passwordEncoder().encode("admin123"))
            .roles("ADMIN")
            .build();

        return new InMemoryUserDetailsManager(client, gestionnaire, admin);
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
