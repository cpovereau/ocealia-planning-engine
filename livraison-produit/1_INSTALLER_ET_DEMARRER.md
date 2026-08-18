# 1 — Installer et démarrer

Le moteur est un programme autonome. Il n'a besoin ni de base de données, ni de serveur
d'application, ni d'installation au sens habituel : on copie un dossier, et on lance un fichier.

---

## 1. Le seul prérequis : Java 21

Le moteur est écrit en Java et a besoin de Java pour tourner, comme un document Word a besoin de
Word. Il faut la **version 21 ou plus récente**.

**Vérifier ce qui est déjà installé.** Ouvrez une invite de commande et tapez :

```
java -version
```

Si la réponse commence par `openjdk version "21` ou plus haut, c'est bon. Si la commande est
inconnue, ou si le numéro est inférieur à 21, il faut installer Java.

**Installer.** Téléchargez « Eclipse Temurin JRE 21 » (gratuit, sans licence à acquérir) depuis
adoptium.net, et laissez toutes les options par défaut. Rouvrez ensuite l'invite de commande — elle
ne voit pas les nouveautés tant qu'elle n'est pas relancée — et refaites le test.

> La version **JRE** suffit. La version **JDK**, plus lourde, sert à développer et n'apporte rien
> ici.

---

## 2. Copier le dossier

Copiez ce dossier complet là où vous voulez : un disque local, un partage réseau, un serveur. Deux
seules précautions.

**Un chemin sans caractères exotiques.** `D:\moteur-planification` va très bien. Un chemin contenant
des accents ou des caractères spéciaux peut poser problème selon la configuration du poste.

**Un droit d'écriture.** Le moteur crée des sous-dossiers de travail à côté de lui et y écrit en
permanence. Un dossier en lecture seule le bloquerait dès le premier fichier reçu.

---

## 3. Démarrer

**Sous Windows**, double-cliquez sur `demarrer-le-moteur.ps1`, ou en invite de commande :

```
powershell -ExecutionPolicy Bypass -File demarrer-le-moteur.ps1
```

**Sous Linux**, dans un terminal :

```
./demarrer-le-moteur.sh
```

Dans les deux cas, la commande directe fonctionne aussi :

```
java -jar moteur/moteur-planification.jar
```

Le démarrage prend quelques secondes. Le moteur affiche ensuite des lignes de journal et **reste
ouvert** : c'est normal, il attend du travail. Fermer la fenêtre l'arrête.

---

## 4. Vérifier qu'il est bien vivant

Ouvrez un navigateur sur cette adresse :

```
http://localhost:8082/scenarios/ping
```

Une réponse s'affiche : le moteur écoute. Si le navigateur dit qu'il ne peut pas joindre le site,
c'est que le moteur n'est pas démarré, ou qu'il tourne sur un autre poste — remplacez alors
`localhost` par le nom ou l'adresse de ce poste.

---

## 5. Le premier essai, en trois minutes

Le dossier `exemples/` contient six demandes réelles, une par scénario. Ce sont les jeux d'essai
utilisés pour valider le moteur : ils fonctionnent, tels quels.

1. Démarrez le moteur.
2. Copiez `exemples/sc01_exemple.json` dans le dossier `data/file-adapter/inbox` (il est créé au
   premier démarrage, à côté du moteur).
3. Attendez cinq secondes.
4. Regardez dans `data/file-adapter/outbox` : un fichier `sc01_exemple_RESULT.json` vous y attend.

C'est tout le fonctionnement du mode fichier, décrit en détail dans
[3_LE_REPERTOIRE_D_ECOUTE.md](3_LE_REPERTOIRE_D_ECOUTE.md).

---

## 6. Les réglages

Tous les réglages tiennent dans un fichier texte, `config/application.properties`, commenté
ligne à ligne. Modifiez-le avec n'importe quel éditeur de texte, puis **redémarrez le moteur** :
les réglages sont lus une fois, au démarrage.

> Le dossier doit s'appeler exactement **`config`** et se trouver à côté du dossier `moteur`.
> C'est là que le moteur va le chercher — renommé ou déplacé, il ne serait pas lu, et le moteur
> repartirait sur ses valeurs d'usine sans le signaler.

Les quatre que vous aurez sans doute à toucher :

| Réglage | À quoi il sert |
|---|---|
| `server.port` | le numéro de porte. `8082` par défaut ; à changer si un autre programme l'occupe déjà |
| `planning.solver.timeLimitSeconds` | le **temps de réflexion** accordé au moteur pour chaque demande, en secondes. Voir ci-dessous |
| `file-adapter.paths.inbox` (et les quatre autres) | où le moteur va chercher le travail et déposer les réponses |
| `file-adapter.enabled` | `false` coupe la surveillance des dossiers et ne laisse que l'appel direct |

### Le temps de réflexion mérite qu'on s'y arrête

C'est le réglage qui change le plus la qualité des réponses, et le seul qui n'a pas de bonne valeur
universelle.

Le moteur ne calcule pas *la* solution : il en essaie un très grand nombre et **garde la meilleure
trouvée dans le temps qu'on lui laisse**. Lui laisser plus de temps, c'est lui laisser en essayer
davantage.

La valeur s'exprime en **secondes**, sous forme d'un nombre entier.

| Valeur | Ce que ça donne |
|---|---|
| `1` | réponse quasi immédiate, correcte sur un petit planning, approximative sur un gros |
| `3` | ce que le moteur applique si la ligne est absente |
| `10` | l'ordre de grandeur raisonnable pour un usage courant |
| `60` | pour un planning large — une optimisation sur trois mois, par exemple |

Il n'y a **aucun risque** à donner beaucoup de temps : le moteur rend toujours une réponse valide,
seulement moins bonne s'il en a eu peu. En revanche il ne rend rien avant la fin du délai, même
s'il a trouvé son optimum en deux secondes — le temps demandé est un temps consommé.

> La valeur livrée est `10`. Montez-la pour un planning large, descendez-la pour enchaîner des
> essais. Vous pouvez vérifier qu'elle est bien prise en compte : le journal affiche la durée de
> chaque traitement, qui doit être légèrement supérieure au temps demandé.

---

## 7. Faire tourner le moteur en permanence sur un serveur

Lancé à la main, le moteur s'arrête quand la session se ferme. Pour qu'il survive à une
déconnexion ou à un redémarrage, il faut le déclarer comme service.

**Sous Windows**, un outil comme NSSM enregistre n'importe quel programme en service Windows en une
commande. **Sous Linux**, un fichier `systemd` d'une dizaine de lignes suffit.

Dans les deux cas, trois points à ne pas oublier :

- le **dossier de travail** doit être celui qui contient le `.jar`, sinon les chemins relatifs des
  dossiers d'échange ne pointeront pas où vous croyez ;
- le compte qui exécute le service doit avoir le **droit d'écrire** dans les dossiers d'échange ;
- prévoyez la **rotation des journaux** : le moteur écrit une ligne par fichier traité, ce qui
  finit par occuper de la place.

---

## 8. Quand ça ne marche pas

| Ce que vous voyez | Ce que c'est | Quoi faire |
|---|---|---|
| `java n'est pas reconnu` | Java absent, ou invite de commande non relancée après l'installation | §1 |
| `Port 8082 already in use` | un autre programme occupe la porte — souvent une instance du moteur déjà lancée | changer `server.port`, ou arrêter l'autre |
| `UnsupportedClassVersionError` | Java présent mais trop ancien | installer la version 21 |
| Le moteur démarre, mais rien ne se passe quand je dépose un fichier | mauvais dossier, ou `file-adapter.enabled=false` | vérifier les chemins ; les dossiers utilisés sont affichés au démarrage |
| Le fichier disparaît de `inbox` et rien n'arrive dans `outbox` | la demande a été refusée | regardez dans `error/` : le rapport dit exactement ce qui a bloqué |

**Le journal dit tout.** Chaque fichier traité produit une ligne : ce qui a été lu, combien de temps
le traitement a pris, où la réponse a été écrite. C'est le premier endroit où regarder, avant même
de se poser des questions.
