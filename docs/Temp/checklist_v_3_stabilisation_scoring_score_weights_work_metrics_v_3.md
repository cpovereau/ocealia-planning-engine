# Checklist V3 — Stabilisation du scoring (ScoreWeights & WorkMetrics V3)

## Objectif général

Cette checklist fournit un **cadre opérationnel** pour concevoir, implémenter et stabiliser le scoring V3.

Elle a pour finalité de :
- structurer les travaux V3 sans ambiguïté,
- garantir la **non-régression du scoring V2**,
- assurer une montée en complexité **progressive, explicable et maîtrisée**.

Elle doit être lue comme un **outil de pilotage**, non comme une simple liste de tâches.

---

## Pré-requis — Verrous conceptuels

Ces pré-requis constituent des **interdits absolus** pour toute la durée de la V3.

- ⛔ **V2 gelée** : aucune modification des poids, des clés de pénalité ou des arbitrages V2.
- ⛔ **Aucune nouvelle contrainte HARD** introduite dans ce cycle.
- ⛔ **WorkMetrics exclusivement post‑résolution** (aucune utilisation décisionnelle).

Si l’un de ces verrous devait être levé, cela impliquerait **l’ouverture d’une V4**, pas une extension de la V3.

---

## Déroulé recommandé — Stabilisation V3

Les étapes ci-dessous sont **ordonnées**. En changer l’ordre revient à introduire un risque conceptuel.

---

### 1) Cadrer explicitement le périmètre V3

Objectif : poser un cadre clair avant toute implémentation.

- [ ] Valider formellement les axes retenus :
  - Séquences observées
  - Équité et répartition collective
  - Pénibilité relative (pré‑analyse RH)
- [ ] Lister explicitement les sujets **hors moteur** (non‑objectifs V3).

---

### 2) Spécifier WorkMetrics V3

Objectif : définir des métriques **descriptives**, stables et non interprétatives.

- [ ] Définir précisément les métriques de séquence (jours / nuits consécutifs observés).
- [ ] Documenter les cas limites :
  - horizon temporel,
  - présence ou absence d’historique,
  - ressources sans affectation.
- [ ] Mettre à jour la documentation de référence (`WORKMETRICS.md`).

---

### 3) Introduire les `PenaliteKey` V3

Objectif : identifier clairement **ce qui est pénalisé**, sans ambiguïté.

- [ ] Créer des clés dédiées V3 (aucune réutilisation ou détournement de clés V2).
- [ ] Associer une **unité explicite** à chaque clé.
- [ ] Garantir l’absence totale de logique métier ou de seuil dans les clés.

---

### 4) Étendre `ScoreWeights` (V3)

Objectif : introduire les pondérations V3 sans altérer la hiérarchie existante.

- [ ] Ajouter les poids V3 **sans modifier** les poids V2.
- [ ] Vérifier la hiérarchie relative :
  - V3 < poste virtuel
  - V3 > préférences personnelles futures
- [ ] Documenter les équivalences et ordres de grandeur retenus.

---

### 5) Implémenter les contraintes SOFT V3

Objectif : produire des pénalités exploitables pour l’arbitrage.

- [ ] Implémenter uniquement des contraintes **collectives**.
- [ ] Fonder les pénalités sur des **écarts relatifs**, jamais sur des seuils absolus.
- [ ] Vérifier qu’aucune contrainte ne devient implicitement bloquante.

---

### 6) Tests V3 — Dominance et non‑régression

Objectif : valider les arbitrages sans tester l’optimalité.

- [ ] Comparer des solutions **toutes valides** (pas de faisabilité).
- [ ] Vérifier les arbitrages équité ↔ pénibilité.
- [ ] Garantir la **non‑régression complète** des dominances V2.

---

## Critères de sortie V3

La V3 peut être considérée comme stabilisée lorsque :

- [ ] Le score est explicable **clé par clé** (clé, unité, poids, contribution).
- [ ] Les arbitrages V3 sont compréhensibles et justifiables.
- [ ] Les tests sont stables et reproductibles.
- [ ] La documentation de référence est synchronisée.

---

## Statut du document

- Document opérationnel et normatif pour la V3.
- Toute dérogation doit être **explicitement discutée et documentée**.
- Sert de référence pour le suivi de développement et les revues techniques.

