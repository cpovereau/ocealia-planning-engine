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
 * @param nature                forme de la solution — voir {@link NatureCandidat}
 * @param affectations          identifiant de créneau de besoin → ressource proposée
 * @param conforme              aucune règle éliminatoire n'est violée
 * @param couvertureComplete    tous les créneaux du besoin sont servis par une ressource réelle
 * @param nbRessourcesRappelees ressources mobilisées qui ne travaillaient pas ce jour-là
 * @param ratioCharge           charge hebdomadaire résultante rapportée au volume habituel ;
 *                              la plus défavorable parmi les ressources mobilisées
 * @param softScore             score SOFT de la solution complète
 * @param motifs                raisons expliquant la conformité et le rang
 */
public record Candidat(
        NatureCandidat nature,
        Map<String, Ressource> affectations,
        boolean conforme,
        boolean couvertureComplete,
        int nbRessourcesRappelees,
        double ratioCharge,
        int softScore,
        List<MotifCandidat> motifs
) {
}
