package fr.project.planning.scenarios.service;

import fr.project.planning.constraints.metier.BlocConfieTropCourt;
import fr.project.planning.domain.creneau.Creneau;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * RecombinaisonSegments — refaire un créneau des morceaux qu'une même personne a repris.
 *
 * <p>Le découpage est un moyen, pas un résultat. Un créneau repris en entier par une seule
 * personne doit ressortir <strong>entier et sous son identifiant d'origine</strong>, comme s'il
 * n'avait jamais été coupé : c'est le cas courant, et l'appelant n'a pas à subir la mécanique
 * interne du moteur.</p>
 *
 * <p>Quand le créneau ressort réellement en plusieurs morceaux, chacun porte un identifiant dérivé
 * — {@code <idOrigine>#S1}, {@code #S2}… dans l'ordre chronologique — et un
 * {@code creneauOrigineId} qui permet de le rattacher sans analyser la chaîne. Les numéros sont
 * <strong>renumérotés après recombinaison</strong> : si le découpage avait produit quatre segments
 * dont trois sont revenus à la même personne, la réponse en montre deux, {@code #S1} et
 * {@code #S2}, et non {@code #S1} et {@code #S4}.</p>
 */
public final class RecombinaisonSegments {

    private RecombinaisonSegments() {
    }

    /**
     * Recombine les segments d'une solution résolue.
     *
     * @param creneauxResolus créneaux tels que le solveur les rend, segments compris
     * @return la même liste, segments contigus de même ressource fusionnés, dans l'ordre d'origine
     *         pour les créneaux non découpés
     */
    public static List<Creneau> recombiner(List<Creneau> creneauxResolus) {
        Map<String, List<Creneau>> segmentsParOrigine = new LinkedHashMap<>();
        List<Creneau> resultat = new ArrayList<>();

        for (Creneau creneau : creneauxResolus) {
            if (creneau.getCreneauOrigineId() == null) {
                resultat.add(creneau);
            } else {
                segmentsParOrigine
                        .computeIfAbsent(creneau.getCreneauOrigineId(), k -> new ArrayList<>())
                        .add(creneau);
            }
        }

        segmentsParOrigine.forEach((origineId, segments) ->
                resultat.addAll(fusionner(origineId, segments)));

        resultat.sort(Comparator
                .comparing(Creneau::getDate)
                .thenComparing(Creneau::getHeureDebut));
        return resultat;
    }

    private static List<Creneau> fusionner(String origineId, List<Creneau> segments) {
        List<List<Creneau>> blocs = BlocConfieTropCourt.blocsContigus(segments);

        // Un seul bloc qui couvre tout : le créneau n'a pas été fragmenté, il retrouve son identité.
        boolean entier = blocs.size() == 1;

        List<Creneau> fusionnes = new ArrayList<>(blocs.size());
        int rang = 1;
        for (List<Creneau> bloc : blocs) {
            fusionnes.add(entier
                    ? bloc(bloc, origineId, null)
                    : bloc(bloc, origineId + "#S" + rang++, origineId));
        }
        return fusionnes;
    }

    /** Un créneau couvrant toute la suite contiguë, affecté à la ressource qu'elle partage. */
    private static Creneau bloc(List<Creneau> suite, String id, String creneauOrigineId) {
        Creneau premier = suite.get(0);
        Creneau dernier = suite.get(suite.size() - 1);

        if (suite.size() == 1 && id.equals(premier.getId())) {
            return premier;
        }

        Creneau fusionne = new Creneau(
                id,
                premier.getDate(),
                premier.getHeureDebut(),
                dernier.getHeureFin(),
                (int) Duration.between(
                        premier.getDebutEffectif(), dernier.getFinEffectif()).toMinutes(),
                premier.getLieu(),
                premier.getCodeActiviteId(),
                premier.getActivite(),
                premier.getPosteComptable(),
                premier.getPriorite(),
                premier.getType(),
                premier.getTypePlageHoraire(),
                premier.isJourFerie(),
                premier.getQualificationJour());
        fusionne.setCreneauOrigineId(creneauOrigineId);
        fusionne.setRessourceAffectee(premier.getRessourceAffectee());
        return fusionne;
    }
}
