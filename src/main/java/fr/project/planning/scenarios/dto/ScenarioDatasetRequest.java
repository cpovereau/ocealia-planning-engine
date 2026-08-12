package fr.project.planning.scenarios.dto;

/**
 * ScenarioDatasetRequest — ce que tout scénario « dataset-driven » apporte au moteur.
 *
 * <p>SC-01 génère ses créneaux à partir de paramètres ; SC-02, SC-03 et SC-06 les reçoivent dans
 * un {@code dataSet}. Cette interface nomme le socle commun à ces derniers — le type de scénario,
 * le contexte de planification, et le jeu de données — de sorte que leur préparation soit écrite
 * <strong>une seule fois</strong>, dans {@code ScenarioDatasetPreparationService}.</p>
 *
 * <p>Elle ne dit rien des <strong>paramètres propres</strong> à chaque scénario : ceux-là restent
 * typés fortement sur le DTO de chacun, et c'est bien ainsi — le contrat d'entrée de SC-02 ne doit
 * pas laisser croire qu'il accepte les paramètres de SC-03.</p>
 */
public interface ScenarioDatasetRequest {

    String getScenarioType();

    PlanningContextDTO getPlanningContext();

    DataSetDTO getDataSet();
}
