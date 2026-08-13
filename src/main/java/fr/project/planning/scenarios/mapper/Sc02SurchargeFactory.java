package fr.project.planning.scenarios.mapper;

import fr.project.planning.constraints.metier.SurchargeAcceptable;
import fr.project.planning.domain.contexte.SeuilsSurcharge;
import fr.project.planning.domain.creneau.Creneau;
import fr.project.planning.domain.metier.ReferentielComptabiliteActivite;
import fr.project.planning.domain.ressource.SalarieReel;
import fr.project.planning.scenarios.dto.ImpactMesureDTO;
import fr.project.planning.scenarios.dto.SurchargeDTO;
import fr.project.planning.scenarios.service.Sc06ChargeCalculator;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Sc02SurchargeFactory — ce que le remplacement change à la charge de ceux qui l'assurent.
 *
 * <h3>Ce que « avant » veut dire</h3>
 * <p>La situation qu'on aurait eue sans remplacement : le <strong>planning épinglé</strong> du
 * salarié, celui que SC-02 n'a pas touché. « Après » est l'état résolu, remplacement compris. Le
 * delta est donc exactement ce que l'absence lui a coûté.</p>
 *
 * <h3>Une ligne par salarié et par jour repris</h3>
 * <p>La mesure hebdomadaire est répétée telle quelle pour deux jours d'une même semaine : chaque
 * ligne se lit seule. Un total hebdomadaire ne se déduit pas d'une somme de journées — le
 * remplaçant peut avoir été mobilisé deux fois dans la semaine.</p>
 *
 * <p>Les mesures viennent de {@link Sc06ChargeCalculator}, comme celles de SC-06 : le périmètre —
 * segments de pause exclus, activités hors charge exclues — est celui des contraintes
 * individuelles, et il n'existe qu'à un seul endroit.</p>
 */
public final class Sc02SurchargeFactory {

    private Sc02SurchargeFactory() {
    }

    /**
     * @param creneauxResolus créneaux de la solution, recombinés
     * @param creneauxLiberes identifiants des créneaux d'origine rendus au solveur
     * @param seuils          seuils déclarés par la demande, ou {@code null}
     * @param referentiel     référentiel d'activités, pour le périmètre de la charge
     */
    public static List<SurchargeDTO> build(List<Creneau> creneauxResolus,
                                           Set<String> creneauxLiberes,
                                           SeuilsSurcharge seuils,
                                           ReferentielComptabiliteActivite referentiel) {

        Double plafondJour = seuils == null ? null : seuils.getHeuresMaximumParJour();
        Double plafondSemaine = seuils == null ? null : seuils.getHeuresMaximumParSemaine();

        List<SurchargeDTO> surcharges = new ArrayList<>();

        for (String ressourceId : remplacants(creneauxResolus, creneauxLiberes)) {

            List<Creneau> avant = creneauxDe(creneauxResolus, ressourceId, true);
            List<Creneau> apres = creneauxDe(creneauxResolus, ressourceId, false);

            for (LocalDate jour : joursRepris(creneauxResolus, creneauxLiberes, ressourceId)) {
                LocalDate lundi = SurchargeAcceptable.lundiDeLaSemaine(jour);

                surcharges.add(new SurchargeDTO(
                        ressourceId,
                        jour,
                        mesure(
                                Sc06ChargeCalculator.minutesTravaillees(
                                        Sc06ChargeCalculator.duJour(avant, jour), referentiel),
                                Sc06ChargeCalculator.minutesTravaillees(
                                        Sc06ChargeCalculator.duJour(apres, jour), referentiel),
                                plafondJour),
                        mesure(
                                Sc06ChargeCalculator.minutesTravaillees(
                                        deLaSemaine(avant, lundi), referentiel),
                                Sc06ChargeCalculator.minutesTravaillees(
                                        deLaSemaine(apres, lundi), referentiel),
                                plafondSemaine)));
            }
        }

        surcharges.sort(Comparator
                .comparing(SurchargeDTO::date)
                .thenComparing(SurchargeDTO::ressourceId));
        return surcharges;
    }

    /** Salariés réels ayant repris au moins un morceau de l'absence. */
    private static Set<String> remplacants(List<Creneau> creneauxResolus, Set<String> liberes) {
        Set<String> ids = new LinkedHashSet<>();
        for (Creneau creneau : creneauxResolus) {
            if (liberes.contains(creneau.getIdBesoin())
                    && creneau.getRessourceAffectee() instanceof SalarieReel salarie) {
                ids.add(salarie.getId());
            }
        }
        return ids;
    }

    /** Jours où ce salarié reprend quelque chose. */
    private static Set<LocalDate> joursRepris(List<Creneau> creneauxResolus, Set<String> liberes,
                                              String ressourceId) {
        Set<LocalDate> jours = new LinkedHashSet<>();
        for (Creneau creneau : creneauxResolus) {
            if (liberes.contains(creneau.getIdBesoin())
                    && creneau.getRessourceAffectee() instanceof SalarieReel salarie
                    && ressourceId.equals(salarie.getId())) {
                jours.add(creneau.getDate());
            }
        }
        return jours;
    }

    /**
     * Créneaux affectés à une ressource — les épinglés seuls pour la situation « avant »,
     * tous pour la situation « après ».
     */
    private static List<Creneau> creneauxDe(List<Creneau> creneaux, String ressourceId,
                                            boolean epinglesSeulement) {
        List<Creneau> resultat = new ArrayList<>();
        for (Creneau creneau : creneaux) {
            if (creneau.getRessourceAffectee() != null
                    && ressourceId.equals(creneau.getRessourceAffectee().getId())
                    && (!epinglesSeulement || creneau.isFige())) {
                resultat.add(creneau);
            }
        }
        return resultat;
    }

    private static List<Creneau> deLaSemaine(List<Creneau> creneaux, LocalDate lundi) {
        List<Creneau> resultat = new ArrayList<>();
        for (Creneau creneau : creneaux) {
            if (lundi.equals(SurchargeAcceptable.lundiDeLaSemaine(creneau.getDate()))) {
                resultat.add(creneau);
            }
        }
        return resultat;
    }

    private static ImpactMesureDTO mesure(int minutesAvant, int minutesApres, Double plafondHeures) {
        return new ImpactMesureDTO(
                Sc06ChargeCalculator.enHeures(minutesAvant),
                Sc06ChargeCalculator.enHeures(minutesApres),
                plafondHeures);
    }
}
