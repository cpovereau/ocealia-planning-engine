# 📐 Pseudo-modèle métier — Moteur de planification

## Objectif du système

Construire et évaluer des plannings de travail à partir de besoins (créneaux),
en utilisant des ressources réelles et potentielles,
sous contraintes physiques, métier, légales, de service et personnelles,
afin de produire un planning **explicable** et une **aide à la décision**.

---

## 1. Créneau (Besoin de travail)

### Rôle
Représente un **besoin de travail à couvrir**.  
C’est l’unité centrale du raisonnement.

### Nature
- Peut être **imposé** (besoin connu)

### Attributs conceptuels
- Date
- Heure de début
- Heure de fin
- Durée (stockée — source de vérité pour les agrégats)
- Lieu
- Activité
- Poste comptable
- Priorité métier

### Propriétés clés
- N’appartient à personne
- Existe indépendamment des ressources
- Peut rester **à affecter**
- N’intègre **aucune règle**

### Qualifiants calendaires / réglementaires
- Jour ouvré / non ouvré (isReposHebdo)
- Jour férié (isJourFerie)
- Nuit / jour (intersection plage nuit)

---

## 2. Ressource (abstraction)

### Rôle
Capacité de travail mobilisable pour couvrir des créneaux.

### Types de ressources
- **Salarié réel**
- **Poste virtuel**
- **État “à affecter”** (absence volontaire de ressource)

> Le moteur ne manipule jamais une ressource sans passer par cette abstraction.

---

## 3. Salarié réel (Ressource existante)

### Rôle
Représente une personne existante et contractualisée.

### Nature
- Fait d’entrée
- Immuable pendant la résolution
- Jamais créé ni modifié par le moteur

### Attributs conceptuels
- Identifiant
- Profil contractuel
- Statut (temps plein, temps partiel…)
- Sites autorisés
- Activités compatibles, avec un code activité si besoin qui permet de faire le lien 
    avec le code activté dans le logiciel de planning si nécessaire
- Postes comptables compatibles

### Contraintes associées (dérivées)
- Bornes physiques (ex. ≤ 13h / jour)
- Bornes métier
- Contraintes légales et contractuelles  
  (violables mais pénalisées)

### Propriétés clés
- Peut être sous-utilisé
- Peut être surchargé (dans des bornes strictes)
- N’exprime aucune préférence personnelle native

---

## 4. Poste virtuel (Ressource potentielle)

### Rôle
Représente une **capacité de travail manquante ou hypothétique**.

### Nature
- Fourni par l’utilisateur
- N’est jamais une personne

### Attributs conceptuels
- Capacité cible (heures / période, ETP)
- Bornes physiques humaines
- Activités autorisées
- Lieux autorisés
- Postes comptables compatibles

### Propriétés clés
- Toujours pénalisé par rapport à un salarié réel  
  *(sauf scénario explicite)*
- Agrégable (quantification du besoin)
- Sert d’aide à la décision RH

---

## 5. Affectation (Décision)

### Rôle
Lien décisionnel entre un créneau et une ressource.  
**C’est la variable de décision centrale du système.**

### Définition
Une affectation est une **proposition** de rattachement :
- d’un créneau
- à une ressource réelle ou virtuelle
- ou à l’état **“à affecter”**

### Propriétés clés
- Une affectation est candidate
- Elle peut être remplacée pendant la résolution
- Elle n’implique ni validité ni légalité
- Elle est évaluée par les règles

### États possibles
- `AFFECTÉ (SALARIÉ RÉEL)`
- `AFFECTÉ (POSTE VIRTUEL)`
- `À AFFECTER`

---

## 6. Contraintes (Évaluation)

Les contraintes **n’agissent jamais sur les données**,
elles **évaluent les affectations**.
Exemples de contraintes "coût/dette" :
- pénaliser le travail sur repos hebdomadaire
- pénaliser l’affectation à un poste virtuel
- pénaliser un créneau non couvert
- pénaliser la création de dette de repos compensateur *(à venir)*
- pénaliser le dépassement d’un contingent *(à venir)*
- arbitrer payé vs récup selon stratégie *(à venir)*

## 6 bis. Indicateurs dérivés (coûts et dettes)

Ces indicateurs ne sont pas des décisions : ils sont dérivés et servent au scoring.

- heuresNuit
- heuresJourFerie
- heuresSurReposHebdo
- minutesDimanche
- minutesIntersection
- heuresSupplementaires / heuresComplementaires *(à venir)*
- detteReposCompensateur, par origine : nuit / férié / repos hebdo / HS *(à venir)*
- coutDirect (si payé) vs coutIndirect (si repos/dette) *(à venir)*

## 6 ter. Catégories de contraintes

#### Contraintes physiques (dures)
- Impossibilités humaines
- Limites absolues (temps, enchaînements)

#### Contraintes métiers (dures ou pénalisées)
- Règles internes
- Organisation du travail
- Limites opérationnelles

#### Contraintes légales (pénalisées)
- Durées légales
- Repos
- Contrats de travail

#### Contraintes de service (pénalisées)
- Couverture minimale
- Continuité de service
- Équilibre collectif

#### Contraintes personnelles (contextuelles)
- Préférences
- Exceptions ponctuelles
- Jamais structurantes

---

## 7. Évaluation globale

Le moteur calcule :
- Des pénalités par affectation
- Des agrégats par salarié
- Des agrégats par poste virtuel
- Des alertes métier

> La meilleure solution est **la moins mauvaise**, pas la parfaite.

---

## 8. Résultats produits

### Résultats principaux
- Planning affecté
- Créneaux à affecter
- Charge par salarié
- Dépassements expliqués

### Résultats décisionnels
- Volume de poste virtuel requis *(à venir)*
- ETP manquant *(à venir)*
- Alertes légales et métier *(à venir)*
- Indicateurs de tension *(à venir)*

---

## 9. Principes structurants

- Le moteur ne décide jamais à la place du métier
- Il rend visibles les compromis
- Il explicite l’impossible
- Il aide à décider, pas à masquer

> Le système ne cherche pas un planning parfait,  
> mais un planning **explicable** et **exploitable**.
