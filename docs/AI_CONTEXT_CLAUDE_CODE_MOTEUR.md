# Contexte de travail pour Claude Code — Moteur de planification

> ## ⚠️ Relu le 2026-08-18 — ce document est daté, et voici ce qui en tient
>
> Il a été écrit **après la phase 4 du plan de migration**, à une époque où SC-01 était le seul
> scénario opérationnel. Le moteur en porte six aujourd'hui, tous exposés par les deux canaux.
>
> **Ce qui ne tient plus :**
> - l'ancrage sur « la phase en cours » du plan de migration — ce plan est achevé, le pilotage se
>   fait par les **rangs** de `90_SUIVI_DEVELOPPEMENT_MOTEUR.md` ;
> - « ne pas activer SC-03 » — SC-03 est livré depuis longtemps, comme SC-02, SC-04, SC-05 et SC-06 ;
> - « SC-01 est le socle de stabilité actuel » — le socle est désormais la suite complète, plus de
>   huit cents tests, et le contrat publié de la série 50.
>
> **Ce qui tient, et qui est la raison de garder ce document :** ne pas réinterpréter seul
> l'architecture cible, ne pas introduire de refactoring transverse non demandé, ne pas affaiblir un
> test pour faire passer un changement, ne pas déplacer la logique métier dans le contrôleur.
> Ces règles n'ont jamais été enfreintes et n'ont pas vieilli.
>
> **À lire à la place, pour l'état du jour :** `README.md` à la racine du dépôt,
> `docs/00_INDEX_DOCUMENTATION.md`, et `docs/90_SUIVI_DEVELOPPEMENT_MOTEUR.md` pour ce qui est
> ouvert.

## Rôle de ce document

Ce fichier donne à Claude Code le **contexte d’architecture**, les **invariants à respecter** et les **règles de modification autorisées** pour intervenir sur le moteur de planification après la phase 4 du plan de migration temporaire WinDev → moteur.

Il complète la documentation technique existante.
Il ne remplace pas le plan de migration.

Document de référence principal :
- `90_PLAN_MIGRATION_TEMPORAIRE_WINDEV_VERS_MOTEUR.md`

---

## Objectif attendu de Claude Code

Claude Code doit aider à :
- sécuriser les évolutions incrémentales du moteur ;
- proposer des modifications cohérentes avec la phase en cours ;
- préserver les invariants du socle existant ;
- éviter les refactorings transverses non demandés ;
- ajouter des tests avant ou en même temps que les évolutions structurelles.

Claude Code n’est **pas autorisé** à réinterpréter seul l’architecture cible ni à anticiper des nettoyages prévus pour des phases ultérieures.

---

## Architecture canonique à respecter

Le flux logique du moteur est le suivant :

1. **WinDev transmet un JSON d’entrée** ;
2. ce JSON est désérialisé dans les **DTO d’API** ;
3. les DTO sont traduits vers le **monde domaine** par le mapping / builder prévu ;
4. le domaine alimente le **PlanningProblem** ;
5. le solveur applique ses contraintes ;
6. le résultat est restitué via le contrat de sortie API existant.

Schéma simplifié :

```text
WinDev JSON
  → ScenarioRequestDTO
  → DTO d’entrée
  → mapping / builder
  → objets domaine
  → PlanningProblem
  → solveur / contraintes / score
  → ScenarioResponseDTO
```

### Règle impérative

Le solveur ne doit **jamais** dépendre directement des DTO.

Toute exploitation métier doit passer par :
- la couche DTO,
- puis le mapping / builder,
- puis le modèle domaine,
- puis les contraintes solveur.

---

## Invariants non négociables

Claude Code doit considérer les points suivants comme **intangibles**, sauf demande explicite contraire.

### Invariant 1 — SC-01 doit rester opérationnel

Le scénario SC-01 est le socle de stabilité actuel.
Aucune évolution ne doit casser :
- sa désérialisation ;
- son exécution solveur ;
- ses tests de stabilité ;
- ses tests de score ;
- ses affectations attendues.

### Invariant 2 — Le contrat de sortie doit rester stable

Le contrat de sortie `ScenarioResponseDTO` ne doit pas être modifié sans décision explicite.

### Invariant 3 — Une phase ne doit pas anticiper la suivante

Un champ peut être :
- transporté,
- validé,
- mappé,
- mais pas encore exploité.

Claude Code ne doit pas activer un champ dans le solveur tant que la phase courante ne le prévoit pas.

### Invariant 4 — Les redondances temporaires sont volontairement tolérées

Certains champs coexistent volontairement pendant la migration.
Claude Code ne doit pas supprimer ces redondances tant que la phase de nettoyage n’est pas atteinte.

### Invariant 5 — Les tests existants ont priorité sur toute “amélioration” supposée

Si une proposition semble plus élégante mais risque de casser les tests ou la progression prévue par phases, elle doit être rejetée.

### Invariant 6 — Le builder / mapper est la traduction officielle du contrat d’entrée

Toute évolution du contrat d’entrée doit être répercutée proprement dans la couche de mapping prévue.
Claude Code ne doit pas contourner cette couche.

---

## Ce que Claude Code peut faire

Claude Code peut, si la phase le justifie :
- ajouter ou étendre des DTO ;
- ajouter de la désérialisation ;
- compléter la validation ;
- enrichir le mapping DTO → domaine ;
- enrichir les objets domaine ;
- ajouter des contraintes solveur ;
- enrichir les métriques ou diagnostics ;
- créer ou compléter des tests unitaires, d’intégration, de non-régression.

---

## Ce que Claude Code ne doit pas faire

Sans demande explicite, Claude Code ne doit jamais :
- faire dépendre le solveur directement d’un DTO ;
- fusionner prématurément plusieurs couches de responsabilité ;
- supprimer un champ simplement parce qu’il semble redondant ;
- modifier le contrat de sortie API ;
- remplacer une stratégie progressive par une refonte globale ;
- supprimer ou affaiblir des tests existants pour faire passer un changement ;
- introduire un refactoring transversal non demandé ;
- déplacer la logique métier dans le controller si elle doit vivre dans un mapper, builder, service ou domaine ;
- ~~activer SC-03 tant que la phase dédiée ne l’exige pas~~ — caduc : SC-03 est livré.

---

## Lecture obligatoire avant toute modification

Avant de proposer un changement, Claude Code doit vérifier :

1. **dans quelle phase du plan se situe la demande** ;
2. **si le champ concerné est seulement transporté, mappé, ou déjà exploité** ;
3. **si le changement impacte SC-01** ;
4. **si les tests existants couvrent déjà la zone concernée** ;
5. **si un nouveau test doit être ajouté avant la modification ou en même temps**.

---

## Stratégie de travail attendue

Pour toute évolution significative, Claude Code doit raisonner dans l’ordre suivant :

1. identifier la phase concernée ;
2. localiser la couche à faire évoluer ;
3. limiter la modification à cette couche ;
4. ajouter ou adapter les tests ciblés ;
5. vérifier qu’aucun invariant n’est violé ;
6. conserver un impact minimal sur le reste du moteur.

---

## Règles spécifiques post phase 4

À partir de la phase 5, le risque principal augmente car les changements touchent la logique solveur de manière plus structurante.

Claude Code doit faire particulièrement attention à :
- la structuration des besoins ;
- les blocs journaliers ;
- l’ordre dans les blocs ;
- les segments de pause ;
- les futures contraintes de continuité / fragmentation ;
- les données de nuit par salarié ;
- l’ouverture progressive de SC-03.

### Consigne importante

À partir de la phase 5, Claude Code doit privilégier :
- des changements incrémentaux ;
- des contraintes isolées et testables ;
- des tests dédiés par règle ;
- des modifications explicables.

Il doit éviter :
- les règles trop “intelligentes” mais opaques ;
- les refactorings massifs du solveur ;
- les optimisations prématurées ;
- les réinterprétations globales du modèle métier.

---

## Politique de tests

Toute modification structurelle doit s’accompagner d’une vérification explicite sur les tests.

### À préserver systématiquement

- `./gradlew test`
- tests de runtime API
- tests de validation API
- tests de stabilité solveur
- tests de score / scoreBreakdown
- tests WorkMetrics

### À ajouter progressivement selon le besoin

- tests de désérialisation JSON ciblés ;
- tests builder → domaine ;
- tests de contraintes isolées ;
- tests de compatibilité SC-03 ;
- tests de diagnostics d’affectation.

### Règle de sécurité

Quand un nouveau champ ou une nouvelle contrainte est introduit, Claude Code doit chercher à produire :
- un test de présence / transport si nécessaire ;
- un test de mapping domaine si applicable ;
- un test d’effet solveur si le champ devient actif ;
- un test de non-régression SC-01.

---

## Gestion des redondances transitoires

Pendant la migration, plusieurs redondances sont documentées et acceptées.

Exemples connus :
- `sitesAutorises` vs `axesOrganisationnels.lieuIds`
- `activite` vs `codeActiviteId`
- `referentiels` JSON vs référentiel hardcodé temporaire
- `contraintesReglementaires` salarié vs paramètres globaux

Claude Code doit considérer ces redondances comme **intentionnelles** tant que la phase de nettoyage n’a pas été atteinte.

---

## Règle sur les propositions de refactoring

Claude Code peut proposer un refactoring uniquement s’il respecte simultanément les conditions suivantes :

1. il reste dans le périmètre de la phase en cours ;
2. il ne modifie pas les invariants listés dans ce document ;
3. il améliore la lisibilité ou l’isolabilité des tests ;
4. il ne transforme pas une migration incrémentale en refonte générale.

Si ces 4 conditions ne sont pas réunies, le refactoring doit être refusé ou reporté.

---

## Attitude attendue de Claude Code dans ses réponses

Quand Claude Code propose une modification, il doit expliciter brièvement :
- la phase concernée ;
- la couche impactée ;
- les invariants préservés ;
- les tests à ajouter ou à rejouer ;
- les éléments volontairement non traités car prévus dans une phase ultérieure.

---

## Résumé opérationnel

Claude Code doit se comporter comme un **assistant de migration incrémentale sous contraintes**, et non comme un refactoriseur libre.

Sa priorité est :
1. préserver SC-01 ;
2. respecter le plan de migration ;
3. maintenir la séparation DTO / builder / domaine / solveur ;
4. sécuriser les changements par les tests ;
5. éviter tout nettoyage ou toute activation prématurée.

