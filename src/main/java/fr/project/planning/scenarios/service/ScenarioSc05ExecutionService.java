package fr.project.planning.scenarios.service;

import fr.project.planning.api.PlanningResponse;
import fr.project.planning.api.PlanningService;
import fr.project.planning.domain.creneau.Creneau;
import fr.project.planning.domain.ressource.PosteVirtuel;
import fr.project.planning.domain.ressource.Ressource;
import fr.project.planning.domain.ressource.SalarieReel;
import fr.project.planning.domain.contexte.ToleranceEquite;
import fr.project.planning.domain.workmetrics.WorkMetrics;
import fr.project.planning.domain.workmetrics.WorkMetricsCalculator;
import fr.project.planning.scenarios.alerte.AlertCode;
import fr.project.planning.scenarios.alerte.AlertSeverity;
import fr.project.planning.scenarios.alerte.CollecteurAlertes;
import fr.project.planning.scenarios.dto.ArbitrageDTO;
import fr.project.planning.scenarios.dto.CreneauArbitreDTO;
import fr.project.planning.scenarios.dto.MotifArbitrage;
import fr.project.planning.scenarios.dto.MouvementSalarieDTO;
import fr.project.planning.scenarios.dto.NatureCouverture;
import fr.project.planning.scenarios.dto.Sc05ScenarioRequestDTO;
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
 * <h3>[A3] La moins mauvaise, avec les motifs qui la disqualifient</h3>
 * <p>§5.6 du cadrage : <em>jamais une erreur</em>. La répartition est toujours rendue, et
 * {@code arbitrage.acceptable} plus {@code arbitrage.motifs} disent ce qui, le cas échéant, la
 * disqualifie. Une inéquité que le périmètre ne permettait pas de résorber est de surcroît
 * signalée par une alerte nominative.</p>
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
        ArbitrageDTO arbitrage = construireArbitrage(
                prepared, creneauxResolus, apres, solved.solution().getScore().hardScore());

        log.info("[SC-05] Arbitrage entre {} — {} créneau(x) au périmètre, {} déplacé(s), "
                        + "{} épinglé(s) sur un tiers, acceptable={}",
                prepared.ressourcesAutorisees(), arbitrage.creneauxArbitres(),
                arbitrage.creneauxDeplaces(), arbitrage.creneauxEpinglesSurUnTiers(),
                arbitrage.acceptable());

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
                alertesCompletes(prepared, arbitrage.parSalarie()),
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
                                                    Map<String, WorkMetrics> apres,
                                                    int hardScore) {
        List<CreneauArbitreDTO> details = new ArrayList<>();
        Map<String, Integer> repris = new LinkedHashMap<>();
        Map<String, Integer> cedes = new LinkedHashMap<>();
        Set<String> besoinsDuPerimetre = new LinkedHashSet<>();
        int deplaces = 0;
        int nonCouverts = 0;
        int perdus = 0;

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
                // Perdu, et non simplement non couvert : quelqu'un l'assurait. Un créneau qui
                // n'était déjà à personne n'a rien perdu, et le compter ici ferait crier au
                // dommage là où il n'y en a pas.
                if (avantId != null) {
                    perdus++;
                }
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

        List<MotifArbitrage> motifs = motifs(prepared, parSalarie, hardScore, deplaces, perdus);

        return new ArbitrageDTO(
                List.copyOf(prepared.ressourcesAutorisees()),
                besoinsDuPerimetre.size(),
                deplaces,
                prepared.creneauxTenusParUnTiers().size(),
                nonCouverts,
                motifs.stream().noneMatch(MotifArbitrage::isEliminatoire),
                motifs.stream().map(MotifArbitrage::toDto).toList(),
                parSalarie,
                details);
    }

    /**
     * [A3] Ce qui disqualifie la répartition rendue, ou la décrit.
     *
     * <p>§5.6 du cadrage : <em>sans répartition acceptable, la moins mauvaise est rendue, jamais
     * une erreur</em>. Ces motifs sont ce qui rend cette restitution honnête — sans eux, l'appelant
     * recevrait une répartition inacceptable présentée comme un résultat ordinaire.</p>
     */
    private static List<MotifArbitrage> motifs(PreparedSc05Scenario prepared,
                                               List<MouvementSalarieDTO> parSalarie,
                                               int hardScore,
                                               int deplaces,
                                               int perdus) {
        List<MotifArbitrage> motifs = new ArrayList<>();

        if (hardScore < 0) {
            motifs.add(MotifArbitrage.REPARTITION_NON_CONFORME);
        }
        if (perdus > 0) {
            motifs.add(MotifArbitrage.CRENEAU_ARBITRE_PERDU);
        }
        if (!inequitesResiduelles(prepared, parSalarie).isEmpty()) {
            motifs.add(MotifArbitrage.INEQUITE_RESIDUELLE);
        }
        if (deplaces == 0) {
            motifs.add(MotifArbitrage.ARBITRAGE_SANS_EFFET);
        }
        return motifs;
    }

    /**
     * [A3] Les salariés dont l'écart au contrat reste au-delà de la tolérance après arbitrage.
     *
     * <p>Sans tolérance déclarée, la liste est vide : <em>une borne absente n'est pas une borne à
     * zéro</em>, et le moteur ne juge pas inéquitable un écart que personne ne lui a dit de juger.
     * C'est la même lecture que celle qui rend la contrainte du lot L5 inerte.</p>
     *
     * <p>Le seuil est franchi selon {@link ToleranceEquite#pointsExcedentaires} — la même méthode
     * que celle dont le score se sert. Deux lectures du même seuil finiraient par diverger, et
     * l'appelant lirait une alerte que le score ne pèse pas, ou l'inverse.</p>
     */
    private static List<MouvementSalarieDTO> inequitesResiduelles(PreparedSc05Scenario prepared,
                                                                  List<MouvementSalarieDTO> parSalarie) {
        ToleranceEquite tolerance = prepared.planningRequest().planningContext().getToleranceEquite();
        if (tolerance == null || tolerance.estVide()) {
            return List.of();
        }
        return parSalarie.stream()
                .filter(m -> m.ecartContratApresPourcent() != null)
                .filter(m -> tolerance.pointsExcedentaires(m.ecartContratApresPourcent()) > 0)
                .toList();
    }

    /**
     * Les alertes de la préparation, complétées de ce que seule la résolution pouvait établir.
     *
     * <p>Une inéquité résiduelle n'est pas un échec du moteur — c'est un constat sur le périmètre
     * remis en jeu, qui ne contenait pas de quoi ramener tout le monde dans la marge. Mais c'est
     * une information que l'appelant doit recevoir, pas déduire d'un écart qu'il aurait pensé à
     * comparer à sa propre tolérance.</p>
     */
    private static List<ScenarioAlertDTO> alertesCompletes(PreparedSc05Scenario prepared,
                                                           List<MouvementSalarieDTO> parSalarie) {
        List<MouvementSalarieDTO> residuelles = inequitesResiduelles(prepared, parSalarie);
        if (residuelles.isEmpty()) {
            return prepared.alerts();
        }

        Double tolere = prepared.planningRequest().planningContext()
                .getToleranceEquite().getEcartTolerePourcent();
        CollecteurAlertes apresResolution = new CollecteurAlertes("SC-05");
        for (MouvementSalarieDTO mouvement : residuelles) {
            apresResolution.signaler(AlertCode.INEQUITE_RESIDUELLE, AlertSeverity.WARNING,
                    "'" + mouvement.ressourceId() + "' reste à "
                            + mouvement.ecartContratApresPourcent() + " % de son contrat, au-delà "
                            + "de la tolérance de " + tolere + " % déclarée par la demande. "
                            + "Le périmètre remis en jeu ne contenait pas de quoi l'y ramener : "
                            + "l'élargir est une décision d'encadrement.");
        }

        List<ScenarioAlertDTO> completes = new ArrayList<>(prepared.alerts());
        completes.addAll(apresResolution.versDto());
        return completes;
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
