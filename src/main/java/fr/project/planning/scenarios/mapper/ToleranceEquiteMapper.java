package fr.project.planning.scenarios.mapper;

import fr.project.planning.domain.contexte.ToleranceEquite;
import fr.project.planning.scenarios.dto.EquiteDTO;
import fr.project.planning.scenarios.dto.PlanningContextDTO;

/**
 * ToleranceEquiteMapper — lit la tolérance d'équité, ou constate qu'il n'y en a pas (L5).
 *
 * <h3>Le silence est la règle, et il ne se signale pas</h3>
 * <p>Sans bloc {@code equite}, la contrainte d'équité ne pèse rien : le moteur mesure l'écart au
 * contrat et le restitue, il ne le sanctionne pas. C'est le cas de <strong>toutes</strong> les
 * demandes à ce jour, et une alerte sur cent pour cent des réponses n'est pas un signal — la même
 * leçon qu'au lot L1 sur les coefficients.</p>
 *
 * <p>Aucune alerte non plus quand la tolérance est transmise : contrairement à un bloc de
 * coefficients neutres, une tolérance déclarée <strong>fait toujours quelque chose</strong>, y
 * compris à zéro — où elle signifie « tout écart compte ». Il n'y a donc pas de cas où l'appelant
 * croirait configurer sans configurer.</p>
 */
public final class ToleranceEquiteMapper {

    private ToleranceEquiteMapper() {
    }

    public static ToleranceEquite depuis(PlanningContextDTO contexte) {
        EquiteDTO dto = contexte == null ? null : contexte.getEquite();
        return new ToleranceEquite(dto == null ? null : dto.getEcartTolerePourcent());
    }
}
