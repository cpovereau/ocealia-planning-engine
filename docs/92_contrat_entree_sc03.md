# Contrat d'entrée SC-03 — Référence des champs

> **Date** : 2026-03-26 (Phase 10C — nettoyage DTO final)
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

> En **SC-01**, `dataSet.creneaux` est désérialisé mais jamais lu. Les créneaux SC-01 sont générés depuis `scenarioParameters`. L'array `creneaux` est vide dans le JSON SC-01.
>
> **[Phase 10C]** Le bloc `creneaux` est désormais **strict** : `CreneauInputDTO` n'absorbe plus les champs inconnus silencieusement. Les champs `type`, `priorite` et `axesOrganisationnels` ont été supprimés du DTO. Tout champ non déclaré provoque une erreur de désérialisation avec un ObjectMapper strict.

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
| `lieu` | non | TOLÉRÉ | Mappé dans le domaine — aucune contrainte active ne l'exploite actuellement |
| `posteComptable` | non | TOLÉRÉ | Mappé dans le domaine — aucune contrainte active |
| `groupeBesoinId` | non | TOLÉRÉ | Mappé dans le domaine — aucune contrainte active |
| `blocJourId` | non | TOLÉRÉ | Mappé dans le domaine — aucune contrainte active |
| `ordreDansBloc` | non | TOLÉRÉ | Mappé dans le domaine — aucune contrainte active |
| `estSegmentDePause` | non | SUPPORTÉ | Activé Phase 8 — exclu des calculs d'amplitude journalière (`AmplitudeJournaliere`), de jours consécutifs (`JoursConsecutifsMax`) et du minimum horaire journalier (`HeuresMinimumParJour`) |

---

## 3. Référentiel d'activités — `dataSet.referentiels.activites[]`

> En **SC-01**, `dataSet.referentiels` est désérialisé mais jamais lu. Un référentiel hardcodé `{"travail": …}` est utilisé à la place.

> **[Phase 10B]** Le bloc `referentiels` est désormais **strict** : `ReferentielsDTO` n'absorbe plus les champs inconnus silencieusement. Seul le champ `activites` est admis à ce niveau. Tout autre champ (ex. `postesComptables`, `configurations`) provoque une erreur de désérialisation avec un ObjectMapper strict. Si un nouveau type de référentiel est nécessaire, il doit être déclaré dans `ReferentielsDTO` avant d'être utilisé.

| Champ | Obligatoire | Statut | Comportement |
|---|---|---|---|
| `codeActiviteId` | oui | SUPPORTÉ | Clé de résolution — détermine l'exclusion pré-solveur des créneaux à activité inconnue |
| `compteDansCharge` | non | SUPPORTÉ | Mappé dans `ComptabiliteActivite` — `false` si absent |
| `genereDetteRepos` | non | SUPPORTÉ | Mappé — utilisé par `DetteReposSurReposHebdomadaire` |
| `estServiceCritique` | non | TOLÉRÉ | Mappé dans `ComptabiliteActivite` — `false` si absent — **aucune contrainte active ne lit `isEstServiceCritique()`** — activation conditionnée à la définition d'une règle métier exploitant ce flag |
| `libelle` | non | IGNORÉ | Ignoré — absent de `ComptabiliteActivite` — champ de présentation uniquement. Signal : commentaire explicite `[Phase 4]` dans `ScenarioResourceMapper.toReferentiel()` — pas de log.warn (champ de présentation sans incidence opérationnelle) |

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
| `contraintesReglementaires.joursConsecutifsMaximum` | non | SUPPORTÉ | Exploité par `JoursConsecutifsMax` (SOFT) |
| `contraintesReglementaires.amplitudeJournaliereMaximum` | non | SUPPORTÉ | Exploité par `AmplitudeJournaliere` (SOFT) |
| `contraintesReglementaires.heuresMinimumParJour` | non | SUPPORTÉ | Activé Phase 8 — exploité par `HeuresMinimumParJour` (SOFT) — inactif si null |
| `contraintesReglementaires.heuresMaximumParJour` | non | TOLÉRÉ | Mappé — `DureeMaximaleLegaleParSalarie` utilise une constante globale (780 min/période), pas ce champ individuel — activation conditionnée à une refonte de cette contrainte |
| `contraintesReglementaires.nuitsMaximumParSemaine` | non | TOLÉRÉ | Mappé — `NuitsConsecutivesMax` lit `SeuilsDeTolerance.maxNuitsConsecutives` (global), pas ce champ individuel — activation conditionnée à un arbitrage entre seuil global et seuil individuel |
| `contraintesReglementaires.reposQuotidienMinimum` | non | TOLÉRÉ | Mappé — `ReposObligatoireApresNuits` lit un repos après nuits (global), sémantique différente du repos quotidien individuel — arbitrage requis |
| `contraintesReglementaires.heuresMinimumParSemaine` | non | TOLÉRÉ | Mappé — aucune contrainte active — activation conditionnée à la définition d'un pattern de groupement par semaine (ISO week, gestion des horizons partiels) |
| `contraintesReglementaires.heuresMaximumParSemaine` | non | TOLÉRÉ | Mappé — aucune contrainte active — même condition que `heuresMinimumParSemaine` |
| `heureDebutNuit` / `heureFinNuit` | non | TOLÉRÉ | Mappés — méthodes utilitaires préparées sur `SalarieReel` — activation conditionnée à l'arbitrage sur la relation avec `segmentNuit` (créneau) : deux sources de vérité possibles pour la qualification de nuit |
| `travailleJourFerie` | non | SUPPORTÉ | Exploité par `JourFerieRefuse` (HARD) |
| `postesComptablesCompatibles` | non | TOLÉRÉ | Mappé — aucune contrainte active |

### 4.2 Postes virtuels — `ressources.postesVirtuels[]`

| Champ | Obligatoire | Statut | Comportement |
|---|---|---|---|
| `id` | oui (implicite) | SUPPORTÉ | Présent dans les diagnostics (`posteVirtuelIds`) |
| `activitesAutorisees` | non | SUPPORTÉ | Utilisé pour le diagnostic `aucuneRessourceDansDataset` — `null` → accepte toutes les activités |
| `type` | non | TOLÉRÉ | Mappé sur `TypePosteVirtuel` — valeur inconnue → fallback sur `POTENTIEL` avec log.warn (Phase 10A) |
| `capaciteCible` | non | TOLÉRÉ | Mappé — aucune contrainte active |
| `lieuxAutorises` | non | TOLÉRÉ | Mappé — aucune contrainte active |
| `postesComptablesCompatibles` | non | TOLÉRÉ | Mappé — aucune contrainte active |

---

## 5. Indisponibilités — `dataSet.indisponibilites`

> **[Phase 10B]** Le bloc `indisponibilites` est désormais **strict** : `IndisponibilitesDTO` n'absorbe plus les champs inconnus silencieusement. Tout champ non déclaré provoque une erreur de désérialisation avec un ObjectMapper strict.

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
