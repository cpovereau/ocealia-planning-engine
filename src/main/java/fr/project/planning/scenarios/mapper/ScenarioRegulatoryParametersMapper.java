package fr.project.planning.scenarios.mapper;

import fr.project.planning.domain.reglementaire.RegulatoryParameters;
import fr.project.planning.scenarios.dto.RegulatoryParametersDTO;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

/**
 * ScenarioRegulatoryParametersMapper — d'où viennent le cadre de nuit et les jours fériés (S8.0)
 *
 * <p>Point unique où se décide la précédence entre ce que l'appelant <strong>déclare</strong> et
 * ce que le moteur <strong>déduit</strong>. Les trois scénarios passent par ici : la règle ne
 * peut pas diverger de l'un à l'autre.</p>
 *
 * <h3>Précédence</h3>
 * <ul>
 *   <li><strong>Plage de nuit</strong> — déclarée si les deux bornes le sont, sinon la plage
 *       légale par défaut 22:00–06:00. Une borne seule est ignorée et tracée : mélanger une
 *       borne déclarée avec une borne par défaut produirait un intervalle que personne n'a
 *       voulu.</li>
 *   <li><strong>Jours fériés</strong> — le calendrier déclaré fait autorité dès qu'il est
 *       <em>présent</em>, fût-il vide : une liste vide dit « aucun jour férié sur la période ».
 *       C'est son <em>absence</em> qui laisse le moteur déduire, depuis {@code holidayDates}
 *       en SC-01 ou le drapeau {@code isJourFerie} des créneaux en SC-03 et SC-06.</li>
 * </ul>
 *
 * <h3>Ce que le calendrier déclaré corrige</h3>
 * <p>Un créneau traversant minuit ne pouvait être rattaché qu'à sa date de début : le drapeau
 * qu'il porte ne dit pas lequel des deux jours civils est férié. Un calendrier de dates lève
 * cette limite — {@code TimeBreakdownCalculator} interroge séparément le jour de début et le
 * jour suivant.</p>
 */
public class ScenarioRegulatoryParametersMapper {

    private static final Logger log =
            LoggerFactory.getLogger(ScenarioRegulatoryParametersMapper.class);

    /** Plage de nuit appliquée à défaut de déclaration. */
    static final LocalTime DEBUT_NUIT_PAR_DEFAUT = LocalTime.of(22, 0);
    static final LocalTime FIN_NUIT_PAR_DEFAUT = LocalTime.of(6, 0);

    /**
     * @param declares          bloc {@code planningContext.regulatoryParameters}, éventuellement null
     * @param joursFeriesDeduits calendrier reconstitué par le scénario, utilisé seulement si
     *                           l'appelant n'en déclare aucun
     * @param scenario           préfixe des journaux, p. ex. {@code "SC-03"}
     */
    public RegulatoryParameters toRegulatoryParameters(
            RegulatoryParametersDTO declares,
            Collection<LocalDate> joursFeriesDeduits,
            String scenario) {

        LocalTime debutNuit = DEBUT_NUIT_PAR_DEFAUT;
        LocalTime finNuit = FIN_NUIT_PAR_DEFAUT;

        if (declares != null) {
            LocalTime debutDeclare = declares.getHeureDebutNuit();
            LocalTime finDeclaree = declares.getHeureFinNuit();

            if (debutDeclare != null && finDeclaree != null) {
                if (debutDeclare.equals(finDeclaree)) {
                    log.warn("[{}] regulatoryParameters : heureDebutNuit et heureFinNuit sont "
                            + "identiques ({}) — la plage est vide, plage par défaut {}–{} appliquée",
                            scenario, debutDeclare, DEBUT_NUIT_PAR_DEFAUT, FIN_NUIT_PAR_DEFAUT);
                } else {
                    debutNuit = debutDeclare;
                    finNuit = finDeclaree;
                }
            } else if (debutDeclare != null || finDeclaree != null) {
                log.warn("[{}] regulatoryParameters : plage de nuit incomplète "
                        + "(heureDebutNuit={}, heureFinNuit={}) — une borne seule ne décrit aucune "
                        + "plage, plage par défaut {}–{} appliquée",
                        scenario, debutDeclare, finDeclaree,
                        DEBUT_NUIT_PAR_DEFAUT, FIN_NUIT_PAR_DEFAUT);
            }
        }

        Set<LocalDate> joursFeries = resoudreJoursFeries(declares, joursFeriesDeduits, scenario);

        return new RegulatoryParameters(debutNuit, finNuit, List.copyOf(joursFeries));
    }

    private Set<LocalDate> resoudreJoursFeries(
            RegulatoryParametersDTO declares,
            Collection<LocalDate> joursFeriesDeduits,
            String scenario) {

        Set<LocalDate> deduits = new TreeSet<>();
        if (joursFeriesDeduits != null) {
            joursFeriesDeduits.stream().filter(java.util.Objects::nonNull).forEach(deduits::add);
        }

        if (declares == null || declares.getJoursFeries() == null) {
            return deduits;
        }

        Set<LocalDate> declaresDates = new TreeSet<>();
        declares.getJoursFeries().stream().filter(java.util.Objects::nonNull).forEach(declaresDates::add);

        if (!deduits.isEmpty() && !declaresDates.equals(deduits)) {
            // Signalé, jamais fusionné : le calendrier réglementaire fait autorité, et une
            // fusion silencieuse rendrait fériée une date que l'appelant n'a pas retenue.
            Set<LocalDate> seulementDeduites = new TreeSet<>(deduits);
            seulementDeduites.removeAll(declaresDates);

            log.warn("[{}] regulatoryParameters.joursFeries fait autorité et diverge de ce que le "
                    + "dataset déclare — {} date(s) marquée(s) fériée(s) dans le dataset mais absente(s) "
                    + "du calendrier : {}", scenario, seulementDeduites.size(), seulementDeduites);
        }
        return declaresDates;
    }
}
