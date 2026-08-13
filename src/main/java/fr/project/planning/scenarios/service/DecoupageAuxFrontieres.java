package fr.project.planning.scenarios.service;

import fr.project.planning.domain.creneau.Creneau;
import fr.project.planning.domain.ressource.Indisponibilite;
import fr.project.planning.domain.ressource.SalarieReel;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;

/**
 * DecoupageAuxFrontieres — où couper un créneau qu'aucun remplaçant ne peut prendre en entier.
 *
 * <h3>Une coupe n'a de sens que là où une disponibilité change</h3>
 * <p>Une grille fixe — toutes les demi-heures, par exemple — produirait à la fois plus de morceaux
 * et des coupes qui ne correspondent à rien. Les seules coupes utiles sont les instants où
 * <strong>quelqu'un devient disponible, ou cesse de l'être</strong> :</p>
 * <ul>
 *   <li>le début d'un créneau déjà affecté à un remplaçant — il n'est plus libre à partir de là ;</li>
 *   <li>la fin d'un tel créneau — il redevient libre ;</li>
 *   <li>les bornes de ses indisponibilités, à la journée.</li>
 * </ul>
 *
 * <p>Exemple de l'arbitrage métier : sur un créneau à remplacer de 13h30–16h00, un remplaçant qui
 * prend son propre service à 15h30 crée une coupe à 15h30. On peut lui confier 13h30–15h30, et les
 * 30 minutes restantes partent à pourvoir. S'il prend son service à 13h45, la coupe tombe à 13h45 —
 * qu'aucune grille de 30 minutes n'aurait produite.</p>
 *
 * <h3>Ce que cette classe ne fait pas</h3>
 * <p>Elle ne juge pas de la taille des morceaux. Le seuil de 30 minutes porte sur le
 * <strong>bloc effectivement confié</strong> après recombinaison, pas sur les segments
 * intermédiaires : deux segments de 15 minutes donnés à la même personne forment une demi-heure
 * parfaitement valide. C'est {@code BlocConfieTropCourt} qui le mesure, sur la solution.</p>
 */
public final class DecoupageAuxFrontieres {

    private DecoupageAuxFrontieres() {
    }

    /**
     * Découpe un créneau libéré aux frontières de disponibilité des remplaçants possibles.
     *
     * @param libere         créneau rendu au solveur par l'absence
     * @param planningExistant créneaux déjà affectés — seuls les figés comptent, ce sont les seuls
     *                       à décrire une occupation certaine
     * @param indisponibilites absences déclarées, toutes ressources confondues
     * @param salarieAbsentId l'absent lui-même n'est pas son propre remplaçant
     * @return les segments, dans l'ordre chronologique ; <strong>une liste réduite au créneau
     *         d'origine</strong> lorsqu'aucune frontière ne tombe à l'intérieur — un créneau que
     *         personne ne fragmente n'a pas à changer d'identité
     */
    public static List<Creneau> decouper(Creneau libere,
                                         List<Creneau> planningExistant,
                                         List<Indisponibilite> indisponibilites,
                                         String salarieAbsentId) {

        LocalDateTime debut = libere.getDebutEffectif();
        LocalDateTime fin = libere.getFinEffectif();

        TreeSet<LocalDateTime> frontieres = new TreeSet<>();

        for (Creneau existant : planningExistant) {
            if (!existant.isFige()
                    || !(existant.getRessourceAffectee() instanceof SalarieReel salarie)
                    || salarie.getId() == null
                    || salarie.getId().equals(salarieAbsentId)) {
                continue;
            }
            ajouterSiInterieure(frontieres, existant.getDebutEffectif(), debut, fin);
            ajouterSiInterieure(frontieres, existant.getFinEffectif(), debut, fin);
        }

        for (Indisponibilite absence : indisponibilites) {
            if (absence.getRessourceId() == null
                    || absence.getRessourceId().equals(salarieAbsentId)
                    || absence.getDateDebut() == null
                    || absence.getDateFin() == null) {
                continue;
            }
            ajouterSiInterieure(frontieres, absence.getDateDebut().atStartOfDay(), debut, fin);
            ajouterSiInterieure(frontieres,
                    absence.getDateFin().plusDays(1).atStartOfDay(), debut, fin);
        }

        if (frontieres.isEmpty()) {
            return List.of(libere);
        }

        List<LocalDateTime> bornes = new ArrayList<>();
        bornes.add(debut);
        bornes.addAll(frontieres);
        bornes.add(fin);

        List<Creneau> segments = new ArrayList<>(bornes.size() - 1);
        for (int i = 0; i < bornes.size() - 1; i++) {
            segments.add(segment(libere, bornes.get(i), bornes.get(i + 1), i + 1));
        }
        return segments;
    }

    private static void ajouterSiInterieure(TreeSet<LocalDateTime> frontieres,
                                            LocalDateTime instant,
                                            LocalDateTime debut,
                                            LocalDateTime fin) {
        if (instant.isAfter(debut) && instant.isBefore(fin)) {
            frontieres.add(instant);
        }
    }

    /**
     * Un morceau du créneau d'origine, de {@code debut} à {@code fin}.
     *
     * <p>Sa durée est calculée ici, une seule fois, avant la résolution — et rien ne la recalcule
     * ensuite. L'invariant {@code Creneau.duree} tient toujours ; ce qui change, c'est que
     * « l'amont » inclut désormais une étape de préparation côté moteur.</p>
     */
    static Creneau segment(Creneau origine, LocalDateTime debut, LocalDateTime fin, int rang) {
        Creneau segment = new Creneau(
                origine.getId() + "#S" + rang,
                debut.toLocalDate(),
                debut.toLocalTime(),
                fin.toLocalTime(),
                (int) Duration.between(debut, fin).toMinutes(),
                origine.getLieu(),
                origine.getCodeActiviteId(),
                origine.getActivite(),
                origine.getPosteComptable(),
                origine.getPriorite(),
                origine.getType(),
                origine.getTypePlageHoraire(),
                origine.isJourFerie(),
                origine.getQualificationJour());
        segment.setCreneauOrigineId(origine.getId());
        return segment;
    }
}
