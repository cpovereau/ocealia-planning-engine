# Moteur de planification — OptaPlanner

Moteur d'affectation de ressources à des créneaux de travail, appelé par un logiciel de planning
WinDev. Il reçoit un planning et une question, et rend le planning amélioré accompagné de tout ce
qu'il faut pour juger sa réponse : ce qui a bougé, qui y gagne, qui y perd, et ce qu'il n'a pas su
faire.

> **Il ne refuse pas : il rend visible l'impossible.** Face à une demande intenable, il ne répond
> pas « pas de solution » — il rend la meilleure solution imparfaite et nomme le problème. Ce
> principe explique la forme de presque tout le code.

**Java 21 · Spring Boot 3.2.5 · OptaPlanner 9.44 · Gradle**

---

## État

Les **six scénarios** annoncés au contrat existent, sont testés et sont documentés.

| | Question posée | Élément propre |
|---|---|---|
| SC-01 | organiser le travail d'une personne | partir de rien |
| SC-02 | untel est absent, qui prend son travail ? | réagir sans casser l'existant |
| SC-03 | réorganiser une portion désignée | traiter un sous-ensemble |
| SC-04 | ce planning peut-il être mieux réparti ? | juger une **période** |
| SC-05 | ces deux-là se partagent mal le travail | arbitrer entre deux personnes |
| SC-06 | qui peut couvrir ce besoin ? | classer sans décider |

Chacun apporte quelque chose qu'aucun autre n'apporte — ils ne se recouvrent pas.

**Deux canaux, un seul service.** Appel HTTP direct, et dépôt de fichier dans un répertoire
surveillé. Les deux entrent dans le même service au même point ; pour SC-02, SC-04 et SC-05, un
test compare les deux réponses entières à chaque exécution.

**844 tests**, dont les jeux d'essai de référence des six scénarios et la confrontation du JSON
produit au schéma publié.

---

## Démarrer

```bash
./gradlew test          # la suite complète — compter une dizaine de minutes
./gradlew bootJar       # construit build/libs/demo-0.0.1-SNAPSHOT.jar
java -jar build/libs/demo-0.0.1-SNAPSHOT.jar
```

Le moteur écoute sur le port **8082**. Vérification :

```
GET http://localhost:8082/scenarios/ping
```

Les endpoints suivent la forme `POST /scenarios/sc0N/solve`. Le répertoire d'écoute est actif par
défaut, sur `data/file-adapter/` : un `.json` déposé dans `inbox` ressort en `_RESULT.json` dans
`outbox`, ou dans `error` s'il n'a pas pu être lu.

> **Le temps de réflexion du solveur** se règle par `planning.solver.timeLimitSeconds`, en secondes
> entières, défaut `3`. C'est le réglage qui change le plus la qualité des réponses. Attention :
> `optaplanner.solver.termination.spent-limit` **n'a aucun effet** ici — le solveur est construit
> par `SolverConfigFactory`, pas par le starter.

---

## Organisation du dépôt

```
src/main/java/fr/project/planning/     le moteur — 233 classes
 ├─ domain/          modèle métier : créneaux, ressources, contrats, mesures
 ├─ constraints/     les règles, en Constraint Streams
 ├─ score/ scoring/  poids, stratégies, décomposition du score
 ├─ solution/        la solution de planification et ses faits de problème
 ├─ solver/          configuration du solveur
 ├─ time/            horizons, découpages temporels
 ├─ scenarios/       un service de préparation et d'exécution par scénario
 ├─ fileadapter/     le canal fichier : surveillance, validation, archivage
 └─ api/             les endpoints HTTP

src/test/java/fr/project/planning/     131 classes de test
src/test/resources/scenarios/          les jeux d'essai, un dossier par scénario

docs/                                  la documentation du moteur
livraison-produit/                     le dossier remis au service Produit
```

**Point d'attention.** Le point d'entrée Spring est
`com.example.planning.PlanningSolverApplication`, qui scanne `fr.project.planning`. Le reste du
package `com.example` est un vestige du POC initial : il n'est ni scanné par Spring, ni compris
dans la suite de tests (`exclude 'com/**'` dans `build.gradle`). Ne pas s'y référer.

---

## La documentation

`docs/00_INDEX_DOCUMENTATION.md` est l'index, et il est **complet** — un test le vérifie, en même
temps que la convention de nommage et l'absence de lien mort.

Les séries disent la nature du document :

| Série | Contenu |
|---|---|
| `10` – `40` | métier, architecture, modèle, règles de scoring, mesures |
| **`50`** | **le contrat publié**, celui que WinDev lit. Ne se modifie pas sans arbitrage |
| `60` | stratégie de test |
| `90` – `91` | suivi et journal de développement |
| `92` | cadrages et audits — le raisonnement derrière chaque décision |

Trois documents pour entrer :

- `docs/00_PRINCIPE_OPTAPLANNER.md` — comment le solveur cherche ;
- `docs/50_SCENARIO_CONTRACT.md` — ce que le moteur attend en entrée ;
- `docs/50_SCENARIO_RESPONSE_CONTRACT.md` — ce qu'il rend.

Le schéma de réponse `docs/50_ScenarioResponse.schema.json` est **confronté au JSON réellement
produit** par des tests dédiés : il ne peut pas décrire autre chose que la réalité.

---

## Le livrable Produit

`livraison-produit/` est un dossier autonome, destiné à être compressé et remis : le programme, un
fichier de réglages commenté, les scripts de démarrage Windows et Linux, six demandes réelles, et
six documents écrits **sans jargon**, pour être lus par des non-techniciens.

Le programme lui-même n'est pas suivi en gestion de version — 66 Mo. Pour reconstituer le dossier :

```powershell
.\preparer-la-livraison.ps1
```

Le script reconstruit le `.jar` et rafraîchit les jeux d'essai et les fiches techniques depuis les
sources, pour que le dossier ne dérive jamais du moteur qu'il décrit.

---

## Les principes qui gouvernent le code

Ils reviennent partout, et expliquent des choix qui, sans eux, paraissent excessifs.

**Le moteur ne refuse pas : il rend visible l'impossible.** Il ne refuse que ce qu'il ne sait pas
*lire* — une requête mal formée. Jamais ce qu'il ne sait pas planifier.

**Une borne absente n'est pas une borne à zéro.** Un seuil non déclaré désactive sa règle ; il ne
prend pas de valeur par défaut. Deviner une valeur, c'est l'appliquer sans que personne l'ait
décidée. Corollaire piégeux : un seuil déclaré à zéro est appliqué à la lettre.

**Un vide ne suppose jamais que la chose est possible.** Une compétence non déclarée n'est pas une
compétence acquise.

**L'existant ne se réécrit pas.** Le passé transmis reste tel qu'il fut, y compris ses trous : un
besoin qu'aucun salarié n'avait couvert ne se verra pas attribuer quelqu'un rétroactivement.

**Un seul calculateur, jamais deux.** Une mesure restituée passe par le calculateur de production,
jamais par une seconde implémentation — deux calculs finissent par diverger, et l'écart se lirait
comme un mouvement du planning.

**On ne pondère pas une mesure dont l'échelle n'est pas calibrée.** C'est pourquoi les poids des
règles ne sont pas réglables par l'appelant tant que les coefficients de pénibilité n'auront pas
été calibrés sur des plannings réels.

**Un champ au contrat que rien ne lit est pire qu'un champ absent** : celui qui le renseigne croit
l'avoir dit.

---

## Ce qui est ouvert

`docs/90_SUIVI_DEVELOPPEMENT_MOTEUR.md` tient le registre. En résumé :

| | Sujet | Arbitre |
|---|---|---|
| Rang 8 | champs annoncés qui ne produisent rien — les activer ou les retirer | Métier |
| Rang 10 | contraintes personnelles : préférences, lieux, compétences, annualisation | Produit / Production |
| Rang 12 | fermer les six blocs encore tolérants aux champs inconnus | technique |
| Rang 13 | aligner le schéma d'entrée sur ce que le moteur applique | technique |
| — | calibration des coefficients de pénibilité | attend des plannings réels |
| — | explicabilité pédagogique du score | Produit |

**Le rang 10 est le plus structurant** : le moteur peut produire un planning irréprochable sur le
papier et inacceptable dans les faits, parce qu'il ne sait rien de ce qui distingue les personnes.
Il pèse d'autant plus depuis SC-04, qui remanie des périodes entières.

---

## Contribuer

Trois habitudes, tenues depuis le début du projet.

**Tracer avant de corriger.** Un défaut se décrit dans le document idoine — ce qui tient, ce qui ne
tient pas, qui décide — avant qu'une ligne de code ne bouge.

**Vérifier par mutation.** Toute affirmation structurante se prouve en cassant délibérément la
ligne qui la porte et en constatant que les tests tombent. Un test qui passe dans les deux cas ne
prouve rien.

**Ne rien pousser sur un build rouge.** La suite complète, pas seulement les tests touchés.

Et une règle qui ne se négocie pas : **la série 50 est le contrat publié**. Rien n'y entre sans
arbitrage, et surtout pas un champ que le moteur ne lit pas encore.
