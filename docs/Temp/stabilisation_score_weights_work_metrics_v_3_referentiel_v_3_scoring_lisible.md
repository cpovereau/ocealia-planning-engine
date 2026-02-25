# Stabilisation ScoreWeights & exploitation WorkMetrics V3 — Référentiel V3

> **Objectif** : étendre le scoring au périmètre V3 (séquences, équité, pénibilité relative) tout en **préservant strictement le socle V2**.
>
> Ce document rend le scoring V3 **lisible, explicable et maîtrisable**, en s’appuyant sur **WorkMetrics V3**, **sans remettre en cause les arbitrages V2**.

La V3 a pour vocation de préparer l’analyse métier aval, sans jamais la réaliser.

Elle fournit notamment :
- des WorkMetrics enrichies (séquences observées, écarts relatifs),
- un score explicable par clé de pénalité,
- une lecture structurée des arbitrages collectifs.

Ces éléments constituent le contrat de sortie du moteur à destination de l’analyse RH.

---

## 1) Cadre et invariants (hérités de la V2)

Les invariants suivants sont **non négociables** et hérités de la V2 :

| Couche                | Rôle                                                | Interdit                                    |
|-----------------------|-----------------------------------------------------|---------------------------------------------|
| **Contraintes**       | Mesurer une situation (HARD ou SOFT)                | Porter des poids ou une logique de priorité |
| **SeuilsDeTolerance** | Définir quand une situation devient problématique   | Arbitrer ou scorer                          |
| **Penalites**         | Identifier la nature logique de la pénalité         | Pondérer ou décider                         |
| **ScoreWeights**      | Pondération technique stable                        | Règles métier, seuils, conditions           |
| **WorkMetrics**       | Constats post-résolution                            | Déclencher des décisions                    |

Principe directeur :
> **La V3 enrichit la lecture et l’arbitrage, elle ne redéfinit pas le jugement.**

---

## 2) Périmètre fonctionnel V3

La V3 introduit **trois axes complémentaires**, tous **SOFT**.

### 2.1 Axe A — Séquences observées

**Finalité** : explicabilité des règles combinatoires.

WorkMetrics V3 associées :
- `maxNuitsConsecutivesObservees`
- `maxJoursConsecutifsObservees`

Statut :
- descriptif uniquement
- aucune pénalité directe
- jamais HARD

---

### 2.2 Axe B — Équité et répartition collective

**Finalité** : arbitrer les déséquilibres persistants.

Exemples de pénalités V3 :
- surcharge relative par rapport à la moyenne
- concentration des nuits sur un sous-groupe

Nature :
- contraintes SOFT collectives
- basées sur des **écarts**, jamais sur des seuils absolus

---

### 2.3 Axe C — Pénibilité relative (pré-RH)

**Finalité** : rendre visible la pénibilité accumulée sans statuer.

Principe :
- le moteur signale
- l’analyse RH interprète

---

## 3) PenaliteKey — extensions V3

Les clés V2 restent **inchangées**.

Extensions V3 (prévisionnelles) :

| Domaine  | PenaliteKey                                   | Unité      |
|----------|-----------------------------------------------|------------|
| Équité   | `EQUITE_SOFT_ECART_CHARGE_MOYENNE`            | minute     |
| Équité   | `EQUITE_SOFT_ECART_NUITS_MOYENNE`             | occurrence |
| Séquence | `SEQUENCE_SOFT_JOURS_CONSECUTIFS_EXCESSIFS`   | jour       |
| Séquence | `SEQUENCE_SOFT_NUITS_CONSECUTIVES_EXCESSIVES` | nuit       |

📌 Ces clés **n’introduisent aucune règle HARD**.

---

## 4) ScoreWeights V3 — principes

- Les poids V2 sont **intouchables**.
- Les poids V3 sont :
  - inférieurs à `POSTE_VIRTUEL`
  - supérieurs aux préférences personnelles futures

Exemple d’ordre de grandeur (indicatif, non figé) :

| PenaliteKey    | Ordre de grandeur |
|----------------|-------------------|
| Équité         | 100 – 500         |
| Séquences SOFT | 500 – 1 000       |

---

## 5) Tests V3 (dominance relative)

Tests attendus :
- comparaison de deux solutions valides
- arbitrage équité vs pénibilité
- non-régression des dominances V2

Interdits :
- tests d’optimalité
- tests mélangeant métriques et décisions

---

## 6) Points volontairement écartés (V3)

- conformité réglementaire exhaustive
- calcul RH / paie
- décisions automatiques
- pénibilités HARD

La surcharge salarié et l’aide à la décision RH sont volontairement exclues du moteur de planification et ne sont pas réintégrées dans la V3.

Cette exclusion est délibérée et structurante.
Elle ne constitue ni un oubli, ni un report temporaire.

### Justification

Ces notions sont par nature :
- interprétatives,
- dépendantes du contexte organisationnel,
- fondées sur des seuils et des tolérances variables,
- susceptibles d’évoluer indépendamment du moteur.

À ce titre, elles ne constituent :
- ni des faits immuables,
- ni des décisions optimisables,
- ni des contraintes au sens OptaPlanner.

Les intégrer dans le moteur créerait une confusion entre :
- constat (WorkMetrics),
- arbitrage (scoring),
- interprétation métier (analyse RH).

### Positionnement clair des responsabilités

Le moteur :
- évalue les affectations,
- arbitre entre des solutions imparfaites,
- rend visibles les tensions et déséquilibres.

L’analyse métier aval :
- interprète les résultats,
- applique ses propres seuils et règles de lecture,
- construit les notions de surcharge salarié,
- produit l’aide à la décision RH.

Aucune rétroaction de cette analyse aval vers le moteur n’est prévue.

### Conséquence pour les évolutions futures

Toute tentative de réintégration de la surcharge salarié ou de l’aide à la décision RH dans le moteur devra être considérée comme un changement de périmètre.

Elle impliquera obligatoirement :
- l’ouverture d’un nouveau cycle majeur (V4 ou équivalent),
- une remise à plat explicite du modèle,
- une mise à jour formelle de la documentation normative.

---

## 7) Critères de sortie V3

- Socle V2 inchangé
- WorkMetrics V3 calculées post-résolution
- Pénalités V3 clairement identifiées
- Tests de dominance V3 stables
- Explicabilité améliorée

