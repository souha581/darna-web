# Matrice des configurations, secrets et DevSecOps (Lot 5)

## 1. Séparation des environnements

| Paramètre | Développement (`dev`) | Intégration CI (`ci`) | Environnement de Test (`test`) |
| :--- | :--- | :--- | :--- |
| Profil Spring (`SPRING_PROFILES_ACTIVE`) | `dev` | `ci` | `test` |
| Fichier de configuration | `application-dev.yml` | `application-ci.yml` | `application-test.yml` |
| Base de données | PostgreSQL (`darna_dev`) | H2 en mémoire (`darna_ci`, mode PostgreSQL) | PostgreSQL dédié (`darna_test`) |
| Port HTTP | `8080` | Éphémère (`MockMvc`) | `8081` |
| Cache Thymeleaf | Désactivé (`false`) | Activé | Activé (`true`) |
| Logs `com.darna` | `DEBUG` | `INFO` | `INFO` |

---

## 2. Variables d'environnement et secrets (sans exposer les valeurs réelles)

> **Règle stricte** : aucun mot de passe réel et aucun fichier `.env` n'est versionné dans Git. Seul `.env.example` est versionné avec des valeurs factices.

| Variable | Sensible ? | Emplacement en `dev` | Emplacement en `test` / CI | Rôle |
| :--- | :--- | :--- | :--- | :--- |
| `SPRING_PROFILES_ACTIVE` | Non | `.env` / `docker-compose.yml` | `docker-compose.test.yml` | Active le profil Spring correspondant |
| `DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USER` | Non | `.env` / `docker-compose.yml` | `docker-compose.test.yml` / GitHub Vars | Paramètres de connexion PostgreSQL |
| `DB_PASSWORD` | **Oui (Secret)** | `.env` local (ignoré par Git) | GitHub Secret `TEST_DB_PASSWORD` | Mot de passe PostgreSQL |
| `APP_OWNER_PASSWORD` | **Oui (Secret)** | `.env` local (ignoré par Git) | GitHub Secret `TEST_OWNER_PASSWORD` | Mot de passe du compte seed `OWNER` |
| `APP_RESIDENT_PASSWORD` | **Oui (Secret)** | `.env` local (ignoré par Git) | GitHub Secret `TEST_RESIDENT_PASSWORD` | Mot de passe du compte seed `RESIDENT` |
| `APP_OUVRIER_PASSWORD` | **Oui (Secret)** | `.env` local (ignoré par Git) | GitHub Secret `TEST_OUVRIER_PASSWORD` | Mot de passe du compte seed `OUVRIER` |
| `GITHUB_TOKEN` | **Oui (Secret)** | N/A | Fourni automatiquement par GitHub Actions | Authentification vers GHCR |

---

## 3. Contrôles de sécurité intégrés (DevSecOps)

1. **Détection de secrets (`Gitleaks`)** : analyse le dépôt Git dans le job `security-scan` de `ci.yml` pour bloquer tout commit contenant un mot de passe, jeton ou clé API.
2. **Scan des dépendances (`Trivy fs`)** : inspecte `pom.xml` à la recherche de CVE connues (`trivy-fs-report.txt` archivé dans Actions).
3. **Scan de l'image Docker (`Trivy image`)** : inspecte les paquets système de l'image JRE et les bibliothèques embarquées dans `app.jar` avant la publication sur GHCR (`trivy-image-report.txt`).
4. **Analyse statique du code (`SpotBugs`)** : exécutée dans `mvn verify` à chaque build.

---

## 4. Analyse et remédiation d'alertes (Actions correctives)

### Action corrective n° 1 — Suppression des mots de passe codés en dur dans `application.yml`
- **Constat initial** : `application.yml` contenait des mots de passe par défaut (`owner123`, `resident123`, `ouvrier123`) en clair dans le dépôt Git.
- **Risque** : exposition de mots de passe par défaut dans l'historique Git et risque d'utilisation en environnement de test avec des identifiants prévisibles.
- **Correction appliquée** : remplacement par des placeholders vides (`${APP_OWNER_PASSWORD:}`), désactivation du seed par défaut (`APP_SEED_ENABLED: false`), séparation des profils `application-dev.yml` / `application-test.yml`, et fourniture d'un gabarit `.env.example`.

### Action corrective n° 2 — Durcissement du conteneur Docker
- **Constat** : exécuter Java en `root` dans un conteneur augmente l'impact d'une faille applicative.
- **Correction appliquée** : création d'un utilisateur système non-privilégié (`appuser`) dans `Dockerfile` (`USER appuser`) et build multi-stage supprimant Maven et le code source de l'image finale.
