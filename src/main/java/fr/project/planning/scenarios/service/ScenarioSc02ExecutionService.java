package fr.project.planning.scenarios.service;

import fr.project.planning.api.PlanningResponse;
import fr.project.planning.api.PlanningService;
import fr.project.planning.domain.creneau.Creneau;
import fr.project.planning.domain.ressource.PosteVirtuel;
import fr.project.planning.domain.ressource.Ressource;
import fr.project.planning.domain.ressource.SalarieReel;
import fr.project.planning.domain.workmetrics.WorkMetrics;
import fr.project.planning.domain.workmetrics.WorkMetricsCalculator;
import fr.project.planning.scenarios.alerte.AlertCode;
import fr.project.planning.scenarios.alerte.AlertSeverity;
import fr.project.planning.scenarios.alerte.CollecteurAlertes;
import fr.project.planning.scenarios.dto.CreneauRemplaceDTO;
import fr.project.planning.scenarios.dto.NatureCouverture;
import fr.project.planning.scenarios.dto.RemplacementDTO;
import fr.project.planning.scenarios.dto.Sc02ScenarioRequestDTO;
import fr.project.planning.scenarios.dto.ScenarioAlertDTO;
import fr.project.planning.scenarios.dto.ScenarioResponseDTO;
import fr.project.planning.scenarios.mapper.ScenarioResponseMapper;
import fr.project.planning.scenarios.mapper.ScoreBreakdownFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * ScenarioSc02ExecutionService — préparation → résolution → conséquences de l'absence.
 *
 * <p>La résolution est celle de SC-03 : le solveur cherche une affectation. Ce que SC-02 ajoute
 * tient à ce qu'il en dit — quels créneaux ont changé de main, et combien d'heures restent à
 * pourvoir.</p>
 */
@Service
public class ScenarioSc02ExecutionService {

    private static final Logger log = LoggerFactory.getLogger(ScenarioSc02ExecutionService.class);

    private final ScenarioSc02PreparationService preparationService;
    private final PlanningService planningService;
    private final ScenarioResponseMapper responseMapper = new ScenarioResponseMapper();

    public ScenarioSc02ExecutionService(ScenarioSc02PreparationService preparationService,
                                        PlanningService planningService) {
        this.preparationService = preparationService;
        this.planningService = planningService;
    }

    public ScenarioResponseDTO solve(Sc02ScenarioRequestDTO request) {
        PreparedSc02Scenario prepared = preparationService.prepare(request);

        PlanningResponse solved = planningService.solve(prepared.planningRequest());
        List<Creneau> creneauxResolus = solved.solution().getCreneaux();

        WorkMetricsCalculator calculator = new WorkMetricsCalculator();
        Map<String, WorkMetrics> byId = new HashMap<>();
        calculator.compute(solved.solution()).forEach((r, wm) -> byId.put(r.getId(), wm));

        RemplacementDTO remplacement = construireRemplacement(prepared, creneauxResolus);

        log.info("[SC-02] Absence de '{}' — {} créneau(x) libéré(s), {} repris, {} h à pourvoir",
                prepared.salarieAbsentId(), remplacement.creneauxLiberes(),
                remplacement.creneauxRepris(), remplacement.heuresAPourvoir());

        ScenarioResponseDTO response = responseMapper.toResponse(
                "SC-02",
                "SOLVED",
                solved.solution().getScore().hardScore(),
                solved.solution().getScore().softScore(),
                ScoreBreakdownFactory.build(solved.explanation()),
                null,                                   // resourceId — multi-ressources
                creneauxResolus,
                prepared.base().marqueursRepos(),
                byId,
                alertesCompletes(prepared, remplacement),
                prepared.base().posteVirtuelIds(),
                prepared.base().ignoredCreneaux(),
                prepared.planningRequest().regulatoryParameters()
        );

        response.setRemplacement(remplacement);
        return response;
    }

    /**
     * Les alertes de la préparation, complétées de ce que seule la résolution pouvait établir.
     *
     * <p>Des heures restées à pourvoir ne sont pas un échec du moteur — c'est même la réponse
     * attendue quand personne n'est disponible (§4.3 du cadrage). Mais c'est une information que
     * l'appelant doit recevoir, pas déduire d'un total qu'il aurait pensé à lire.</p>
     */
    private static List<ScenarioAlertDTO> alertesCompletes(PreparedSc02Scenario prepared,
                                                           RemplacementDTO remplacement) {
        if (remplacement.heuresAPourvoir() <= 0) {
            return prepared.alerts();
        }

        CollecteurAlertes apresResolution = new CollecteurAlertes("SC-02");
        apresResolution.signaler(AlertCode.HEURES_RESTANT_A_POURVOIR, AlertSeverity.WARNING,
                remplacement.heuresAPourvoir() + " h de l'absence de '"
                        + prepared.salarieAbsentId() + "' n'ont trouvé aucun salarié : "
                        + (remplacement.creneauxLiberes() - remplacement.creneauxRepris())
                        + " créneau(x) sur " + remplacement.creneauxLiberes()
                        + ". Le détail est dans remplacement.details.");

        List<ScenarioAlertDTO> completes = new ArrayList<>(prepared.alerts());
        completes.addAll(apresResolution.versDto());
        return completes;
    }

    private static RemplacementDTO construireRemplacement(PreparedSc02Scenario prepared,
                                                          List<Creneau> creneauxResolus) {
        List<CreneauRemplaceDTO> details = new ArrayList<>();
        int repris = 0;
        int minutesAPourvoir = 0;

        for (Creneau creneau : creneauxResolus) {
            if (!prepared.creneauxLiberes().contains(creneau.getId())) {
                continue;
            }

            Ressource apres = creneau.getRessourceAffectee();
            NatureCouverture nature = natureDe(apres);
            if (nature == NatureCouverture.SALARIE) {
                repris++;
            } else {
                minutesAPourvoir += creneau.getDuree();
            }

            details.add(new CreneauRemplaceDTO(
                    creneau.getId(),
                    null,                               // creneauOrigineId — le lot S1 ne découpe pas
                    creneau.getDate(),
                    creneau.getHeureDebut(),
                    creneau.getHeureFin(),
                    creneau.getDuree(),
                    prepared.salarieAbsentId(),
                    nature == NatureCouverture.NON_COUVERT ? null : apres.getId(),
                    nature));
        }

        details.sort(Comparator
                .comparing(CreneauRemplaceDTO::date)
                .thenComparing(CreneauRemplaceDTO::heureDebut));

        return new RemplacementDTO(
                prepared.salarieAbsentId(),
                details.size(),
                repris,
                enHeures(minutesAPourvoir),
                details);
    }

    private static NatureCouverture natureDe(Ressource ressource) {
        if (ressource instanceof SalarieReel) return NatureCouverture.SALARIE;
        if (ressource instanceof PosteVirtuel) return NatureCouverture.POSTE_VIRTUEL;
        return NatureCouverture.NON_COUVERT;
    }

    /** Heures décimales, arrondies au centième — même unité que {@code workMetrics.byRessource}. */
    private static double enHeures(int minutes) {
        return Math.round(minutes / 60.0 * 100.0) / 100.0;
    }
}
