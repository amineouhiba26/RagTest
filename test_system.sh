#!/bin/bash

# 🧪 Script de Test Rapide - Système Multi-Agents d'Assurance
# Ce script teste les fonctionnalités principales sans dépendances externes

echo "🚀 Démarrage des tests du système multi-agents..."

# Configuration
BASE_URL="http://localhost:8080"
CLIENT_AUTH="client:client123"
GESTIONNAIRE_AUTH="gestionnaire:gestionnaire123"

# Fonction d'aide
wait_for_app() {
    echo "⏳ Attente du démarrage de l'application..."
    for i in {1..30}; do
        if curl -s "$BASE_URL/actuator/health" >/dev/null 2>&1; then
            echo "✅ Application démarrée !"
            return 0
        fi
        sleep 2
        echo "   Tentative $i/30..."
    done
    echo "❌ Timeout - Application non accessible"
    return 1
}

# Test 1: Vérification de la santé de l'application
test_health() {
    echo
    echo "🔍 Test 1: Health Check"
    response=$(curl -s "$BASE_URL/actuator/health")
    if echo "$response" | grep -q '"status":"UP"'; then
        echo "✅ Health check réussi"
        return 0
    else
        echo "❌ Health check échoué: $response"
        return 1
    fi
}

# Test 2: Test de sécurité - accès sans authentification
test_security() {
    echo
    echo "🔒 Test 2: Sécurité - Accès non autorisé"
    status_code=$(curl -s -o /dev/null -w "%{http_code}" "$BASE_URL/api/sinistres/test/statut")
    if [ "$status_code" = "401" ]; then
        echo "✅ Sécurité OK - Accès refusé sans authentification (401)"
        return 0
    else
        echo "❌ Problème de sécurité - Code retour: $status_code"
        return 1
    fi
}

# Test 3: Soumission d'un sinistre
test_submission() {
    echo
    echo "📝 Test 3: Soumission d'un sinistre"
    
    # Données de test
    payload='{
        "emailClient": "test@example.com",
        "contenuDemande": "Test automatisé - Dégât des eaux dans ma cuisine suite à une fuite de canalisation. Dommages sur le parquet et les meubles.",
        "photosUrls": ["test1.jpg", "test2.jpg"]
    }'
    
    response=$(curl -s -u "$CLIENT_AUTH" \
        -H "Content-Type: application/json" \
        -d "$payload" \
        "$BASE_URL/api/sinistres/soumettre")
    
    if echo "$response" | grep -q '"success":true'; then
        sinistre_id=$(echo "$response" | grep -o '"data":"[^"]*"' | cut -d'"' -f4)
        echo "✅ Soumission réussie - ID: $sinistre_id"
        echo "$sinistre_id" > /tmp/sinistre_id.txt
        return 0
    else
        echo "❌ Échec de soumission: $response"
        return 1
    fi
}

# Test 4: Consultation du statut
test_status() {
    echo
    echo "📊 Test 4: Consultation du statut"
    
    if [ -f /tmp/sinistre_id.txt ]; then
        sinistre_id=$(cat /tmp/sinistre_id.txt)
    else
        sinistre_id="test-id"
    fi
    
    response=$(curl -s -u "$CLIENT_AUTH" "$BASE_URL/api/sinistres/$sinistre_id/statut")
    
    if echo "$response" | grep -q '"success":true'; then
        echo "✅ Consultation du statut réussie"
        return 0
    else
        echo "❌ Échec consultation statut: $response"
        return 1
    fi
}

# Test 5: Test de contrôle d'accès (gestionnaire vs client)
test_access_control() {
    echo
    echo "🛡️ Test 5: Contrôle d'accès - Audit"
    
    # Test avec CLIENT (doit échouer)
    status_code=$(curl -s -o /dev/null -w "%{http_code}" \
        -u "$CLIENT_AUTH" \
        "$BASE_URL/api/sinistres/test/audit")
    
    if [ "$status_code" = "403" ]; then
        echo "✅ Contrôle d'accès OK - CLIENT refusé pour audit (403)"
    else
        echo "❌ Problème contrôle d'accès CLIENT - Code: $status_code"
        return 1
    fi
    
    # Test avec GESTIONNAIRE (doit réussir)
    status_code=$(curl -s -o /dev/null -w "%{http_code}" \
        -u "$GESTIONNAIRE_AUTH" \
        "$BASE_URL/api/sinistres/test/audit")
    
    if [ "$status_code" = "200" ]; then
        echo "✅ Contrôle d'accès OK - GESTIONNAIRE autorisé pour audit (200)"
        return 0
    else
        echo "❌ Problème contrôle d'accès GESTIONNAIRE - Code: $status_code"
        return 1
    fi
}

# Test 6: Métriques et observabilité
test_metrics() {
    echo
    echo "📈 Test 6: Métriques et Observabilité"
    
    # Test métriques Prometheus
    if curl -s "$BASE_URL/actuator/prometheus" | head -5 | grep -q "^#"; then
        echo "✅ Métriques Prometheus disponibles"
    else
        echo "❌ Métriques Prometheus indisponibles"
        return 1
    fi
    
    # Test endpoint info
    if curl -s "$BASE_URL/actuator/info" | grep -q "{"; then
        echo "✅ Endpoint info accessible"
        return 0
    else
        echo "❌ Endpoint info inaccessible"
        return 1
    fi
}

# Test 7: Test de validation des données
test_validation() {
    echo
    echo "✅ Test 7: Validation des données d'entrée"
    
    # Payload invalide (champs manquants)
    invalid_payload='{"emailClient": ""}'
    
    status_code=$(curl -s -o /dev/null -w "%{http_code}" \
        -u "$CLIENT_AUTH" \
        -H "Content-Type: application/json" \
        -d "$invalid_payload" \
        "$BASE_URL/api/sinistres/soumettre")
    
    if [ "$status_code" = "400" ]; then
        echo "✅ Validation des données OK - Données invalides rejetées (400)"
        return 0
    else
        echo "❌ Problème validation - Code retour: $status_code"
        return 1
    fi
}

# Exécution des tests
main() {
    echo "======================================"
    echo " Tests du Système Multi-Agents"
    echo "======================================"
    
    # Vérifier si l'application est démarrée
    if ! curl -s "$BASE_URL/actuator/health" >/dev/null 2>&1; then
        echo "⚠️ Application non démarrée. Démarrez avec:"
        echo "   ./mvnw spring-boot:run"
        echo ""
        echo "Puis relancez ce script."
        exit 1
    fi
    
    # Compteur de tests
    total=0
    passed=0
    
    # Exécution de tous les tests
    tests=(
        "test_health"
        "test_security" 
        "test_submission"
        "test_status"
        "test_access_control"
        "test_metrics"
        "test_validation"
    )
    
    for test in "${tests[@]}"; do
        total=$((total + 1))
        if $test; then
            passed=$((passed + 1))
        fi
    done
    
    # Nettoyage
    rm -f /tmp/sinistre_id.txt
    
    # Résultats
    echo
    echo "======================================"
    echo "📊 Résultats des Tests"
    echo "======================================"
    echo "✅ Tests réussis: $passed/$total"
    
    if [ $passed -eq $total ]; then
        echo "🎉 Tous les tests sont PASSÉS !"
        echo "   Le système est opérationnel et conforme."
    else
        failed=$((total - passed))
        echo "❌ Tests échoués: $failed"
        echo "   Vérifiez les logs ci-dessus pour les détails."
    fi
    
    echo
    echo "📚 Guide complet disponible dans: TESTING_GUIDE.md"
}

# Démarrage
main "$@"
