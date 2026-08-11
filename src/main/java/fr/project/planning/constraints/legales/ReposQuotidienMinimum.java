package fr.project.planning.constraints.legales;

import fr.project.planning.domain.creneau.Creneau;
import fr.project.planning.domain.metier.ComptabiliteActivite;
import fr.project.planning.domain.metier.ReferentielComptabiliteActivite;
import fr.project.planning.domain.ressource.SalarieReel;
import fr.project.planning.scoring.PenaliteKey;

import org.optaplanner.core.api.score.buildin.hardsoft.HardSoftScore;
import org.optaplanner.core.api.score.stream.Constraint;
import org.optaplanner.core.api.score.stream.ConstraintCollectors;
import org.optaplanner.core.api.score.stream.ConstraintFactory;
import org.optaplanner.core.api.score.stream.Joiners;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;

/**
 * ReposQuotidienMinimum — contrainte SOFT (lot S3, scénario SC-06)
 *
 * <p>Pénalise un repos insuffisant entre deux journées travaillées successives d'un même
 * salarié. Le repos est mesuré entre la <strong>fin de la dernière activité</strong> d'une
 * journée travaillée et le <strong>début de la première activité</strong> de la journée
 * travaillée suivante.</p>
 *
 * <h3>Journées travaillées successives, pas jours calendaires consécutifs</h3>
 * <p>La comparaison porte sur les journées <em>où le salarié travaille effectivement</em>,
 * prises dans l'ordre chronologique. Un jour de repos intercalé n'interrompt pas le calcul :
 * il produit simplement un repos long, donc sans déficit. Il n'y a donc pas lieu de tester
 * l'adjacence des dates.</p>
 *
 * <h3>Créneaux à cheval sur minuit</h3>
 * <p>Un créneau dont l'heure de fin n'est pas postérieure à son heure de début se termine le
 * lendemain (convention du contrat d'entrée : {@code date} = jour de début). Sa fin réelle est
 * donc reportée de 24 h, ce qui raccourcit d'autant le repos qui suit — c'est le cas d'usage
 * principal de cette règle.</p>
 *
 * <h3>Périmètre du calcul</h3>
 * <p>Identique aux autres contraintes individuelles (voir {@link AmplitudeJournaliere} et
 * {@link HeuresMinimumParJour}) : les segments de pause sont exclus, ainsi que les activités
 * dont {@code compteDansCharge = false}.</p>
 *
 * <h3>Activation</h3>
 * <p>Inactive si {@code ContraintesReglementairesSalarie.reposQuotidienMinimum == null}.
 * Une valeur {@code 0} activerait la contrainte avec un seuil nul — le contrat d'entrée impose
 * d'<strong>omettre le champ</strong> pour désactiver la règle, jamais d'envoyer 0.
 * Voir {@code 92_cadrage_scenario_sc-06.md} §4.7.</p>
 *
 * <h3>Pénalité</h3>
 * <p>{@code PENALITE_REPOS_QUOTIDIEN} × minutes de déficit, cumulées sur toutes les transitions
 * en défaut. Un chevauchement de créneaux produit un repos négatif, donc un déficit supérieur au
 * seuil : c'est voulu, et le chevauchement reste par ailleurs sanctionné en HARD par
 * {@code LimitePhysique.pasDeChevauchement}.</p>
 */
public class ReposQuotidienMinimum {

    private ReposQuotidienMinimum() {
        // Utility class
    }

    /**
     * Pénalité par minute de repos manquante.
     *
     * <p>Fixée au double de {@code penaliteAmplitude} (50) : le dépassement d'amplitude est une
     * borne de confort, le repos quotidien insuffisant est une borne légale.</p>
     *
     * <p>Constante locale, comme dans {@link HeuresMinimumParJour}. L'externalisation vers
     * {@code Penalites} n'a d'intérêt que si ce poids doit être calibré indépendamment — ce que
     * l'activation progressive des règles (lot S7) déterminera.</p>
     */
    private static final int PENALITE_REPOS_QUOTIDIEN = 100;

    public static Constraint reposQuotidienMinimum(ConstraintFactory factory) {

        return factory
            // 1) Salariés réels avec un repos quotidien minimum configuré
            .forEach(SalarieReel.class)
            .filter(s -> s.getContraintesReglementaires() != null
                    && s.getContraintesReglementaires().getReposQuotidienMinimum() != null)

            // 2) Jointure avec les créneaux affectés à ce salarié, pauses exclues
            .join(
                factory.forEach(Creneau.class)
                    .filter(c -> c.getRessourceAffectee() != null)
                    .filter(c -> !Boolean.TRUE.equals(c.getEstSegmentDePause())),
                Joiners.equal(
                    SalarieReel::getId,
                    c -> c.getRessourceAffectee().getId()
                )
            )

            // 3) Filtre : uniquement les créneaux travaillés (compteDansCharge = true)
            //    ifExists évite le cross join : vérifie l'existence sans produit cartésien
            .ifExists(
                ReferentielComptabiliteActivite.class,
                Joiners.filtering((salarie, creneau, ref) -> {
                    ComptabiliteActivite ca = ref.getByCode(creneau.getCodeActiviteEffectif());
                    return ca != null && ca.isCompteDansCharge();
                })
            )

            // 4) Groupement par salarié — le calcul est chronologique, il exige la vue d'ensemble
            .groupBy(
                (salarie, creneau) -> salarie,
                ConstraintCollectors.toList((salarie, creneau) -> creneau)
            )

            // 5) Pénalité SOFT proportionnelle au total des minutes de repos manquantes
            .penalize(
                HardSoftScore.ONE_SOFT,
                (salarie, creneaux) -> {
                    int seuilMinutes = (int)(
                        salarie.getContraintesReglementaires().getReposQuotidienMinimum() * 60
                    );
                    int deficit = totalDeficitMinutes(creneaux, seuilMinutes);
                    return deficit > 0 ? PENALITE_REPOS_QUOTIDIEN * deficit : 0;
                }
            )
            .asConstraint(PenaliteKey.LEGAL_SOFT_REPOS_QUOTIDIEN_MINIMUM.name());
    }

    /**
     * Somme, sur toutes les transitions entre journées travaillées successives, des minutes de
     * repos manquantes par rapport au seuil.
     *
     * <p>Les créneaux sont d'abord réduits, par date, à un couple (début le plus précoce, fin la
     * plus tardive). Les dates sont ensuite parcourues dans l'ordre chronologique.</p>
     *
     * @param creneaux     créneaux travaillés du salarié, toutes dates confondues
     * @param seuilMinutes repos minimum exigé, en minutes
     * @return total des minutes manquantes ; 0 si aucune transition n'est en défaut
     */
    static int totalDeficitMinutes(List<Creneau> creneaux, int seuilMinutes) {
        if (creneaux.size() < 2) return 0;

        // TreeMap : les dates sont naturellement ordonnées, sans tri explicite
        TreeMap<LocalDate, LocalDateTime> debutParJour = new TreeMap<>();
        TreeMap<LocalDate, LocalDateTime> finParJour = new TreeMap<>();

        for (Creneau c : creneaux) {
            LocalDate date = c.getDate();

            LocalDateTime debut = LocalDateTime.of(date, c.getHeureDebut());
            LocalDateTime fin = LocalDateTime.of(date, c.getHeureFin());
            if (!fin.isAfter(debut)) {
                fin = fin.plusDays(1); // créneau à cheval sur minuit
            }

            debutParJour.merge(date, debut, (a, b) -> a.isBefore(b) ? a : b);
            finParJour.merge(date, fin, (a, b) -> a.isAfter(b) ? a : b);
        }

        List<LocalDate> journeesTravaillees = new ArrayList<>(debutParJour.keySet());

        int total = 0;
        for (int i = 0; i < journeesTravaillees.size() - 1; i++) {
            LocalDateTime finJournee = finParJour.get(journeesTravaillees.get(i));
            LocalDateTime debutSuivante = debutParJour.get(journeesTravaillees.get(i + 1));

            long reposMinutes = Duration.between(finJournee, debutSuivante).toMinutes();
            if (reposMinutes < seuilMinutes) {
                total += (int)(seuilMinutes - reposMinutes);
            }
        }

        return total;
    }
}
