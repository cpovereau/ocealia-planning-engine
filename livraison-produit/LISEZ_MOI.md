# Le moteur de planification — dossier de mise en œuvre

Ce dossier contient **tout ce qu'il faut pour faire tourner le moteur** sur un autre poste ou sur
un serveur, et tout ce qu'il faut pour comprendre ce qu'il fait.

Il est écrit pour être lu sans connaissance technique préalable. Là où un terme est nécessaire, il
est expliqué à sa première apparition.

---

## Ce qu'il y a dans le carton

| | |
|---|---|
| `moteur/` | le programme lui-même, un seul fichier |
| `config/` | le fichier de réglages, commenté ligne à ligne |
| `demarrer-le-moteur.ps1` · `.sh` | démarrage, sous Windows et sous Linux |
| `exemples/` | six demandes réelles, une par scénario, prêtes à envoyer |
| `reference/` | les fiches techniques destinées aux développeurs |

Et cinq documents, à lire dans cet ordre — ou séparément, chacun se tient seul.

---

## Par où commencer

**Vous voulez juste le voir tourner.** → [1_INSTALLER_ET_DEMARRER.md](1_INSTALLER_ET_DEMARRER.md).
Comptez dix minutes, dont neuf d'installation de Java.

**Vous voulez comprendre ce que fait ce moteur.** →
[2_COMPRENDRE_LE_MOTEUR.md](2_COMPRENDRE_LE_MOTEUR.md). C'est le document central : il explique le
principe, et surtout ce que le moteur **ne fait pas**, qui est la source de la plupart des
malentendus.

**Vous voulez lui envoyer du travail.** → [3_LE_REPERTOIRE_D_ECOUTE.md](3_LE_REPERTOIRE_D_ECOUTE.md)
pour le mode fichier — déposer une demande dans un dossier et récupérer la réponse à côté — puis
[4_LES_SIX_SCENARIOS.md](4_LES_SIX_SCENARIOS.md) pour savoir quelle question poser et ce qui
revient.

**Vous voulez savoir jusqu'où on peut aller.** →
[5_CE_QUI_RESTE_A_COUVRIR.md](5_CE_QUI_RESTE_A_COUVRIR.md). Ce qui est possible, ce qui est en
attente d'une décision — et de qui.

---

## En une phrase

> Le moteur reçoit un planning et une question, et rend le même planning amélioré, accompagné de
> tout ce qu'il faut pour juger sa réponse : ce qui a bougé, qui y gagne, qui y perd, et ce qu'il
> n'a pas su faire.

Il ne dit jamais non. Quand la demande est impossible, il rend la meilleure solution imparfaite
**et il nomme le problème**. C'est le principe qui explique presque tout le reste.

---

## Ce que ce dossier n'est pas

Ce n'est pas le code source, ni la documentation de développement. Le moteur est livré sous forme
d'un programme fini ; le dépôt de développement, ses quelque huit cents tests automatiques et ses
documents de conception vivent ailleurs.

Ce n'est pas non plus un manuel utilisateur du logiciel de planning. Le moteur n'a pas d'écran :
il travaille pour le logiciel qui l'appelle, et jamais directement pour une personne.
