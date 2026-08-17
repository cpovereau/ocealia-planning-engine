package fr.project.planning.domain.workmetrics;

import fr.project.planning.domain.contexte.HorizonTemporel;
import fr.project.planning.domain.ressource.Indisponibilite;

import java.time.LocalDate;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

/**
 * JoursDisponibles — les jours de la fenêtre où le salarié pouvait travailler.
 *
 * <h3>Le défaut que cette classe corrige (rang 14)</h3>
 * <p>La mesure rapportait ce que chacun fait à ce qu'il doit <strong>sur l'horizon entier</strong>,
 * identique pour tout le monde. Une absence n'y était donc pas lue comme une absence : elle était
 * lue comme du <em>temps disponible non travaillé</em>. Le salarié revenant de congé apparaissait
 * sous son contrat, donc préférable, et le moteur rattrapait l'absence de part et d'autre.</p>
 *
 * <p>Le moteur ne plaçait déjà aucun créneau dans un congé — la contrainte HARD
 * {@code METIER_HARD_INDISPONIBILITE} y veille depuis l'origine. Mais <strong>on n'optimise pas un
 * planning en annulant les congés</strong>, et compenser une absence est une manière de l'annuler.
 * Le projet tient qu'« un vide ne suppose jamais que la chose est possible » : la contrainte
 * l'honorait, la mesure lisait le vide comme une disponibilité.</p>
 *
 * <h3>Ce qui est déduit, et ce qui ne l'est pas</h3>
 * <p>Seule l'<strong>indisponibilité déclarée</strong> est déduite — le bloc {@code
 * indisponibilites}, que le schéma d'entrée désigne comme source unique de l'absence. Un jour sans
 * créneau n'est pas déduit : ne rien faire un jour ouvré n'est pas être absent, c'est précisément
 * ce que l'équité doit voir.</p>
 *
 * <p>Les périodes qui se chevauchent ne comptent qu'une fois — l'union des jours couverts, non leur
 * somme — et une période débordant l'horizon n'est comptée que sur sa partie visible : le moteur ne
 * juge que sur la fenêtre qu'on lui a transmise.</p>
 */
public final class JoursDisponibles {

    private JoursDisponibles() {
    }

    /**
     * Jours de la fenêtre où la ressource n'était pas déclarée indisponible, bornes comprises.
     *
     * <p>Vaut {@code 0} si l'absence couvre toute la fenêtre. Ce cas n'est pas un salarié à
     * −100 % : c'est un salarié dont <strong>rien n'est comparable</strong>, et les appelants
     * doivent le traiter comme tel plutôt que le désigner comme le plus sous-chargé de tous.</p>
     *
     * @param ressourceId      la ressource concernée ; {@code null} ne déduit rien
     * @param indisponibilites toutes les indisponibilités du problème, tous salariés confondus
     */
    public static long pour(String ressourceId, HorizonTemporel horizon,
                            Collection<Indisponibilite> indisponibilites) {

        long joursDeLaFenetre = EcartAuContrat.joursObserves(horizon);
        if (ressourceId == null || indisponibilites == null || indisponibilites.isEmpty()) {
            return joursDeLaFenetre;
        }

        Set<LocalDate> couverts = new HashSet<>();
        for (Indisponibilite indisponibilite : indisponibilites) {
            if (indisponibilite == null
                    || !ressourceId.equals(indisponibilite.getRessourceId())
                    || indisponibilite.getDateDebut() == null
                    || indisponibilite.getDateFin() == null) {
                continue;
            }
            /*
             * Bornes ramenées à la fenêtre : une absence de trois mois dont l'horizon ne voit
             * qu'une semaine ne retire qu'une semaine. Le moteur ne juge pas hors de ce qu'il a
             * reçu — même doctrine que joursObserves lui-même.
             */
            LocalDate debut = max(indisponibilite.getDateDebut(), horizon.getDateDebut());
            LocalDate fin = min(indisponibilite.getDateFin(), horizon.getDateFin());

            for (LocalDate jour = debut; !jour.isAfter(fin); jour = jour.plusDays(1)) {
                couverts.add(jour);
            }
        }
        return Math.max(0, joursDeLaFenetre - couverts.size());
    }

    private static LocalDate max(LocalDate a, LocalDate b) {
        return a.isAfter(b) ? a : b;
    }

    private static LocalDate min(LocalDate a, LocalDate b) {
        return a.isBefore(b) ? a : b;
    }
}
