package fr.project.planning.scenarios.service;

import fr.project.planning.api.PlanningResponse;
import fr.project.planning.api.PlanningService;
import fr.project.planning.domain.contexte.ToleranceEquite;
import fr.project.planning.domain.creneau.Creneau;
import fr.project.planning.domain.ressource.PosteVirtuel;
import fr.project.planning.domain.ressource.Ressource;
import fr.project.planning.domain.ressource.SalarieReel;
import fr.project.planning.domain.workmetrics.TrancheTemporelle;
import fr.project.planning.domain.workmetrics.WorkMetrics;
import fr.project.planning.domain.workmetrics.WorkMetricsCalculator;
import fr.project.planning.domain.workmetrics.WorkMetricsParTranche;
import fr.project.planning.scenarios.dto.MotifOptimisation;
import fr.project.planning.scenarios.dto.OptimisationDTO;
import fr.project.planning.scenarios.dto.Sc04ScenarioRequestDTO;
import fr.project.planning.scenarios.dto.ScenarioResponseDTO;
import fr.project.planning.scenarios.dto.SerieSalarieDTO;
import fr.project.planning.scenarios.dto.TrancheChargeDTO;
import fr.project.planning.scenarios.mapper.ScenarioResponseMapper;
import fr.project.planning.scenarios.mapper.ScoreBreakdownFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * ScenarioSc04ExecutionService — préparation → résolution → restitution de l'optimisation.
 *
 * <h3>Ce que ce service ajoute au socle</h3>
 * <p>Le bloc {@code optimisation} : pour chaque salarié touché, sa charge <strong>avant et
 * après</strong>, semaine par semaine, mois par mois, puis sur la période. Le bloc {@code planning}
 * ne restitue que l'après — or SC-04 remanie une période entière, et un tel résultat doit pouvoir
 * se justifier devant ceux qu'il déplace.</p>
 *
 * <h3>Le même calculateur des deux côtés</h3>
 * <p>L'avant vient de la préparation, l'après de la solution, et les deux passent par
 * {@link WorkMetricsParTranche} — donc par {@code WorkMetricsCalculator}. Deux implémentations de
 * la même règle finissent par diverger, et une divergence de méthode se lirait ici comme un
 * mouvement du planning : c'est l'arbitrage §5.1 du cadrage.</p>
 */
@Service
public class ScenarioSc04ExecutionService {

    private static final Logger log = LoggerFactory.getLogger(ScenarioSc04ExecutionService.class);

    private final ScenarioSc04PreparationService preparationService;
    private final PlanningService planningService;
    private final ScenarioResponseMapper responseMapper = new ScenarioResponseMapper();
    private final WorkMetricsParTranche metriquesParTranche = new WorkMetricsParTranche();

    public ScenarioSc04ExecutionService(ScenarioSc04PreparationService preparationService,
                                        PlanningService planningService) {
        this.preparationService = preparationService;
        this.planningService = planningService;
    }

    public ScenarioResponseDTO solve(Sc04ScenarioRequestDTO request) {
        PreparedSc04Scenario prepared = preparationService.prepare(request);

        PlanningResponse solved = planningService.solve(prepared.planningRequest());

        Map<String, WorkMetrics> apresGlobal = new HashMap<>();
        new WorkMetricsCalculator().compute(solved.solution())
                .forEach((r, wm) -> apresGlobal.put(r.getId(), wm));

        Map<TrancheTemporelle, Map<Ressource, WorkMetrics>> apresParTranche =
                metriquesParTranche.calculer(solved.solution());

        List<Creneau> creneauxResolus = solved.solution().getCreneaux();
        OptimisationDTO optimisation = construireOptimisation(
                prepared, creneauxResolus, apresParTranche,
                solved.solution().getScore().hardScore());

        log.info("[SC-04] Pivot {} — {} créneau(x) figé(s), {} ajustable(s), {} déplacé(s), "
                        + "acceptable={}",
                optimisation.datePivot(), optimisation.creneauxFiges(),
                optimisation.creneauxAjustables(), optimisation.creneauxDeplaces(),
                optimisation.acceptable());

        ScenarioResponseDTO response = responseMapper.toResponse(
                "SC-04",
                "SOLVED",
                solved.solution().getScore().hardScore(),
                solved.solution().getScore().softScore(),
                ScoreBreakdownFactory.build(solved.explanation()),
                null,                                   // resourceId — multi-ressources
                creneauxResolus,
                prepared.base().marqueursRepos(),
                apresGlobal,
                prepared.alerts(),
                prepared.base().posteVirtuelIds(),
                prepared.base().ignoredCreneaux(),
                prepared.planningRequest().regulatoryParameters()
        );

        response.setOptimisation(optimisation);
        return response;
    }

    /**
     * Le bloc {@code optimisation} : qu'est-ce qui a bougé, et ce que cela a fait à chacun.
     *
     * <p>Seuls les salariés <strong>concernés</strong> apparaissent — ceux qui ont repris ou cédé
     * un créneau, et ceux qui en tenaient un d'ajustable. Restituer toute la population noierait le
     * mouvement dans les inchangés, sur un scénario qui peut porter des centaines de personnes.</p>
     */
    private static OptimisationDTO construireOptimisation(
            PreparedSc04Scenario prepared,
            List<Creneau> creneauxResolus,
            Map<TrancheTemporelle, Map<Ressource, WorkMetrics>> apresParTranche,
            int hardScore) {

        Map<String, Integer> repris = new LinkedHashMap<>();
        Map<String, Integer> cedes = new LinkedHashMap<>();
        Set<String> concernes = new LinkedHashSet<>();
        int deplaces = 0;
        int nonCouverts = 0;
        int perdus = 0;

        for (Creneau creneau : creneauxResolus) {
            if (!prepared.creneauxAjustables().contains(creneau.getId())) {
                continue;
            }
            String avantId = prepared.ressourceAvantParCreneau().get(creneau.getId());
            Ressource apres = creneau.getRessourceAffectee();
            boolean couvert = apres instanceof SalarieReel || apres instanceof PosteVirtuel;
            String apresId = couvert ? apres.getId() : null;

            if (avantId != null) {
                concernes.add(avantId);
            }
            if (apres instanceof SalarieReel) {
                concernes.add(apresId);
            }

            if (!Objects.equals(avantId, apresId)) {
                deplaces++;
                if (apres instanceof SalarieReel) {
                    repris.merge(apresId, 1, Integer::sum);
                }
                if (avantId != null) {
                    cedes.merge(avantId, 1, Integer::sum);
                }
            }
            if (!couvert) {
                nonCouverts++;
                if (avantId != null) {
                    perdus++;
                }
            }
        }

        List<SerieSalarieDTO> parSalarie = new ArrayList<>();
        for (String salarieId : concernes) {
            parSalarie.add(new SerieSalarieDTO(
                    salarieId,
                    repris.getOrDefault(salarieId, 0),
                    cedes.getOrDefault(salarieId, 0),
                    series(prepared, apresParTranche, salarieId)));
        }

        List<MotifOptimisation> motifs = motifs(prepared, parSalarie, hardScore, deplaces, perdus);

        return new OptimisationDTO(
                prepared.datePivot(),
                prepared.creneauxFiges().size(),
                prepared.creneauxAjustables().size(),
                deplaces,
                nonCouverts,
                motifs.stream().noneMatch(MotifOptimisation::isEliminatoire),
                motifs.stream().map(MotifOptimisation::toDto).toList(),
                parSalarie);
    }

    /**
     * Les séries d'un salarié, dans l'ordre du découpage : semaines, mois, puis période.
     *
     * <p>L'ordre vient de {@code metriquesAvant}, produit par le découpage lui-même. Re-découper
     * l'horizon ici donnerait la même liste — et prendrait le risque qu'un jour elle diffère.</p>
     */
    private static List<TrancheChargeDTO> series(
            PreparedSc04Scenario prepared,
            Map<TrancheTemporelle, Map<Ressource, WorkMetrics>> apresParTranche,
            String salarieId) {

        List<TrancheChargeDTO> series = new ArrayList<>();
        for (TrancheTemporelle tranche : prepared.tranches()) {
            WorkMetrics avant = pour(prepared.metriquesAvant().get(tranche), salarieId);
            WorkMetrics apres = pour(apresParTranche.get(tranche), salarieId);

            series.add(new TrancheChargeDTO(
                    tranche.granularite().name(),
                    tranche.debut(),
                    tranche.fin(),
                    tranche.partielle(),
                    avant == null ? 0 : avant.getJoursObserves(),
                    apres == null ? 0 : apres.getJoursObserves(),
                    enHeures(avant),
                    enHeures(apres),
                    avant == null ? null : avant.getEcartContratPourcent(),
                    apres == null ? null : apres.getEcartContratPourcent()));
        }
        return series;
    }

    /**
     * Ce qui disqualifie le planning rendu, ou le décrit.
     *
     * <p>Le planning est toujours restitué — <em>le moteur ne refuse pas</em> — et ces motifs sont
     * ce qui rend la restitution honnête : sans eux, l'appelant recevrait un planning inacceptable
     * présenté comme un résultat ordinaire.</p>
     */
    private static List<MotifOptimisation> motifs(PreparedSc04Scenario prepared,
                                                  List<SerieSalarieDTO> parSalarie,
                                                  int hardScore,
                                                  int deplaces,
                                                  int perdus) {
        List<MotifOptimisation> motifs = new ArrayList<>();

        if (hardScore < 0) {
            motifs.add(MotifOptimisation.PLANNING_NON_CONFORME);
        }
        if (perdus > 0) {
            motifs.add(MotifOptimisation.CRENEAU_PERDU);
        }
        if (parSalarie.stream().anyMatch(ScenarioSc04ExecutionService::aRegresse)) {
            motifs.add(MotifOptimisation.REGRESSION_INDIVIDUELLE);
        }
        if (!inequitesResiduelles(prepared, parSalarie).isEmpty()) {
            motifs.add(MotifOptimisation.INEQUITE_RESIDUELLE);
        }
        if (deplaces == 0) {
            motifs.add(MotifOptimisation.OPTIMISATION_SANS_EFFET);
        }
        return motifs;
    }

    /**
     * Ce salarié sort-il plus loin de son contrat qu'il n'y entrait, sur la période ?
     *
     * <p>Sur la <strong>valeur absolue</strong> de l'écart : passer de −20 % à −25 % est une
     * régression au même titre que passer de +20 % à +25 %. Un moteur qui ne compterait que la
     * surcharge laisserait s'installer la sous-charge, et l'équité ne serait qu'à moitié tenue.</p>
     *
     * <p>La période fait foi, non les semaines : une semaine dégradée compensée par une autre
     * améliorée n'est pas une régression, c'est un rééquilibrage — et c'est ce qu'on demande.</p>
     */
    private static boolean aRegresse(SerieSalarieDTO serie) {
        return serie.tranches().stream()
                .filter(t -> TrancheTemporelle.Granularite.PERIODE.name().equals(t.granularite()))
                .anyMatch(t -> t.ecartContratAvantPourcent() != null
                        && t.ecartContratApresPourcent() != null
                        && Math.abs(t.ecartContratApresPourcent())
                                > Math.abs(t.ecartContratAvantPourcent()));
    }

    /**
     * Les salariés dont l'écart au contrat reste au-delà de la tolérance après optimisation.
     *
     * <p>Sans tolérance déclarée, la liste est vide : <em>une borne absente n'est pas une borne à
     * zéro</em>. Le seuil est franchi selon {@link ToleranceEquite#pointsExcedentaires} — la même
     * méthode que celle dont le score se sert, sans quoi l'appelant lirait un motif que le score ne
     * pèse pas, ou l'inverse.</p>
     */
    private static List<SerieSalarieDTO> inequitesResiduelles(PreparedSc04Scenario prepared,
                                                              List<SerieSalarieDTO> parSalarie) {
        ToleranceEquite tolerance = prepared.planningRequest().planningContext().getToleranceEquite();
        if (tolerance == null || tolerance.estVide()) {
            return List.of();
        }
        return parSalarie.stream()
                .filter(serie -> serie.tranches().stream()
                        .filter(t -> TrancheTemporelle.Granularite.PERIODE.name().equals(t.granularite()))
                        .anyMatch(t -> t.ecartContratApresPourcent() != null
                                && tolerance.pointsExcedentaires(t.ecartContratApresPourcent()) > 0))
                .toList();
    }

    private static WorkMetrics pour(Map<Ressource, WorkMetrics> parRessource, String salarieId) {
        if (parRessource == null) {
            return null;
        }
        return parRessource.entrySet().stream()
                .filter(e -> salarieId.equals(e.getKey().getId()))
                .map(Map.Entry::getValue)
                .findFirst()
                .orElse(null);
    }

    /**
     * Heures décimales, arrondies au centième — même unité que {@code workMetrics.byRessource}.
     *
     * <p>Un salarié absent des métriques d'une tranche n'y a rien travaillé : c'est zéro, pas une
     * inconnue.</p>
     */
    private static double enHeures(WorkMetrics metriques) {
        int minutes = metriques == null ? 0 : metriques.getMinutesTravaillees();
        return Math.round(minutes / 60.0 * 100.0) / 100.0;
    }
}
