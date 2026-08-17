package fr.project.planning.domain.workmetrics;

import fr.project.planning.domain.contexte.CoefficientsPenibilite;
import fr.project.planning.domain.contexte.HorizonTemporel;
import fr.project.planning.domain.creneau.Creneau;
import fr.project.planning.domain.creneau.TypePlageHoraire;
import fr.project.planning.domain.metier.ComptabiliteActivite;
import fr.project.planning.domain.metier.ReferentielComptabiliteActivite;
import fr.project.planning.domain.reglementaire.RegulatoryParameters;
import fr.project.planning.domain.repos.ReposHebdomadaire;
import fr.project.planning.domain.ressource.Ressource;
import fr.project.planning.domain.ressource.SalarieReel;
import fr.project.planning.solution.PlanningProblem;
import fr.project.planning.scoring.PenibiliteType;
import fr.project.planning.time.RepartitionPenibilites;
import fr.project.planning.time.TimeBreakdown;
import fr.project.planning.time.TimeBreakdownCalculator;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.*;

public class WorkMetricsCalculator {

    public Map<Ressource, WorkMetrics> compute(PlanningProblem solution) {

        ReferentielComptabiliteActivite ref = solution.getReferentielComptabiliteActivite();
        HorizonTemporel horizon = solution.getPlanningContext().getHorizonTemporel();
        RegulatoryParameters rp = solution.getRegulatoryParameters();

        TimeBreakdownCalculator tbc = new TimeBreakdownCalculator();

        Map<String, WorkMetrics> metricsParRessourceId = new HashMap<>();
        Map<String, Ressource> ressourceParId = new HashMap<>();

        Map<String, Set<LocalDate>> rhdParRessourceId = new HashMap<>();
        Map<String, Set<LocalDate>> detteReposParRessourceId = new HashMap<>();

        /*
         * [S7.9c] Jours de repos hebdomadaire, par salarié.
         *
         * Le calendrier du problème fait foi — repos déclarés par l'appelant, complétés par le
         * repli samedi/dimanche. Ce calcul lisait auparavant le jour de la semaine, ce qui le
         * faisait diverger de DetteReposSurReposHebdomadaire dès qu'un salarié se reposait un
         * autre jour : la métrique et la contrainte annonçaient la même règle et n'en
         * appliquaient pas la même.
         */
        Map<String, Set<LocalDate>> reposParRessourceId = new HashMap<>();
        for (ReposHebdomadaire repos : solution.getReposHebdomadaires()) {
            reposParRessourceId
                    .computeIfAbsent(repos.getSalarieId(), x -> new HashSet<>())
                    .add(repos.getDate());
        }

        Map<String, Set<LocalDate>> joursTravaillesParRessourceId = new HashMap<>();
        Map<String, Set<LocalDate>> nuitsTravailleesParRessourceId = new HashMap<>();

        /*
         * Initialisation avec les ressources connues
         */
        for (Ressource r : solution.getRessources()) {
            String id = r.getId();
            metricsParRessourceId.putIfAbsent(id, new WorkMetrics(id));
            ressourceParId.putIfAbsent(id, r);
        }

        /*
         * Analyse des créneaux
         */
        for (Creneau c : solution.getCreneaux()) {

            Ressource r = c.getRessourceAffectee();
            if (r == null) continue;

            String ressourceId = r.getId();
            if (ressourceId == null) continue;

            if (!horizon.contient(c.getDate())) continue;

            /*
             * Sécurisation ressource (dataset imparfait possible)
             */
            metricsParRessourceId.computeIfAbsent(ressourceId, WorkMetrics::new);
            ressourceParId.putIfAbsent(ressourceId, r);

            /*
             * Résolution activité
             */
            String codeActivite = c.getCodeActiviteEffectif();

            /*
             * Sécurisation dataset : si aucune clé activité exploitable
             */
            if (codeActivite == null) {
                // activité non exploitable → créneau neutre
                continue;
            }

           ComptabiliteActivite ca = (ref != null) ? ref.getByCode(codeActivite) : null;

            if (ca == null) {
                continue;
            }

            boolean compteDansCharge = ca.isCompteDansCharge();

            WorkMetrics wm = metricsParRessourceId.get(ressourceId);

            TimeBreakdown breakdown = tbc.compute(c, rp, compteDansCharge);

            int minutesTravaillees = Math.toIntExact(breakdown.minutesTravaillees());
            int minutesNuit = Math.toIntExact(breakdown.minutesNuit());
            int minutesFerie = Math.toIntExact(breakdown.minutesFerie());

            /*
             * Travail total
             */
            if (compteDansCharge) {

                wm.addTravail(minutesTravaillees);

                joursTravaillesParRessourceId
                        .computeIfAbsent(ressourceId, x -> new HashSet<>())
                        .add(c.getDate());

                /*
                 * [Équité L1] Répartition par dominance, puis pondération.
                 *
                 * Une minute n'appartient qu'à une catégorie — la dominante — de sorte qu'une
                 * nuit du dimanche n'est jamais pondérée deux fois. C'est le même calcul que
                 * celui du score, et c'est la même classe qui le fait : deux implémentations de
                 * la dominance auraient fini par diverger.
                 */
                RepartitionPenibilites repartition = RepartitionPenibilites.de(
                        breakdown, solution.getPlanningContext().getDominancePenibilites());

                CoefficientsPenibilite coefficients =
                        solution.getPlanningContext().getCoefficientsPenibilite();

                wm.addPenibilites(
                        Math.toIntExact(repartition.minutes(PenibiliteType.NUIT)),
                        Math.toIntExact(repartition.minutes(PenibiliteType.DIMANCHE)),
                        Math.toIntExact(repartition.minutes(PenibiliteType.FERIE)),
                        Math.toIntExact(repartition.minutesOrdinaires()),
                        // [Équité L3] Pondérer se dit à un seul endroit : le harnais de calibration
                        // rejoue cette mesure-là, pas une réécriture qui lui ressemble.
                        repartition.minutesPondereesPar(coefficients));
            }

            /*
             * Travail de nuit
             */
            if (compteDansCharge && minutesNuit > 0) {

                wm.addNuit(minutesNuit);

                nuitsTravailleesParRessourceId
                        .computeIfAbsent(ressourceId, x -> new HashSet<>())
                        .add(c.getDate());
            }

            /*
             * Jour férié
             */
            if (compteDansCharge && minutesFerie > 0) {
                wm.addJourFerie(minutesFerie);
            }

            DayOfWeek dow = c.getDate().getDayOfWeek();

            /*
             * [S7.9c] Repos hebdomadaire travaillé — heures posées un dimanche *calendaire*.
             *
             * Indicateur RH d'observation, volontairement indépendant du calendrier de repos
             * individuel : il doit rester comparable entre salariés et entre clients, y compris
             * quand aucun repos n'est déclaré. Le dimanche est un fait de calendrier ; le repos
             * d'une personne est une déclaration.
             *
             * Même maille que nbDimanchesTravailles ci-dessous — l'un compte les heures, l'autre
             * les jours.
             */
            if (dow == DayOfWeek.SUNDAY && compteDansCharge) {

                wm.addReposHebdoTravaille(minutesTravaillees);

                rhdParRessourceId
                        .computeIfAbsent(ressourceId, x -> new HashSet<>())
                        .add(c.getDate());
            }

            /*
             * [S7.9c] Dette de repos — jours où ce salarié-là a travaillé *son* repos avec une
             * activité ouvrant une dette compensatoire.
             *
             * Contrepartie observée de DetteReposSurReposHebdomadaire : les deux lisent le même
             * calendrier, sans quoi la métrique annoncerait une règle que le score n'applique
             * pas. Un repos hebdomadaire peut tomber n'importe quel jour de la semaine.
             */
            boolean estJourDeRepos = reposParRessourceId
                    .getOrDefault(ressourceId, Set.of())
                    .contains(c.getDate());

            if (estJourDeRepos && compteDansCharge && ca.isGenereDetteRepos()) {

                detteReposParRessourceId
                        .computeIfAbsent(ressourceId, x -> new HashSet<>())
                        .add(c.getDate());
            }

            /*
             * Phase 8 — inadéquation nuit :
             * créneau de nuit affecté à un salarié non déclaré travailleur de nuit.
             */
            if (compteDansCharge
                    && c.getTypePlageHoraire() == TypePlageHoraire.NUIT
                    && r instanceof SalarieReel sr
                    && !sr.estTravailleurDeNuit()) {
                wm.incNuitNonNuit();
            }
        }

        /*
         * Finalisation métriques V2
         */
        for (Map.Entry<String, WorkMetrics> entry : metricsParRessourceId.entrySet()) {

            String ressourceId = entry.getKey();
            WorkMetrics wm = entry.getValue();

            Set<LocalDate> dimanches = rhdParRessourceId.getOrDefault(ressourceId, Set.of());
            for (int i = 0; i < dimanches.size(); i++) {
                wm.incDimancheTravaille();
            }

            Set<LocalDate> dettes = detteReposParRessourceId.getOrDefault(ressourceId, Set.of());
            for (int i = 0; i < dettes.size(); i++) {
                wm.incReposHebdoDetteRepos();
            }
        }

        /*
         * Finalisation séquences observées (V3-B)
         */
        for (Map.Entry<String, WorkMetrics> entry : metricsParRessourceId.entrySet()) {

            String ressourceId = entry.getKey();
            WorkMetrics wm = entry.getValue();

            Set<LocalDate> jours = joursTravaillesParRessourceId.getOrDefault(ressourceId, Set.of());
            Set<LocalDate> nuits = nuitsTravailleesParRessourceId.getOrDefault(ressourceId, Set.of());

            wm.setMaxJoursConsecutifsObservees(longestConsecutiveDates(jours));
            wm.setMaxNuitsConsecutivesObservees(longestConsecutiveDates(nuits));
        }

        /*
         * Projection finale Ressource -> WorkMetrics
         */
        Map<Ressource, WorkMetrics> result = new HashMap<>();

        for (Map.Entry<String, WorkMetrics> entry : metricsParRessourceId.entrySet()) {

            Ressource r = ressourceParId.get(entry.getKey());

            if (r != null) {
                result.put(r, entry.getValue());
            }
        }

        /*
         * [Équité L2] Seconde passe : ce que chacun fait, rapporté à ce qu'il doit.
         *
         * Séparée de l'accumulation, comme 40_WORKMETRICS.md §5.1.5 le prévoyait : une mesure
         * rapportée au contrat suppose que tout ait été compté. Le poste virtuel en est exclu —
         * il ne porte pas de contrat, et n'a donc rien à quoi être comparé.
         */
        for (Map.Entry<Ressource, WorkMetrics> entry : result.entrySet()) {
            WorkMetrics wm = entry.getValue();

            /*
             * [Rang 14] La fenêtre est celle du problème, mais les jours observés sont ceux de
             * CHACUN : l'horizon dit sur quoi le moteur a jugé, les indisponibilités déclarées
             * disent quand la personne pouvait travailler. Compter l'absence comme du temps
             * disponible ferait lire un retour de congé comme une sous-charge, et le moteur
             * rattraperait le congé au lieu de le respecter.
             *
             * Un poste virtuel ne porte pas de contrat : rien n'est comparable pour lui, et il
             * garde la largeur brute de la fenêtre — elle ne dit alors que ce qui a été vu.
             */
            long joursDisponibles = entry.getKey() instanceof SalarieReel
                    ? JoursDisponibles.pour(entry.getKey().getId(), horizon,
                            solution.getIndisponibilites())
                    : EcartAuContrat.joursObserves(horizon);

            Double attendues = entry.getKey() instanceof SalarieReel salarie
                    ? EcartAuContrat.minutesAttendues(salarie.getContrat(), joursDisponibles)
                    : null;

            wm.setMesuresContractuelles(
                    (int) joursDisponibles,
                    EcartAuContrat.ecartPourcent(wm.getMinutesPonderees(), attendues),
                    EcartAuContrat.pourcentageDuContrat(wm.getMinutesNuitDominante(), attendues),
                    EcartAuContrat.pourcentageDuContrat(wm.getMinutesDimancheDominante(), attendues),
                    EcartAuContrat.pourcentageDuContrat(wm.getMinutesFerieDominante(), attendues));
        }

        return result;
    }

    private static int longestConsecutiveDates(Set<LocalDate> dates) {

        if (dates == null || dates.isEmpty()) return 0;

        List<LocalDate> sorted = new ArrayList<>(dates);
        Collections.sort(sorted);

        int best = 1;
        int run = 1;

        for (int i = 1; i < sorted.size(); i++) {

            LocalDate prev = sorted.get(i - 1);
            LocalDate cur = sorted.get(i);

            if (cur.equals(prev.plusDays(1))) {
                run++;
                best = Math.max(best, run);
            } else {
                run = 1;
            }
        }

        return best;
    }
}