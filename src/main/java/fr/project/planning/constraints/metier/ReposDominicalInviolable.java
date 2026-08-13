package fr.project.planning.constraints.metier;

import fr.project.planning.domain.creneau.Creneau;
import fr.project.planning.domain.creneau.QualificationJour;
import fr.project.planning.domain.repos.ReposHebdomadaire;
import fr.project.planning.domain.ressource.SalarieReel;
import fr.project.planning.scoring.PenaliteKey;

import org.optaplanner.core.api.score.buildin.hardsoft.HardSoftScore;
import org.optaplanner.core.api.score.stream.Constraint;
import org.optaplanner.core.api.score.stream.ConstraintFactory;
import org.optaplanner.core.api.score.stream.Joiners;

/**
 * ReposDominicalInviolable — contrainte HARD, lot L0 du chantier équité.
 *
 * <h3>Une règle forte, et la raison est juridique</h3>
 * <p>Arbitrage métier du 13/08 : <strong>on ne retire pas à un salarié son repos hebdomadaire
 * dominical</strong>. La loi impose un délai de prévenance le plus souvent incompatible avec une
 * réorganisation qui, par nature, se décide dans l'urgence — précisément la situation que SC-02
 * traite. Ce n'est donc pas une préférence que l'on pondère : c'est une interdiction.</p>
 *
 * <h3>Ce que cette contrainte répare</h3>
 * <p>{@link DetteReposSurReposHebdomadaire} était jusqu'ici la seule règle à regarder le travail
 * posé un jour de repos, et elle cumulait trois limites : SOFT, ne distinguant pas RH de RHD, et
 * conditionnée à {@code genereDetteRepos}. <strong>Pour toute autre activité, faire travailler
 * quelqu'un son dimanche de repos ne coûtait rien du tout.</strong> La donnée manquait si peu que
 * {@link ReposHebdomadaire} portait déjà sa nature.</p>
 *
 * <p>Le partage est désormais net : <strong>RHD interdit ici, RH pesé là-bas.</strong> Sa sœur
 * SOFT ne juge plus que le RH, sans quoi le même fait serait compté deux fois.</p>
 *
 * <h3>Seules les décisions du solveur sont jugées</h3>
 * <p>Un créneau <strong>épinglé</strong> est hors de portée : c'est du planning existant, transmis
 * comme un fait. Le pénaliser rendrait le problème insoluble pour une faute que le solveur n'a pas
 * commise et ne peut pas défaire — SC-02 et SC-06 épinglent l'essentiel de ce qu'ils reçoivent.
 * C'est la même doctrine que les motifs de SC-06, qui ne sont levés que si un candidat
 * <em>aggrave</em> la situation de référence.</p>
 *
 * <p>Une violation déjà présente dans le planning transmis n'est donc pas pesée — elle est
 * <strong>signalée</strong> à la préparation, par une alerte {@code REPOS_DOMINICAL_TRAVAILLE}.
 * Le moteur ne refuse pas ce qu'il n'a pas décidé : il le rend visible.</p>
 *
 * <h3>⚠️ Seul un repos DÉCLARÉ est inviolable</h3>
 * <p>{@code CalendrierReposHebdomadaire} complète les semaines sans marqueur par un repli —
 * <strong>samedi vaut RH, dimanche vaut RHD</strong>. Appliquée à ce repli, l'interdiction
 * rendrait <em>tout</em> travail du dimanche impossible pour quiconque ne transmet pas ses
 * marqueurs de repos, c'est-à-dire pour la plupart des appelants. Ce n'est pas l'arbitrage rendu.</p>
 *
 * <p>La règle vise le repos qu'une personne <strong>a</strong>, et l'argument qui la fonde — le
 * délai de prévenance — ne vaut que contre un repos planifié et connu. On ne manque pas de
 * prévenance envers un jour que personne n'a jamais déclaré. Un dimanche déduit reste donc
 * <strong>pesé</strong> par {@link DetteReposSurReposHebdomadaire}, jamais interdit.</p>
 *
 * <p>{@code ReposHebdomadaire.estDeclare()} portait déjà la distinction, en annonçant qu'elle « ne
 * change pas la règle appliquée ». Elle la change désormais.</p>
 *
 * <h3>Le problème reste soluble</h3>
 * <p>Comme {@link BlocConfieTropCourt}, cette contrainte HARD laisse toujours une issue : le
 * créneau part sans ressource ou sur un poste virtuel, et devient des heures à pourvoir. Interdire
 * de confier n'est jamais interdire de résoudre.</p>
 */
public final class ReposDominicalInviolable {

    private ReposDominicalInviolable() {
    }

    public static Constraint reposDominicalInviolable(ConstraintFactory factory) {
        return factory
                .forEach(Creneau.class)

                // 1) Confié à un salarié réel — un poste virtuel n'a pas de repos.
                .filter(creneau -> creneau.getRessourceAffectee() instanceof SalarieReel)

                // 2) Décidé par le solveur. L'existant épinglé est un fait, pas une faute.
                .filter(creneau -> !creneau.isFige())

                // 3) Un segment de pause n'est pas un motif de déplacement : il accompagne un
                //    travail, qui est lui-même jugé. Toute autre activité compte, y compris celles
                //    qui n'entrent pas dans la charge — être rappelé un dimanche de repos pour
                //    quoi que ce soit reste être rappelé.
                .filter(creneau -> !Boolean.TRUE.equals(creneau.getEstSegmentDePause()))

                // 4) Ce jour-là, ce salarié-là est en repos dominical — et il l'a DÉCLARÉ.
                //    Un dimanche seulement déduit du repli n'interdit rien : il est pesé ailleurs.
                .join(ReposHebdomadaire.class,
                        Joiners.equal(Creneau::getDate, ReposHebdomadaire::getDate),
                        Joiners.equal(creneau -> creneau.getRessourceAffectee().getId(),
                                ReposHebdomadaire::getSalarieId),
                        Joiners.filtering((creneau, repos) ->
                                repos.getNature() == QualificationJour.RHD && repos.estDeclare()))

                .penalize(HardSoftScore.ONE_HARD)
                .asConstraint(PenaliteKey.METIER_HARD_REPOS_DOMINICAL.name());
    }
}
