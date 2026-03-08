┌──────────────────────────┐
│ ÉVALUATION / SCORE       │
│ (cerveau du moteur)      │
│                          │
│ - contraintes physiques  │
│ - contraintes légales    │
│ - contraintes métier     │
│ - contraintes service    │
│ - arbitrages             │
└─────────────┬────────────┘
              ▲
              │
┌─────────────┴────────────┐   ┌──────────────────────────┐
│ PLANNING SOLUTION        │   │        PARAMÈTRES        │
│ (monde du solveur)       │   │  (métier / stratégie)    │
│                          │   │                          │
│ - créneaux               │   │ - bornes physiques       │
│ - ressources             │   │ - règles métier          │
│ - indicateurs dérivés    │   │ - règles légales         │
│ - score                  │   │ - stratégie (payé/récup) │
└─────────────┬────────────┘   │ - priorités              │
              │                │ - pénalités              │
              │                └─────────────┬────────────┘
              │                              │
              ▼                              ▼
┌───────────────────┐           ┌──────────────────────┐
│   SALARIÉ RÉEL    │           │    POSTE VIRTUEL     │
│  (ressource)      │           │    (potentiel)       │
│                   │           │                      │
│ - identité        │           │ - capacité cible     │
│ - profil contrat  │           │ - type d’activité    │
│ - contraintes     │           │ - lieu / poste       │
│   dérivées        │           │ - bornes humaines    │
│ - sites autorisés │           │                      │
└─────────┬─────────┘           └───────────┬──────────┘
          │                                 │
          │                                 │
          └──────────────┬──────────────────┘
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
                └────────┬─────────┘
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
                │ - qualifiants    │
                │   (nuits, féries,│
                │    repos hebdo)  │
                └─────────┬────────┘
                          │
                          ▼
                ┌──────────────────┐
                │ INDICATEURS      │
                │ DÉRIVÉS          │
                │                  │
                │ - heures nuit    │
                │ - heures férié   │
                │ - heures repos   │
                │ hebdo            │
                │ - heures sup /   │
                │ compl.           │
                │ - dette repos    │
                │ compensateur     │
                │ - coût direct    │
                │ - coût indirect  │
                └─────────┬────────┘
                          │
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
                │   légales        |
                | - contraintes    |
                |   de service     │
                │ - pénalités      │
                │ - agrégations    │
                └─────────┬────────┘
                          │
                          ▼
                ┌──────────────────┐
                │   RÉSULTATS      │
                │                  │
                │ - planning       │
                │   affecté        │
                │ - créneaux       │
                │   à affecter     │
                │ - charge par     │
                │   salarié        |
                | - dettes         |
                |   générées       │
                │ - besoin RH      │
                │   (ETP / heures) │
                │ - alertes        │
                └──────────────────┘
