package fr.project.planning.scenarios.dto;

/**
 * NatureCouverture — ce qui couvre, au bout du compte, un créneau libéré par une absence.
 *
 * <p>Les deux dernières valeurs ne se confondent pas. Un poste virtuel est une décision de
 * l'appelant, qui a demandé que les heures non couvertes soient garées sur un poste fictif ;
 * l'absence de ressource est ce qui reste quand il ne l'a pas demandé. Le planning dit
 * <strong>où</strong> sont ces heures ; {@code remplacement.heuresAPourvoir} dit
 * <strong>combien</strong> il y en a, et compte les deux cas.</p>
 */
public enum NatureCouverture {

    /** Un salarié réel a repris le créneau. */
    SALARIE,

    /** Les heures sont garées sur un poste virtuel — demandé par la requête. */
    POSTE_VIRTUEL,

    /** Personne : le créneau revient sans ressource. */
    NON_COUVERT
}
