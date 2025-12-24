# Principes de fonctionnement d’OptaPlanner

OptaPlanner est un **moteur d’optimisation par contraintes**.
Il sert à **trouver la meilleure solution possible** à un problème complexe de planification, en respectant des règles métier parfois contradictoires.

👉 Il ne “calcule” pas une solution unique parfaite, il **cherche la meilleure solution acceptable** selon un score.

---

## 1. Le solveur

Le **solveur** est le **moteur de recherche** d’OptaPlanner.

### Rôle

* Explorer l’espace des solutions possibles
* Tester différentes combinaisons
* Améliorer progressivement la solution trouvée
* S’arrêter quand un critère est atteint (temps, score, stabilité…)

### Fonctionnement simplifié

1. Le solveur part d’une solution initiale (souvent naïve)
2. Il applique des modifications (petits changements)
3. Il évalue chaque solution via le score
4. Il conserve ou rejette les modifications
5. Il itère jusqu’à atteindre une condition d’arrêt

### À retenir

* Le solveur **n’est pas déterministe** par défaut
* Deux exécutions peuvent produire des solutions différentes
* Son comportement est largement configurable

---

## 2. Le scénario (solution de planification)

Le **scénario** représente **une solution complète à un instant donné**.

### Contenu typique

* L’ensemble des entités planifiées
* Les valeurs affectées à ces entités
* Le score global associé

### Exemple

> « Voici comment tous les agents sont affectés aux postes, sur tous les jours, avec ce niveau de satisfaction des contraintes. »

### Points clés

* Un scénario = **un état du monde**
* Le solveur manipule des scénarios
* Il compare les scénarios entre eux pour conserver le meilleur

---

## 3. Le score

Le **score** mesure la **qualité d’un scénario**.

### Principe

* Chaque contrainte ajoute ou retire des points
* Plus le score est élevé, meilleure est la solution
* Un score négatif est fréquent en début de résolution

### Types de scores

* **Hard** : contraintes non négociables (légal, impossible, interdit)
* **Soft** : contraintes de confort ou d’optimisation (préférences, équilibre, qualité)

Exemple :

```
Hard: -2
Soft: -15
```

Interprétation :

* ❌ 2 contraintes bloquantes violées → solution invalide
* ⚠️ Des règles de confort sont mal respectées

### À retenir

* Les contraintes **Hard doivent être satisfaites en priorité**
* Les contraintes **Soft servent à départager les solutions valides**

---

## 4. Les variables de décision

Les **variables de décision** sont ce que le solveur **a le droit de modifier**.

### Définition

Ce sont les **choix que le système peut prendre automatiquement**.

### Exemples

* Affecter un agent à un poste
* Choisir un créneau horaire
* Sélectionner une équipe
* Décider d’un jour de repos

### Rôle dans l’optimisation

* Le solveur teste différentes valeurs possibles
* Il modifie une variable à la fois ou par petits groupes
* Il observe l’impact sur le score

### À retenir

* Sans variables de décision → pas d’optimisation
* Trop de variables → explosion combinatoire
* Leur choix conditionne fortement les performances

---

## 5. Vision d’ensemble

```
Données métier
     ↓
Scénario initial
     ↓
Solveur
 ├─ modifie les variables de décision
 ├─ évalue via le score
 └─ compare les scénarios
     ↓
Meilleur scénario trouvé
```

---

## 6. Idée clé à retenir

> OptaPlanner n’automatise pas le métier.
> Il automatise la **recherche du meilleur compromis**, à partir de règles métier explicites.

Ce n’est ni une IA magique, ni un simple moteur de règles :

* Les règles viennent du métier
* Le solveur explore intelligemment
* Le score arbitre les conflits
