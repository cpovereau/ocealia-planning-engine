# Fiches techniques — pour les développeurs

Ce dossier n'est pas destiné à la lecture courante. Il contient les descriptions formelles du
format d'échange, celles qu'un développeur ouvre pour brancher un programme sur le moteur.

| Fichier | Ce que c'est |
|---|---|
| `50_openapi_windev_moteur_v_1.yaml` | La description complète de l'interface d'appel direct : toutes les adresses, tous les champs, tous les codes de retour. La plupart des outils de développement savent la lire et en générer du code |
| `50_ScenarioContract.schema.json` | Le format attendu **en entrée**, vérifiable automatiquement |
| `50_ScenarioResponse.schema.json` | Le format rendu **en sortie**, vérifiable automatiquement |

Les deux schémas servent à valider un fichier avant de l'envoyer, ou à contrôler une réponse reçue.
Le schéma de sortie est confronté au contenu réellement produit par le moteur à chaque livraison :
il ne peut donc pas décrire autre chose que la réalité.

> Le schéma d'entrée, lui, est aujourd'hui **un peu plus permissif** que ce que le moteur applique
> réellement. Une demande peut donc le satisfaire et être malgré tout refusée. L'alignement est
> identifié et prévu — voir [5_CE_QUI_RESTE_A_COUVRIR.md](../5_CE_QUI_RESTE_A_COUVRIR.md), §6.

Pour comprendre ce que veut dire chaque champ, ces fichiers ne suffisent pas : c'est
[4_LES_SIX_SCENARIOS.md](../4_LES_SIX_SCENARIOS.md) qu'il faut lire d'abord.
