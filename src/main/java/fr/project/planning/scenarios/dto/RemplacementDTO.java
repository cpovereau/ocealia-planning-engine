package fr.project.planning.scenarios.dto;

import java.util.List;

/**
 * RemplacementDTO — ce que SC-02 restitue en propre, absent des autres scénarios.
 *
 * <p>Comme {@code candidats[]} pour SC-06, cette clé n'apparaît que dans la réponse du scénario
 * qui la produit : une réponse SC-01 ou SC-03 ne doit pas gagner un bloc vide qui laisserait
 * croire à une capacité inexistante.</p>
 *
 * <h3>Deux lectures, et elles ne se déduisent pas l'une de l'autre</h3>
 * <p>Les <strong>compteurs de créneaux</strong> disent combien d'objets du planning ont changé de
 * main ; les <strong>volumes en heures</strong> disent ce que l'absence a réellement coûté. Un
 * créneau de huit heures et un créneau d'une heure pèsent pareil dans le premier décompte et pas
 * du tout dans le second — c'est pourquoi les deux sont rendus. Jusqu'au lot S3, seul
 * {@code heuresAPourvoir} était chiffré : l'appelant lisait « trois créneaux sur cinq repris »
 * sans jamais savoir de combien d'heures on parlait.</p>
 *
 * <p>Les volumes se recomposent, et l'appelant peut le vérifier :</p>
 * <pre>
 *   heuresLiberees   = heuresReprises + heuresSurPosteVirtuel + heuresNonCouvertes
 *   heuresAPourvoir  = heuresSurPosteVirtuel + heuresNonCouvertes
 * </pre>
 *
 * @param salarieAbsentId              salarié dont l'absence a motivé le scénario
 * @param creneauxLiberes              nombre de créneaux <strong>d'origine</strong> rendus au
 *                                     solveur par cette absence — le découpage éventuel ne le
 *                                     gonfle pas
 * @param creneauxRepris               nombre d'entre eux repris <strong>en entier</strong> par des
 *                                     salariés réels
 * @param creneauxPartiellementRepris  nombre d'entre eux dont un salarié réel a pris une part sans
 *                                     prendre le tout. Ni repris ni abandonnés : les confondre avec
 *                                     l'un ou l'autre ferait mentir le décompte
 * @param heuresLiberees               volume total que l'absence a rendu au solveur, en heures
 *                                     décimales — la situation « avant »
 * @param heuresReprises               volume que des salariés réels assurent au bout du compte —
 *                                     la situation « après »
 * @param heuresSurPosteVirtuel        volume garé sur un poste fictif. Toujours 0 quand le scénario
 *                                     n'a pas demandé le poste virtuel : il ne s'invite jamais de
 *                                     lui-même
 * @param heuresNonCouvertes           volume restitué sans aucune ressource
 * @param heuresAPourvoir              heures que <strong>personne de réel</strong> ne couvre —
 *                                     poste virtuel compris. Du point de vue de l'encadrement la
 *                                     question est « combien me reste-t-il à staffer », et la
 *                                     réponse est la même que les heures soient garées sur un poste
 *                                     fictif ou laissées vides. Le planning dit où elles sont, ce
 *                                     total dit combien il y en a
 * @param details                      le sort de chaque morceau restitué, y compris ceux que
 *                                     personne n'a repris. Un créneau couvert en deux fois y
 *                                     apparaît deux fois, chaque entrée portant alors son
 *                                     {@code creneauOrigineId}
 * @param surchargeParRessource        ce que le remplacement change à la charge de ceux qui
 *                                     l'assurent, jour par jour (lot S3). Vide quand personne n'a
 *                                     rien repris
 */
public record RemplacementDTO(
        String salarieAbsentId,
        int creneauxLiberes,
        int creneauxRepris,
        int creneauxPartiellementRepris,
        double heuresLiberees,
        double heuresReprises,
        double heuresSurPosteVirtuel,
        double heuresNonCouvertes,
        double heuresAPourvoir,
        List<CreneauRemplaceDTO> details,
        List<SurchargeDTO> surchargeParRessource) {

    public RemplacementDTO {
        details = details == null ? List.of() : List.copyOf(details);
        surchargeParRessource = surchargeParRessource == null
                ? List.of() : List.copyOf(surchargeParRessource);
    }
}
