package fr.project.planning.scenarios.service;

import fr.project.planning.domain.ressource.Ressource;
import fr.project.planning.scenarios.dto.MotifCandidat;
import fr.project.planning.scenarios.dto.NatureCandidat;

import java.util.List;
import java.util.Map;

/**
 * Candidat — une manière de couvrir le besoin, évaluée (SC-06, lot S4).
 *
 * <p>Un candidat est une <strong>solution</strong>, pas une personne : l'affectation complète
 * des créneaux du besoin. La distinction s'efface quand une seule personne couvre tout, mais
 * elle devient nécessaire dès qu'un besoin se répartit.</p>
 *
 * <p>[Équité L4] Les trois grandeurs de départage — série de jours, écart au contrat, amplitude —
 * retiennent la valeur <strong>la plus défavorable</strong> parmi les ressources mobilisées. Une
 * solution ne vaut pas mieux que ce qu'elle coûte à celui qu'elle sollicite le plus ; en faire la
 * moyenne laisserait une solution éreintant une personne passer devant une solution partagée.</p>
 *
 * @param nature                forme de la solution — voir {@link NatureCandidat}
 * @param affectations          identifiant de créneau de besoin → ressource proposée
 * @param conforme              aucune règle éliminatoire n'est violée
 * @param couvertureComplete    tous les créneaux du besoin sont servis par une ressource réelle
 * @param nbRessourcesRappelees ressources mobilisées qui ne travaillaient pas ce jour-là
 * @param joursConsecutifs      [Équité L4] la plus longue série de jours travaillés d'affilée que
 *                              le besoin prolongerait — aptitude, palier 5
 * @param ecartContratPourcent  [Équité L4] écart <strong>signé</strong> au volume contractuel après
 *                              affectation, le plus élevé parmi les ressources mobilisées — équité,
 *                              palier 6. {@code null} dès qu'une ressource ne déclare pas de
 *                              contrat : rien n'est alors comparable, et le candidat est classé en
 *                              dernier de ce palier plutôt que crédité d'un écart favorable
 * @param softScore             score SOFT de la solution complète
 * @param amplitudeApresMinutes [Équité L4] amplitude du jour du besoin après affectation, la plus
 *                              élevée parmi les ressources mobilisées — confort, palier 8
 * @param motifs                raisons expliquant la conformité et le rang
 */
public record Candidat(
        NatureCandidat nature,
        Map<String, Ressource> affectations,
        boolean conforme,
        boolean couvertureComplete,
        int nbRessourcesRappelees,
        int joursConsecutifs,
        Double ecartContratPourcent,
        int softScore,
        int amplitudeApresMinutes,
        List<MotifCandidat> motifs
) {
}
