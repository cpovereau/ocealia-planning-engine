# Checklist V2 — Stabilisation du scoring (ScoreWeights & WorkMetrics V2)

> Cette checklist est **opérationnelle** : elle décrit l’ordre de réalisation recommandé pour stabiliser un scoring V2 **lisible, explicable et maîtrisable**, en s’appuyant sur WorkMetrics V2, **sans ajouter de nouvelles règles métier**.

## Placement recommandé
- ✅ Cette checklist a vocation à être **référencée depuis** `@suivi_developpement_moteur.md` (tableau de bord).
- ✅ Elle peut vivre **en document dédié** pour rester actionnable et éviter d’alourdir le suivi.
- ✅ Le lien à ce document a été ajouté dans `@suivi_developpement_moteur.md` un lien/renvoi vers ce document (section “Prochaines étapes V2 — Scoring”).

---

## Pré-requis (garde-fous)
- ✅ Le périmètre V2 n’ajoute **aucune nouvelle règle**.
- ✅ Choix sémantique validé : **NUIT/FÉRIÉ en MINUTES**, non-couvert/poste virtuel en OCCURRENCE.
- ✅ `StrategieScoring` reste un **multiplicateur centralisé** (pas de coefficients cachés).

---

## Étapes V2 (ordre recommandé)

### 1) Normaliser les clés de pénalité
- ✅ Introduction de `PenaliteKey` pour les **4 clés SOFT V2** :
  - `METIER_SOFT_CRENEAU_NON_COUVERT`
  - `METIER_SOFT_AFFECTATION_POSTE_VIRTUEL`
  - `LEGAL_SOFT_TRAVAIL_JOUR_FERIE_MINUTES`
  - `LEGAL_SOFT_TRAVAIL_NUIT_MINUTES`
- ✅ Suppression de toute clé générique type `VIOLATION_LEGALE` / `VIOLATION_SERVICE` comme identifiant final de pondération.

### 2) Aligner la mesure des contraintes sur les unités V2
- ✅ `CreneauDeNuit` pénalise en **minutes** effectives.
- ✅ `CreneauJourFerie` pénalise en **minutes** effectives.
- ✅ `CreneauNonAffecte` et `AffectationPosteVirtuel` restent en **occurrence**.

### 3) Centraliser les coefficients de stratégie
- ✅ Suppression des coefficients locaux (`coefficient(StrategieScoring)`) dans les contraintes.
- ✅ Règle normative appliquée :
  - la contrainte mesure (unites + PénaliteKey)
  - ScoreWeights = applique les poids par stratégie
  - ScoreUtils construit le score final OptaPlanner

### 4) Implémenter la table ScoreWeights V2 (baseline)
- ✅ Poids V2 validés (SoftScore) :
  - `NON_COUVERT` = 10 000 / occurrence
  - `POSTE_VIRTUEL` = 2 000 / occurrence
  - `FERIE_MINUTES` = 5 / minute
  - `NUIT_MINUTES` = 3 / minute
- ✅ Les poids sont centralisés, versionnés et documentés.
- ✅ Aucune pondération n'est définie dans les contraintes

### 5) Implémenter ScoreUtils (point de passage unique)
- ✅ ScoreUtils est l'unique composant qui transforme : :
    - (PenaliteKey, unités, StrategiesScoring) -> HardSofScore
- ✅ Conversion assumée long -> int via Math.toIntExact.
- ✅ Aucun calcul métier dans ScoreUtils.

### 6) Tests V2 — dominance (sans optimum)
- ✅ Test de “dominance” sans solver implémentés (ScoreDominanceTest).
- ✅ Les seils de dominance sont cohérents avec ScoreWeights.
- ✅ Cas validés : 
    - NON_COUVERT >> NUIT / FÉRIÉ
    - POSTE_VIRTUEL vs FÉRIÉ (seuil = 400 min en EXPLOITATION)
    - FÉRIÉ >> NUIT à volume égal
    - Changement de StrategieScoring => score différent

### 7) Vérification de non-régression (sanity)
- [ ] Vérifier que les solutions qui étaient “meilleures” avant restent globalement ordonnées (à périmètre constant).
- [ ] Vérifier qu’aucune contrainte HARD n’est devenue “softisée” accidentellement.

---

## Hors périmètre V2 (rappel)
- ✅ Ne pas brancher service/perso (couverture minimale, préférences) avant stabilisation ScoreWeights.
- ✅ Ne pas ajouter pénibilité par occurrence pour nuit/férié (V3).
- ✅ Ne pas introduire WorkMetrics V3 (séquences, équité) dans ce cycle.

---

## Critères de sortie V2
- ✅ Les 4 clés SOFT V2 sont pondérées explicitement et centralisées.
- ✅ Aucune logique de stratégie dans les contraintes.
- ✅ Les tests “dominance” passent et documentent les invariants.
- [ ] Le scoring peut être expliqué en : (clé, unités, poids, multiplicateur, total).
