package fr.project.planning.constraints.metier;

import fr.project.planning.domain.contexte.PlanningContext;
import fr.project.planning.domain.creneau.Creneau;
import fr.project.planning.domain.metier.ComptabiliteActivite;
import fr.project.planning.domain.metier.ReferentielComptabiliteActivite;
import fr.project.planning.domain.repos.ReposHebdomadaire;
import fr.project.planning.domain.ressource.SalarieReel;
import fr.project.planning.scoring.PenaliteKey;

import org.optaplanner.core.api.score.buildin.hardsoft.HardSoftScore;
import org.optaplanner.core.api.score.stream.Constraint;
import org.optaplanner.core.api.score.stream.ConstraintFactory;
import org.optaplanner.core.api.score.stream.Joiners;

/**
 * DetteReposSurReposHebdomadaire — travailler le jour de son repos hebdomadaire (SOFT)
 *
 * <p>Pénalise un créneau <strong>de travail</strong> posé un jour où le salarié qui le reçoit est
 * censé se reposer, lorsque l'activité génère une dette de repos compensateur. La situation est
 * autorisée : elle est pénalisée pour encourager une autre répartition.</p>
 *
 * <h3>Réécrite au lot S7.9b, parce qu'elle ne se déclenchait jamais</h3>
 * <p>La version précédente exigeait qu'un créneau soit <em>lui-même</em> qualifié
 * {@code RH}/{@code RHD} <strong>et</strong> que son activité compte dans la charge — deux
 * conditions qui ne peuvent pas être vraies ensemble, un repos n'étant pas du travail. Elle
 * lisait de surcroît {@code Creneau.qualificationJour}, champ qu'aucun mapper n'alimente : tous
 * écrivent {@code OUVRE} en dur, faute de savoir le calculer depuis le contrat. La contrainte
 * était donc muette pour tout client, au même titre que les six du chantier S7.</p>
 *
 * <p>Elle croise désormais <strong>deux objets distincts</strong> : le créneau de travail, et le
 * fait {@link ReposHebdomadaire} qui dit quel jour ce salarié-là se repose.</p>
 *
 * <h3>Le repos vient du calendrier, pas du créneau</h3>
 * <p>Le calendrier est construit à la préparation, à partir des créneaux porteurs du code
 * activité de repos déclaré par l'appelant, complétés semaine par semaine par le repli
 * samedi/dimanche. Voir {@code domain/repos/CalendrierReposHebdomadaire}.</p>
 */
public class DetteReposSurReposHebdomadaire {

    private DetteReposSurReposHebdomadaire() {
        // Utility class
    }

    public static Constraint penaliser(ConstraintFactory factory) {

        return factory
            // 1) Créneaux confiés à un salarié réel — un poste virtuel n'a pas de repos
            .forEach(Creneau.class)
            .filter(creneau -> creneau.getRessourceAffectee() instanceof SalarieReel)

            // 2) Faits singletons
            .join(factory.forEach(PlanningContext.class))
            .join(factory.forEach(ReferentielComptabiliteActivite.class))

            // 3) Dans l'horizon, et activité générant une dette de repos
            .filter((creneau, context, referentiel) -> {
                if (!context.getHorizonTemporel().contient(creneau.getDate())) {
                    return false;
                }
                ComptabiliteActivite comptabilite =
                        referentiel.getByCode(creneau.getCodeActiviteEffectif());

                return comptabilite != null
                        && comptabilite.isCompteDansCharge()
                        && comptabilite.isGenereDetteRepos();
            })

            // 4) Ce jour-là, ce salarié-là est censé se reposer
            .ifExists(
                ReposHebdomadaire.class,
                Joiners.equal((creneau, context, referentiel) -> creneau.getDate(),
                        ReposHebdomadaire::getDate),
                Joiners.equal((creneau, context, referentiel) ->
                                creneau.getRessourceAffectee().getId(),
                        ReposHebdomadaire::getSalarieId)
            )

            // 5) Pénalité SOFT
            .penalize(
                HardSoftScore.ONE_SOFT,
                (creneau, context, referentiel) -> {

                    int base = context.getPenalites()
                                      .getDetteReposSurReposHebdomadaire();

                    return base * creneau.getDuree();
                }
            )
            .asConstraint(PenaliteKey.METIER_SOFT_DETTE_REPOS_SUR_REPOS_HEBDOMADAIRE.name());
    }
}
