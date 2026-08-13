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
 * @param creneauxLiberes          nombre de créneaux <strong>d'origine</strong> rendus au solveur
 *                                 par cette absence — le découpage éventuel ne le gonfle pas
 * @param creneauxRepris           nombre d'entre eux repris <strong>en entier</strong> par des
 *                                 salariés réels. Un créneau couvert à moitié n'y figure pas : il
 *                                 laisse des heures à pourvoir
 * @param heuresAPourvoir          heures décimales que <strong>personne de réel</strong> ne couvre
 *                                 — poste virtuel compris. Le planning dit où elles sont, ce total
 *                                 dit combien il y en a
 * @param details                  le sort de chaque morceau restitué, y compris ceux que personne
 *                                 n'a repris. Un créneau couvert en deux fois y apparaît deux fois,
 *                                 chaque entrée portant alors son {@code creneauOrigineId}
 * @param surchargeParRessource    ce que le remplacement change à la charge de ceux qui l'assurent,
 *                                 jour par jour (lot S3). Vide quand personne n'a rien repris
 */
public record RemplacementDTO(
        String salarieAbsentId,
        int creneauxLiberes,
        int creneauxRepris,
        double heuresAPourvoir,
        List<CreneauRemplaceDTO> details,
        List<SurchargeDTO> surchargeParRessource) {

    public RemplacementDTO {
        details = details == null ? List.of() : List.copyOf(details);
        surchargeParRessource = surchargeParRessource == null
                ? List.of() : List.copyOf(surchargeParRessource);
    }
}
