# Fiche d'Incident — Darna Web

| Champ | Détail |
| :--- | :--- |
| **Identifiant** | `INC-2026-001` |
| **Application / Environnement** | `Darna Web` — Environnement `test` (`http://localhost:8081`) |
| **Version impactée** | Image `ghcr.io/souha581/darna-web:<tag_defectueux>` |
| **Détecté par** | Échec des smoke tests post-déploiement (`scripts/smoke-test.sh`) et sonde `/actuator/health` |

## 1. Symptômes observés
- L'application ne répond pas sur `http://localhost:8081/actuator/health` dans le délai imparti (`24` tentatives).
- Le tableau de bord Prometheus (`http://localhost:9090`) indique la cible `darna-web-test` à l'état `DOWN`.

## 2. Diagnostic (logs et pipeline)
Commande de diagnostic exécutée :
```bash
docker compose -f docker-compose.test.yml logs --tail=60 app-test
```
Cause racine identifiée :
- Erreur d'authentification à la base PostgreSQL de test (`FATAL: password authentication failed for user "darna_test"`) suite à une variable `DB_PASSWORD` invalide ou incohérente lors de la mise à jour du conteneur.

## 3. Action corrective immédiate (Rollback)
- Déclenchement du workflow **Rollback Test Environment** (`.github/workflows/rollback.yml`) / `scripts/rollback.sh` vers le dernier tag stable validé.
- Exécution automatique des smoke tests confirmant le retour à `status: UP` sur `/actuator/health`, `200 OK` sur `/` et `/api/apartments`.
- Délai de restauration (MTTR) : `< 1 minute`.

## 4. Mesures préventives
- Vérification préalable des secrets GitHub de l'environnement `test` avant tout déploiement.
- Conservation systématique des tags immuables `sha-<commit>` dans GHCR pour permettre un retour arrière rapide.
