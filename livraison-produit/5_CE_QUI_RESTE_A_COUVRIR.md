# 5 — Ce qu'il reste possible de couvrir

Le moteur est utilisable en l'état : les six scénarios existent, sont testés et sont documentés. Ce
document dit ce qui **n'est pas encore couvert**, pourquoi, et surtout **qui doit décider** pour que
ce le soit.

Un point de méthode d'abord, parce qu'il explique la forme de tout ce qui suit.

> **Le moteur n'invente jamais une règle que personne n'a posée.** Quand une information manque,
> il ne prend pas de valeur par défaut : il ne fait rien, et il le dit. Ce n'est pas de la
> timidité. Une valeur devinée est une décision prise par personne, appliquée à tout le monde, et
> que personne ne peut retrouver six mois plus tard.
>
> C'est pourquoi la plupart des sujets ci-dessous n'attendent pas du développement, mais **un
> arbitrage**.

---

## Vue d'ensemble

| Sujet | Ce qui manque | Qui décide |
|---|---|---|
| **Les contraintes personnelles** | préférences, lieux, compétences, annualisation | Produit / Production |
| **L'explicabilité du score** | savoir *pourquoi* le moteur a choisi ceci | Produit |
| **Le réglage de la pénibilité** | des valeurs calibrées sur du réel | Terrain, puis Produit |
| **Les champs annoncés sans effet** | les activer ou les retirer | Métier |
| **Le réglage fin des compteurs** | dire quel écart compte comme anormal | Produit |
| **La solidité du contrat d'entrée** | fermer les blocs encore tolérants | Technique |

---

## 1. Les contraintes personnelles — le sujet le plus important

**Ce qui manque.** Le moteur raisonne aujourd'hui sur des contrats, des seuils et des absences. Il
ne sait rien de ce qui distingue les personnes entre elles.

| Ce qui n'est pas pris en compte | Situation actuelle |
|---|---|
| Les **préférences** individuelles | pas transmises au moteur du tout |
| Les **lieux autorisés** | transmis, mais aucune règle ne les lit |
| Les **compétences** (activités compatibles) | transmises, appliquées aux salariés, mais tolérées au contrat |
| L'**annualisation** du temps de travail | transmise, aucun lecteur |

**Pourquoi c'est le sujet numéro un.** Le moteur peut produire un planning irréprochable sur le
papier — légal, équilibré, complet — et parfaitement inacceptable dans les faits, parce qu'il a
placé quelqu'un un samedi qu'il avait négocié, ou sur un site à quarante minutes de chez lui.

Cela pèse d'autant plus que les scénarios les plus ambitieux remanient beaucoup : SC-04 rouvre des
périodes entières. Plus le moteur bouge de choses, plus l'absence de préférences se fait sentir.

**Ce qu'il faut décider, et par qui.** Le moteur n'a pas de question technique en suspens : il sait
épingler, filtrer, pondérer. Ce qui manque est une décision de **produit** :

1. Quelles préférences existent, et sous quelle forme ? *« Je préfère le matin »* n'est pas
   *« je ne peux pas travailler le mercredi »*.
2. Quel poids ont-elles ? Une préférence est-elle un souhait que le moteur essaie de satisfaire, ou
   un engagement qu'il ne franchit pas ?
3. Qui les saisit, et qui les valide ?

**Un piège technique à connaître avant de se lancer.** L'appariement des compétences doit reposer
exclusivement sur des **identifiants**, jamais sur des libellés. Comme simple compteur, un libellé
mal orthographié produit une statistique fausse. Comme règle stricte, il rend tout créneau
inaffectable — et le moteur ne rendrait plus aucune solution. La différence entre les deux est un
mot mal écrit.

---

## 2. L'explicabilité — faire dire au moteur *pourquoi*

**Ce qui existe déjà.** Beaucoup, en réalité. Le moteur rend la mesure de chacun, les alertes, les
créneaux écartés avec leur raison, et pour SC-04 et SC-05 la charge de chacun avant et après. Un
gestionnaire dispose donc déjà de quoi justifier l'essentiel.

**Ce qui manque.** Le passage du *quoi* au *pourquoi*. Le moteur sait dire « ce planning coûte 760 »
et « ce salarié est à six heures au-dessus de son contrat ». Il ne sait pas encore dire :

> *« J'ai donné le samedi 23 à Marie plutôt qu'à Paul parce que Paul aurait dépassé son repos
> hebdomadaire, et que Marie était la seule autre personne compétente disponible ce jour-là. »*

Cette phrase-là est ce qui transforme une proposition en décision défendable devant la personne
concernée.

**Ce que ça demande.** Deux choses de nature différente. Techniquement, tracer pour chaque
affectation les possibilités écartées et le motif — le moteur connaît ces informations, il ne les
conserve pas. Et du côté produit, décider **jusqu'où** aller : l'explication complète d'un planning
d'un mois est illisible. Il faut choisir ce qu'on explique — les décisions contestées, les écarts
notables — et à qui.

C'est le chantier qui améliorerait le plus l'**adoption** de l'outil, sans rien changer à la qualité
des plannings produits.

---

## 3. Le réglage de la pénibilité

**Ce que c'est.** Toutes les heures ne se valent pas. Une nuit de dimanche pèse plus qu'un mardi
après-midi. Le moteur sait en tenir compte : on lui donne des coefficients, et il compare des
charges pondérées plutôt que des heures brutes.

**Ce qui manque.** Les coefficients eux-mêmes. Combien vaut une heure de nuit — 1,3 fois une heure
normale ? 1,5 ? 2 ? Personne ne le sait, et le moteur ne l'inventera pas.

**Comment on sort de là.** Pas par une discussion, mais par une **mesure**. Il faut passer de vrais
plannings dans le moteur, avec plusieurs jeux de coefficients, et regarder ce que chacun produit.
La bonne valeur est celle qui reproduit les arbitrages que l'encadrement fait déjà à la main.

**L'état actuel.** Le mécanisme est en place et testé. Il fonctionne avec les coefficients qu'on lui
donne, et signale par une alerte quand ceux-ci ne servent à rien. Ce qui manque est la matière : des
plannings réels.

> **Une règle du projet à connaître ici** : *on ne pondère pas une mesure dont l'échelle n'est pas
> calibrée.* C'est la raison pour laquelle SC-04 ne permet pas encore à l'appelant de régler les
> poids des règles. Laisser régler une échelle fausse produit des résultats faux avec l'apparence
> du sur-mesure. Cette possibilité s'ouvrira quand la calibration sera faite — pas avant.

---

## 4. Les champs annoncés qui ne produisent rien

**Le problème.** Quatre champs peuvent être transmis, sont bien reçus, et ne sont lus par aucune
règle. C'est plus gênant qu'un champ absent : celui qui le renseigne croit avoir dit quelque chose.

| Champ | Ce qu'on croit dire | Ce qui se passe |
|---|---|---|
| `capaciteCible` d'un poste virtuel | « ce renfort vaut deux personnes » | rien ne le limite : il absorbe autant de créneaux qu'il en faut |
| `groupeBesoinId` | « ces créneaux vont ensemble » | ils sont traités séparément |
| `blocJourId` | « cette journée forme un tout » | elle peut être éclatée entre plusieurs personnes |
| `ordreDansBloc` | « dans cet ordre » | rien n'est ordonné |

Le premier est le plus visible : une capacité déclarée à 2 n'empêche pas quarante affectations.

**Ce qu'il faut décider.** Pour chacun : l'activer, avec la règle qui va avec — ou le retirer du
contrat. Les trois derniers ne s'activent utilement qu'**ensemble** : un bloc journalier sans ordre
ni groupe n'exprime rien.

**Qui décide.** Le Métier. Ce point est en attente depuis le **25/08/2026**.

---

## 5. Le réglage fin des compteurs

Les compteurs mesurent. Ils ne jugent pas — sauf sur les points où on leur a dit à partir de quand
un écart devient anormal. Ces seuils existent, mais tous ne sont pas renseignés, et un seuil absent
signifie « ne juge pas ».

**Les leviers disponibles aujourd'hui :**

| Réglage | Ce qu'il permet | Sans lui |
|---|---|---|
| `equite.ecartTolerePourcent` | à partir de quel écart au contrat il y a inéquité | le moteur ne juge aucun écart |
| `surchargeMaxHeuresJour` / `Semaine` (SC-02) | de combien on accepte de charger un collègue | aucune limite de surcharge |
| Les seuils réglementaires individuels | repos, amplitude, jours consécutifs | la règle correspondante ne s'applique pas |
| Les coefficients de pénibilité | ce que vaut une heure selon quand elle est faite | toutes les heures se valent |

**Deux pièges, toujours les mêmes.** Un seuil **absent** désactive la règle. Un seuil **à zéro** est
appliqué à la lettre — ce n'est pas « pas de limite », c'est « limite à zéro ».

**Ce qui reste à faire.** Établir un jeu de valeurs de référence par type d'organisation, plutôt que
de laisser chaque appelant improviser les siennes. C'est un travail de produit, qui demande de
regarder ce que les gestionnaires font déjà.

---

## 6. La solidité du contrat d'entrée

Deux sujets purement techniques, sans arbitrage à demander, mais qui protègent contre des erreurs
silencieuses.

**Fermer les blocs encore tolérants.** Le moteur refuse en principe tout champ qu'il ne connaît
pas — c'est ainsi qu'une faute de frappe dans un nom de champ est détectée plutôt qu'ignorée. Six
blocs sont encore tolérants et acceptent l'inconnu sans rien dire. C'est visible côté appelant : le
fermer peut faire échouer des demandes qui passaient, à juste titre.

**Aligner la description du contrat d'entrée.** La description technique du format d'entrée est
moins stricte que ce que le moteur applique réellement. Elle décrit donc un format un peu plus
permissif que la réalité, ce qui peut faire croire à un développeur qu'une demande passera alors
qu'elle sera refusée. Le format de **sortie**, lui, est vérifié automatiquement à chaque livraison.

---

## 7. Dans quel ordre s'y prendre

Si la question est « par quoi commencer », voici l'ordre qui donne le plus de résultat pour le
moins d'effort.

**1. Faire tourner de vrais plannings.** Avant toute décision, il faut de la matière. Le moteur est
prêt à en recevoir, et c'est ce qui débloque la calibration de la pénibilité et les valeurs de
référence des compteurs. C'est aussi ce qui fera apparaître les vrais problèmes, qui ne sont
peut-être pas ceux de cette liste.

**2. Trancher les champs sans effet.** C'est rapide, cela ne dépend de rien d'autre, et cela retire
du contrat des promesses qui ne sont pas tenues.

**3. Ouvrir le chantier des contraintes personnelles.** C'est le plus long et le plus structurant.
Plus tôt il commence, mieux c'est — mais il commence par une décision de produit, pas par du
développement.

**4. L'explicabilité.** À faire après, parce qu'elle gagne à expliquer un moteur qui prend déjà en
compte les préférences. Expliquer une décision qui ignore les gens ne convainc personne.

---

## 8. Ce qui n'est pas prévu, et pourquoi

Pour éviter les malentendus, trois choses que le moteur ne fera pas.

**Il ne remplacera pas le logiciel de planning.** Il n'a pas d'écran, pas d'utilisateurs, pas de
base de données. Il calcule et il rend.

**Il ne gardera pas de mémoire.** C'est un choix, pas une limite : sans mémoire, la même demande
donne toujours la même réponse, et rien ne dérive avec le temps. Les compteurs historiques, quand
ils seront nécessaires, seront **transmis** avec la demande, pas stockés par le moteur.

**Il ne validera pas un planning.** Il dira qu'il n'y voit aucun défaut éliminatoire. La validation
est un acte humain, et le moteur est construit pour ne jamais donner l'illusion de l'avoir prise.
