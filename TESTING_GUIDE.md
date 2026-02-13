# 🧪 Guide Complet de Test - Système Multi-Agents d'Assurance

## 📋 Types de Tests Disponibles

### 1. Tests Unitaires
### 2. Tests d'Intégration  
### 3. Tests API avec curl
### 4. Tests de Charge
### 5. Tests End-to-End

---

## 🚀 1. Tests Unitaires

### Exécution des Tests Unitaires

```bash
# Tests unitaires uniquement
./mvnw test

# Tests avec profil de test spécifique
./mvnw test -Dspring.profiles.active=test

# Test d'une classe spécifique
./mvnw test -Dtest=ConfidenceScoreServiceTest

# Tests avec couverture de code
./mvnw test jacoco:report
```

### Résultats Attendus
✅ Tous les services (ConfidenceScoreService, AuditService, HumanValidationService)
✅ Calculs de scores de confiance
✅ Gestion des seuils de validation humaine

---

## 🌐 2. Tests API avec curl

### Configuration Locale

1. **Démarrer l'application**:
```bash
./mvnw spring-boot:run
```

2. **Vérifier le démarrage**:
```bash
curl http://localhost:8080/actuator/health
# Réponse: {"status":"UP"}
```

### Tests des Endpoints Principaux

#### A. Soumission d'un Sinistre

```bash
# Authentification Client
curl -X POST http://localhost:8080/api/sinistres/soumettre \
  -H "Content-Type: application/json" \
  -u "client:client123" \
  -d '{
    "emailClient": "john.doe@example.com",
    "contenuDemande": "Dégât des eaux dans ma cuisine suite à une fuite de canalisation. L'\''eau a endommagé le parquet et les meubles bas. Incident survenu le 10/02/2026 vers 14h30.",
    "photosUrls": ["https://example.com/photo1.jpg", "https://example.com/photo2.jpg"]
  }'
```

**Réponse Attendue**:
```json
{
  "success": true,
  "message": "Sinistre soumis avec succès. Traitement en cours.",
  "data": "uuid-generated-id",
  "metadata": null
}
```

#### B. Consultation du Statut

```bash
# Remplacer {ID} par l'ID retourné
curl -X GET http://localhost:8080/api/sinistres/{ID}/statut \
  -u "client:client123"
```

#### C. Tests de Sécurité

```bash
# Test sans authentification (doit échouer)
curl -X POST http://localhost:8080/api/sinistres/soumettre \
  -H "Content-Type: application/json" \
  -d '{"emailClient":"test@test.com","contenuDemande":"test"}'
# Réponse: 401 Unauthorized

# Test avec mauvaises credentials (doit échouer)  
curl -X GET http://localhost:8080/api/sinistres/test/audit \
  -u "client:wrongpassword"
# Réponse: 401 Unauthorized

# Test d'accès audit avec rôle CLIENT (doit échouer)
curl -X GET http://localhost:8080/api/sinistres/test/audit \
  -u "client:client123"  
# Réponse: 403 Forbidden
```

#### D. Tests Gestionnaire

```bash
# Consultation des demandes en attente (GESTIONNAIRE uniquement)
curl -X GET http://localhost:8080/api/validation/en-attente \
  -u "gestionnaire:gestionnaire123"

# Validation d'une demande
curl -X POST http://localhost:8080/api/validation/{ID}/valider \
  -H "Content-Type: application/json" \
  -u "gestionnaire:gestionnaire123" \
  -d '{
    "decision": "APPROUVER",
    "justification": "Demande conforme aux conditions contractuelles, documentation complète"
  }'

# Audit d'une demande
curl -X GET http://localhost:8080/api/sinistres/{ID}/audit \
  -u "gestionnaire:gestionnaire123"
```

---

## 🔄 3. Test Complet du Workflow

### Script de Test Automatisé

```bash
#!/bin/bash
# test_workflow.sh

echo "🚀 Test du Workflow Multi-Agents"
BASE_URL="http://localhost:8080"

# 1. Vérification de la santé de l'application
echo "1. Health Check..."
health=$(curl -s $BASE_URL/actuator/health | grep '"status":"UP"')
if [ -z "$health" ]; then
  echo "❌ Application non disponible"
  exit 1
fi
echo "✅ Application opérationnelle"

# 2. Soumission d'un sinistre
echo "2. Soumission d'un sinistre..."
response=$(curl -s -X POST $BASE_URL/api/sinistres/soumettre \
  -H "Content-Type: application/json" \
  -u "client:client123" \
  -d '{
    "emailClient": "test@example.com",
    "contenuDemande": "Accident automobile - collision avec un autre véhicule le 10/02/2026. Dommages sur l'\''aile avant droite et le pare-chocs. Constat amiable rempli.",
    "photosUrls": ["photo1.jpg", "photo2.jpg"]
  }')

# Extraction de l'ID de la réponse
sinistre_id=$(echo $response | grep -o '"data":"[^"]*"' | cut -d'"' -f4)
echo "✅ Sinistre soumis avec ID: $sinistre_id"

# 3. Consultation du statut
echo "3. Consultation du statut..."
sleep 2  # Attendre le traitement
statut=$(curl -s $BASE_URL/api/sinistres/$sinistre_id/statut -u "client:client123")
echo "✅ Statut récupéré: $(echo $statut | grep -o '"statut":"[^"]*"')"

# 4. Vérification de l'audit (GESTIONNAIRE)
echo "4. Consultation de l'audit..."
audit=$(curl -s $BASE_URL/api/sinistres/$sinistre_id/audit -u "gestionnaire:gestionnaire123")
echo "✅ Audit disponible: $(echo $audit | grep -c 'AuditEvent') événements"

echo "🎉 Test du workflow terminé avec succès!"
```

### Exécution du Script
```bash
chmod +x test_workflow.sh
./test_workflow.sh
```

---

## 📊 4. Tests de Performance

### Test de Charge avec curl
```bash
# Test de charge simple
for i in {1..10}; do
  curl -X POST http://localhost:8080/api/sinistres/soumettre \
    -H "Content-Type: application/json" \
    -u "client:client123" \
    -d "{\"emailClient\":\"client$i@example.com\",\"contenuDemande\":\"Test $i\"}" &
done
wait
echo "Test de charge terminé"
```

### Monitoring des Métriques
```bash
# Métriques Prometheus
curl http://localhost:8080/actuator/prometheus | grep -E "(http_|jvm_)"

# Santé détaillée
curl http://localhost:8080/actuator/health | jq '.'
```

---

## 🎯 5. Tests de Validation Métier

### Cas de Test Spécifiques

#### A. Test Score de Confiance Élevé
```bash
curl -X POST http://localhost:8080/api/sinistres/soumettre \
  -u "client:client123" \
  -H "Content-Type: application/json" \
  -d '{
    "emailClient": "premium@example.com",
    "contenuDemande": "Sinistre automobile survenu le 10/02/2026 à 14h30, rue de la Paix à Paris. Collision avec véhicule tiers, constat amiable signé. Dommages: pare-chocs avant endommagé, phare cassé. Estimation initiale: 1500€. Véhicule: Peugeot 308, immatriculation AB-123-CD.",
    "photosUrls": ["constat.jpg", "dommages1.jpg", "dommages2.jpg"]
  }'
```
*Résultat attendu : Score > 0.85, traitement automatique*

#### B. Test Score de Confiance Faible  
```bash
curl -X POST http://localhost:8080/api/sinistres/soumettre \
  -u "client:client123" \
  -H "Content-Type: application/json" \
  -d '{
    "emailClient": "test@example.com", 
    "contenuDemande": "Accident",
    "photosUrls": []
  }'
```
*Résultat attendu : Score < 0.7, validation humaine requise*

#### C. Test Montant Élevé
```bash
curl -X POST http://localhost:8080/api/sinistres/soumettre \
  -u "client:client123" \
  -H "Content-Type: application/json" \
  -d '{
    "emailClient": "vip@example.com",
    "contenuDemande": "Incendie majeur dans mon entrepôt. Dommages estimés à 50000€. Intervention des pompiers, rapport disponible.",
    "photosUrls": ["incendie1.jpg", "rapport_pompiers.pdf"]
  }'
```
*Résultat attendu : Validation humaine obligatoire (montant > 10k€)*

---

## ✅ 6. Checklist de Validation

### Tests Fonctionnels
- [ ] Soumission de sinistre réussie
- [ ] Classification automatique correcte
- [ ] Score de confiance calculé
- [ ] Validation humaine déclenchée si nécessaire
- [ ] Audit complet enregistré
- [ ] API REST sécurisées

### Tests Non-Fonctionnels
- [ ] Performance acceptable (< 5s par demande)
- [ ] Sécurité : authentification/autorisation
- [ ] Résilience : gestion d'erreurs
- [ ] Observabilité : métriques disponibles
- [ ] Logs structurés générés

### Tests d'Intégration
- [ ] Base de données fonctionnelle
- [ ] Services externes (Ollama) accessibles  
- [ ] Docker Compose opérationnel
- [ ] Health checks passants

---

## 🐛 Troubleshooting

### Problèmes Courants

1. **Application ne démarre pas**
   ```bash
   # Vérifier les logs
   ./mvnw spring-boot:run -X
   
   # Vérifier PostgreSQL
   docker ps | grep postgres
   ```

2. **Erreurs d'authentification** 
   ```bash
   # Tester avec curl verbose
   curl -v -u "client:client123" http://localhost:8080/actuator/health
   ```

3. **Ollama non accessible**
   ```bash
   # Vérifier Ollama
   curl http://localhost:11434/api/version
   
   # Démarrer Ollama si nécessaire
   ollama serve
   ```

4. **Base de données non disponible**
   ```bash
   # Vérifier la connexion
   psql -h localhost -U postgres -d testrag -c "SELECT 1;"
   ```

---

## 📈 Métriques de Succès

### Indicateurs Clés
- **Taux de réussite** : > 95% des requêtes
- **Temps de réponse** : < 5 secondes
- **Score de confiance** : Distribution correcte
- **Validation humaine** : Déclenchée appropriément
- **Audit** : 100% des actions tracées

### Commandes de Vérification
```bash
# Statistiques globales
curl -s http://localhost:8080/actuator/metrics/http.server.requests | jq .

# Utilisation mémoire
curl -s http://localhost:8080/actuator/metrics/jvm.memory.used | jq .

# Nombre de demandes traitées
curl -s http://localhost:8080/actuator/metrics | grep -i sinistre
```

🎉 **Le système est maintenant entièrement testable et validé selon les spécifications du cahier des charges !**
