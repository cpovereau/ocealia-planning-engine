package fr.project.planning.equite.calibration;

import java.util.List;

/**
 * CasDeCalibration — un planning observé, réduit à ce que la calibration a besoin d'en savoir.
 *
 * <h3>Ce qu'un cas doit être pour servir</h3>
 * <p>Un cas de calibration n'est pas un jeu d'essai : c'est une <strong>situation réelle</strong>
 * sur laquelle le métier sait dire qui a été le plus sollicité. Le harnais répond « à quel
 * coefficient l'ordre change » ; c'est le métier qui répond « et de quel côté est la vérité ». Un
 * cas fabriqué pour donner la réponse qu'on attend ne calibre rien — il la déguise.</p>
 *
 * <p>Deux conditions rendent un cas exploitable, et elles se vérifient avant de l'utiliser :</p>
 * <ul>
 *   <li><strong>des contrats déclarés</strong> — sans référence, rien n'est comparable ;</li>
 *   <li><strong>des pénibilités contrastées</strong> — si tout le monde a la même proportion de
 *       nuits, le coefficient de nuit ne départage personne, quelle que soit sa valeur. Le cas est
 *       alors muet, ce qui n'est pas la même chose que neutre.</li>
 * </ul>
 *
 * <p>La fenêtre compte aussi : un écart au contrat lu sur trois jours n'est pas faux, il est
 * faible. Plus la fenêtre transmise est large, plus la calibration porte.</p>
 *
 * @param nom     de quoi ce cas est le cas — repris tel quel dans le rapport
 * @param charges les charges observées, une par ressource
 */
public record CasDeCalibration(String nom, List<ChargeObservee> charges) {

    /** Les ressources auxquelles ce cas peut faire dire quelque chose. */
    public List<ChargeObservee> comparables() {
        return charges.stream().filter(ChargeObservee::estComparable).toList();
    }

    /**
     * Pourquoi ce cas ne peut rien calibrer — et les deux raisons ne se soignent pas pareil.
     *
     * <p>Le dire évite l'erreur de lecture qui guette : conclure d'une absence de bascule que les
     * coefficients sont indifférents. Ils ne le sont pas ; c'est ce cas-là qui n'en dit rien.</p>
     */
    public Mutisme mutisme() {
        List<ChargeObservee> comparables = comparables();

        if (comparables.size() < 2) {
            return Mutisme.PERSONNE_A_COMPARER;
        }
        boolean sansPenibilite = comparables.stream().allMatch(charge ->
                charge.minutesNuit() == 0 && charge.minutesDimanche() == 0
                        && charge.minutesFerie() == 0);

        return sansPenibilite ? Mutisme.AUCUNE_PENIBILITE : Mutisme.AUCUN;
    }

    public boolean estMuet() {
        return mutisme() != Mutisme.AUCUN;
    }

    /** Ce qui manque à un cas pour qu'il puisse trancher quelque chose. */
    public enum Mutisme {

        /** Le cas parle : au moins deux personnes comparables, et des pénibilités contrastées. */
        AUCUN,

        /**
         * Moins de deux personnes à comparer — l'équité est une question sur un groupe. Se corrige
         * en élargissant le jeu transmis, pas en changeant d'échelle.
         */
        PERSONNE_A_COMPARER,

        /**
         * Personne ne porte de nuit, de dimanche ni de férié : toutes les échelles y donnent le
         * même classement. Se corrige en choisissant une période qui en contient.
         */
        AUCUNE_PENIBILITE
    }
}
