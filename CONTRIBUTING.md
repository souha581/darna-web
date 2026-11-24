# Guide de Contribution — Darna Web (Lot 1)

## 1. Stratégie de branches
- `main` : branche stable protégée (fusion uniquement par Pull Request).
- `develop` : branche d'intégration continue du binôme.
- `feature/<nom>` / `fix/<nom>` / `docs/<nom>` : branches de travail créées depuis `develop`.

## 2. Convention de commits (Conventional Commits)
Format : `<type>(<portée>): <description>`
- `feat(...)` : nouvelle fonctionnalité
- `fix(...)` : correction de bug
- `test(...)` : ajout ou mise à jour de tests
- `ci(...)` : pipeline CI/CD ou Docker
- `docs(...)` : documentation

## 3. Règles de Pull Request (Binôme)
1. Créer une branche depuis `develop`.
2. Vérifier que le projet compile et passe les tests (`mvn test`).
3. Ouvrir une Pull Request vers `develop` (ou `main` pour une release).
4. Faire relire et approuver la PR par l'autre membre du binôme avant de fusionner.

## 4. Répartition indicative du binôme
- **Membre A (Intégration & Qualité)** : Git, CI, tests automatisés, linting/analyse statique.
- **Membre B (Livraison & Exploitation)** : Docker, registre, secrets, CD, sécurité et supervision.