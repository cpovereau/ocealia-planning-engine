package fr.project.planning.scenarios.service;

import fr.project.planning.constraints.legales.AmplitudeJournaliere;
import fr.project.planning.domain.contexte.CoefficientsPenibilite;
import fr.project.planning.domain.contexte.DominancePenibilites;
import fr.project.planning.domain.creneau.Creneau;
import fr.project.planning.domain.metier.ComptabiliteActivite;
import fr.project.planning.domain.metier.ReferentielComptabiliteActivite;
import fr.project.planning.domain.reglementaire.RegulatoryParameters;
import fr.project.planning.domain.ressource.Ressource;
import fr.project.planning.time.RepartitionPenibilites;
import fr.project.planning.time.TimeBreakdownCalculator;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

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

    /**
     * [Équité L4] Minutes ramenées à l'unité de l'heure ordinaire.
     *
     * <p>La grandeur que compare le palier d'équité : <em>on ne juge l'équité qu'à pénibilité
     * équivalente</em>. Chaque minute est pondérée par le coefficient de sa seule catégorie —
     * celle que la dominance retient — de sorte qu'une nuit du dimanche n'est jamais comptée deux
     * fois.</p>
     *
     * <p>La répartition et la pondération viennent des mêmes classes que
     * {@code workMetrics.heuresPonderees} : le classement de SC-06 et la mesure restituée disent
     * la même chose, ou le rang contredirait la réponse qui le justifie.</p>
     *
     * <p>Sans coefficients transmis — le cas de toutes les demandes jusqu'à leur calibration — la
     * pondération est neutre et cette grandeur vaut exactement les minutes travaillées.</p>
     */
    public static double minutesPonderees(List<Creneau> creneaux, PreparedSc06Scenario prepared) {
        TimeBreakdownCalculator decoupage = new TimeBreakdownCalculator();
        RegulatoryParameters reglementaire = prepared.problem().getRegulatoryParameters();
        DominancePenibilites dominance =
                prepared.problem().getPlanningContext().getDominancePenibilites();
        CoefficientsPenibilite coefficients =
                prepared.problem().getPlanningContext().getCoefficientsPenibilite();

        double total = 0.0;
        for (Creneau creneau : creneaux) {
            if (!compteDansCharge(creneau, prepared.referentiel())) {
                continue;
            }
            total += RepartitionPenibilites
                    .de(decoupage.compute(creneau, reglementaire, true), dominance)
                    .minutesPondereesPar(coefficients);
        }
        return total;
    }

    /**
     * [Équité L4] Longueur de la série de jours travaillés d'affilée qui contient {@code jour}.
     *
     * <h3>Pourquoi cette série-là, et pas le maximum de la fenêtre</h3>
     * <p>Le critère métier est <em>ne pas rappeler qui enchaîne</em>. Ce qui compte est donc la
     * série à laquelle le besoin rattacherait la personne, pas une série plus longue survenue
     * ailleurs dans la fenêtre : celle-là est acquise, et aucun choix ne la change. Mesurer le
     * maximum ferait perdre le candidat pour un enchaînement dont il n'est pas question.</p>
     *
     * @return 0 si la personne ne travaille pas ce jour-là — auquel cas il n'y a pas de série à
     *         prolonger
     */
    public static int joursConsecutifsAutour(List<Creneau> creneaux,
                                             ReferentielComptabiliteActivite referentiel,
                                             LocalDate jour) {
        Set<LocalDate> travailles = new HashSet<>();
        for (Creneau creneau : creneaux) {
            if (compteDansCharge(creneau, referentiel)) {
                travailles.add(creneau.getDate());
            }
        }
        if (!travailles.contains(jour)) {
            return 0;
        }

        int serie = 1;
        for (LocalDate avant = jour.minusDays(1); travailles.contains(avant); avant = avant.minusDays(1)) {
            serie++;
        }
        for (LocalDate apres = jour.plusDays(1); travailles.contains(apres); apres = apres.plusDays(1)) {
            serie++;
        }
        return serie;
    }

    /** Conversion en heures décimales, alignée sur {@code workMetrics.byRessource}. */
    public static double enHeures(int minutes) {
        return minutes / 60.0;
    }
}
