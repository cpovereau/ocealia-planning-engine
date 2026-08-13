package fr.project.planning.constraints.metier;

import fr.project.planning.domain.creneau.Creneau;
import fr.project.planning.domain.ressource.Ressource;
import fr.project.planning.domain.ressource.SalarieReel;
import fr.project.planning.scoring.PenaliteKey;
import org.optaplanner.core.api.score.buildin.hardsoft.HardSoftScore;
import org.optaplanner.core.api.score.stream.Constraint;
import org.optaplanner.core.api.score.stream.ConstraintCollectors;
import org.optaplanner.core.api.score.stream.ConstraintFactory;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * BlocConfieTropCourt — contrainte HARD, lot S2 de SC-02.
 *
 * <p>Arbitrage métier du 11/08 : <strong>un bloc confié à un salarié ne fait jamais moins de
 * 30 minutes</strong>. Sur un créneau à remplacer de 13h30–16h00, un remplaçant qui prend son
 * propre service à 13h45 ne se verra pas proposer les quinze minutes qui précèdent : quinze
 * minutes ne se confient pas.</p>
 *
 * <h3>Le seuil porte sur le bloc, pas sur le segment</h3>
 * <p>Le découpage produit des morceaux aux frontières de disponibilité, sans se soucier de leur
 * taille. Deux segments de 15 minutes attribués à la <strong>même personne et bout à bout</strong>
 * forment une demi-heure, qu'elle travaillera d'une traite : c'est un bloc valide. Cette contrainte
 * reconstitue donc les suites contiguës avant de mesurer, et compte une violation par suite trop
 * courte — un salarié peut en avoir deux sur le même créneau d'origine si on l'y fait revenir.</p>
 *
 * <h3>Elle ne rend jamais le problème insoluble</h3>
 * <p>Le morceau trop court a toujours une issue : rester à pourvoir. C'est précisément la réponse
 * attendue (§4.3 du cadrage) — le moteur ne refuse pas, il rend visible ce qu'il n'a pas su
 * couvrir.</p>
 *
 * <p>La contrainte ne porte que sur les <strong>segments</strong> : un créneau reçu tel quel n'a
 * pas d'origine, et sa durée est un fait d'entrée que le moteur n'a pas à juger.</p>
 */
public final class BlocConfieTropCourt {

    /** Arbitrage métier : en deçà, on ne confie pas. */
    static final int MINUTES_MINIMUM = 30;

    private BlocConfieTropCourt() {
    }

    public static Constraint blocConfieTropCourt(ConstraintFactory factory) {
        return factory
                .forEach(Creneau.class)
                .filter(c -> c.getCreneauOrigineId() != null
                        && c.getRessourceAffectee() instanceof SalarieReel)
                .groupBy(Creneau::getCreneauOrigineId,
                        Creneau::getRessourceAffectee,
                        ConstraintCollectors.toList())
                .filter((origine, ressource, segments) -> blocsTropCourts(segments) > 0)
                .penalize(HardSoftScore.ONE_HARD,
                        (origine, ressource, segments) -> blocsTropCourts(segments))
                .asConstraint(PenaliteKey.METIER_HARD_BLOC_CONFIE_TROP_COURT.name());
    }

    /**
     * Nombre de suites contiguës de moins de {@value #MINUTES_MINIMUM} minutes.
     *
     * <p>Deux segments se suivent lorsque l'un commence exactement là où l'autre finit — la
     * comparaison porte sur les instants effectifs, minuit franchi compris.</p>
     */
    static int blocsTropCourts(List<Creneau> segments) {
        List<Creneau> ordonnes = new ArrayList<>(segments);
        ordonnes.sort(Comparator.comparing(Creneau::getDebutEffectif));

        int tropCourts = 0;
        int minutesDuBloc = 0;
        Creneau precedent = null;

        for (Creneau segment : ordonnes) {
            boolean contigu = precedent != null
                    && precedent.getFinEffectif().equals(segment.getDebutEffectif());

            if (!contigu && precedent != null) {
                if (minutesDuBloc < MINUTES_MINIMUM) {
                    tropCourts++;
                }
                minutesDuBloc = 0;
            }

            minutesDuBloc += segment.getDuree();
            precedent = segment;
        }

        if (precedent != null && minutesDuBloc < MINUTES_MINIMUM) {
            tropCourts++;
        }
        return tropCourts;
    }

    /** Exposé pour la restitution : les suites contiguës affectées à une même ressource. */
    public static List<List<Creneau>> blocsContigus(List<Creneau> segments) {
        List<Creneau> ordonnes = new ArrayList<>(segments);
        ordonnes.sort(Comparator.comparing(Creneau::getDebutEffectif));

        List<List<Creneau>> blocs = new ArrayList<>();
        List<Creneau> courant = new ArrayList<>();

        for (Creneau segment : ordonnes) {
            boolean contigu = !courant.isEmpty()
                    && memeRessource(courant.get(courant.size() - 1), segment)
                    && courant.get(courant.size() - 1).getFinEffectif()
                            .equals(segment.getDebutEffectif());

            if (!contigu && !courant.isEmpty()) {
                blocs.add(courant);
                courant = new ArrayList<>();
            }
            courant.add(segment);
        }
        if (!courant.isEmpty()) {
            blocs.add(courant);
        }
        return blocs;
    }

    private static boolean memeRessource(Creneau a, Creneau b) {
        Ressource ra = a.getRessourceAffectee();
        Ressource rb = b.getRessourceAffectee();
        if (ra == null || rb == null) {
            return ra == rb;
        }
        return ra.getId() != null && ra.getId().equals(rb.getId());
    }
}
