package fr.project.planning.scenarios.mapper;

import fr.project.planning.domain.creneau.Creneau;
import fr.project.planning.domain.ressource.ContraintesReglementairesSalarie;
import fr.project.planning.domain.ressource.Ressource;
import fr.project.planning.domain.ressource.SalarieReel;
import fr.project.planning.scenarios.dto.ImpactCandidatDTO;
import fr.project.planning.scenarios.dto.ImpactMesureDTO;
import fr.project.planning.scenarios.service.Candidat;
import fr.project.planning.scenarios.service.PreparedSc06Scenario;
import fr.project.planning.scenarios.service.Sc06ChargeCalculator;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Sc06ImpactFactory — construit le bloc {@code impacts[]} d'un candidat (SC-06, lot S5).
 *
 * <p>Répond à la question « à quel prix ? » : pour chaque ressource réelle mobilisée, ce que
 * l'affectation du besoin change à son amplitude journalière, à ses heures du jour et à ses
 * heures de la semaine.</p>
 *
 * <p>Les mesures viennent de {@link Sc06ChargeCalculator}, qui sert également au classement :
 * la réponse ne peut donc pas affirmer une chose pendant que le rang en applique une autre.</p>
 *
 * <p>L'ordre des entrées suit celui des créneaux du besoin, donc celui de la requête.</p>
 */
public final class Sc06ImpactFactory {

    private Sc06ImpactFactory() {
        // utilitaire
    }

    public static List<ImpactCandidatDTO> build(PreparedSc06Scenario prepared, Candidat candidat) {

        List<ImpactCandidatDTO> impacts = new ArrayList<>();

        for (String ressourceId : ressourcesReellesMobilisees(prepared, candidat)) {

            List<Creneau> avant = Sc06ChargeCalculator.planningFige(prepared, ressourceId);
            List<Creneau> apres = Sc06ChargeCalculator.avecLeBesoin(
                    avant, Sc06ChargeCalculator.partDuBesoin(prepared, candidat.affectations(), ressourceId));

            List<Creneau> avantCeJour = Sc06ChargeCalculator.duJour(avant, prepared.dateBesoin());
            List<Creneau> apresCeJour = Sc06ChargeCalculator.duJour(apres, prepared.dateBesoin());

            ContraintesReglementairesSalarie contraintes = contraintesDe(prepared, ressourceId);

            impacts.add(new ImpactCandidatDTO(
                    ressourceId,
                    mesure(
                            Sc06ChargeCalculator.amplitudeMinutes(avantCeJour, prepared.referentiel()),
                            Sc06ChargeCalculator.amplitudeMinutes(apresCeJour, prepared.referentiel()),
                            contraintes == null ? null : contraintes.getAmplitudeJournaliereMaximum()),
                    mesure(
                            Sc06ChargeCalculator.minutesTravaillees(avantCeJour, prepared.referentiel()),
                            Sc06ChargeCalculator.minutesTravaillees(apresCeJour, prepared.referentiel()),
                            contraintes == null ? null : contraintes.getHeuresMaximumParJour()),
                    mesure(
                            Sc06ChargeCalculator.minutesTravaillees(avant, prepared.referentiel()),
                            Sc06ChargeCalculator.minutesTravaillees(apres, prepared.referentiel()),
                            contraintes == null ? null : contraintes.getHeuresMaximumParSemaine()),
                    volumeHebdomadaireHabituel(prepared, ressourceId)
            ));
        }

        return impacts;
    }

    // =========================================================
    // Helpers
    // =========================================================

    private static ImpactMesureDTO mesure(int minutesAvant, int minutesApres, Double plafondHeures) {
        return new ImpactMesureDTO(
                Sc06ChargeCalculator.enHeures(minutesAvant),
                Sc06ChargeCalculator.enHeures(minutesApres),
                plafondHeures
        );
    }

    /**
     * Identifiants des ressources réelles mobilisées, dans l'ordre des créneaux du besoin.
     * Les postes virtuels et la pseudo-ressource « à pourvoir » sont écartés : ils ne portent
     * ni contrat ni contraintes individuelles.
     */
    private static Set<String> ressourcesReellesMobilisees(PreparedSc06Scenario prepared, Candidat candidat) {
        Set<String> ids = new LinkedHashSet<>();
        for (Creneau creneau : prepared.creneauxBesoin()) {
            Ressource affectee = candidat.affectations().get(creneau.getId());
            if (affectee instanceof SalarieReel) {
                ids.add(affectee.getId());
            }
        }
        return ids;
    }

    private static ContraintesReglementairesSalarie contraintesDe(PreparedSc06Scenario prepared, String ressourceId) {
        SalarieReel salarie = salarie(prepared, ressourceId);
        return salarie == null ? null : salarie.getContraintesReglementaires();
    }

    private static Double volumeHebdomadaireHabituel(PreparedSc06Scenario prepared, String ressourceId) {
        SalarieReel salarie = salarie(prepared, ressourceId);
        if (salarie == null || salarie.getContrat() == null) {
            return null;
        }
        return salarie.getContrat().getHeuresHebdomadairesHabituelles();
    }

    private static SalarieReel salarie(PreparedSc06Scenario prepared, String ressourceId) {
        for (SalarieReel salarie : prepared.salaries()) {
            if (ressourceId.equals(salarie.getId())) {
                return salarie;
            }
        }
        return null;
    }
}
