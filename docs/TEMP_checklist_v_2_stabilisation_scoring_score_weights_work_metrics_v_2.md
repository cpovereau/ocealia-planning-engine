# Checklist V2 — Stabilisation du scoring (ScoreWeights & WorkMetrics V2)

> Cette checklist est **opérationnelle** : elle décrit l’ordre de réalisation recommandé pour stabiliser un scoring V2 **lisible, explicable et maîtrisable**, en s’appuyant sur WorkMetrics V2, **sans ajouter de nouvelles règles métier**.

## Placement recommandé
- ✅ Cette checklist a vocation à être **référencée depuis** `@suivi_developpement_moteur.md` (tableau de bord).
- ✅ Elle peut vivre **en document dédié** pour rester actionnable et éviter d’alourdir le suivi.
- ➜ Recommandation : ajouter dans `@suivi_developpement_moteur.md` un lien/renvoi vers ce document (section “Prochaines étapes V2 — Scoring”).

---

## Pré-requis (garde-fous)
- [ ] Confirmer que le périmètre V2 n’ajoute **aucune nouvelle règle**.
- [ ] Confirmer le choix sémantique : **NUIT/FÉRIÉ en MINUTES**, non-couvert/poste virtuel en OCCURRENCE.
- [ ] Confirmer que `StrategieScoring` reste un **multiplicateur centralisé** (pas de coefficients cachés).

---

## Étapes V2 (ordre recommandé)

### 1) Normaliser les clés de pénalité
- [ ] Introduire (ou documenter) la notion de `PenaliteKey` (enum ou équivalent) pour les **4 clés SOFT V2** :
  - `METIER_SOFT_CRENEAU_NON_COUVERT`
  - `METIER_SOFT_AFFECTATION_POSTE_VIRTUEL`
  - `LEGAL_SOFT_TRAVAIL_JOUR_FERIE_MINUTES`
  - `LEGAL_SOFT_TRAVAIL_NUIT_MINUTES`
- [ ] Vérifier qu’aucune clé générique type `VIOLATION_LEGALE` / `VIOLATION_SERVICE` ne reste utilisée comme identifiant final de pondération.

### 2) Aligner la mesure des contraintes sur les unités V2
- [ ] Modifier `CreneauDeNuit` pour pénaliser en **minutes** (durée réelle) et non en “par créneau”.
- [ ] Modifier `CreneauJourFerie` pour pénaliser en **minutes**.
- [ ] Conserver `CreneauNonAffecte` et `AffectationPosteVirtuel` en **occurrence**.

### 3) Centraliser les coefficients de stratégie
- [ ] Retirer les coefficients “cachés” dans les contraintes (`coefficient(StrategieScoring)`).
- [ ] Définir la règle normative :
  - contrainte = calcule uniquement des **unités** associées à une `PenaliteKey`
  - ScoreWeights = applique `poids(PenaliteKey) × multiplicateur(StrategieScoring)`

### 4) Implémenter la table ScoreWeights V2 (baseline)
- [ ] Fixer les poids V2 validés (SoftScore) :
  - `NON_COUVERT` = 10 000 / occurrence
  - `POSTE_VIRTUEL` = 2 000 / occurrence
  - `FERIE_MINUTES` = 5 / minute
  - `NUIT_MINUTES` = 3 / minute
- [ ] Rendre ces poids explicites, versionnés, documentés.

### 5) Garantir la traçabilité de la pondération
- [ ] Ajouter une méthode de debug/trace (non-UI) pour exporter :
  - la liste des pénalités calculées (clé + unités)
  - le poids appliqué (clé + poids)
  - le multiplicateur stratégie
  - le total soft par clé

### 6) Tests V2 — dominance (sans optimum)
- [ ] Écrire un test “dominance” : +1 `NON_COUVERT` domine toute variation raisonnable nuit/férié.
- [ ] Écrire un test “dominance” : à hard égal, +60 minutes nuit dégrade le soft.
- [ ] Écrire un test “dominance” : à hard égal, +60 minutes férié dégrade le soft.
- [ ] Écrire un test “arbitrage” : poste virtuel vs exposition férié/nuit (selon vos fixtures).

### 7) Vérification de non-régression (sanity)
- [ ] Vérifier que les solutions qui étaient “meilleures” avant restent globalement ordonnées (à périmètre constant).
- [ ] Vérifier qu’aucune contrainte HARD n’est devenue “softisée” accidentellement.

---

## Hors périmètre V2 (rappel)
- [ ] Ne pas brancher service/perso (couverture minimale, préférences) avant stabilisation ScoreWeights.
- [ ] Ne pas ajouter pénibilité par occurrence pour nuit/férié (V3).
- [ ] Ne pas introduire WorkMetrics V3 (séquences, équité) dans ce cycle.

---

## Critères de sortie V2
- [ ] Les 4 clés SOFT V2 sont pondérées explicitement et centralisées.
- [ ] Aucun coefficient caché dans les contraintes.
- [ ] Les tests “dominance” passent et documentent les invariants.
- [ ] Le scoring peut être expliqué en : (clé, unités, poids, multiplicateur, total).
