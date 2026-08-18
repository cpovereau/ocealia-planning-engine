# 2 — Comprendre le moteur

Ce document explique ce que fait le moteur, et surtout **ce qu'il ne fait pas**. La plupart des
surprises viennent de là.

---

## 1. Le problème qu'il résout

Un planning, c'est du travail à faire et des gens pour le faire. La difficulté n'est pas de trouver
*une* répartition — n'importe qui en trouve une — mais d'en trouver une qui respecte tout à la fois
le droit du travail, les contrats, les absences, les compétences, et une certaine idée de l'équité.

Ces exigences se contredisent en permanence. Donner sa journée du samedi à quelqu'un peut être la
seule façon de couvrir le besoin, et en même temps la façon la plus sûre de le mettre au-delà de
son contrat.

**Le moteur ne tranche pas ces contradictions à votre place : il les rend visibles et propose le
meilleur compromis qu'il sache trouver.**

---

## 2. Trois mots, et le reste en découle

**Le créneau** est l'unité de travail : un moment, un lieu, une activité. « Le 18 mai, de 8 h à
12 h, aux soins, à l'hôpital nord ». C'est ce qu'il faut couvrir. On dit aussi *besoin*.

**La ressource** est ce qui peut couvrir un créneau. Trois formes :

- un **salarié réel**, avec son contrat, ses absences, ses seuils ;
- un **poste virtuel**, qui représente un renfort qu'on n'a pas encore recruté — l'intérim, le
  remplaçant à trouver ;
- **« non affecté »**, qui n'est pas une absence de réponse mais une réponse : *ce besoin n'est
  couvert par personne*.

**L'affectation** est le lien entre les deux. Le travail du moteur consiste, très exactement, à
choisir qui va sur quoi.

> Ce troisième cas — « non affecté » — est le premier signe du principe qui gouverne tout le reste.
> Là où beaucoup d'outils diraient « pas de solution », celui-ci répond « voici le planning, et ces
> quatre besoins-là ne sont couverts par personne ». Un trou nommé vaut mieux qu'un refus.

---

## 3. Le principe central : le moteur ne refuse pas

**Il rend visible l'impossible.**

Demandez-lui de couvrir quarante heures avec un seul salarié à vingt heures : il ne répondra pas
« impossible ». Il rendra un planning où vingt heures sont couvertes et vingt ne le sont pas, avec
la mention explicite de ce qui manque.

Ce choix a une conséquence pratique importante : **une réponse du moteur n'est jamais un feu vert.**
Elle est toujours accompagnée de son propre jugement — ce qui a été violé, ce qui manque, qui est
au-delà de son contrat. C'est ce jugement qu'il faut lire, pas seulement le planning.

Le moteur refuse dans un seul cas : quand la **demande** est incompréhensible — un champ obligatoire
absent, une date impossible. Il refuse de lire, jamais de planifier.

---

## 4. Interdits et regrets

Toutes les règles ne pèsent pas pareil. Le moteur en connaît deux familles.

**Les interdits.** Le repos quotidien de onze heures, l'amplitude maximale, l'affectation pendant
une absence déclarée. Le moteur ne les franchit pas pour améliorer un résultat — il préfère laisser
un besoin découvert.

**Les regrets.** L'écart au contrat, le déséquilibre entre deux personnes, un besoin non couvert, le
recours à un renfort extérieur. Ceux-là, il les accepte quand il le faut, mais chacun lui coûte, et
il cherche la solution qui lui en coûte le moins.

C'est tout le mécanisme du **score** : chaque défaut a un poids, le moteur additionne, et garde la
combinaison la moins coûteuse. Deux plannings également légaux ne se valent donc pas : celui qui
laisse quelqu'un à trente heures sous son contrat coûte plus cher que celui qui répartit.

> **Le score n'est pas une note.** Ce n'est pas un pourcentage de qualité, ce n'est pas comparable
> d'un planning à l'autre. C'est un outil de comparaison **interne** : le moteur s'en sert pour
> choisir entre deux versions du même problème, et il ne veut rien dire hors de là. Ne l'affichez
> pas à un utilisateur.

---

## 5. Ce que le moteur ne sait pas

**Il n'a aucune mémoire.** Chaque demande est un monde neuf. Il ne se souvient pas du planning de la
semaine dernière, ni de ce qu'il a répondu hier. Tout ce qu'il sait, c'est ce que la demande
contient.

Cela paraît une faiblesse ; c'en est surtout une garantie : la même demande donnera toujours la
même réponse, et rien ne se dégrade avec le temps. Mais cela veut dire aussi qu'un compteur d'heures
annuel, un solde de congés, un historique de pénibilité **doivent être transmis** si l'on veut
qu'ils comptent. Le moteur ne les devinera pas.

**Il ne connaît pas les gens.** Il connaît des contrats, des seuils, des absences. Il ne sait pas
qu'un tel préfère le matin, qu'un autre s'est arrangé avec sa collègue. Ces informations existent —
le logiciel de planning les a — mais elles ne lui sont pas encore transmises. C'est le sujet le plus
important de [5_CE_QUI_RESTE_A_COUVRIR.md](5_CE_QUI_RESTE_A_COUVRIR.md).

**Il ne décide pas.** Il propose. Un planning que le moteur juge acceptable reste un planning que
personne n'a validé — et il le dit sans ambiguïté : *acceptable* signifie « aucun défaut
éliminatoire », pas « tout le monde est d'accord ».

---

## 6. Deux choses différentes : la mesure et le jugement

Le moteur produit systématiquement, pour chaque personne, un jeu de **compteurs** : heures
travaillées, jours occupés, écart au contrat, week-ends, nuits, charge pondérée. C'est la
**mesure** — un constat, sans opinion.

Le **score**, lui, est un jugement : il traduit la mesure en coût.

La distinction compte, parce que la mesure est faite pour être montrée et le score ne l'est pas.
Quand vous voulez expliquer une décision à un salarié, c'est la mesure qui parle : *« vous étiez à
six heures au-dessus de votre contrat, votre collègue à huit heures en dessous »*. Aucun score
n'explique cela.

> Un défaut corrigé en août 2026 illustre bien le lien entre les deux. Le moteur ne plaçait jamais
> personne pendant ses congés — l'interdit tenait — mais il comptait la semaine de congé comme du
> temps disponible non travaillé, et voyait donc un déficit là où il n'y avait qu'une absence. Il
> avait alors tendance à *rattraper* les heures manquantes de part et d'autre du congé. Une manière
> détournée de l'annuler. La mesure et le jugement disent désormais la même chose.

---

## 7. Quelques règles qui ne se devinent pas

Ces quatre principes reviennent partout et expliquent des comportements qui, sans eux, paraissent
étranges.

### L'existant ne se réécrit pas

Quand vous transmettez un planning déjà fait, le moteur ne le refait pas de zéro. Il n'ouvre que ce
que vous l'autorisez à ouvrir. Le passé, en particulier, reste tel qu'il fut — **y compris ses
trous**. Un besoin du 12 mai que personne n'avait couvert ne se verra pas attribuer quelqu'un le
15 : cela reviendrait à inventer une histoire qui n'a pas eu lieu.

### Une borne absente n'est pas une borne à zéro

Si vous ne déclarez pas de durée maximale de travail, le moteur n'en applique aucune. Il ne
suppose ni valeur légale par défaut, ni infini prudent : **il n'applique rien**, et le dit. La
raison est simple : deviner une valeur, c'est l'appliquer sans que personne l'ait décidée.

Le corollaire est piégeux : déclarer un seuil **à zéro**, ce n'est pas « pas de limite », c'est
« limite à zéro », et le moteur l'applique à la lettre.

### Un vide ne suppose jamais que la chose est possible

Face à une information manquante, le moteur prend l'option prudente. Une compétence non déclarée
n'est pas une compétence acquise. Une disponibilité non renseignée n'est pas une disponibilité.

### Il ne crée jamais de travail

Le moteur redistribue ce que vous lui donnez. Il n'ajoute pas un créneau, n'allonge pas une
journée, n'invente pas un besoin. Si le travail transmis dépasse ce que les gens transmis peuvent
absorber, il rendra un planning avec des trous — et jamais un planning tenu par des journées de
quatorze heures que personne n'a demandées.

---

## 8. Comment il cherche

Le moteur n'applique pas une recette. Il **explore**.

Il part d'une répartition quelconque, en essaie une variante, la garde si elle coûte moins cher, et
recommence — des dizaines de milliers de fois. À la fin du temps imparti, il rend la meilleure
version rencontrée.

Trois conséquences pratiques :

**Le temps de calcul est un réglage, pas une fatalité.** Plus de temps, plus de tentatives,
meilleure réponse. Voir [1_INSTALLER_ET_DEMARRER.md](1_INSTALLER_ET_DEMARRER.md), §6.

**Il n'y a pas de « la » solution.** Sur un problème large, deux exécutions peuvent donner deux
plannings différents et également bons. Ce n'est pas un défaut ; c'est la nature du problème, qui
admet des millions de solutions équivalentes.

**Un gros problème n'échoue pas, il s'approxime.** Doubler la taille du planning ne provoque pas
d'erreur : cela réduit la part de l'espace que le moteur a eu le temps d'explorer. D'où
l'importance du temps accordé.

---

## 9. Ce qu'il faut retenir

1. Le moteur affecte des gens à des besoins. Il ne crée ni l'un ni l'autre.
2. Il ne refuse jamais de planifier : il rend visible ce qui coince.
3. Il ne connaît que ce qu'on lui transmet, et n'a aucune mémoire.
4. Il distingue les interdits, qu'il ne franchit pas, des regrets, qu'il minimise.
5. La mesure se montre, le score ne se montre pas.
6. « Acceptable » veut dire « sans défaut éliminatoire », pas « validé ».
