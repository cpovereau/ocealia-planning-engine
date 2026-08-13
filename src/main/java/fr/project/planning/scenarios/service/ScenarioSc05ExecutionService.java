package fr.project.planning.scenarios.service;

import fr.project.planning.api.PlanningResponse;
import fr.project.planning.api.PlanningService;
import fr.project.planning.domain.creneau.Creneau;
import fr.project.planning.domain.ressource.PosteVirtuel;
import fr.project.planning.domain.ressource.Ressource;
import fr.project.planning.domain.ressource.SalarieReel;
import fr.project.planning.domain.workmetrics.WorkMetrics;
import fr.project.planning.domain.workmetrics.WorkMetricsCalculator;
import fr.project.planning.scenarios.dto.ArbitrageDTO;
import fr.project.planning.scenarios.dto.CreneauArbitreDTO;
import fr.project.planning.scenarios.dto.MouvementSalarieDTO;
import fr.project.planning.scenarios.dto.NatureCouverture;
import fr.project.planning.scenarios.dto.Sc05ScenarioRequestDTO;
import fr.project.planning.scenarios.dto.ScenarioResponseDTO;
import fr.project.planning.scenarios.mapper.ScenarioResponseMapper;
import fr.project.planning.scenarios.mapper.ScoreBreakdownFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * ScenarioSc05ExecutionService — préparation → résolution → restitution de l'arbitrage.
 *
 * <p>La résolution est celle de SC-02 : le solveur cherche une affectation, et c'est le score qui
 * arbitre. SC-05 ne classe pas — il <strong>rend une répartition</strong>, pas un podium.</p>
 *
 * <h3>Ce que ce service ajoute au socle</h3>
 * <p>Le bloc {@code arbitrage} : l'avant et l'après de chaque salarié arbitré, et le sort de chaque
 * créneau du périmètre. Le bloc {@code planning} ne restitue que l'après — il ne dit ni d'où l'on
 * vient, ni ce que l'arbitrage a coûté à chacun, alors que SC-05 rend précisément une répartition
 * qui doit se justifier ligne à ligne devant les intéressés.</p>
 *
 * <p>L'alerte d'inéquité résiduelle, et la restitution assumée de la moins mauvaise répartition
 * quand aucune n'est acceptable, sont le lot <strong>A3</strong>.</p>
 */
@Service
public class ScenarioSc05ExecutionService {

    private static final Logger log = LoggerFactory.getLogger(ScenarioSc05ExecutionService.class);

    private final ScenarioSc05PreparationService preparationService;
    private final PlanningService planningService;
    private final ScenarioResponseMapper responseMapper = new ScenarioResponseMapper();

    public ScenarioSc05ExecutionService(ScenarioSc05PreparationService preparationService,
                                        PlanningService planningService) {
        this.preparationService = preparationService;
        this.planningService = planningService;
    }

    public ScenarioResponseDTO solve(Sc05ScenarioRequestDTO request) {
        PreparedSc05Scenario prepared = preparationService.prepare(request);

        PlanningResponse solved = planningService.solve(prepared.planningRequest());

        WorkMetricsCalculator calculator = new WorkMetricsCalculator();
        Map<String, WorkMetrics> apres = new HashMap<>();
        calculator.compute(solved.solution()).forEach((r, wm) -> apres.put(r.getId(), wm));

        List<Creneau> creneauxResolus = solved.solution().getCreneaux();
        ArbitrageDTO arbitrage = construireArbitrage(prepared, creneauxResolus, apres);

        log.info("[SC-05] Arbitrage entre {} — {} créneau(x) au périmètre, {} déplacé(s), "
                        + "{} épinglé(s) sur un tiers",
                prepared.ressourcesAutorisees(), arbitrage.creneauxArbitres(),
                arbitrage.creneauxDeplaces(), arbitrage.creneauxEpinglesSurUnTiers());

        ScenarioResponseDTO response = responseMapper.toResponse(
                "SC-05",
                "SOLVED",
                solved.solution().getScore().hardScore(),
                solved.solution().getScore().softScore(),
                ScoreBreakdownFactory.build(solved.explanation()),
                null,                                   // resourceId — multi-ressources
                creneauxResolus,
                prepared.base().marqueursRepos(),
                apres,
                prepared.alerts(),
                prepared.base().posteVirtuelIds(),
                prepared.base().ignoredCreneaux(),
                prepared.planningRequest().regulatoryParameters()
        );

        response.setArbitrage(arbitrage);
        return response;
    }

    /**
     * Le bloc {@code arbitrage} : qu'est-ce qui a bougé, et pour qui.
     *
     * <p>Les compteurs sont totalisés ici, sur les mêmes créneaux que ceux qui alimentent
     * {@code details[]}. Les faire calculer par l'appelant l'exposerait à confondre un poste
     * virtuel avec un salarié, ou à compter comme un déplacement un créneau qui n'était à
     * personne.</p>
     */
    private static ArbitrageDTO construireArbitrage(PreparedSc05Scenario prepared,
                                                    List<Creneau> creneauxResolus,
                                                    Map<String, WorkMetrics> apres) {
        List<CreneauArbitreDTO> details = new ArrayList<>();
        Map<String, Integer> repris = new LinkedHashMap<>();
        Map<String, Integer> cedes = new LinkedHashMap<>();
        Set<String> besoinsDuPerimetre = new LinkedHashSet<>();
        int deplaces = 0;
        int nonCouverts = 0;

        for (Creneau creneau : creneauxResolus) {
            if (!prepared.perimetreRetenu().contains(creneau.getIdBesoin())) {
                continue;
            }
            besoinsDuPerimetre.add(creneau.getIdBesoin());

            String avantId = prepared.ressourceAvantParCreneau().get(creneau.getId());
            Ressource apresRessource = creneau.getRessourceAffectee();
            NatureCouverture nature = natureDe(apresRessource);
            String apresId = nature == NatureCouverture.NON_COUVERT ? null : apresRessource.getId();

            boolean deplace = !Objects.equals(avantId, apresId);
            if (deplace) {
                deplaces++;
                if (nature == NatureCouverture.SALARIE) {
                    repris.merge(apresId, 1, Integer::sum);
                }
                if (avantId != null) {
                    cedes.merge(avantId, 1, Integer::sum);
                }
            }
            if (nature == NatureCouverture.NON_COUVERT) {
                nonCouverts++;
            }

            details.add(new CreneauArbitreDTO(
                    creneau.getId(),
                    creneau.getCreneauOrigineId(),
                    creneau.getDate(),
                    creneau.getHeureDebut(),
                    creneau.getHeureFin(),
                    creneau.getDuree(),
                    avantId,
                    apresId,
                    nature,
                    deplace,
                    prepared.creneauxTenusParUnTiers().contains(creneau.getId())));
        }

        details.sort(Comparator
                .comparing(CreneauArbitreDTO::date)
                .thenComparing(CreneauArbitreDTO::heureDebut));

        List<MouvementSalarieDTO> parSalarie = new ArrayList<>();
        for (String salarieId : prepared.ressourcesAutorisees()) {
            WorkMetrics avant = prepared.metriquesAvant().get(salarieId);
            WorkMetrics apresLui = apres.get(salarieId);
            parSalarie.add(new MouvementSalarieDTO(
                    salarieId,
                    repris.getOrDefault(salarieId, 0),
                    cedes.getOrDefault(salarieId, 0),
                    enHeures(avant),
                    enHeures(apresLui),
                    avant == null ? null : avant.getEcartContratPourcent(),
                    apresLui == null ? null : apresLui.getEcartContratPourcent()));
        }

        return new ArbitrageDTO(
                List.copyOf(prepared.ressourcesAutorisees()),
                besoinsDuPerimetre.size(),
                deplaces,
                prepared.creneauxTenusParUnTiers().size(),
                nonCouverts,
                parSalarie,
                details);
    }

    private static NatureCouverture natureDe(Ressource ressource) {
        if (ressource instanceof SalarieReel) return NatureCouverture.SALARIE;
        if (ressource instanceof PosteVirtuel) return NatureCouverture.POSTE_VIRTUEL;
        return NatureCouverture.NON_COUVERT;
    }

    /**
     * Heures décimales, arrondies au centième — même unité que {@code workMetrics.byRessource}.
     *
     * <p>Un salarié absent des métriques n'a rien travaillé : c'est zéro, pas une inconnue.</p>
     */
    private static double enHeures(WorkMetrics metriques) {
        int minutes = metriques == null ? 0 : metriques.getMinutesTravaillees();
        return Math.round(minutes / 60.0 * 100.0) / 100.0;
    }
}
