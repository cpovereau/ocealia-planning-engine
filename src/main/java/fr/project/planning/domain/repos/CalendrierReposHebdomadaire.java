package fr.project.planning.domain.repos;

import fr.project.planning.domain.contexte.HorizonTemporel;
import fr.project.planning.domain.creneau.Creneau;
import fr.project.planning.domain.creneau.QualificationJour;
import fr.project.planning.domain.metier.ReferentielComptabiliteActivite;
import fr.project.planning.domain.ressource.Ressource;
import fr.project.planning.domain.ressource.SalarieReel;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 * CalendrierReposHebdomadaire — quand tombe le repos de chaque salarié (lot S7.9b)
 *
 * <p>Deux sources, dans cet ordre :</p>
 * <ol>
 *   <li><strong>Déclaré</strong> — un créneau porteur du code activité de repos, transmis par
 *       l'appelant avec son {@code ressourceAffecteeId}. Le repos peut tomber n'importe quel
 *       jour de la semaine, pas seulement le week-end.</li>
 *   <li><strong>Repli</strong> — à défaut de déclaration, samedi vaut RH et dimanche vaut RHD.</li>
 * </ol>
 *
 * <h3>Le repli s'applique par salarié et par semaine</h3>
 * <p>Chaque semaine est jugée pour elle-même : une semaine sans marqueur pour ce salarié retombe
 * sur samedi et dimanche, <strong>même s'il en déclare ailleurs</strong>. La maille alternative —
 * faire confiance aux déclarations partout dès qu'il en existe une — rendrait une semaine oubliée
 * silencieusement travaillable. Un vide ne suppose jamais que la chose est possible.</p>
 *
 * <h3>Semaine calendaire</h3>
 * <p>Lundi → dimanche, comme partout ailleurs dans le moteur.</p>
 */
public final class CalendrierReposHebdomadaire {

    private CalendrierReposHebdomadaire() {
        // Utility class
    }

    /**
     * Vrai si ce créneau est un marqueur de repos plutôt qu'un travail à planifier.
     *
     * <p>Appelé à la préparation pour retirer ces créneaux du problème : ils n'ont rien à y
     * faire, ni comme variable de décision ni comme charge.</p>
     */
    public static boolean estMarqueurDeRepos(Creneau creneau, ReferentielComptabiliteActivite ref) {
        return creneau != null && ref != null
                && ref.estReposHebdomadaire(creneau.getCodeActiviteEffectif());
    }

    /**
     * Convertit les créneaux marqueurs en repos déclarés.
     *
     * <p>Un marqueur sans ressource affectée est ignoré : sans salarié, il ne dit le repos de
     * personne. C'est à l'appelant de le signaler s'il le juge anormal.</p>
     */
    public static List<ReposHebdomadaire> depuisLesMarqueurs(
            Collection<Creneau> marqueurs, ReferentielComptabiliteActivite ref) {

        List<ReposHebdomadaire> declares = new ArrayList<>();
        if (marqueurs == null || ref == null) {
            return declares;
        }
        for (Creneau marqueur : marqueurs) {
            QualificationJour nature = ref.natureRepos(marqueur.getCodeActiviteEffectif());
            Ressource ressource = marqueur.getRessourceAffectee();
            if (nature == null || marqueur.getDate() == null
                    || !(ressource instanceof SalarieReel salarie) || salarie.getId() == null) {
                continue;
            }
            declares.add(new ReposHebdomadaire(salarie.getId(), marqueur.getDate(), nature, true));
        }
        return declares;
    }

    /**
     * Calendrier complet : les repos déclarés, complétés semaine par semaine par le repli.
     *
     * @param salarieIds salariés du problème — chacun a droit à un repos, déclaré ou non
     * @param declares   repos transmis par l'appelant (voir {@link #depuisLesMarqueurs})
     * @param horizon    période de résolution ; aucune date hors horizon n'est produite
     * @return les repos du problème, dédoublonnés, dans un ordre stable
     */
    public static List<ReposHebdomadaire> construire(
            Collection<String> salarieIds,
            Collection<ReposHebdomadaire> declares,
            HorizonTemporel horizon) {

        if (salarieIds == null || salarieIds.isEmpty() || horizon == null) {
            return List.of();
        }

        // (salarié → lundi de semaine → repos déclarés cette semaine-là)
        Map<String, Map<LocalDate, Set<ReposHebdomadaire>>> parSalariePuisSemaine = new TreeMap<>();
        if (declares != null) {
            for (ReposHebdomadaire repos : declares) {
                if (!horizon.contient(repos.getDate())) {
                    continue;
                }
                parSalariePuisSemaine
                        .computeIfAbsent(repos.getSalarieId(), x -> new TreeMap<>())
                        .computeIfAbsent(lundiDeLaSemaine(repos.getDate()), x -> new LinkedHashSet<>())
                        .add(repos);
            }
        }

        List<LocalDate> lundis = lundisDeLHorizon(horizon);
        List<ReposHebdomadaire> calendrier = new ArrayList<>();

        for (String salarieId : new TreeSet<>(salarieIds)) {
            if (salarieId == null) {
                continue;
            }
            Map<LocalDate, Set<ReposHebdomadaire>> semaines =
                    parSalariePuisSemaine.getOrDefault(salarieId, Map.of());

            for (LocalDate lundi : lundis) {
                Set<ReposHebdomadaire> declaresDeLaSemaine = semaines.get(lundi);

                if (declaresDeLaSemaine != null && !declaresDeLaSemaine.isEmpty()) {
                    calendrier.addAll(declaresDeLaSemaine);
                    continue;
                }

                // Repli : samedi = RH, dimanche = RHD, dans la limite de l'horizon.
                ajouterSiDansHorizon(calendrier, salarieId, lundi.plusDays(5),
                        QualificationJour.RH, horizon);
                ajouterSiDansHorizon(calendrier, salarieId, lundi.plusDays(6),
                        QualificationJour.RHD, horizon);
            }
        }
        return calendrier;
    }

    // ---------------------------------------------------------

    private static void ajouterSiDansHorizon(List<ReposHebdomadaire> calendrier, String salarieId,
                                             LocalDate date, QualificationJour nature,
                                             HorizonTemporel horizon) {
        if (horizon.contient(date)) {
            calendrier.add(new ReposHebdomadaire(salarieId, date, nature, false));
        }
    }

    /** Lundis de toutes les semaines touchant l'horizon, y compris entamées. */
    private static List<LocalDate> lundisDeLHorizon(HorizonTemporel horizon) {
        List<LocalDate> lundis = new ArrayList<>();
        LocalDate lundi = lundiDeLaSemaine(horizon.getDateDebut());
        while (!lundi.isAfter(horizon.getDateFin())) {
            lundis.add(lundi);
            lundi = lundi.plusWeeks(1);
        }
        return lundis;
    }

    static LocalDate lundiDeLaSemaine(LocalDate date) {
        return date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
    }
}
