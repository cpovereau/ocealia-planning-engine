# 3 — Le répertoire d'écoute

Le moteur peut être sollicité de deux façons. La plus simple à mettre en place, et celle retenue
pour démarrer, consiste à **déposer un fichier dans un dossier et à récupérer la réponse à côté**.
Aucun réseau, aucune authentification, aucun développement : une copie de fichier.

C'est ce qu'on appelle ici le *répertoire d'écoute*, ou le *mode fichier*.

---

## 1. Le principe en une image

```
  vous déposez ici                            vous lisez ici
        │                                            │
        ▼                                            ▼
   ┌─────────┐    ┌────────────┐    ┌──────────┐  ┌────────┐
   │  inbox  │───▶│ processing │───▶│  outbox  │  │ error  │
   └─────────┘    └────────────┘    └──────────┘  └────────┘
                        │                              ▲
                        │      demande incompréhensible│
                        └──────────────────────────────┘
                        │
                        ▼
                   ┌─────────┐
                   │ archive │   copie conservée, classée par mois et par client
                   └─────────┘
```

**Cinq dossiers**, créés automatiquement au premier démarrage à côté du moteur, dans
`data/file-adapter/` :

| Dossier | Ce qu'il contient |
|---|---|
| `inbox` | **votre boîte de dépôt.** C'est le seul dans lequel vous écrivez |
| `processing` | les demandes en cours de traitement. Un fichier n'y reste que quelques secondes |
| `outbox` | **les réponses.** C'est le seul que vous avez à lire en fonctionnement normal |
| `error` | les demandes que le moteur n'a pas su lire, avec l'explication |
| `archive` | une copie de tout, demandes et réponses, classée par année, mois et client |

---

## 2. Ce qui se passe, seconde par seconde

Le moteur regarde dans `inbox` **toutes les cinq secondes** (réglable). Quand il y trouve un
fichier `.json` :

1. il le **déplace** dans `processing` — le fichier disparaît donc de votre vue, c'est normal, et
   c'est ce qui garantit qu'il ne sera pas traité deux fois ;
2. il vérifie que la demande est lisible ;
3. il la traite — c'est ici que passe le temps de réflexion, une à soixante secondes selon le
   réglage ;
4. il écrit la réponse dans `outbox`, ou le rapport d'erreur dans `error` ;
5. il déplace la demande et copie la réponse dans `archive`.

Un fichier ne repasse jamais deux fois. Pour rejouer une demande, redéposez-la dans `inbox`.

> **N'écrivez jamais directement dans `inbox`.** Un fichier volumineux copié lentement peut être
> ramassé alors qu'il n'est écrit qu'à moitié. Écrivez-le à côté, sous n'importe quel nom, puis
> **renommez-le** en `.json` dans `inbox` : un renommage est instantané, une copie ne l'est pas.

---

## 3. Comment nommer les fichiers

**Le nom de la demande est libre**, à une condition : l'extension `.json`. Tout le reste est ignoré
par le moteur.

**Le nom de la réponse en découle** : le nom de la demande, suivi de `_RESULT`.

```
inbox/  planning-mai-agence-nord.json
   ↓
outbox/ planning-mai-agence-nord_RESULT.json
```

La même règle vaut pour un échec : le rapport d'erreur porte lui aussi le nom `_RESULT.json`, mais
il se trouve dans `error` et non dans `outbox`. **Le dossier où le fichier arrive vous dit tout de
son sort** — vous n'avez pas à ouvrir le fichier pour savoir si ça s'est bien passé.

Un conseil : donnez à vos fichiers des noms **uniques**, en y glissant une date et une heure. Deux
demandes du même nom produiraient deux réponses du même nom, et la seconde écraserait la première.

---

## 4. Ce que toute demande doit porter

Quel que soit le scénario, quatre informations sont obligatoires. Sans elles, le moteur ne lit
même pas la suite.

| Champ | À quoi il sert |
|---|---|
| `requestId` | votre numéro de demande. Il est repris tel quel dans la réponse : c'est lui qui vous permet de rapprocher les deux |
| `scenarioType` | la question posée : `SC-01` à `SC-06`. Voir [4_LES_SIX_SCENARIOS.md](4_LES_SIX_SCENARIOS.md) |
| `metadata.clientId` | de quel client il s'agit. Sert aussi au classement dans `archive` |
| `metadata.timestamp` | la date et l'heure de la demande |

Exemple d'en-tête, identique pour les six scénarios :

```json
{
  "requestId": "REQ-2026-05-18-001",
  "scenarioType": "SC-04",
  "metadata": {
    "clientId": "CLIENT-NORD",
    "timestamp": "2026-05-18T09:30:00"
  },
  "planningContext": { "...": "..." },
  "scenarioParameters": { "...": "..." },
  "dataSet": { "...": "..." }
}
```

Les six fichiers du dossier `exemples/` sont des demandes complètes et valides. Le plus simple est
de partir de l'une d'elles.

---

## 5. Quand la demande part dans `error`

Le rapport d'erreur est court et lisible. Il ressemble toujours à ceci :

```json
{
  "requestId": "REQ-2026-05-18-001",
  "status": "ERROR",
  "timestamp": "2026-05-18T09:30:04Z",
  "sourceFile": "planning-mai-agence-nord.json",
  "error": {
    "code": "MISSING_FIELD",
    "message": "Champ obligatoire absent : metadata.clientId"
  }
}
```

Le `code` est destiné à un programme, le `message` à un humain. Les cas les plus fréquents :

| Ce qui s'est passé | Ce qu'il faut corriger |
|---|---|
| Extension autre que `.json` | renommer le fichier |
| Fichier trop volumineux (plus de 10 Mo par défaut) | découper la demande, ou relever la limite dans la configuration |
| JSON mal formé — une virgule en trop, une accolade manquante | passer le fichier dans un validateur JSON avant de le déposer |
| Champ obligatoire absent | voir le §4 ci-dessus |
| Champ inconnu | le moteur **refuse un champ qu'il ne connaît pas**, plutôt que de l'ignorer en silence. Un champ ignoré, c'est une information que vous croyez avoir transmise |
| Demande incohérente | par exemple une date de fin antérieure à la date de début : le message le dit précisément |

> **Un fichier dans `error` n'est pas une panne du moteur.** C'est le moteur qui fait son travail :
> il refuse ce qu'il ne peut pas lire, plutôt que de deviner. Une vraie panne se voit dans le
> journal, pas dans `error`.

---

## 6. L'archive

Tout est conservé, demandes comme réponses, dans une arborescence prévisible :

```
archive/2026/05/CLIENT-NORD/planning-mai-agence-nord.json
archive/2026/05/CLIENT-NORD/planning-mai-agence-nord_RESULT.json
```

Année, mois, client. C'est ce qui permet, trois semaines plus tard, de retrouver **exactement** ce
qui avait été demandé le jour où quelqu'un conteste un planning — et de le rejouer à l'identique,
puisque le moteur donne toujours la même réponse à la même demande.

**Rien n'est purgé automatiquement.** Prévoyez un nettoyage périodique : sur un usage soutenu,
l'archive grossit vite.

---

## 7. L'autre voie : l'appel direct

Le mode fichier n'est pas la seule voie. Le moteur expose aussi une interface d'appel direct, par
le réseau, sur le port 8082 :

```
POST http://<serveur>:8082/scenarios/sc04/solve
```

Le contenu envoyé et la réponse reçue sont **les mêmes** qu'en mode fichier : les deux voies
entrent dans le même service, au même endroit, et aucune ne dispose d'une variante à elle. Seul le
transport change.

Pour trois scénarios — SC-02, SC-04 et SC-05 — un test automatique va plus loin et **compare les
deux réponses entières** à chaque livraison, caractère par caractère. Les six passent par le canal
fichier ; trois sont ainsi verrouillés.

| | Mode fichier | Appel direct |
|---|---|---|
| Mise en place | copier un fichier | développer un appel réseau |
| Réponse | dans les secondes qui suivent | immédiate, l'appel attend |
| Traçabilité | native, tout est archivé | à construire |
| Gros volumes | à l'aise | à surveiller — la connexion reste ouverte pendant tout le calcul |
| Interactif | non | oui |

**Le mode fichier convient au traitement de fond** : optimiser un mois, régénérer un planning
d'agence. **L'appel direct convient à l'interactif** : proposer un remplaçant pendant qu'un
utilisateur attend devant son écran.

Les deux peuvent fonctionner en même temps. La description technique complète de l'interface d'appel
direct est dans `reference/50_openapi_windev_moteur_v_1.yaml`, destinée aux développeurs.

---

## 8. Les réglages du mode fichier

Dans `config/application.properties` :

```properties
file-adapter.enabled=true                 # false coupe la surveillance des dossiers
file-adapter.polling-interval-ms=5000     # toutes les 5 secondes
file-adapter.max-input-file-size-bytes=10485760   # 10 Mo

file-adapter.paths.inbox=data/file-adapter/inbox
file-adapter.paths.processing=data/file-adapter/processing
file-adapter.paths.outbox=data/file-adapter/outbox
file-adapter.paths.error=data/file-adapter/error
file-adapter.paths.archive=data/file-adapter/archive
```

Les chemins acceptent des dossiers réseau, ce qui permet de déposer depuis un autre poste sans rien
développer.

**Deux mises en garde si vous passez par un partage réseau.** Le renommage n'y est pas toujours
instantané — le conseil du §2 devient une obligation. Et **ne faites jamais pointer deux moteurs
sur la même boîte de dépôt** : le mécanisme qui empêche de traiter deux fois le même fichier
protège un moteur contre lui-même, pas deux moteurs l'un contre l'autre.
