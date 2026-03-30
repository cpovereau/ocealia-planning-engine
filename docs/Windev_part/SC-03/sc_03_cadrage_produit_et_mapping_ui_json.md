# SC-03 — Cadrage Produit

## 1. Objet du document

Ce document donne une lecture **fonctionnelle et exploitable** du scénario **SC-03** pour permettre au service Produit de :
- concevoir l’interface,
- préparer les données,
- lancer des simulations,
- interpréter les résultats.

---

## 2. Finalité du scénario SC-03

SC-03 permet d’envoyer au moteur un **jeu de données complet déjà structuré** (créneaux, ressources, référentiel, contexte) afin d’obtenir une **proposition d’affectation**.

👉 Question métier :
> « Avec mes ressources et mes besoins, que propose le moteur et où sont les tensions ? »

---

## 3. Capacités côté Produit

Le scénario permet de :
- simuler un planning,
- détecter des créneaux non couverts,
- identifier un manque de ressources,
- analyser les impacts (charge, nuit, etc.),
- comprendre les raisons de non-affectation.

---

## 4. Parcours utilisateur cible

### Étape 1 — Préparer les données
- définir période
- saisir ressources
- saisir créneaux
- fournir référentiel

### Étape 2 — Lancer simulation
- bouton « Simuler »

### Étape 3 — Lire le résultat
- planning affecté
- créneaux non couverts
- diagnostics
- indicateurs

---

## 5. Données d’entrée (vue Produit)

### Contexte
- date début
- date fin
- stratégie de scoring

### Ressources
- salariés
- postes virtuels

### Créneaux
- date
- horaires
- activité

### Référentiel
- code activité
- règles métier associées

### Indisponibilités
- ressource
- période

---

## 6. Résultats à afficher

### Indispensable
- statut
- nombre de créneaux
- non couverts
- planning

### Important
- diagnostics
- score

### Avancé
- workMetrics

---

## 7. Règles de lecture

- Créneau non couvert = besoin non satisfait
- Poste virtuel = manque de ressource
- Diagnostics = explication
- WorkMetrics = constats uniquement

---

# Mapping UI ↔ JSON

## Contexte

| UI | JSON | Type | Obligatoire |
|----|------|------|------------|
| Type scénario | scenarioType | caché | Oui |
| Date début | planningContext.horizon.dateDebut | date | Oui |
| Date fin | planningContext.horizon.dateFin | date | Oui |
| Stratégie | planningContext.strategieScoring | liste | Oui |

---

## Ressources

| UI | JSON | Type | Obligatoire |
|----|------|------|------------|
| Id salarié | salaries[].id | texte | Oui |
| Activités | salaries[].activitesCompatibles | multi | Non |
| Travail nuit | salaries[].travailDeNuit | liste | Non |
| Jour férié | salaries[].travailleJourFerie | bool | Non |

---

## Créneaux

| UI | JSON | Type | Obligatoire |
|----|------|------|------------|
| Id | creneaux[].id | texte | Non |
| Date | creneaux[].date | date | Oui |
| Début | creneaux[].heureDebut | heure | Oui |
| Fin | creneaux[].heureFin | heure | Oui |
| Activité | creneaux[].codeActiviteId | liste | Oui |

---

## Référentiel

| UI | JSON | Type | Obligatoire |
|----|------|------|------------|
| Code activité | activites[].codeActiviteId | texte | Oui |
| Compte charge | activites[].compteDansCharge | bool | Non |

---

## Indisponibilités

| UI | JSON | Type | Obligatoire |
|----|------|------|------------|
| Ressource | indisponibilites[].ressourceId | select | Oui |
| Début | indisponibilites[].dateDebut | date | Oui |
| Fin | indisponibilites[].dateFin | date | Oui |

---

## Exemple JSON

```json
{
  "scenarioType": "SC-03",
  "planningContext": {
    "horizon": {
      "dateDebut": "2026-04-01",
      "dateFin": "2026-04-07"
    },
    "strategieScoring": "EXPLOITATION"
  }
}
```

---

## Synthèse Produit

- Interface simple mais structurée
- Mettre en avant les données utiles
- Ne pas exposer toute la complexité dès la V1

👉 Objectif : permettre rapidement des simulations fiables sans surcharger l’utilisateur

