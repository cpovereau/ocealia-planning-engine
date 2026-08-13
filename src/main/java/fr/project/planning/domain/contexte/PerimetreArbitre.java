package fr.project.planning.domain.contexte;

import java.util.Set;

/**
 * PerimetreArbitre — les créneaux qu'un arbitrage remet en jeu, et entre qui.
 *
 * <p>Fait d'entrée du lot <strong>A0</strong> de SC-05. Il porte la seule brique que le moteur
 * n'avait pas : de quoi dire « ce créneau ne peut revenir qu'à ces personnes-là ». Le domaine de la
 * variable de décision est global — toute ressource du dataset est candidate pour tout créneau — et
 * rien ne permettait de le restreindre. SC-02 s'en passe parce qu'il libère et laisse le solveur
 * choisir librement ; SC-05 ne le peut pas, son objet même est un arbitrage borné.</p>
 *
 * <h3>Un ensemble, jamais un couple</h3>
 * <p>Arbitrage métier du 13/08 (§5.5 du cadrage) : SC-05 arbitre entre <strong>deux</strong>
 * salariés, et <strong>l'ouverture à N est attendue à brève échéance</strong>. Cette classe porte
 * donc un <em>ensemble</em> de ressources autorisées, dont la taille se trouve valoir deux. Aucun
 * {@code if (deux)} ici ni dans la contrainte qui la lit : la limite à deux est une règle du
 * contrat d'entrée, pas une hypothèse de calcul. Passer à N sera un élargissement du contrat, pas
 * une réécriture du moteur.</p>
 *
 * <h3>Le périmètre est transmis, jamais déduit</h3>
 * <p>§5.1 du cadrage : l'appelant liste les créneaux à arbitrer. Un périmètre déduit trop large
 * déplacerait des créneaux que personne ne voulait bouger ; trop étroit, il rendrait l'arbitrage
 * sans effet — et dans les deux cas l'appelant ne verrait pas pourquoi.</p>
 *
 * <h3>Ce que le vide veut dire</h3>
 * <p>Aucun {@code PerimetreArbitre} au problème — donc tous les scénarios sauf SC-05 — et la
 * contrainte ne se déclenche jamais. C'est la lecture habituelle du moteur : <em>un vide ne suppose
 * jamais que la chose est possible</em>, il dit qu'aucun arbitrage n'est demandé.</p>
 *
 * <p>Un périmètre <strong>présent mais vide</strong> est autre chose : ce serait un arbitrage sans
 * créneau, ou entre personne. Les deux sont refusés à la construction plutôt que tolérés en
 * silence — un ensemble autorisé vide interdirait à quiconque de tenir les créneaux du périmètre,
 * et l'appelant lirait le résultat comme une impossibilité métier au lieu d'une demande mal
 * formée.</p>
 */
public final class PerimetreArbitre {

    private final Set<String> creneauxArbitres;
    private final Set<String> ressourcesAutorisees;

    /**
     * @param creneauxArbitres     identifiants des besoins remis en jeu — au moins un
     * @param ressourcesAutorisees identifiants des ressources entre lesquelles arbitrer — au moins
     *                             une. Deux aujourd'hui ; la taille n'est pas contrainte ici
     */
    public PerimetreArbitre(Set<String> creneauxArbitres, Set<String> ressourcesAutorisees) {
        if (creneauxArbitres == null || creneauxArbitres.isEmpty()) {
            throw new IllegalArgumentException(
                    "Un arbitrage porte sur au moins un créneau.");
        }
        if (ressourcesAutorisees == null || ressourcesAutorisees.isEmpty()) {
            throw new IllegalArgumentException(
                    "Un arbitrage se fait entre au moins une ressource autorisée.");
        }
        this.creneauxArbitres = Set.copyOf(creneauxArbitres);
        this.ressourcesAutorisees = Set.copyOf(ressourcesAutorisees);
    }

    /**
     * Indique si ce besoin fait partie de l'arbitrage.
     *
     * <p>La comparaison porte sur l'<strong>identifiant du besoin</strong> et non sur celui du
     * créneau : un créneau découpé en segments garde l'identifiant de son origine, et l'appelant
     * désigne le périmètre avec les identifiants qu'il a transmis, pas avec ceux que la préparation
     * a fabriqués.</p>
     */
    public boolean contientLeBesoin(String idBesoin) {
        return idBesoin != null && creneauxArbitres.contains(idBesoin);
    }

    /** Indique si cette ressource fait partie de celles entre lesquelles on arbitre. */
    public boolean autorise(String ressourceId) {
        return ressourceId != null && ressourcesAutorisees.contains(ressourceId);
    }

    public Set<String> getCreneauxArbitres() {
        return creneauxArbitres;
    }

    public Set<String> getRessourcesAutorisees() {
        return ressourcesAutorisees;
    }
}
