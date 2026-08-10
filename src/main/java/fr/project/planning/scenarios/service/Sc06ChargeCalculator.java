package fr.project.planning.scenarios.service;

import fr.project.planning.constraints.legales.AmplitudeJournaliere;
import fr.project.planning.domain.creneau.Creneau;
import fr.project.planning.domain.metier.ComptabiliteActivite;
import fr.project.planning.domain.metier.ReferentielComptabiliteActivite;
import fr.project.planning.domain.ressource.Ressource;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Sc06ChargeCalculator — primitives de charge partagées par SC-06 (lot S5).
 *
 * <p><strong>Source unique</strong> des notions « heures travaillées » et « amplitude » du
 * scénario. Le classement (palier 6) et les impacts restitués s'appuient sur les mêmes méthodes :
 * deux définitions concurrentes finiraient par diverger, et la réponse affirmerait alors une
 * chose pendant que le classement en appliquerait une autre.</p>
 *
 * <p>Le périmètre retenu est celui des contraintes individuelles : segments de pause exclus,
 * activités dont {@code compteDansCharge = false} exclues. L'amplitude est déléguée à
 * {@link AmplitudeJournaliere#calculerAmplitudeMinutes} — la réponse mesure donc exactement ce
 * que la contrainte mesure.</p>
 */
public final class Sc06ChargeCalculator {

    private Sc06ChargeCalculator() {
        // utilitaire
    }

    /**
     * Créneaux du planning existant affectés à une ressource.
     * Ce sont les faits acquis : la situation « avant ».
     */
    public static List<Creneau> planningFige(PreparedSc06Scenario prepared, String ressourceId) {
        List<Creneau> resultat = new ArrayList<>();
        for (Creneau creneau : prepared.problem().getCreneaux()) {
            if (creneau.isFige()
                    && creneau.getRessourceAffectee() != null
                    && ressourceId.equals(creneau.getRessourceAffectee().getId())) {
                resultat.add(creneau);
            }
        }
        return resultat;
    }

    /**
     * Créneaux du besoin qu'une affectation confie à une ressource.
     * C'est ce que le candidat ajoute à la situation « avant ».
     */
    public static List<Creneau> partDuBesoin(PreparedSc06Scenario prepared,
                                             Map<String, Ressource> affectations,
                                             String ressourceId) {
        List<Creneau> resultat = new ArrayList<>();
        for (Creneau creneau : prepared.creneauxBesoin()) {
            Ressource affectee = affectations.get(creneau.getId());
            if (affectee != null && ressourceId.equals(affectee.getId())) {
                resultat.add(creneau);
            }
        }
        return resultat;
    }

    /** Concatène la situation « avant » et la part du besoin : la situation « après ». */
    public static List<Creneau> avecLeBesoin(List<Creneau> avant, List<Creneau> partDuBesoin) {
        List<Creneau> apres = new ArrayList<>(avant.size() + partDuBesoin.size());
        apres.addAll(avant);
        apres.addAll(partDuBesoin);
        return apres;
    }

    /** Restreint une liste de créneaux à une journée. */
    public static List<Creneau> duJour(List<Creneau> creneaux, LocalDate jour) {
        List<Creneau> resultat = new ArrayList<>();
        for (Creneau creneau : creneaux) {
            if (jour.equals(creneau.getDate())) {
                resultat.add(creneau);
            }
        }
        return resultat;
    }

    /**
     * Minutes travaillées : somme des durées des créneaux qui comptent dans la charge.
     *
     * <p>La durée provient de {@code Creneau.duree}, jamais recalculée depuis les horaires —
     * invariant du moteur sur la source de vérité de la durée.</p>
     */
    public static int minutesTravaillees(List<Creneau> creneaux, ReferentielComptabiliteActivite referentiel) {
        int total = 0;
        for (Creneau creneau : creneaux) {
            if (compteDansCharge(creneau, referentiel)) {
                total += creneau.getDuree();
            }
        }
        return total;
    }

    /**
     * Amplitude d'une journée, en minutes : du début du premier créneau à la fin du dernier.
     * Délègue à la contrainte, qui gère les créneaux à cheval sur minuit.
     */
    public static int amplitudeMinutes(List<Creneau> creneauxDuJour,
                                       ReferentielComptabiliteActivite referentiel) {
        List<Creneau> comptabilises = new ArrayList<>();
        for (Creneau creneau : creneauxDuJour) {
            if (compteDansCharge(creneau, referentiel)) {
                comptabilises.add(creneau);
            }
        }
        return AmplitudeJournaliere.calculerAmplitudeMinutes(comptabilises);
    }

    /**
     * Un créneau entre-t-il dans la charge de travail ?
     * Pauses exclues, activités {@code compteDansCharge = false} exclues, activité inconnue exclue.
     */
    public static boolean compteDansCharge(Creneau creneau, ReferentielComptabiliteActivite referentiel) {
        if (Boolean.TRUE.equals(creneau.getEstSegmentDePause())) {
            return false;
        }
        ComptabiliteActivite activite = referentiel.getByCode(creneau.getCodeActiviteEffectif());
        return activite != null && activite.isCompteDansCharge();
    }

    /** Conversion en heures décimales, alignée sur {@code workMetrics.byRessource}. */
    public static double enHeures(int minutes) {
        return minutes / 60.0;
    }
}
