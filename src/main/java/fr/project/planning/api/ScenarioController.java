package fr.project.planning.api;

import fr.project.planning.scenarios.dto.Sc02ScenarioRequestDTO;
import fr.project.planning.scenarios.dto.Sc03ScenarioRequestDTO;
import fr.project.planning.scenarios.dto.Sc06ScenarioRequestDTO;
import fr.project.planning.scenarios.dto.ScenarioRequestDTO;
import fr.project.planning.scenarios.dto.ScenarioResponseDTO;
import fr.project.planning.scenarios.service.ScenarioSc01ExecutionService;
import fr.project.planning.scenarios.service.ScenarioSc02ExecutionService;
import fr.project.planning.scenarios.service.ScenarioSc03ExecutionService;
import fr.project.planning.scenarios.service.ScenarioSc06ExecutionService;

import jakarta.validation.Valid;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * ScenarioController — Endpoints API V1
 *
 * Routes exposées (stables V1) :
 *   GET  /scenarios/ping
 *   POST /scenarios/sc01/solve
 *   POST /scenarios/sc01/solve/file
 *   POST /scenarios/sc02/solve
 *   POST /scenarios/sc03/solve
 *   POST /scenarios/sc06/solve
 *
 * Principe : validation HTTP minimale (@Valid) et délégation aux services.
 * Toute la logique métier est dans les services d'exécution par scénario.
 * La gestion d'erreur unifiée est assurée par GlobalExceptionHandler.
 */
@RestController
@RequestMapping("/scenarios")
public class ScenarioController {

    private final ScenarioSc01ExecutionService scenarioSc01ExecutionService;
    private final ScenarioSc02ExecutionService scenarioSc02ExecutionService;
    private final ScenarioSc03ExecutionService scenarioSc03ExecutionService;
    private final ScenarioSc06ExecutionService scenarioSc06ExecutionService;

    public ScenarioController(ScenarioSc01ExecutionService scenarioSc01ExecutionService,
                               ScenarioSc02ExecutionService scenarioSc02ExecutionService,
                               ScenarioSc03ExecutionService scenarioSc03ExecutionService,
                               ScenarioSc06ExecutionService scenarioSc06ExecutionService) {
        this.scenarioSc01ExecutionService = scenarioSc01ExecutionService;
        this.scenarioSc02ExecutionService = scenarioSc02ExecutionService;
        this.scenarioSc03ExecutionService = scenarioSc03ExecutionService;
        this.scenarioSc06ExecutionService = scenarioSc06ExecutionService;
    }

    /**
     * GET /scenarios/ping — Santé du service.
     * Retourne la chaîne littérale "OK" (HTTP 200).
     */
    @GetMapping("/ping")
    public String ping() {
        return "OK";
    }

    /**
     * POST /scenarios/sc01/solve — Mode paramétrique SC-01.
     *
     * Pipeline : scenarioParameters → génération des créneaux (ScenarioDatasetBuilderSc01)
     *            → résolution solveur → ScenarioResponseDTO
     *
     * Les créneaux ne sont PAS fournis par l'appelant : ils sont générés par le moteur
     * à partir des paramètres (amplitude, heure de début, jours travaillés, etc.).
     * dataSet.creneaux est ignoré s'il est fourni.
     */
    @PostMapping("/sc01/solve")
    public ScenarioResponseDTO solveSc01(@Valid @RequestBody ScenarioRequestDTO request) {
        return scenarioSc01ExecutionService.solve(request);
    }

    /**
     * POST /scenarios/sc03/solve — Mode dataset-driven SC-03.
     *
     * Pipeline : dataSet.creneaux (fournis par l'appelant)
     *            → résolution solveur → ScenarioResponseDTO
     *
     * Les créneaux sont fournis directement dans le corps de la requête.
     * Aucune phase de génération n'est effectuée par le moteur.
     * dataSet.creneaux est obligatoire et ne peut pas être vide.
     */
    @PostMapping("/sc03/solve")
    public ScenarioResponseDTO solveSc03(@Valid @RequestBody Sc03ScenarioRequestDTO request) {
        return scenarioSc03ExecutionService.solve(request);
    }

    /**
     * POST /scenarios/sc02/solve — Remplacement d'un salarié absent.
     *
     * Pipeline : dataSet (planning existant + indisponibilités) + salarieAbsentId
     *            → épinglage de l'existant, libération des seuls créneaux de l'absent
     *            → résolution solveur → ScenarioResponseDTO
     *
     * Aucun créneau déjà affecté à un salarié présent n'est déplaçable : le moteur répond
     * « à pourvoir » plutôt que de remanier le planning de ceux qui n'ont rien demandé.
     * Il ne rend jamais « pas de solution ».
     *
     * La réponse porte un bloc supplémentaire, remplacement, propre à ce scénario.
     */
    @PostMapping("/sc02/solve")
    public ScenarioResponseDTO solveSc02(@Valid @RequestBody Sc02ScenarioRequestDTO request) {
        return scenarioSc02ExecutionService.solve(request);
    }

    /**
     * POST /scenarios/sc06/solve — Désignation de la ressource la plus à même de couvrir un besoin.
     *
     * Pipeline : besoin (scenarioParameters) + planning existant figé (dataSet)
     *            → énumération des candidats éligibles → classement → ScenarioResponseDTO
     *
     * Aucun appel au solveur : SC-06 classe des possibilités, il n'en cherche pas une.
     * Chaque candidat est évalué par SolutionManager, sans recherche — d'où un podium
     * déterministe, exhaustif et explicable.
     *
     * La réponse porte un bloc supplémentaire, candidats[], propre à ce scénario.
     */
    @PostMapping("/sc06/solve")
    public ScenarioResponseDTO solveSc06(@Valid @RequestBody Sc06ScenarioRequestDTO request) {
        return scenarioSc06ExecutionService.solve(request);
    }

    /**
     * POST /scenarios/sc01/solve/file — Mode paramétrique SC-01, restitution en pièce jointe.
     *
     * Pipeline identique à POST /scenarios/sc01/solve.
     * Seule différence : la réponse est retournée avec un en-tête
     * Content-Disposition: attachment; filename="SC-01-result-<dateDebut>-<dateFin>.json"
     *
     * Usage typique : téléchargement direct du résultat depuis un navigateur ou WinDev.
     */
    @PostMapping(value = "/sc01/solve/file", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ScenarioResponseDTO> solveSc01File(@Valid @RequestBody ScenarioRequestDTO request) {

        ScenarioResponseDTO response = solveSc01(request);

        String filename = "SC-01-result-" +
                request.getPlanningContext().getHorizon().getDateDebut() + "-" +
                request.getPlanningContext().getHorizon().getDateFin() + ".json";

        return ResponseEntity
                .ok()
                .contentType(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment()
                                .filename(filename)
                                .build()
                                .toString())
                .body(response);
    }
}