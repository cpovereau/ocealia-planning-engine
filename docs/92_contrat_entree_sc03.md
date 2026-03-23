# Contrat d'entrée SC-03 — Référence des champs

> **Date** : 2026-03-23
> **Périmètre** : SC-03 uniquement
> **Nature** : référence pour les intégrateurs — statut de chaque champ
>
> S'appuie sur : `92_audit_contrat_entree.md` (constat), phases 1–4 (instrumentation et signalements).
> Produit dans le cadre de la **Phase 5** du chantier de stabilisation du contrat d'entrée.

---

## Légende des statuts

| Statut | Signification | Garantie côté client |
|---|---|---|
| **SUPPORTÉ** | Champ lu, traité, influence la résolution | La valeur fournie est exploitée par le moteur |
| **TOLÉRÉ** | Champ reçu, mappé ou stocké, sans effet sur la résolution actuelle | Le moteur ne rejette pas la valeur — peut être activé dans une version future |
| **⚠️ DÉPRÉCIÉ** | Champ fonctionnel mais destiné à être supprimé | Ne pas en dépendre en production — migration vers le champ successeur |
| **IGNORÉ** | Ignoré avec signal explicite (log.warn) | La valeur est perdue — un signal WARN est émis si une valeur est fournie |

---

## 1. Contrat racine

| Champ | Obligatoire | Statut | Comportement |
|---|---|---|---|
| `scenarioType` | oui | SUPPORTÉ | Doit valoir `"SC-03"` — rejet si absent ou différent |
| `planningContext.horizon.dateDebut` | oui | SUPPORTÉ | Source de vérité pour la partition hors-horizon |
| `planningContext.horizon.dateFin` | oui | SUPPORTÉ | Source de vérité pour la partition hors-horizon |
| `planningContext.strategieScoring` | oui | SUPPORTÉ | Mappé sur `StrategieScoring` — exception si valeur inconnue |
| `scenarioParameters.prioriteCouverture` | non | TOLÉRÉ | Reçu — log.warn si valeur fournie — aucun effet sur la résolution |
| `scenarioParameters.periode` | non | TOLÉRÉ | Reçu — log.warn si valeur fournie — `planningContext.horizon` fait toujours foi |
| `dataSet` | oui | SUPPORTÉ | Doit être présent et non nul |
| `dataSet.creneaux` | oui | SUPPORTÉ | Doit être présent et non vide |

---

## 2. Créneaux — `dataSet.creneaux[]`

> En **SC-01**, `dataSet.creneaux` est désérialisé mais jamais lu. Les créneaux SC-01 sont générés depuis `scenarioParameters`.

| Champ | Obligatoire | Statut | Comportement |
|---|---|---|---|
| `id` | non | SUPPORTÉ | Clé des logs et diagnostics pré-résolution — `null` accepté sans erreur |
| `date` | oui (implicite) | SUPPORTÉ | Détermine l'exclusion pré-solveur hors-horizon — `null` traité comme dans l'horizon (cas limite documenté) |
| `heureDebut` | oui | SUPPORTÉ | Mappé sur `Creneau.heureDebut` — traversée minuit supportée si `heureDebut > heureFin` |
| `heureFin` | oui | SUPPORTÉ | Mappé sur `Creneau.heureFin` |
| `isJourFerie` | non | SUPPORTÉ | `null` → `false` par défaut — impact sur `JourFerieRefuse` (HARD) et pénibilités légales |
| `segmentNuit` | non | SUPPORTÉ | `null` → `JOUR` par défaut — impact sur contraintes nuit et WorkMetrics (`minutesNuit`) |
| `codeActiviteId` | non | SUPPORTÉ | Clé de résolution dans le référentiel — prioritaire sur `activite` |
| `activite` | non | ⚠️ DÉPRÉCIÉ | Fallback si `codeActiviteId` absent — log.warn émis — **utiliser `codeActiviteId` à la place** |
| `type` | non | TOLÉRÉ | Toujours écrasé par `TypeCreneau.IMPOSE` — log.warn si valeur fournie — la valeur envoyée est ignorée |
| `priorite` | non | IGNORÉ | Ignoré avec signal explicite (log.warn) — mismatch de type : DTO `Integer`, domaine `PrioriteCreneau` |
| `axesOrganisationnels` | non | IGNORÉ | Ignoré avec signal explicite (log.warn) — jamais lu par le mapper créneau |
| `lieu` | non | TOLÉRÉ | Mappé dans le domaine — aucune contrainte active ne l'exploite actuellement |
| `posteComptable` | non | TOLÉRÉ | Mappé dans le domaine — aucune contrainte active |
| `groupeBesoinId` | non | TOLÉRÉ | Mappé dans le domaine — aucune contrainte active |
| `blocJourId` | non | TOLÉRÉ | Mappé dans le domaine — aucune contrainte active |
| `ordreDansBloc` | non | TOLÉRÉ | Mappé dans le domaine — aucune contrainte active |
| `estSegmentDePause` | non | TOLÉRÉ | Mappé dans le domaine — aucune contrainte active |

---

## 3. Référentiel d'activités — `dataSet.referentiels.activites[]`

> En **SC-01**, `dataSet.referentiels` est désérialisé mais jamais lu. Un référentiel hardcodé `{"travail": …}` est utilisé à la place.

| Champ | Obligatoire | Statut | Comportement |
|---|---|---|---|
| `codeActiviteId` | oui | SUPPORTÉ | Clé de résolution — détermine l'exclusion pré-solveur des créneaux à activité inconnue |
| `compteDansCharge` | non | SUPPORTÉ | Mappé dans `ComptabiliteActivite` — `false` si absent |
| `genereDetteRepos` | non | SUPPORTÉ | Mappé — utilisé par `DetteReposSurReposHebdomadaire` |
| `estServiceCritique` | non | SUPPORTÉ | Mappé dans `ComptabiliteActivite` — `false` si absent |
| `libelle` | non | IGNORÉ | Ignoré avec signal explicite (log.warn) — absent de `ComptabiliteActivite` — champ de présentation uniquement |

> **Note** : deux champs absents du contrat JSON sont calculés avec des valeurs par défaut côté moteur — `prioritaireSurConfort = false` et `typeImpact = CHARGE_STANDARD`.

---

## 4. Ressources — `dataSet.ressources`

### 4.1 Salariés — `ressources.salaries[]`

| Champ | Obligatoire | Statut | Comportement |
|---|---|---|---|
| `id` | oui (implicite) | SUPPORTÉ | Identifiant dans toutes les contraintes solveur |
| `activitesCompatibles` | non | SUPPORTÉ | Utilisé pour le diagnostic pré-résolution `aucuneRessourceDansDataset` — `null` → accepte toutes les activités |
| `travailDeNuit` | non | SUPPORTÉ | Utilisé par la contrainte `NuitSalarieNonNuit` |
| `statut` | non | TOLÉRÉ | Mappé — aucune contrainte solveur ne l'exploite |
| `sitesAutorises` | non | TOLÉRÉ | Mappé — aucune contrainte active sur les sites |
| `contraintesReglementaires.joursConsecutifsMaximum` | non | TOLÉRÉ | Exploité par `JoursConsecutifsMax` — les autres champs de `contraintesReglementaires` sont mappés mais sans contrainte active |
| `heureDebutNuit` / `heureFinNuit` | non | TOLÉRÉ | Mappés — `RegulatoryParameters.neutre()` est utilisé globalement, les plages individuelles ne sont pas encore exploitées |
| `travailleJourFerie` | non | TOLÉRÉ | Mappé — aucune contrainte active |
| `postesComptablesCompatibles` | non | TOLÉRÉ | Mappé — aucune contrainte active |
| `axesOrganisationnels` | non | IGNORÉ | Ignoré — jamais passé au mapper domaine — sans signal actuellement |
| `contratTravail` | non | IGNORÉ | Ignoré — jamais passé au mapper domaine — sans signal actuellement |

### 4.2 Postes virtuels — `ressources.postesVirtuels[]`

| Champ | Obligatoire | Statut | Comportement |
|---|---|---|---|
| `id` | oui (implicite) | SUPPORTÉ | Présent dans les diagnostics (`posteVirtuelIds`) |
| `activitesAutorisees` | non | SUPPORTÉ | Utilisé pour le diagnostic `aucuneRessourceDansDataset` — `null` → accepte toutes les activités |
| `type` | non | TOLÉRÉ | Mappé sur `TypePosteVirtuel` — valeur inconnue → fallback silencieux sur `POTENTIEL` (sans signal) |
| `capaciteCible` | non | TOLÉRÉ | Mappé — aucune contrainte active |
| `lieuxAutorises` | non | TOLÉRÉ | Mappé — aucune contrainte active |
| `postesComptablesCompatibles` | non | TOLÉRÉ | Mappé — aucune contrainte active |

---

## 5. Indisponibilités — `dataSet.indisponibilites`

| Champ | Obligatoire | Statut | Comportement |
|---|---|---|---|
| `items[].ressourceId` | oui (implicite) | SUPPORTÉ | Utilisé par `IndisponibiliteSalarie` (HARD) |
| `items[].dateDebut` | oui | SUPPORTÉ | Borne de l'indisponibilité |
| `items[].dateFin` | oui | SUPPORTÉ | Borne de l'indisponibilité |
| `items[].motif` | non | TOLÉRÉ | Mappé dans le domaine — aucune contrainte ne l'utilise |

---

## 6. Comportements documentés

### Résolution de l'activité d'un créneau

1. Si `codeActiviteId` est présent et non blank → utilisé comme clé référentiel.
2. Si `codeActiviteId` est absent ou blank → fallback sur `activite` (log.warn émis) — ⚠️ `activite` est déprécié.
3. Si la clé résolue est absente du référentiel → créneau **exclu avant solveur** (log.warn "créneau exclu avant solveur").

### Valeurs par défaut silencieuses

| Champ | Valeur si absent / null |
|---|---|
| `segmentNuit` | `TypePlageHoraire.JOUR` |
| `isJourFerie` | `false` |
| Champs `Set` ressources (`sitesAutorises`, `activitesCompatibles`, etc.) | `Set.of()` — ressource non contrainte |

---

## 7. Ordre de partition pré-résolution

Les créneaux sont filtrés dans cet ordre avant d'atteindre le solveur :

```
dataSet.creneaux
    │
    ├─ [activité inconnue] ──────────────────► ignoredCreneaux.activiteInconnue  (exclu)
    │   (Phase 2 — log.warn "créneau exclu avant solveur")
    │
    ├─ [hors horizon] ───────────────────────► ignoredCreneaux.horsHorizon       (exclu)
    │   (Phase 3 — log.warn "créneau exclu avant solveur")
    │
    └─ [dans horizon, activité connue] ──────► SOLVEUR
           │
           └─ [sans ressource compatible] ──► ignoredCreneaux.aucuneRessourceDansDataset  (diagnostic — non exclu)
```

Un créneau **inconnu ET hors-horizon** est compté dans `activiteInconnue` uniquement.

Un créneau `date = null` n'est pas évalué par la partition hors-horizon — il passe au solveur si son activité est connue (cas limite, hors périmètre Phase 3).

---

## 8. Diagnostics pré-résolution (`ignoredCreneaux`)

| Compteur | Sémantique | Nature |
|---|---|---|
| `activiteInconnue` | Nombre de créneaux exclus avant solveur (activité absente du référentiel) | **Exclusion réelle** |
| `horsHorizon` | Nombre de créneaux exclus avant solveur (date hors `[dateDebut, dateFin]`) | **Exclusion réelle** |
| `aucuneRessourceDansDataset` | Nombre de créneaux transmis au solveur sans ressource déclarée compatible | **Diagnostic** — le créneau atteint le solveur (affecté à `RessourceNonAffectee`) |
