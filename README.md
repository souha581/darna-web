# Darna Web — Gestion de Résidences et Copropriétés

Version web de l'application **DARNA** (Java 21 / Spring Boot / Spring Security / Spring Data JPA / Thymeleaf).

## Acteurs & Fonctionnalités
- **Visiteur (sans compte)** : consultation du catalogue public des appartements disponibles (`/`), filtrage par typologie (`S+0`..`S+3`) et loyer max (TND), location en ligne avec création automatique de compte résident.
- **Propriétaire / Syndic (`OWNER`)** : gestion des appartements, émission des charges de copropriété, suivi des encaissements, affectation des pannes aux ouvriers.
- **Résident (`RESIDENT`)** : consultation et règlement des charges (Carte / Virement, paiements partiels), signalement d'incidents techniques.
- **Ouvrier (`OUVRIER`)** : consultation des interventions assignées avec localisation complète (résidence, bâtiment, étage, appartement) et clôture avec rapport et coût en TND.

## Démarrage local rapide
```bash
mvn spring-boot:run
```
Puis ouvrir `http://localhost:8080`.

Comptes de démonstration locaux :
- Propriétaire (`OWNER`) : `owner` / `owner123`
- Résident (`RESIDENT`) : `resident` / `resident123`
- Ouvrier (`OUVRIER`) : `ouvrier` / `ouvrier123`
