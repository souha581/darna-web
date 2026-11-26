# Guide de supervision, diagnostic d'incident et rollback — Darna Web (Lot 7)

## 1. Outils de supervision disponibles

| Composant | URL ou commande | Utilité |
| :--- | :--- | :--- |
| **Sonde de santé** | `GET http://localhost:8081/actuator/health` | Vérifie l'état de Darna Web et de PostgreSQL (`status: UP / DOWN`) |
| **Version déployée** | `GET http://localhost:8081/actuator/info` | Affiche `APP_VERSION`, le profil actif et les infos de build Maven |
| **Métriques Actuator** | `GET http://localhost:8081/actuator/metrics` | JVM, connexions HikariCP, temps HTTP, compteurs `darna.rentals.completed`, `darna.charges.paid`, `darna.repairs.reported` |
| **Prometheus** | `http://localhost:9090` (stack test) | Scrape `/actuator/prometheus` toutes les 15 s (`up`, requêtes HTTP, mémoire JVM, compteurs métier) |
| **Logs applicatifs (fichier)** | `./logs/darna-web.log` (`dev`) / volume `darna_test_logs` (`test`) | Journaux horodatés des opérations métier (locations, paiements, signalements) et erreurs |
| **Logs Docker en direct** | `docker compose -f docker-compose.test.yml logs -f app-test` | Inspection en temps réel des logs du conteneur test |
| **Historique des livraisons** | `deployments/history.log` | Trace horodatée des déploiements et rollbacks validés |

---

## 2. Scénario de simulation d'incident contrôlé (Soutenance)

### Étape A — Déployer une version saine de référence (`v1`)
Vérifier que `docker-compose.test.yml` tourne avec une image stable (ex. `sha-<bon_commit>`) et que `smoke-test` passe (`status: UP`).

### Étape B — Déclencher l'incident contrôlé
Simuler une erreur de configuration ou de version défectueuse dans l'environnement `test` :
```powershell
# Exemple : mot de passe PostgreSQL erroné lors d'un déploiement test
$env:DB_PASSWORD="MAUVAIS_MOT_DE_PASSE"
$env:DARNA_IMAGE="ghcr.io/souha581/darna-web:develop"
docker compose -f docker-compose.test.yml up -d --force-recreate app-test
```

### Étape C — Observer et diagnostiquer
1. **Sonde de santé / Smoke test** :
   ```powershell
   curl.exe -i http://localhost:8081/actuator/health
   # Échec de connexion ou HTTP 503 ("status":"DOWN")
   ```
2. **Consulter les logs du conteneur** :
   ```powershell
   docker compose -f docker-compose.test.yml logs --tail=60 app-test
   ```
   Diagnostic observé : `FATAL: password authentication failed for user "darna_test"` / `HikariPool-1 - Exception during pool initialization`.

---

## 3. Procédure de Rollback vers une version stable

### Option 1 — Via GitHub Actions (recommandé)
1. Ouvrir **Actions → Rollback Test Environment → Run workflow**.
2. Renseigner `target_image_tag` avec le tag GHCR stable précédent (ex. `sha-<ancien_commit>` ou `develop`).
3. Renseigner `reason` (ex. `INC-2026-001 : Erreur configuration DB test`).
4. Approuver le déploiement dans l'environnement `test` si la revue manuelle est activée.
5. Le pipeline exécute `scripts/rollback.sh`, vérifie les smoke tests et archive `deployments/history.log`.

### Option 2 — Via script local (`scripts/rollback.ps1` ou `scripts/rollback.sh`)
```powershell
# Restaurer les bonnes variables et l'image stable précédente
$env:DB_PASSWORD="<MOT_DE_PASSE_TEST_VALIDE>"
.\scripts\rollback.ps1 -TargetImage "ghcr.io/souha581/darna-web:develop" -TargetVersion "develop"
```

### Vérification post-rollback
- `http://localhost:8081/actuator/health` retourne `{"status":"UP",...}`.
- `http://localhost:9090/targets` affiche `darna-web-test` à l'état `UP`.
- `deployments/history.log` enregistre une ligne `ROLLBACK | status=SUCCESS`.
