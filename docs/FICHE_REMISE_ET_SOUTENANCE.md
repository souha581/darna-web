# Guide de Soutenance, Indicateurs & Fiche de Remise — Darna Web

## 1. Déroulé de la démonstration finale (Section 10 de l'énoncé)

1. **Présentation & Git :** montrer le dépôt `souha581/darna-web`, les branches (`main`, `develop`, `feat/*`), la protection de `main` et les Pull Requests relues par le binôme (`#1`, `#2`, etc.).
2. **Modification en direct :** effectuer une petite modification sur une branche (ex. ajout d'un test ou mise à jour texte) et pousser sur GitHub.
3. **Pipeline CI (`ci.yml`) :** montrer dans **Actions** les 3 jobs (`build-and-package`, `security-scan`, `docker-build-and-publish`).
4. **Tests & Qualité :** montrer Checkstyle (`0` violation), SpotBugs (`0` bug), les tests JUnit/MockMvc (`12` tests) et le rapport JaCoCo archivé.
5. **Artefact & Registre :** montrer l'artefact JAR versionné dans **Actions** et les images taguées (`sha-<commit>`, `develop`, `latest`) dans **GitHub Packages (GHCR)**.
6. **Déploiement en test (`cd.yml`) :** montrer l'approbation manuelle sur l'environnement `test`, le déploiement de `docker-compose.test.yml` (port `8081`) et les résultats des smoke tests.
7. **Sécurité (DevSecOps) :** montrer le rapport **Gitleaks** (`0` secret) et les rapports **Trivy** (`trivy-fs-report.txt`, `trivy-image-report.txt`), et expliquer les actions correctives documentées dans `docs/CONFIG_MATRIX.md`.
8. **Incident & Rollback :** simuler l'incident contrôlé, diagnostiquer via `docker compose -f docker-compose.test.yml logs app-test`, exécuter `rollback.yml` (ou `scripts/rollback.ps1`) et vérifier `/actuator/health` (`UP`) + Prometheus (`http://localhost:9090`).
9. **Bilan :** présenter les indicateurs ci-dessous, les limites et améliorations possibles (ex. SonarQube, Kubernetes, alerting).

---

## 2. Indicateurs de suivi et d'amélioration continue (Section 13)

| Indicateur | Valeur / Source |
| :--- | :--- |
| Taux de réussite des pipelines | Relevé dans **Actions → CI Pipeline** |
| Durée moyenne d'exécution CI | ~1 à 2 min (grâce au cache Maven & Docker Buildx `type=gha`) |
| Couverture des tests (JaCoCo) | > 55 % (seuil bloquant configuré dans `pom.xml`) |
| Défauts bloqués avant déploiement | Checkstyle + SpotBugs + 12 tests unitaires/API/Web |
| Contrôles sécurité intégrés | Gitleaks (secrets) + Trivy FS & Image (CVE) + conteneur non-root |
| Délai de restauration (Rollback) | < 60 s via image GHCR immuable + smoke tests automatiques |

---

## 3. Fiche synthétique de remise (Section 15 de l'énoncé)

| Information | Valeur |
| :--- | :--- |
| **Nom(s) et prénom(s)** | Amir Menzli & Souha Bouagila |
| **Intitulé de l'application** | Darna Web — Gestion de Résidences et Copropriétés |
| **Lien du dépôt Git** | `https://github.com/souha581/darna-web` |
| **Technologies de l'application** | Java 21, Spring Boot 3.4, Spring Security, Spring Data JPA, Thymeleaf, PostgreSQL / H2, Maven |
| **Plateforme CI/CD utilisée** | GitHub Actions |
| **Registre d'images / artefacts** | GitHub Container Registry (`ghcr.io/souha581/darna-web`) & GitHub Actions Artifacts |
| **Environnement de test et procédure d'accès** | Stack Docker Compose (`docker-compose.test.yml`) sur port `8081` + Prometheus `9090` |
| **Contrôle(s) de sécurité intégré(s)** | Gitleaks (secrets), Trivy FS & Image (vulnérabilités), SpotBugs (analyse statique) |
| **Procédure de rollback** | Workflow `.github/workflows/rollback.yml` + `scripts/rollback.sh` / `rollback.ps1` |
| **Date de remise** | 26/11/2026 |
