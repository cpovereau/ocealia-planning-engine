package fr.project.planning.scenarios.dto;

import java.util.List;

/**
 * RemplacementDTO — ce que SC-02 restitue en propre, absent des autres scénarios.
 *
 * <p>Comme {@code candidats[]} pour SC-06, cette clé n'apparaît que dans la réponse du scénario
 * qui la produit : une réponse SC-01 ou SC-03 ne doit pas gagner un bloc vide qui laisserait
 * croire à une capacité inexistante.</p>
 *
 * @param salarieAbsentId          salarié dont l'absence a motivé le scénario
 * @param creneauxLiberes          nombre de créneaux rendus au solveur par cette absence
 * @param creneauxRepris           nombre d'entre eux repris par un salarié réel
 * @param heuresAPourvoir          heures décimales que <strong>personne de réel</strong> ne couvre
 *                                 — poste virtuel compris. Le planning dit où elles sont, ce total
 *                                 dit combien il y en a
 * @param details                  le sort de chaque créneau libéré, y compris ceux non repris
 */
public record RemplacementDTO(
        String salarieAbsentId,
        int creneauxLiberes,
        int creneauxRepris,
        double heuresAPourvoir,
        List<CreneauRemplaceDTO> details) {

    public RemplacementDTO {
        details = details == null ? List.of() : List.copyOf(details);
    }
}
