package fr.project.planning.scenarios.service;

import fr.project.planning.scenarios.builder.ScenarioDatasetBuilderSc01;
import org.springframework.stereotype.Service;

/**
 * CreneauGenerationService — Phase D
 *
 * Encapsule la génération de créneaux à partir de paramètres utilisateur.
 * Isole la logique de construction des créneaux SC-01 dans un composant dédié,
 * indépendant du scénario qui l'invoque.
 *
 * Rôle dans l'architecture cible :
 *
 *   court terme  : SC-01 utilise ce service pour générer ses créneaux
 *   moyen terme  : le service peut être réutilisé par d'autres scénarios paramétriques
 *   long terme   : peut exposer une API produisant des List<CreneauInputDTO>
 *                  pour alimenter directement dataSet.creneaux (convergence dataset-driven)
 *
 * Pourquoi ce service existe :
 *   - isoler la génération du pipeline de préparation SC-01
 *   - rendre la logique de construction testable et réutilisable indépendamment
 *   - préparer le chemin vers un pipeline unifié : génération → dataset → solveur
 *
 * Ce que ce service remplace :
 *   - l'appel direct à new ScenarioDatasetBuilderSc01() dans ScenarioSc01PreparationService
 *
 * Point d'extension futur (Option 2, non implémenté en Phase D) :
 *   - une méthode generateAsInputDtos(BuildRequest) pourra retourner List<CreneauInputDTO>
 *     pour permettre l'injection dans dataSet.creneaux avant le solveur
 */
@Service
public class CreneauGenerationService {

    private final ScenarioDatasetBuilderSc01 builder = new ScenarioDatasetBuilderSc01();

    /**
     * Génère les créneaux à partir des paramètres de construction.
     *
     * @param request paramètres de génération (horizon, ressource, amplitude, horaires, jours travaillés)
     * @return résultat de la construction : liste de créneaux + alertes de configuration
     */
    public ScenarioDatasetBuilderSc01.BuildResult generate(ScenarioDatasetBuilderSc01.BuildRequest request) {
        return builder.build(request);
    }
}
