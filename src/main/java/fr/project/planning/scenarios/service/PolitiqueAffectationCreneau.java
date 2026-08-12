package fr.project.planning.scenarios.service;

import fr.project.planning.domain.creneau.Creneau;
import fr.project.planning.domain.ressource.Ressource;
import fr.project.planning.scenarios.dto.input.CreneauInputDTO;

import java.util.Map;

/**
 * PolitiqueAffectationCreneau — que faire de l'affectation déjà portée par un créneau d'entrée.
 *
 * <p>La préparation d'un scénario dataset-driven est commune ; ce qu'un scénario fait de
 * {@code ressourceAffecteeId} ne l'est pas. SC-03 l'ignore et laisse tout ouvert au solveur ;
 * SC-02 fige l'existant et ne libère que les créneaux du salarié absent ; SC-06 fige tout.
 * C'est le seul point où ces scénarios divergent, et c'est donc le seul qu'ils fournissent.</p>
 *
 * <p>Le contrat est volontairement impératif — la politique <strong>modifie</strong> le créneau —
 * parce que figer est un acte, pas un calcul : {@link Creneau#figerSur} est l'unique moyen de le
 * faire, et il n'existe pas de {@code setFige(boolean)} qui permettrait de reconstruire cet état
 * autrement.</p>
 */
@FunctionalInterface
public interface PolitiqueAffectationCreneau {

    /**
     * Politique de SC-01 et SC-03 : l'affectation d'entrée n'est pas reprise, tout créneau reste
     * une variable de décision. C'est le comportement historique du moteur.
     */
    PolitiqueAffectationCreneau AUCUNE = (dto, creneau, ressourcesParId) -> { };

    /**
     * @param dto             créneau tel que reçu, seul porteur de {@code ressourceAffecteeId}
     * @param creneau         créneau mappé, à figer ou à laisser libre
     * @param ressourcesParId ressources du problème, par identifiant. Les instances sont
     *                        <strong>celles du value range</strong> : une copie distincte
     *                        sortirait du domaine de la variable de décision.
     */
    void appliquer(CreneauInputDTO dto, Creneau creneau, Map<String, Ressource> ressourcesParId);
}
