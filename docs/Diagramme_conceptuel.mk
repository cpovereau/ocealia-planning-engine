                          ┌──────────────────────────┐
                          │        PARAMÈTRES        │
                          │  (métier / stratégie)    │
                          │                          │
                          │ - bornes physiques       │
                          │ - règles métier          │
                          │ - règles légales         │
                          │ - priorités              │
                          │ - pénalités              │
                          └─────────────┬────────────┘
                                        │
                                        ▼
┌───────────────────┐        ┌──────────────────────┐
│   SALARIÉ RÉEL    │        │    POSTE VIRTUEL     │
│  (ressource)      │        │    (potentiel)       │
│                   │        │                      │
│ - identité        │        │ - capacité cible     │
│ - profil contrat  │        │ - type d’activité    │
│ - contraintes     │        │ - lieu / poste       │
│   dérivées        │        │ - bornes humaines    │
│ - sites autorisés │        │                      │
└─────────┬─────────┘        └───────────┬──────────┘
          │                              │
          │                              │
          └──────────────┬───────────────┘
                         │
                         ▼
                ┌──────────────────┐
                │   AFFECTATION    │   ◄─── variable de décision
                │ (proposition)    │
                │                  │
                │ - créneau        │
                │ - ressource      │
                │   (réel / virt.) │
                │ - état           │
                │   (affecté /     │
                │    à affecter)   │
                └─────────┬────────┘
                          │
                          ▼
                ┌──────────────────┐
                │     CRÉNEAU      │
                │   (besoin)       │
                │                  │
                │ - date           │
                │ - heure début    │
                │ - heure fin      │
                │ - lieu           │
                │ - activité       │
                │ - poste compt.   │
                │ - priorité       │
                │ - type           │
                │   (imposé / gen.)│
                └──────────────────┘


                         ▼
                ┌──────────────────┐
                │   ÉVALUATION     │
                │  DE LA SOLUTION  │
                │                  │
                │ - contraintes    │
                │   physiques      │
                │ - contraintes    │
                │   métier         │
                │ - contraintes    │
                │   légales        │
                │ - pénalités      │
                │ - agrégations    │
                └──────────────────┘


                         ▼
                ┌──────────────────┐
                │   RÉSULTATS      │
                │                  │
                │ - planning       │
                │   affecté        │
                │ - créneaux       │
                │   à affecter     │
                │ - charge par     │
                │   salarié        │
                │ - besoin RH      │
                │   (ETP / heures) │
                │ - alertes        │
                └──────────────────┘
