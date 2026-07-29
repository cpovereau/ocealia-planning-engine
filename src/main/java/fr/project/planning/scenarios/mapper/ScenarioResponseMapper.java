package fr.project.planning.scenarios.mapper;

import fr.project.planning.domain.ressource.RessourceNonAffectee;
import fr.project.planning.domain.workmetrics.WorkMetrics;
import fr.project.planning.scenarios.dto.GlobalWorkMetricsDTO;
import fr.project.planning.scenarios.dto.WorkMetricsByRessourceDTO;
import fr.project.planning.scenarios.dto.WorkMetricsDTO;
import fr.project.planning.domain.creneau.Creneau;
import fr.project.planning.scenarios.dto.CreneauPlanningDTO;
import fr.project.planning.scenarios.dto.DiagnosticsDTO;
import fr.project.planning.scenarios.dto.IgnoredCreneauxDTO;
import fr.project.planning.scenarios.dto.JourPlanningDTO;
import fr.project.planning.scenarios.dto.ScoreBreakdownItemDTO;
import fr.project.planning.scenarios.dto.ScoreDTO;
import fr.project.planning.scenarios.dto.SolutionSummaryDTO;
import fr.project.planning.scenarios.dto.ScenarioAlertDTO;
import fr.project.planning.scenarios.dto.ScenarioPlanningDTO;
import fr.project.planning.scenarios.dto.ScenarioResponseDTO;
import fr.project.planning.scenarios.dto.SolverResultDTO;    

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.ArrayList;
import java.util.stream.Collectors;

public class ScenarioResponseMapper {

    public ScenarioResponseDTO toResponse(
        String scenarioType,
        String solverStatus,
        int hardScore,
        int softScore,
        List<ScoreBreakdownItemDTO> scoreBreakdown,
        String idSalarie,
        List<Creneau> creneauxResolus,
        Map<String, WorkMetrics> workMetricsById,
        List<ScenarioAlertDTO> alerts,
        Set<String> posteVirtuelIds,
        IgnoredCreneauxDTO ignoredCreneaux
        ) {
        ScenarioPlanningDTO planning = buildPlanning(idSalarie, creneauxResolus);
        SolverResultDTO solverResult = buildSolverResult(solverStatus, hardScore, softScore, scoreBreakdown);
        DiagnosticsDTO diagnostics = buildDiagnostics(alerts, creneauxResolus, posteVirtuelIds, ignoredCreneaux);

        return new ScenarioResponseDTO(
                scenarioType,
                solverResult,
                planning,
                buildWorkMetrics(creneauxResolus, workMetricsById),
                buildSolutionSummary(creneauxResolus),
                diagnostics
        );
    }

    private SolverResultDTO buildSolverResult(
            String solverStatus,
            int hardScore,
            int softScore,
            List<ScoreBreakdownItemDTO> scoreBreakdown
    ) {
        return new SolverResultDTO(
                solverStatus,
                new ScoreDTO(hardScore, softScore),
                scoreBreakdown
        );
    }

    private DiagnosticsDTO buildDiagnostics(
        List<ScenarioAlertDTO> alerts,
        List<Creneau> creneauxResolus,
        Set<String> posteVirtuelIds,
        IgnoredCreneauxDTO ignoredCreneaux
    ) {
        return new DiagnosticsDTO(
            alerts,
            ignoredCreneaux != null ? ignoredCreneaux : new IgnoredCreneauxDTO(0, 0, 0),
            AssignmentDiagnosticsFactory.build(creneauxResolus, posteVirtuelIds)
        );
   }    

    private ScenarioPlanningDTO buildPlanning(String idSalarie, List<Creneau> creneauxResolus) {
        Map<java.time.LocalDate, List<CreneauPlanningDTO>> grouped = creneauxResolus.stream()
                .sorted(Comparator
                        .comparing(Creneau::getDate)
                        .thenComparing(Creneau::getHeureDebut))
                .collect(Collectors.groupingBy(
                        Creneau::getDate,
                        java.util.LinkedHashMap::new,
                        Collectors.mapping(this::toCreneauPlanningDTO, Collectors.toList())
                ));

        List<JourPlanningDTO> jours = grouped.entrySet().stream()
                .map(e -> new JourPlanningDTO(e.getKey(), e.getValue()))
                .toList();

        return new ScenarioPlanningDTO(idSalarie, jours);
    }

    private CreneauPlanningDTO toCreneauPlanningDTO(Creneau creneau) {
        return new CreneauPlanningDTO(
                creneau.getCodeActiviteEffectif(),
                creneau.getHeureDebut(),
                creneau.getHeureFin(),
                formatDuree(creneau),
                null,
                creneau.getRessourceAffectee() != null
                        ? creneau.getRessourceAffectee().getId()
                        : RessourceNonAffectee.INSTANCE.getId()
        );
    }

    private String formatDuree(Creneau creneau) {
        long totalMinutes = creneau.getDuree();
        long hours = totalMinutes / 60;
        long minutes = totalMinutes % 60;
        return String.format("%02d:%02d", hours, minutes);
    }

    private SolutionSummaryDTO buildSolutionSummary(List<Creneau> creneauxResolus) {
        int nbCreneaux = creneauxResolus.size();

        int nbCreneauxNonAffectes = (int) creneauxResolus.stream()
                .filter(c -> c.getRessourceAffectee() instanceof RessourceNonAffectee)
                .count();

        int nbCreneauxAffectes = nbCreneaux - nbCreneauxNonAffectes;

        int nbRessourcesMobilisees = (int) creneauxResolus.stream()
                .map(Creneau::getRessourceAffectee)
                .filter(Objects::nonNull)
                .filter(r -> !(r instanceof RessourceNonAffectee))
                .map(r -> r.getId())
                .distinct()
                .count();

        double heuresTravailleesTotales = creneauxResolus.stream()
                .mapToLong(Creneau::getDuree)
                .sum() / 60.0;

        SolutionSummaryDTO dto = new SolutionSummaryDTO();
        dto.setNbCreneaux(nbCreneaux);
        dto.setNbCreneauxAffectes(nbCreneauxAffectes);
        dto.setNbCreneauxNonAffectes(nbCreneauxNonAffectes);
        dto.setNbRessourcesMobilisees(nbRessourcesMobilisees);
        dto.setHeuresTravailleesTotales(heuresTravailleesTotales);

        return dto;
        }

        private WorkMetricsDTO buildWorkMetrics(
                List<Creneau> creneauxResolus,
                Map<String, WorkMetrics> workMetricsById
                ) {
                String idRessourceNonAffectee = RessourceNonAffectee.INSTANCE.getId();

                List<WorkMetricsByRessourceDTO> byRessource = new ArrayList<>();

                LocalDate periodeDebut = creneauxResolus.stream()
                        .map(Creneau::getDate)
                        .min(LocalDate::compareTo)
                        .orElse(null);

                LocalDate periodeFin = creneauxResolus.stream()
                        .map(Creneau::getDate)
                        .max(LocalDate::compareTo)
                        .orElse(null);

                double heuresNuitTotales = 0.0;
                double heuresJourFerieTotales = 0.0;

                for (Map.Entry<String, WorkMetrics> entry : workMetricsById.entrySet()) {
                        String resourceId = entry.getKey();
                        WorkMetrics wm = entry.getValue();

                        double heuresTravaillees = toHours(wm.getMinutesTravaillees());
                        double heuresNuit = toHours(wm.getMinutesNuit());
                        double heuresJourFerie = toHours(wm.getMinutesJourFerie());
                        double heuresReposHebdoTravaille = toHours(wm.getMinutesReposHebdoTravaille());

                        // Global = somme de toutes les métriques, y compris NON_AFFECTE
                        heuresNuitTotales += heuresNuit;
                        heuresJourFerieTotales += heuresJourFerie;

                        // byRessource = uniquement les ressources exposables
                        if (resourceId.equals(idRessourceNonAffectee)) {
                                continue;
                        }

                        WorkMetricsByRessourceDTO dto = new WorkMetricsByRessourceDTO();
                        dto.setResourceId(resourceId);
                        dto.setPeriodeDebut(periodeDebut);
                        dto.setPeriodeFin(periodeFin);

                        dto.setHeuresTravaillees(heuresTravaillees);
                        dto.setHeuresNuit(heuresNuit);
                        dto.setHeuresJourFerie(heuresJourFerie);
                        dto.setHeuresReposHebdoTravaille(heuresReposHebdoTravaille);

                        dto.setNbDimanchesTravailles(wm.getNbDimanchesTravailles());
                        dto.setMaxJoursConsecutifsObservees(wm.getMaxJoursConsecutifsObservees());
                        dto.setMaxNuitsConsecutivesObservees(wm.getMaxNuitsConsecutivesObservees());
                        dto.setNbCreneauxNuitNonNuit(wm.getNbCreneauxNuitNonNuit());

                        byRessource.add(dto);
        }

        byRessource.sort(java.util.Comparator.comparing(WorkMetricsByRessourceDTO::getResourceId));

        GlobalWorkMetricsDTO global = new GlobalWorkMetricsDTO();
        global.setNbCreneaux(creneauxResolus.size());
        global.setNbCreneauxNonAffectes((int) creneauxResolus.stream()
                .filter(c -> c.getRessourceAffectee() instanceof RessourceNonAffectee)
                .count());

        // Global = vision complète de la solution, pas seulement des ressources exposées
        global.setHeuresTravailleesTotales(creneauxResolus.stream()
                .mapToLong(Creneau::getDuree)
                .sum() / 60.0);

        global.setHeuresNuitTotales(heuresNuitTotales);
        global.setHeuresJourFerieTotales(heuresJourFerieTotales);

        return new WorkMetricsDTO(byRessource, global);
        }

        private double toHours(int minutes) {
                return minutes / 60.0;
        }
}