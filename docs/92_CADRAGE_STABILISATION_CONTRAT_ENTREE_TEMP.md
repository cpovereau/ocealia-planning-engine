# Cadrage — Stabilisation du contrat d’entrée cible

**Contexte**
Le moteur a été branché progressivement pendant la migration WinDev → moteur.
SC-01 a été réalisé en avance de phase comme scénario technique de démonstration et ne fait pas partie du périmètre de réalignement du contrat d’entrée.
Le présent chantier vise donc le contrat cible exploité autour de SC-03, ainsi que les tolérances transitoires encore présentes.

**Objectif**

Stabiliser progressivement le contrat d’entrée cible afin de :
- clarifier les comportements réellement supportés,
- réduire les tolérances silencieuses les plus risquées,
- fiabiliser la politique de traitement des activités inconnues,
- préparer un durcissement ultérieur du contrat sans casser l’existant utile.


**Périmètre**

Le chantier couvre :
- le contrat d’entrée cible SC-03,
- les DTO, mappers, préparation de scénario et diagnostics associés,
- les comportements silencieux ou ambigus identifiés par l’audit,
- la clarification de l’usage des clés techniques et des libellés.


**Hors périmètre**

Le chantier ne couvre pas :
- le réalignement de SC-01 sur le contrat d’entrée cible,
- une refonte générale du solveur,
- une suppression immédiate de toutes les tolérances,
- une réécriture du modèle métier,
- une refonte complète des sorties API.

Ordre de traitement retenu
- politique sur les activités inconnues
- tolérances silencieuses critiques côté SC-03
- clarification id / libellé / clé technique
- nettoyages complémentaires ensuite


**Principe de mise en œuvre**

Chaque évolution devra :
- rester petite et testable,
- préserver le comportement utile déjà validé,
- être documentée au fil de l’eau,
- éviter toute dérive de périmètre vers SC-01.