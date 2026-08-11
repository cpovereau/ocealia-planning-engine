# 📌 Cadrage — Stabilisation du scénario SC-01

## Contexte

Le scénario SC-01 constitue le **socle historique** du moteur de planification.

Il a été conçu initialement pour :

* valider le branchement du solveur ;
* démontrer la capacité d’affectation simple ;
* sécuriser les premiers flux API.

Cependant :

* il repose sur un contrat d’entrée **simplifié et partiellement contourné** ;
* il ne s’appuie pas sur le dataset standard introduit en SC-03 ;
* certaines données sont **hardcodées ou reconstruites dans le builder** ;
* il n’exploite pas pleinement les DTO enrichis ni le référentiel métier.

---

## Problématique

SC-01 est aujourd’hui :

```text
fonctionnel → mais structurellement non conforme
```

Conséquences :

* divergence entre SC-01 et SC-03 ;
* risque de comportements incohérents ;
* difficulté de maintenance ;
* impossibilité de faire évoluer proprement le contrat.

---

## Objectif

Faire de SC-01 un scénario :

* **aligné sur le contrat d’entrée stabilisé (Phase 9)** ;
* basé sur le même pipeline que SC-03 ;
* sans logique spécifique cachée ;
* entièrement explicable et testable.

---

## Périmètre

### Inclus

* contrat d’entrée SC-01 ;
* mapping DTO → domaine ;
* builder SC-01 ;
* dépendances hardcodées ;
* compatibilité avec le solveur ;
* cohérence avec WorkMetrics et scoring.

### Exclu

* évolution métier des contraintes ;
* optimisation du solveur ;
* nouveaux scénarios.

---

## Cible

SC-01 doit devenir :

```text
SC-01 = SC-03 simplifié
```

Avec :

* même structure de dataset ;
* même mécanisme de mapping ;
* même logique de résolution ;
* différences uniquement dans :

  * les données d’entrée ;
  * les paramètres.

---

## Axes de travail

### 1. Contrat d’entrée

* identifier les écarts SC-01 vs Phase 9 ;
* supprimer les champs implicites ;
* aligner les DTO.

---

### 2. Dataset

* supprimer les reconstructions internes ;
* utiliser un dataset explicite ;
* aligner sur le dataset SC-03.

---

### 3. Builder

* supprimer les logiques spécifiques SC-01 ;
* utiliser les mêmes mappers que SC-03 ;
* garantir un mapping complet.

---

### 4. Référentiel

* supprimer le référentiel hardcodé ;
* utiliser `referentiels` comme SC-03.

---

### 5. Solveur

* vérifier que SC-01 utilise :

  * les mêmes contraintes ;
  * les mêmes règles ;
  * les mêmes WorkMetrics.

---

### 6. Tests

* garantir :

  * score stable ;
  * comportement identique (ou expliqué) ;
  * non-régression.

---

## Critères de réussite

SC-01 est stabilisé lorsque :

* aucun champ n’est implicite ;
* aucun comportement n’est hardcodé ;
* le dataset est explicite ;
* le builder est générique ;
* SC-01 et SC-03 partagent le même pipeline ;
* les tests sont verts.

---

## Risques

* casser le comportement historique ;
* introduire des régressions silencieuses ;
* complexifier inutilement SC-01.

---

## Stratégie

* audit complet (Claude) ;
* plan de correction incrémental ;
* validation à chaque étape.

---

## Résultat attendu

Un SC-01 :

* propre ;
* aligné ;
* maintenable ;
* utilisable comme base de test fiable.
