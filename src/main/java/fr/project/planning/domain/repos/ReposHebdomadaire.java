package fr.project.planning.domain.repos;

import fr.project.planning.domain.creneau.QualificationJour;

import java.time.LocalDate;
import java.util.Objects;

/**
 * ReposHebdomadaire — un jour de repos hebdomadaire, pour un salarié donné (lot S7.9b)
 *
 * <p>Fait immuable du problème, pas une variable de décision : le repos d'une personne n'est pas
 * un besoin à pourvoir, c'est une donnée sur elle. Le solveur ne peut ni le déplacer ni
 * l'attribuer à quelqu'un d'autre.</p>
 *
 * <h3>Pourquoi un fait et non un créneau</h3>
 * <p>Un repos couvre la journée entière, 00:00–23:59. Introduit comme créneau, il violerait
 * immédiatement trois contraintes HARD de {@code LimitePhysique}, qui ne consultent pas le
 * référentiel et ne savent donc pas distinguer un repos d'un travail : durée maximale de créneau
 * (720 minutes), non-chevauchement, cumul journalier. Réduit à une date et une nature, il n'a
 * plus d'horaires et le problème disparaît.</p>
 *
 * <h3>Deux origines</h3>
 * <p>{@link #estDeclare()} distingue un repos <strong>déclaré</strong> par l'appelant — un
 * créneau porteur du code activité de repos — d'un repos <strong>déduit</strong> par le repli
 * samedi/dimanche. La distinction ne change pas la règle appliquée ; elle sert au diagnostic et
 * à la restitution, seuls les repos déclarés devant être rendus à l'appelant.</p>
 */
public final class ReposHebdomadaire {

    private final String salarieId;
    private final LocalDate date;
    private final QualificationJour nature;
    private final boolean declare;

    public ReposHebdomadaire(String salarieId, LocalDate date, QualificationJour nature,
                             boolean declare) {
        this.salarieId = Objects.requireNonNull(salarieId, "salarieId");
        this.date = Objects.requireNonNull(date, "date");
        this.nature = exigerNatureDeRepos(nature);
        this.declare = declare;
    }

    private static QualificationJour exigerNatureDeRepos(QualificationJour nature) {
        if (nature != QualificationJour.RH && nature != QualificationJour.RHD) {
            throw new IllegalArgumentException(
                    "La nature d'un repos hebdomadaire vaut RH ou RHD, reçu : " + nature);
        }
        return nature;
    }

    public String getSalarieId() {
        return salarieId;
    }

    public LocalDate getDate() {
        return date;
    }

    /** {@link QualificationJour#RH} ou {@link QualificationJour#RHD}. */
    public QualificationJour getNature() {
        return nature;
    }

    /** Vrai si l'appelant l'a transmis ; faux s'il vient du repli samedi/dimanche. */
    public boolean estDeclare() {
        return declare;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ReposHebdomadaire autre)) return false;
        return declare == autre.declare
                && salarieId.equals(autre.salarieId)
                && date.equals(autre.date)
                && nature == autre.nature;
    }

    @Override
    public int hashCode() {
        return Objects.hash(salarieId, date, nature, declare);
    }

    @Override
    public String toString() {
        return "ReposHebdomadaire{" + salarieId + " " + date + " " + nature
                + (declare ? " (déclaré)" : " (repli)") + "}";
    }
}
