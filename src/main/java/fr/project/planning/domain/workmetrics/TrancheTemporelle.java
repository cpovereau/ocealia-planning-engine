package fr.project.planning.domain.workmetrics;

import fr.project.planning.domain.contexte.HorizonTemporel;

import java.time.LocalDate;

/**
 * TrancheTemporelle — une portion de l'horizon sur laquelle la charge est mesurée (lot O1 de SC-04).
 *
 * <p>SC-04 juge une <strong>période</strong>, non une semaine : c'est son élément propre. Mais un
 * chiffre unique sur trois mois ne dit pas où le déséquilibre s'est installé. Le regroupement à la
 * semaine et au mois le montre, et la période garde le mot de la fin.</p>
 *
 * <p>Les bornes sont <strong>toujours ramenées à l'horizon</strong> : une semaine à cheval sur le
 * début de la fenêtre commence au premier jour transmis, pas au lundi. Le moteur ne juge jamais
 * hors de ce qu'il a reçu.</p>
 *
 * @param granularite le pas de regroupement
 * @param debut       premier jour de la tranche, inclus, jamais avant le début de l'horizon
 * @param fin         dernier jour de la tranche, inclus, jamais après la fin de l'horizon
 */
public record TrancheTemporelle(Granularite granularite, LocalDate debut, LocalDate fin) {

    public enum Granularite {
        SEMAINE,
        MOIS,
        PERIODE
    }

    public TrancheTemporelle {
        if (granularite == null || debut == null || fin == null) {
            throw new IllegalArgumentException("Une tranche porte une granularité et deux bornes.");
        }
        if (fin.isBefore(debut)) {
            throw new IllegalArgumentException(
                    "Une tranche ne se termine pas avant de commencer : " + debut + " → " + fin);
        }
    }

    /** L'horizon correspondant, pour rejouer le calculateur de production sur cette tranche. */
    public HorizonTemporel horizon() {
        return new HorizonTemporel(debut, fin);
    }

    /**
     * ⚠️ Une tranche est <strong>partielle</strong> quand elle ne couvre pas toute sa granularité.
     *
     * <p>Une semaine tronquée par le bord de l'horizon porte moins de jours qu'une semaine pleine :
     * comparer ses heures à celles d'une semaine entière n'a pas de sens, et l'écart au contrat y
     * répond déjà — il proratise. Le drapeau existe pour que l'appelant ne s'y trompe pas sur les
     * <em>volumes bruts</em>.</p>
     */
    public boolean partielle() {
        return switch (granularite) {
            case SEMAINE -> debut.getDayOfWeek().getValue() != 1 || fin.getDayOfWeek().getValue() != 7;
            case MOIS -> debut.getDayOfMonth() != 1 || fin.getDayOfMonth() != fin.lengthOfMonth();
            case PERIODE -> false;
        };
    }

    @Override
    public String toString() {
        return granularite + "[" + debut + " → " + fin + (partielle() ? ", partielle]" : "]");
    }
}
