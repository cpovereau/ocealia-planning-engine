# 🧠 Lecture pédagogique (ce que chaque stéréotype apporte)

## 🟦 `<<PlanningSolution>>`
👉 **Le monde complet du solveur**

- contient tout ce que le solveur manipule
- porte le score
- porte les paramètres de scénario (*what-if*)

📌Transcription :  
> *« C’est l’état global du problème pendant la résolution. »*

---

## 🟩 `<<PlanningEntity>>` (Créneau)

👉 **Là où OptaPlanner agit**

- OptaPlanner ne touche **que** ces objets
- ici : *quel créneau est couvert par quoi*

📌 Message clé :  
> *« Si ce n’est pas une PlanningEntity, OptaPlanner ne décide rien dessus. »*

---

## 🟨 `<<PlanningVariable>>` (ressourceAffectee)

👉 **La décision elle-même**

- une seule variable de décision
- valeur possible :
  - salarié réel
  - poste virtuel
  - état “à affecter”

📌 Message clé :  
> *« Toute la complexité est dans les règles, pas dans la variable. »*

---

## 🟪 `<<ProblemFact>>` (Salarié réel, Poste virtuel)

👉 **Les faits immuables**

- OptaPlanner les lit
- OptaPlanner ne les modifie jamais

📌 Message clé :  
> *« Ce sont des données de référence, jamais des décisions. »*

---

## 🟥 `<<ConstraintProvider>>`

👉 **Le cerveau métier**

- toutes les règles sont centralisées ici
- aucune règle cachée dans les objets
- séparation claire :
  - données
  - décisions
  - évaluation

📌 Message clé :  
> *« Les règles jugent, elles ne modifient rien. »*

