# Audit du contrat d'entrée — État des lieux factuel

> **Date** : 2026-03-20
> **Périmètre** : SC-01 et SC-03
> **Nature** : constat uniquement — aucune modification de code

---

## 1. Tableau principal — contrat d'entrée champ par champ

### Légende

| Statut | Signification |
|--------|---------------|
| **stable** | champ actif, comportement documenté et prévisible |
| **tolérance** | champ accepté mais non finalisé (transition, défaut silencieux, partiel) |
| **transporté** | désérialisé et mappé, mais aucun effet sur le solveur ni le scoring |
| **ignoré** | désérialisé mais non mappé, aucun effet |
| **ambigu** | comportement différent selon le contexte (SC-01 vs SC-03, null vs vide…) |

> Colonne "utilisé solveur / scoring / diagnostics / WorkMetrics" : précise la chaîne exacte d'exploitation.

---

### 1.1 Bloc racine

| Champ JSON | Bloc | Oblig. | Documenté | Désérialisé | Validé | Mappé domaine | Utilisé builder | Utilisé solveur / scoring / diagnostics / WorkMetrics | Statut | Commentaire |
|---|---|---|---|---|---|---|---|---|---|---|
| `scenarioType` | racine | oui | oui | oui | oui (comparaison string dans service) | non (consommé dans service) | non | non | stable | La validation est un `if (!"SC-01".equals(...))` — rejet métier, pas Bean Validation |
| `planningContext` | racine | oui | oui | oui | oui (null check dans service) | oui → `PlanningContext` | oui (horizon) | oui (StrategieScoring, horizon) | stable | |
| `scenarioParameters` | racine | oui (SC-01) / optionnel (SC-03) | oui | oui | oui (null check pour SC-01) | non (consommé dans service) | oui (SC-01) | non | stable | Le type Java est différent : `Sc01ScenarioParametersDTO` vs `Sc03ScenarioParametersDTO` |
| `dataSet` | racine | oui | oui | oui | oui (null check dans service) | partiel | partiel | partiel | stable | Voir sous-blocs |

---

### 1.2 Bloc `planningContext`

| Champ JSON | Bloc | Oblig. | Documenté | Désérialisé | Validé | Mappé domaine | Utilisé builder | Utilisé solveur / scoring / diagnostics / WorkMetrics | Statut | Commentaire |
|---|---|---|---|---|---|---|---|---|---|---|
| `horizon.dateDebut` | planningContext | oui | oui | oui | oui (null check) | oui → `PlanningContext` | oui (SC-01 : borne de génération) | oui (filtrage créneaux hors horizon — SC-03) | stable | |
| `horizon.dateFin` | planningContext | oui | oui | oui | oui (null check) | oui → `PlanningContext` | oui | oui | stable | |
| `strategieScoring` | planningContext | oui | oui | oui | oui (`StrategieScoring.valueOf()` — exception si inconnu) | oui → `PlanningContext` | non | oui (scoring) | stable | |

---

### 1.3 Bloc `scenarioParameters` — SC-01

| Champ JSON | Bloc | Oblig. | Documenté | Désérialisé | Validé | Mappé domaine | Utilisé builder | Utilisé solveur / scoring / diagnostics / WorkMetrics | Statut | Commentaire |
|---|---|---|---|---|---|---|---|---|---|---|
| `resourceRef.kind` | SC-01 params | oui | oui | oui | oui (enum `ResourceKind`) | non | oui (dispatch salarié/poste) | non | stable | |
| `resourceRef.id` | SC-01 params | oui | oui | oui | oui (introuvable → exception) | non | oui (lookup dans dataSet) | non | stable | |
| `dailyAmplitudeHours` | SC-01 params | oui | oui | oui | oui (>0 dans builder) | non | oui (calcul durée créneaux) | non | stable | Converti en minutes dans `validateRequest()` |
| `shiftStart` | SC-01 params | oui | oui | oui | oui (null check dans builder) | non | oui (heure de début créneaux) | non | stable | |
| `shiftEndAlert` | SC-01 params | oui | oui | oui | oui (null check dans builder) | non | oui (génère alerte SHIFT_END_EXCEEDED) | diagnostics : oui (alerte) | stable | |
| `lunchBreak.start` | SC-01 params | non | oui | oui | non (null accepté) | non | oui (défaut 12:00 si null) | non | stable | Valeur par défaut silencieuse : 12h00 |
| `lunchBreak.end` | SC-01 params | non | oui | oui | non (null accepté) | non | oui (défaut 13:00 si null) | non | stable | Valeur par défaut silencieuse : 13h00 |
| `workedDays` | SC-01 params | oui | oui | oui | oui (non vide dans builder) | non | oui (jours travaillés, RH/RHD) | non | stable | |
| `holidayDates` | SC-01 params | non | oui | oui | non (null → `Set.of()` silencieux) | non | oui (jours non travaillés) | non | stable | |

---

### 1.4 Bloc `scenarioParameters` — SC-03

| Champ JSON | Bloc | Oblig. | Documenté | Désérialisé | Validé | Mappé domaine | Utilisé builder | Utilisé solveur / scoring / diagnostics / WorkMetrics | Statut | Commentaire |
|---|---|---|---|---|---|---|---|---|---|---|
| `prioriteCouverture` | SC-03 params | non | oui (commentaire DTO) | oui | non | non | non | non | tolérance | `ScenarioSc03PreparationService` ne lit pas `getScenarioParameters()` — champ totalement ignoré à l'exécution |
| `periode` | SC-03 params | non | oui (commentaire DTO) | oui | non | non | non | non | tolérance | Prévu comme surcharge de `planningContext.horizon`, mais non implémenté — le service utilise toujours `planningContext.horizon` |

---

### 1.5 Bloc `dataSet.ressources.salaries[]`

| Champ JSON | Bloc | Oblig. | Documenté | Désérialisé | Validé | Mappé domaine | Utilisé builder | Utilisé solveur / scoring / diagnostics / WorkMetrics | Statut | Commentaire |
|---|---|---|---|---|---|---|---|---|---|---|
| `id` | SalarieInputDTO | oui (implicite) | oui | oui | non (pas de @NotBlank) | oui → `SalarieReel.id` | oui (lookup SC-01) | oui (identifiant dans toutes les contraintes) | stable | |
| `statut` | SalarieInputDTO | non | oui | oui | non | oui → `SalarieReel.statut` | non | non | transporté | Stocké mais aucune contrainte ne l'utilise |
| `sitesAutorises` (alias `lieuxAutorises`) | SalarieInputDTO | non | oui | oui | non | oui → `SalarieReel.sitesAutorises` (Set.of() si null) | non | non | tolérance | Mappé mais aucune contrainte active ne filtre sur les sites. Alias `lieuxAutorises` pour rétrocompatibilité |
| `activitesCompatibles` (alias `activitesAutorisees`) | SalarieInputDTO | non | oui | oui | non | oui → `SalarieReel.activitesCompatibles` (Set.of() si null) | non | diagnostics : oui (pré-résolution SC-03 `aucuneRessourceDansDataset`) | tolérance | Utilisé en pré-résolution SC-03 pour vérifier la compatibilité. Aucune contrainte solveur active |
| `postesComptablesCompatibles` | SalarieInputDTO | non | oui | oui | non | oui → `SalarieReel.postesComptablesCompatibles` (Set.of() si null) | non | non | transporté | Mappé, aucune contrainte ne l'utilise |
| `axesOrganisationnels` | SalarieInputDTO | non | oui (Phase 3) | oui | non | **non** | non | non | ignoré | Désérialisé dans le DTO, jamais passé au mapper domaine |
| `contratTravail` | SalarieInputDTO | non | oui (Phase 3) | oui | non | **non** | non | non | ignoré | Désérialisé dans le DTO, jamais passé au mapper domaine |
| `contraintesReglementaires` | SalarieInputDTO | non | oui (Phase 3) | oui | non | oui → `ContraintesReglementairesSalarie` (Phase 3) | non | partiel (voir § 2) | tolérance | 8 champs mappés, mais usage solveur partiel : `joursConsecutifsMaximum` exploité par `JoursConsecutifsMax`. Les autres champs sont stockés mais pas encore exploités |
| `travailDeNuit` | SalarieInputDTO | non | oui (Phase 3) | oui | non | oui → `SalarieReel.travailDeNuit` | non | solveur : oui (`NuitSalarieNonNuit`, Phase 8) / WorkMetrics : oui (`nbCreneauxNuitNonNuit`) | stable (partiel) | Première contrainte exploitant ce champ activée en Phase 8 |
| `heureDebutNuit` | SalarieInputDTO | non | oui (Phase 3) | oui | non | oui → `SalarieReel.heureDebutNuit` | non | non | tolérance | Mappé, mais `RegulatoryParameters.neutre()` est utilisé globalement — plages individuelles non exploitées |
| `heureFinNuit` | SalarieInputDTO | non | oui (Phase 3) | oui | non | oui → `SalarieReel.heureFinNuit` | non | non | tolérance | Idem |
| `travailleJourFerie` | SalarieInputDTO | non | oui (Phase 3) | oui | non | oui → `SalarieReel.travailleJourFerie` | non | non | tolérance | Mappé, aucune contrainte solveur ne l'exploite actuellement |

---

### 1.6 Bloc `dataSet.ressources.postesVirtuels[]`

| Champ JSON | Bloc | Oblig. | Documenté | Désérialisé | Validé | Mappé domaine | Utilisé builder | Utilisé solveur / scoring / diagnostics / WorkMetrics | Statut | Commentaire |
|---|---|---|---|---|---|---|---|---|---|---|
| `id` | PosteVirtuelInputDTO | oui (implicite) | oui | oui | non | oui → `PosteVirtuel.id` | non (SC-01) | oui (diagnostics : collecte `posteVirtuelIds`) | stable | |
| `type` | PosteVirtuelInputDTO | non | oui | oui | non | oui → `TypePosteVirtuel` (défaut `POTENTIEL` si inconnu) | non | oui (`AffectationPosteVirtuel`) | tolérance | Type inconnu → fallback silencieux sur POTENTIEL sans log ni alerte |
| `capaciteCible` | PosteVirtuelInputDTO | non | oui | oui | non | oui → `PosteVirtuel.capaciteCible` | non | non | transporté | Mappé, aucune contrainte ne l'utilise actuellement |
| `activitesAutorisees` | PosteVirtuelInputDTO | non | oui | oui | non | oui → `PosteVirtuel.activitesAutorisees` (Set.of() si null) | non | diagnostics : oui (pré-résolution SC-03 `aucuneRessourceDansDataset`) | tolérance | Aucune contrainte solveur active sur les activités des postes virtuels |
| `lieuxAutorises` | PosteVirtuelInputDTO | non | oui | oui | non | oui → `PosteVirtuel.lieuxAutorises` (Set.of() si null) | non | non | transporté | |
| `postesComptablesCompatibles` | PosteVirtuelInputDTO | non | oui | oui | non | oui → `PosteVirtuel.postesComptablesCompatibles` (Set.of() si null) | non | non | transporté | |

---

### 1.7 Bloc `dataSet.creneaux[]` — SC-03 uniquement

> En **SC-01**, `dataSet.creneaux` est désérialisé mais **jamais lu** par le service. Le builder SC-01 génère ses propres créneaux depuis les paramètres.

| Champ JSON | Bloc | Oblig. | Documenté | Désérialisé | Validé | Mappé domaine | Utilisé builder | Utilisé solveur / scoring / diagnostics / WorkMetrics | Statut | Commentaire |
|---|---|---|---|---|---|---|---|---|---|---|
| `id` | CreneauInputDTO | non | oui | oui | non | oui → `Creneau.id` (@PlanningId) | non | solveur : oui | stable (SC-03) | Un id null est accepté sans erreur de désérialisation |
| `date` | CreneauInputDTO | oui (implicite) | oui | oui | non | oui → `Creneau.date` | non | solveur : oui (contraintes temporelles), diagnostics : oui (horsHorizon) | stable (SC-03) | Un créneau avec `date=null` est filtré par le comptage `horsHorizon` (prédicat `dto.getDate() != null`) |
| `heureDebut` | CreneauInputDTO | oui | oui | oui | non | oui → `Creneau.heureDebut` | non | solveur : oui (chevauchement, amplitude) | stable (SC-03) | Traversée minuit gérée si `heureDebut > heureFin` |
| `heureFin` | CreneauInputDTO | oui | oui | oui | non | oui → `Creneau.heureFin` | non | solveur : oui | stable (SC-03) | |
| `lieu` | CreneauInputDTO | non | oui | oui | non | oui → `Creneau.lieu` | non | non | transporté | Mappé, aucune contrainte ne l'utilise |
| `codeActiviteId` | CreneauInputDTO | non | oui | oui | non | oui → `Creneau.codeActiviteId` | non | diagnostics : oui (activiteInconnue, aucuneRessourceDansDataset) | tolérance | Clé stable attendue, mais optionnelle — fallback sur `activite` si absent ou blank |
| `activite` | CreneauInputDTO | non | oui | oui | non | oui → `Creneau.activite` | non | diagnostics : oui (fallback de `codeActiviteId`) | tolérance | Libellé utilisé comme clé en fallback — sémantique ambiguë (libellé ≠ clé) |
| `posteComptable` | CreneauInputDTO | non | oui | oui | non | oui → `Creneau.posteComptable` | non | non | transporté | |
| `priorite` | CreneauInputDTO | non | oui | oui | non | oui → `Creneau.priorite` (null transmis) | non | non | tolérance | `PrioriteCreneau` mappé à null — aucune contrainte ne l'exploite |
| `type` | CreneauInputDTO | non | oui | oui | non | **non** (remplacé par `TypeCreneau.IMPOSE` hardcodé) | non | non | ignoré | Le champ JSON est absorbé silencieusement, la valeur est écrasée par le mapper |
| `isJourFerie` | CreneauInputDTO | non | oui | oui | non | oui → `Creneau.jourFerie` (false si null) | non | solveur : oui (`JourFerieRefuse`, pénibilités légales) / WorkMetrics : oui | stable (SC-03) | `Boolean.TRUE.equals()` : null → false |
| `segmentNuit` | CreneauInputDTO | non | oui | oui | non | oui → `Creneau.typePlageHoraire` (JOUR si null) | non | solveur : oui (contraintes nuit) / WorkMetrics : oui (`minutesNuit`) | tolérance | `Boolean.TRUE.equals()` : null → JOUR par défaut |
| `axesOrganisationnels` | CreneauInputDTO | non | oui (Phase 5) | oui | non | **non** | non | non | ignoré | Désérialisé, jamais passé au mapper créneau |
| `groupeBesoinId` | CreneauInputDTO | non | oui (Phase 5) | oui | non | oui → `Creneau.groupeBesoinId` (setter) | non | non | tolérance | Mappé Phase 5, non exploité par le solveur actuellement |
| `blocJourId` | CreneauInputDTO | non | oui (Phase 5) | oui | non | oui → `Creneau.blocJourId` (setter) | non | non | tolérance | Idem |
| `ordreDansBloc` | CreneauInputDTO | non | oui (Phase 5) | oui | non | oui → `Creneau.ordreDansBloc` (setter) | non | non | tolérance | Idem |
| `estSegmentDePause` | CreneauInputDTO | non | oui (Phase 5) | oui | non | oui → `Creneau.estSegmentDePause` (setter) | non | non | tolérance | Idem |

---

### 1.8 Bloc `dataSet.referentiels` — SC-03 uniquement

> En **SC-01**, `dataSet.referentiels` est désérialisé mais **jamais lu** par `ScenarioSc01PreparationService`. Un référentiel hardcodé `{"travail": ...}` est utilisé à la place.

| Champ JSON | Bloc | Oblig. | Documenté | Désérialisé | Validé | Mappé domaine | Utilisé builder | Utilisé solveur / scoring / diagnostics / WorkMetrics | Statut | Commentaire |
|---|---|---|---|---|---|---|---|---|---|---|
| `activites[].codeActiviteId` | ReferentielActiviteDTO | oui (clé de map) | oui | oui | non | oui → clé de `ReferentielComptabiliteActivite` | non | diagnostics : oui (activiteInconnue) / solveur : indirect (lookup sur créneaux) | stable (SC-03) | |
| `activites[].libelle` | ReferentielActiviteDTO | non | non | oui | non | **non** | non | non | ignoré | `ComptabiliteActivite` n'a pas de champ libellé — désérialisé mais perdu au mapping |
| `activites[].compteDansCharge` | ReferentielActiviteDTO | non | oui | oui | non | oui → `ComptabiliteActivite.compteDansCharge` (false si null) | non | solveur : à confirmer (WorkMetrics charge) | stable (SC-03) | |
| `activites[].genereDetteRepos` | ReferentielActiviteDTO | non | oui | oui | non | oui → `ComptabiliteActivite.genereDetteRepos` (false si null) | non | solveur : oui (`DetteReposSurReposHebdomadaire`) | stable (SC-03) | |
| `activites[].estServiceCritique` | ReferentielActiviteDTO | non | oui | oui | non | oui → `ComptabiliteActivite.estServiceCritique` (false si null) | non | solveur : à confirmer | stable (SC-03) | |
| *(absent du contrat)* `prioritaireSurConfort` | ReferentielActiviteDTO | — | non | non | — | oui (défaut `false`) | non | non | tolérance | Champ absent du JSON WinDev Phase 7 — valeur par défaut appliquée silencieusement dans le mapper |
| *(absent du contrat)* `typeImpact` | ReferentielActiviteDTO | — | non | non | — | oui (défaut `CHARGE_STANDARD`) | non | non | tolérance | Idem |

---

### 1.9 Bloc `dataSet.indisponibilites`

| Champ JSON | Bloc | Oblig. | Documenté | Désérialisé | Validé | Mappé domaine | Utilisé builder | Utilisé solveur / scoring / diagnostics / WorkMetrics | Statut | Commentaire |
|---|---|---|---|---|---|---|---|---|---|---|
| `items[].ressourceId` | IndisponibiliteItemDTO | oui (implicite) | oui | oui | non | oui → `Indisponibilite.ressourceId` | non | solveur : oui (`IndisponibiliteSalarie` HARD) | stable | |
| `items[].dateDebut` | IndisponibiliteItemDTO | oui | oui | oui | non | oui → `Indisponibilite.dateDebut` | non | solveur : oui | stable | |
| `items[].dateFin` | IndisponibiliteItemDTO | oui | oui | oui | non | oui → `Indisponibilite.dateFin` | non | solveur : oui | stable | |
| `items[].motif` | IndisponibiliteItemDTO | non | oui | oui | non | oui → `Indisponibilite.motif` | non | non | transporté | Mappé dans le domaine, aucune contrainte ne l'utilise |

---

## 2. Inventaire des tolérances transitoires

### T-01 — `@JsonIgnoreProperties` absent sur `ScenarioRequestDTO` (SC-01)

- **Description** : `ScenarioRequestDTO` n'est pas annoté `@JsonIgnoreProperties(ignoreUnknown = true)`. Tout champ JSON inconnu à la racine de la requête SC-01 provoque une erreur de désérialisation.
- **Localisation** : `ScenarioRequestDTO` (sans annotation Jackson)
- **Type** : absence de tolérance (comportement strict non documenté)
- **Impact** : les clients SC-01 ne peuvent pas envoyer de champs supplémentaires, contrairement à SC-03

---

### T-02 — `@JsonIgnoreProperties` généralisé sur les sous-DTOs

- **Description** : tous les sous-DTOs d'entrée (`DataSetDTO`, `SalarieInputDTO`, `PosteVirtuelInputDTO`, `CreneauInputDTO`, `ReferentielsDTO`, `IndisponibilitesDTO`, `Sc03ScenarioRequestDTO`, `Sc03ScenarioParametersDTO`) absorbent silencieusement tout champ JSON inconnu.
- **Localisation** : annotations `@JsonIgnoreProperties(ignoreUnknown = true)` sur les classes listées
- **Type** : absorption silencieuse
- **Impact** : un champ mal orthographié ou renommé côté WinDev passe sans erreur. Aucun log d'avertissement.

---

### T-03 — Null → `Set.of()` silencieux sur les ensembles de ressources

- **Description** : si `sitesAutorises`, `activitesCompatibles`, `postesComptablesCompatibles` (salarié) ou `activitesAutorisees`, `lieuxAutorises`, `postesComptablesCompatibles` (poste virtuel) sont null dans le JSON, le mapper produit `Set.of()` sans alerte.
- **Localisation** : `ScenarioResourceMapper.toSalarieReel()` et `toPosteVirtuel()`
- **Type** : valeur par défaut silencieuse
- **Impact** : une ressource sans restrictions déclarées pourrait se voir appliquer un comportement permissif par défaut dès que les contraintes d'activité/lieu seront activées

---

### T-04 — Type `PosteVirtuel` inconnu → `POTENTIEL` silencieux

- **Description** : si le champ `type` d'un poste virtuel ne correspond à aucune valeur de l'enum `TypePosteVirtuel`, l'exception `IllegalArgumentException` est capturée silencieusement et le type est remplacé par `POTENTIEL`.
- **Localisation** : `ScenarioResourceMapper.toPosteVirtuel()` — `catch (IllegalArgumentException ignored)`
- **Type** : fallback silencieux sans log ni alerte
- **Impact** : un type mal orthographié passe inaperçu ; la ressource est traitée comme POTENTIEL

---

### T-05 — `segmentNuit` null → `JOUR` par défaut

- **Description** : `Boolean.TRUE.equals(dto.getSegmentNuit())` — si le champ est null ou absent, le créneau est qualifié `TypePlageHoraire.JOUR`.
- **Localisation** : `ScenarioCreneauMapper.toCreneau()`
- **Type** : valeur par défaut silencieuse
- **Impact** : un créneau sans indication nuit est traité comme un créneau de jour. Impact sur WorkMetrics (pas de `minutesNuit`) et sur les contraintes nuit.

---

### T-06 — `isJourFerie` null → `false` par défaut

- **Description** : même mécanique que T-05.
- **Localisation** : `ScenarioCreneauMapper.toCreneau()`
- **Type** : valeur par défaut silencieuse
- **Impact** : un créneau sans indication jours fériés n'est pas traité comme tel. Impact sur `JourFerieRefuse` (HARD) et pénibilités légales.

---

### T-07 — `type` (CreneauInputDTO) ignoré silencieusement

- **Description** : le champ `type` du créneau entrant est désérialisé mais remplacé systématiquement par `TypeCreneau.IMPOSE` dans le mapper.
- **Localisation** : `ScenarioCreneauMapper.toCreneau()` — `TypeCreneau.IMPOSE` hardcodé
- **Type** : champ ignoré sans alerte
- **Impact** : quelle que soit la valeur envoyée par WinDev, elle est écrasée

---

### T-08 — `priorite` (CreneauInputDTO) passé null

- **Description** : le champ `priorite` est désérialisé mais passé `null` dans le constructeur de `Creneau` (`null` pour `PrioriteCreneau`).
- **Localisation** : `ScenarioCreneauMapper.toCreneau()` — `null` explicite
- **Type** : champ non exploité
- **Impact** : nul pour l'instant, mais les contraintes futures dépendant de la priorité ne fonctionneront pas tant que ce mapping n'est pas finalisé

---

### T-09 — `axesOrganisationnels` (SalarieInputDTO) non mappé

- **Description** : le bloc `axesOrganisationnels` est désérialisé dans `SalarieInputDTO` mais jamais passé à `ScenarioResourceMapper.toSalarieReel()`.
- **Localisation** : `ScenarioResourceMapper.toSalarieReel()` — les setters `axesOrganisationnels` n'existent pas sur `SalarieReel`
- **Type** : transporté sans usage
- **Impact** : `directionIds`, `serviceIds`, `lieuIds`, `posteComptableIds` sont perdus après désérialisation

---

### T-10 — `contratTravail` (SalarieInputDTO) non mappé

- **Description** : `dateDebut`, `dateFin`, `dureeMoyenneHeuresParJour` désérialisés, jamais transmis au domaine.
- **Localisation** : `ScenarioResourceMapper.toSalarieReel()` — aucun setter `contratTravail` sur `SalarieReel`
- **Type** : transporté sans usage
- **Impact** : informations contractuelles non exploitées

---

### T-11 — `contraintesReglementaires` mappées mais partiellement exploitées

- **Description** : les 8 champs sont mappés vers `ContraintesReglementairesSalarie`. Seul `joursConsecutifsMaximum` est exploité par la contrainte `JoursConsecutifsMax`. Les 7 autres (`heuresMinimumParJour`, `heuresMaximumParJour`, `amplitudeJournaliereMaximum`, `reposQuotidienMinimum`, `heuresMinimumParSemaine`, `heuresMaximumParSemaine`, `nuitsMaximumParSemaine`) sont stockés sur le domaine mais sans contrainte solveur active.
- **Localisation** : `ScenarioResourceMapper.toContraintesReglementaires()`, contrainte `JoursConsecutifsMax`
- **Type** : mappé, partiellement exploité
- **Impact** : les limites individuelles par salarié (heures max/jour, amplitude, repos quotidien…) sont ignorées par le solveur

---

### T-12 — `travailleJourFerie` mappé mais non exploité par le solveur

- **Description** : stocké sur `SalarieReel.travailleJourFerie`, mais aucune contrainte ne teste ce booléen.
- **Localisation** : `ScenarioResourceMapper.toSalarieReel()`, `SalarieReel.setTravailleJourFerie()`
- **Type** : mappé sans usage solveur
- **Impact** : un salarié non autorisé à travailler les jours fériés peut néanmoins être affecté à un créneau férié

---

### T-13 — `heureDebutNuit` / `heureFinNuit` mappés mais non exploités

- **Description** : plages individuelles de nuit par salarié mappées, mais le solveur utilise `RegulatoryParameters.neutre()` pour les plages globales réglementaires.
- **Localisation** : `ScenarioResourceMapper.toSalarieReel()`, `ScenarioSc01PreparationService` et `ScenarioSc03PreparationService` — `RegulatoryParameters.neutre()` systématique
- **Type** : mappé sans usage solveur
- **Impact** : les plages de nuit individuelles ne sont pas prises en compte dans les calculs de pénibilité nuit (qui reposent sur les paramètres globaux)

---

### T-14 — `RegulatoryParameters.neutre()` systématique

- **Description** : les paramètres réglementaires globaux (plages de nuit, seuils légaux globaux) sont toujours instanciés avec `RegulatoryParameters.neutre()` en SC-01 et SC-03. Ils ne viennent pas du contrat d'entrée.
- **Localisation** : `ScenarioSc01PreparationService.prepare()` ligne 103, `ScenarioSc03PreparationService.prepare()` ligne 101
- **Type** : valeur hardcodée sans alimentation depuis le JSON
- **Impact** : les contraintes légales dépendant des paramètres réglementaires globaux opèrent avec des valeurs neutres

---

### T-15 — `dataSet.referentiels` ignoré par SC-01

- **Description** : `ScenarioSc01PreparationService` n'appelle pas `resourceMapper.toReferentiel()`. Un référentiel hardcodé `{"travail": ComptabiliteActivite(...)}` est utilisé à la place.
- **Localisation** : `ScenarioSc01PreparationService.prepare()` lignes 106-115
- **Type** : champ JSON ignoré, remplacement hardcodé
- **Impact** : même si WinDev envoie un bloc `referentiels` complet dans une requête SC-01, il est ignoré

---

### T-16 — `dataSet.creneaux` ignoré par SC-01

- **Description** : `ScenarioSc01PreparationService` n'utilise pas `dataSet.creneaux`. Le builder SC-01 génère ses propres créneaux.
- **Localisation** : `ScenarioSc01PreparationService.prepare()` — seul `buildResult.creneaux()` est utilisé
- **Type** : champ JSON ignoré
- **Impact** : même si WinDev envoie des créneaux dans `dataSet.creneaux` pour SC-01, ils sont ignorés

---

### T-17 — `libelle` (ReferentielActiviteDTO) perdu au mapping

- **Description** : le libellé de l'activité est désérialisé, mais `ComptabiliteActivite` n'a pas de champ `libelle`. La valeur est perdue.
- **Localisation** : `ScenarioResourceMapper.toReferentiel()` — `new ComptabiliteActivite(codeActiviteId, compteDansCharge, ...)` sans libellé
- **Type** : champ ignoré
- **Impact** : les diagnostics ne peuvent pas afficher le libellé de l'activité à partir du référentiel

---

### T-18 — `prioriteCouverture` et `periode` (Sc03ScenarioParametersDTO) totalement ignorés

- **Description** : `ScenarioSc03PreparationService.prepare()` ne lit pas `request.getScenarioParameters()`. Les deux champs de `Sc03ScenarioParametersDTO` n'ont donc aucun effet.
- **Localisation** : `ScenarioSc03PreparationService.prepare()` — seuls `getScenarioType()`, `getPlanningContext()`, `getDataSet()` sont utilisés
- **Type** : champs ignorés
- **Impact** : `prioriteCouverture` ne peut pas influencer le scoring, `periode` ne peut pas surcharger l'horizon

---

### T-19 — `motif` (IndisponibiliteItemDTO) transporté sans usage

- **Description** : `motif` est mappé vers `Indisponibilite.motif`. La contrainte `IndisponibiliteSalarie` ne l'utilise que pour l'identifiant (`ressourceId`) et les dates.
- **Localisation** : `ScenarioResourceMapper.toIndisponibilites()`, `IndisponibiliteSalarie.indisponibiliteSalarie()`
- **Type** : transporté sans usage
- **Impact** : nul (diagnostic)

---

### T-20 — SC-01 génère des créneaux sans `codeActiviteId`

- **Description** : le builder SC-01 utilise un constructeur de `Creneau` à 13 paramètres qui ne positionne pas `codeActiviteId`. Les créneaux SC-01 ont donc `codeActiviteId = null` et `activite = "travail"`.
- **Localisation** : `ScenarioDatasetBuilderSc01.createCreneau()` — constructeur sans `codeActiviteId`
- **Type** : incohérence structurelle SC-01 vs SC-03
- **Impact** : si du code futur cherche `codeActiviteId` sur un créneau SC-01, il obtiendra null

---

## 3. Analyse des activités inconnues — comportement réel observé

### 3.1 Pré-résolution (SC-03 uniquement)

Dans `ScenarioSc03PreparationService.prepare()`, après construction du référentiel depuis `dataSet.referentiels`, le service exécute le comptage suivant :

```
Pour chaque créneau de dataSet.creneaux :
    code = codeActiviteId si non null/blank, sinon activite
    si code est null, blank, ou absent du référentiel
        → incrémenter activiteInconnue
```

**Ce que ce code fait :**
- Il identifie les créneaux dont l'activité n'est pas reconnue dans le référentiel.
- Le résultat est stocké dans `IgnoredCreneauxDTO.activiteInconnue` (champ entier — compteur).

**Ce que ce code ne fait pas :**
- Il ne filtre pas les créneaux. Tous les créneaux sont convertis et envoyés au solveur, y compris ceux avec une activité inconnue.
- Il ne produit pas d'alerte nommée par créneau (pas d'identifiant de créneau dans le compteur).
- Il ne rejette pas la requête.

**Politique réelle** : tolérance silencieuse avec diagnostic compteur.

**Remarque sur le nom `ignoredCreneaux`** : le champ `diagnostics.ignoredCreneaux.activiteInconnue` porte un nom qui suggère que ces créneaux sont ignorés. Ce n'est pas le cas — ils sont transmis au solveur comme tous les autres.

---

### 3.2 Builder (SC-01)

La notion d'activité inconnue ne s'applique pas à SC-01. Le builder génère ses propres créneaux avec `activite = "travail"` et un référentiel hardcodé contenant uniquement la clé `"travail"`. Il n'y a donc pas de chemin où une activité inconnue pourrait apparaître côté SC-01.

---

### 3.3 Solveur

Aucune contrainte dans `ConstraintProviderImpl` ne rejette ni ne pénalise un créneau sur la base d'une activité inconnue.

Le comportement réel est décrit dans le commentaire du mapper référentiel (`ScenarioResourceMapper.toReferentiel()`) :

> « Si le bloc est null ou vide, retourne un référentiel neutre (map vide) : toutes les contraintes qui font `getByCode(...)` retournent null et sont ignorées. »

Ce comportement s'applique aussi à un créneau dont l'activité est absente du référentiel : le `getByCode()` retourne null, et les contraintes dépendant du référentiel (`DetteReposSurReposHebdomadaire`, calcul de charge WorkMetrics, etc.) court-circuitent silencieusement.

Concrètement :
- Un créneau d'activité inconnue est **affecté normalement** par le solveur.
- Les contraintes dépendant de `compteDansCharge`, `genereDetteRepos`, `estServiceCritique` ne s'appliquent pas à ce créneau.

---

### 3.4 Diagnostics

Le seul signal produit est `IgnoredCreneauxDTO.activiteInconnue` : un entier indiquant le nombre de créneaux dont l'activité n'a pas été trouvée dans le référentiel. Il est calculé une fois, avant la résolution (Phase 9), et intégré dans la réponse `diagnostics.ignoredCreneaux`.

**Test de non-régression existant** : `Phase9IntegrationTest.sc03_activiteInconnue_doitComptabiliserActiviteInconnue()` vérifie que `activiteInconnue = 1` pour un JSON de test spécifique (`sc03_activite_inconnue.json`).

---

## 4. Redondances du contrat d'entrée

| Champ 1 | Champ 2 | Nature de la redondance | Statut actuel |
|---|---|---|---|
| `CreneauInputDTO.codeActiviteId` | `CreneauInputDTO.activite` | Clé stable vs libellé — le système utilise `codeActiviteId` en priorité, `activite` en fallback (pré-résolution). En SC-01, `codeActiviteId = null` et `activite = "travail"`. | Ambigu : `activite` peut être utilisé comme clé en l'absence de `codeActiviteId` — sémantique libellé/clé mélangée |
| `SalarieInputDTO.sitesAutorises` | `SalarieInputDTO.axesOrganisationnels.lieuIds` | Ancienne source vs future source de vérité (documenté §D du plan de migration). | Redondance en cours : `sitesAutorises` est actif et mappé, `axesOrganisationnels.lieuIds` est ignoré |
| `SalarieInputDTO.sitesAutorises` | `SalarieInputDTO.sitesAutorises` (alias `lieuxAutorises`) | Deux noms JSON pour le même champ (`@JsonAlias`). | Rétrocompatibilité — les deux sont acceptés |
| `SalarieInputDTO.activitesCompatibles` | `SalarieInputDTO.activitesCompatibles` (alias `activitesAutorisees`) | Idem — deux noms JSON (`@JsonAlias`). | Rétrocompatibilité — les deux sont acceptés |
| `IgnoredCreneauxDTO.aucuneRessourceDansDataset` | alias `sansRessource` | Deux noms JSON pour le même champ de sortie (`@JsonAlias` en lecture, `@JsonProperty` en écriture). | Rétrocompatibilité sortie |
| `dataSet.creneaux` (SC-01) | Créneaux générés par `ScenarioDatasetBuilderSc01` | Même clé JSON `dataSet.creneaux`, comportement radicalement différent selon le scénario : ignorée en SC-01, source de vérité en SC-03. | Ambiguïté contractuelle — le même champ a deux sémantiques selon le scénario |
| `Sc03ScenarioParametersDTO.periode` | `PlanningContextDTO.horizon` | `periode` est prévu pour surcharger `horizon`, mais ce n'est pas implémenté. L'horizon de `planningContext` est toujours utilisé. | Redondance non implémentée — `periode` est ignoré |
| `dataSet.referentiels` (SC-01) | Référentiel hardcodé `{"travail":...}` dans `ScenarioSc01PreparationService` | Deux sources de référentiel selon le scénario — le champ JSON est ignoré en SC-01 au profit d'un référentiel hardcodé. | Incohérence SC-01 vs SC-03 |
| `SalarieInputDTO.contraintesReglementaires` (individuelle) | `RegulatoryParameters.neutre()` (globale) | Deux niveaux de paramètres réglementaires coexistent sans articulation claire dans le contrat. Les paramètres globaux ne viennent pas du JSON. | Ambigu — les contraintes individuelles sont partiellement exploitées (joursConsecutifsMaximum), les globaux sont toujours neutres |
| `PosteVirtuelInputDTO.activitesAutorisees` | `SalarieInputDTO.activitesCompatibles` | Même concept (quelles activités une ressource peut couvrir), nommage différent selon le type de ressource. | Cohérent dans l'intention, mais noms différents (`autorisees` vs `compatibles`) |

---

## 5. Synthèse rapide par axe

| Axe | Constat |
|---|---|
| **Robustesse à la désérialisation** | Très permissive sur les sous-DTOs (ignoreUnknown généralisé), stricte sur la racine SC-01 (pas d'annotation). Aucune Bean Validation (`@NotNull`, `@Valid`) visible. |
| **Validation métier** | Réalisée dans les services (`if (null)`, `valueOf()`, comparaisons string). Pas de couche Bean Validation dédiée. |
| **Couverture du mapping** | Environ 60 % des champs désérialisés sont réellement mappés vers le domaine. Le reste est soit ignoré, soit transporté sans usage. |
| **Couverture du solveur** | Un sous-ensemble encore plus réduit est effectivement exploité par les contraintes ou le scoring. Les champs Phase 3-5 sont mappés mais en attente d'exploitation. |
| **Cohérence SC-01 vs SC-03** | Plusieurs comportements divergents : `dataSet.creneaux` ignoré vs utilisé, `dataSet.referentiels` ignoré vs utilisé, `ignoredCreneaux` non applicable vs calculé. Le contrat JSON est partagé mais la sémantique est scenario-dépendante. |
| **Tolérances actives** | 20 tolérances identifiées (valeurs par défaut silencieuses, champs ignorés, fallbacks). Toutes documentées dans le plan de migration comme transitoires. |
