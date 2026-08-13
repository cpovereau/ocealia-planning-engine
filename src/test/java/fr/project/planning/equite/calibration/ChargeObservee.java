package fr.project.planning.equite.calibration;

import fr.project.planning.domain.contexte.CoefficientsPenibilite;
import fr.project.planning.scoring.PenibiliteType;
import fr.project.planning.time.RepartitionPenibilites;

/**
 * ChargeObservee — ce qu'une personne a effectivement fait, réparti par pénibilité.
 *
 * <h3>Pourquoi la répartition, et pas les créneaux</h3>
 * <p>La calibration ne rejoue pas une résolution : elle rejoue une <strong>pondération</strong>.
 * La dominance a déjà tranché à quelle catégorie appartient chaque minute — c'est une propriété du
 * planning, pas du choix des coefficients — et la rejouer donnerait chaque fois le même résultat.
 * Ce qui change d'un jeu de coefficients à l'autre, c'est uniquement ce que ces minutes
 * <em>pèsent</em>.</p>
 *
 * <p>D'où une propriété qui fait tout l'intérêt du harnais : <strong>une résolution suffit pour
 * évaluer autant de jeux de coefficients qu'on veut</strong>, et les points de bascule se calculent
 * exactement plutôt que de se chercher à tâtons sur une grille.</p>
 *
 * <p>⚠️ Cette propriété tient tant que les coefficients <strong>ne participent pas au score</strong>.
 * C'est le cas à ce jour : {@code heuresPonderees} est une mesure post-résolution, aucune contrainte
 * ne la lit. Le lot L5 y introduira une contrainte SOFT d'équité — à partir de là, changer les
 * coefficients pourra changer le planning lui-même, et le harnais devra résoudre à nouveau pour
 * chaque jeu. La limite est ici parce qu'elle ne se verrait pas autrement.</p>
 *
 * @param ressourceId      identifiant de la personne
 * @param minutesNuit      minutes attribuées à la nuit par la dominance
 * @param minutesDimanche  minutes attribuées au dimanche par la dominance
 * @param minutesFerie     minutes attribuées au jour férié par la dominance
 * @param minutesOrdinaires minutes ne relevant d'aucune pénibilité
 * @param minutesAttendues volume contractuel sur la fenêtre, ou {@code null} si le salarié ne
 *                         déclare pas de contrat — rien n'est alors comparable
 */
public record ChargeObservee(
        String ressourceId,
        long minutesNuit,
        long minutesDimanche,
        long minutesFerie,
        long minutesOrdinaires,
        Double minutesAttendues) {

    /** Vrai si cette charge peut entrer dans une comparaison. */
    public boolean estComparable() {
        return minutesAttendues != null && minutesAttendues > 0;
    }

    public long minutesTravaillees() {
        return minutesNuit + minutesDimanche + minutesFerie + minutesOrdinaires;
    }

    public long minutes(PenibiliteType type) {
        return switch (type) {
            case NUIT -> minutesNuit;
            case DIMANCHE -> minutesDimanche;
            case FERIE -> minutesFerie;
        };
    }

    /** La même pondération que celle du moteur — jamais une réécriture qui lui ressemble. */
    public double minutesPondereesPar(CoefficientsPenibilite coefficients) {
        return RepartitionPenibilites
                .deMinutesDejaReparties(minutesNuit, minutesDimanche, minutesFerie, minutesOrdinaires)
                .minutesPondereesPar(coefficients);
    }

    /**
     * Les minutes pondérées <strong>hors</strong> une catégorie donnée.
     *
     * <p>C'est la part que faire varier ce coefficient-là ne touche pas : la constante de la
     * droite dont {@link SimulationEquite} cherche les intersections.</p>
     */
    public double minutesPondereesHors(PenibiliteType variable, CoefficientsPenibilite base) {
        return minutesPondereesPar(base) - minutes(variable) * base.pour(variable);
    }
}
