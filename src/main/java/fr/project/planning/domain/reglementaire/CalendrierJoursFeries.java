package fr.project.planning.domain.reglementaire;

import fr.project.planning.domain.creneau.Creneau;

import java.time.LocalDate;
import java.util.Collection;
import java.util.Set;
import java.util.TreeSet;

/**
 * CalendrierJoursFeries — d'où le moteur apprend qu'une date est fériée (lot S7.9)
 *
 * <p>Le férié est une propriété de la <strong>date</strong>, pas du créneau : s'il est férié
 * pour l'un, il l'est pour tous. Cette classe convertit donc les déclarations portées créneau
 * par créneau en un calendrier de dates, seule forme que
 * {@link RegulatoryParameters#estJourFerie(LocalDate)} sache interroger.</p>
 *
 * <h3>Deux sources selon le scénario</h3>
 * <ul>
 *   <li><strong>SC-01</strong> transmet directement le calendrier, via
 *       {@code scenarioParameters.holidayDates}. Aucune conversion nécessaire.</li>
 *   <li><strong>SC-03 et SC-06</strong> ne transmettent pas de calendrier : la seule
 *       information disponible est le drapeau {@code isJourFerie} porté par chaque créneau
 *       entrant. C'est cette déclaration que reconstruit {@link #declaresParLesCreneaux}.</li>
 * </ul>
 *
 * <h3>Un seul créneau suffit à qualifier la journée</h3>
 * <p>Marquer un seul créneau d'une date suffit : la date entière devient fériée, pour tous les
 * salariés. Une déclaration partielle — fréquente quand le planning amont est composé de
 * plusieurs sources — est donc réparée plutôt que subie.</p>
 *
 * <h3>Limite assumée du drapeau porté par le créneau</h3>
 * <p>Un créneau qui traverse minuit n'est rattaché qu'à sa <strong>date de début</strong> : le
 * drapeau ne dit pas lequel des deux jours civils est férié, et le moteur retient celui où le
 * créneau commence. Un créneau 22:00–06:00 déclaré férié verra donc ses seules minutes d'avant
 * minuit valorisées. La levée de cette limite passe par un calendrier transmis au contrat, à
 * l'emplacement {@code planningContext.regulatoryParameters} déjà prévu par la spécification
 * d'interface mais jamais implémenté.</p>
 */
public final class CalendrierJoursFeries {

    private CalendrierJoursFeries() {
        // Utility class
    }

    /**
     * Dates déclarées fériées par au moins un créneau.
     *
     * @param creneaux créneaux du problème, figés comme variables de décision
     * @return les dates concernées, triées ; jamais {@code null}
     */
    public static Set<LocalDate> declaresParLesCreneaux(Collection<Creneau> creneaux) {
        Set<LocalDate> dates = new TreeSet<>();
        if (creneaux == null) {
            return dates;
        }
        for (Creneau creneau : creneaux) {
            if (creneau != null && creneau.isJourFerie() && creneau.getDate() != null) {
                dates.add(creneau.getDate());
            }
        }
        return dates;
    }
}
