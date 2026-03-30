# SC-01 — Cadrage Produit

## 1. Objet du document

Ce document donne une lecture **fonctionnelle et exploitable** du scénario **SC-01** pour permettre au service Produit de :

* concevoir l’interface,
* préparer les paramètres,
* lancer des simulations,
* comprendre le comportement du moteur.

---

## 2. Finalité du scénario SC-01

SC-01 permet de simuler un planning **à partir de paramètres simples**, sans fournir de créneaux.

👉 Question métier :

"Si un salarié travaille selon ce rythme, que produit le moteur et quelles sont les tensions ?"

---

## 3. Positionnement par rapport à SC-03

* SC-01 : génération automatique des créneaux (mode paramétrique)
* SC-03 : consommation de créneaux existants (mode dataset)

👉 SC-01 est un scénario :

* simple
* rapide
* orienté simulation

---

## 4. Capacités côté Produit

Le scénario permet de :

* simuler l’arrivée d’un salarié,
* générer un planning type,
* visualiser la charge produite,
* analyser les impacts (jours fériés, nuit, etc.),
* détecter des incohérences de paramétrage.

---

## 5. Parcours utilisateur cible

### Étape 1 — Paramétrer le scénario

* définir période
* sélectionner une ressource
* définir les horaires de travail
* définir les jours travaillés

---

### Étape 2 — Lancer simulation

* bouton "Simuler"

---

### Étape 3 — Lire le résultat

* planning généré et affecté
* indicateurs
* diagnostics

---

## 6. Données d’entrée (vue Produit)

### Contexte

* date début
* date fin
* stratégie de scoring

---

### Paramètres SC-01

* ressource cible
* amplitude journalière
* heure de début
* jours travaillés
* pause déjeuner (optionnel)
* jours fériés (optionnel)

---

### Référentiel (optionnel)

* activités (ex : "travail")

👉 Si absent : fallback automatique côté moteur

---

## 7. Comportement spécifique SC-01

### Génération interne des créneaux

Le moteur génère automatiquement les créneaux à partir des paramètres.

👉 Important :

```text
dataSet.creneaux est ignoré dans SC-01
```

---

### Référentiel

* utilisé si fourni
* sinon fallback "travail"

---

### Ressource

* une seule ressource cible
* pas de logique multi-ressources

---

## 8. Résultats à afficher

### Indispensable

* planning généré
* nombre de créneaux
* indicateurs

---

### Important

* diagnostics
* score

---

### Avancé

* WorkMetrics

---

## 9. Règles de lecture

* Le planning est généré, pas fourni
* Les créneaux reflètent les paramètres
* Les diagnostics signalent les anomalies de paramétrage
* Les métriques décrivent le comportement

---

## 10. Synthèse Produit

* interface très simple
* peu de champs
* forte valeur pour simulation rapide

👉 Objectif : permettre une projection rapide sans complexité dataset
