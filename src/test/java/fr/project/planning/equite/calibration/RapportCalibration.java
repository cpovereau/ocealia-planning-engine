package fr.project.planning.equite.calibration;

import fr.project.planning.domain.contexte.CoefficientsPenibilite;
import fr.project.planning.scoring.PenibiliteType;

import java.util.List;
import java.util.Locale;

/**
 * RapportCalibration — mettre le résultat du harnais sous une forme qu'un décideur peut trancher.
 *
 * <h3>Ce que le rapport doit dire, et à qui</h3>
 * <p>Il ne s'adresse pas au moteur mais à celui qui choisira les coefficients. Il évite donc de
 * répondre à sa place : il ne propose pas de valeur, il montre <strong>ce que chaque valeur
 * changerait</strong>, et laisse la décision là où elle se prend.</p>
 *
 * <p>Trois choses y figurent, dans cet ordre : le classement sous l'échelle neutre — l'état des
 * lieux avant toute pondération —, les valeurs où ce classement s'inverse, et ce que le cas ne
 * permet pas de décider. Cette dernière partie est la plus facile à oublier et la plus utile : une
 * absence de bascule veut dire que le cas est muet sur ce coefficient, jamais que la valeur est
 * indifférente.</p>
 */
public final class RapportCalibration {

    private RapportCalibration() {
    }

    public static String de(List<CasDeCalibration> cas, CoefficientsPenibilite base) {
        StringBuilder rapport = new StringBuilder();

        rapport.append("# Calibration des coefficients de penibilite\n\n")
                .append("> Rapport genere par `HarnaisDeCalibrationTest`. Ne pas editer a la main :\n")
                .append("> relancer le test le reecrit. Voir 92_CALIBRATION_PENIBILITE dans docs/\n")
                .append("> pour la methode et pour ce qu'un cas doit reunir pour servir.\n\n")
                .append("Echelle de reference du rapport : ")
                .append(echelle(base)).append("\n\n");

        for (CasDeCalibration cible : cas) {
            rapport.append(unCas(cible, base));
        }

        return rapport.toString();
    }

    private static String unCas(CasDeCalibration cas, CoefficientsPenibilite base) {
        StringBuilder bloc = new StringBuilder();
        bloc.append("---\n\n## Cas : ").append(cas.nom()).append("\n\n");

        if (cas.comparables().isEmpty()) {
            bloc.append("Aucune ressource comparable : aucun contrat declare. ")
                    .append("Rien n'est calibrable sur ce cas.\n\n");
            return bloc.toString();
        }

        bloc.append(classement(cas, base));

        if (cas.estMuet()) {
            bloc.append("\n**Ce cas est muet.** ").append(pourquoiMuet(cas.mutisme()))
                    .append(" Ce n'est pas que les coefficients soient indifferents — c'est que ce ")
                    .append("cas-la ne peut rien en dire.\n\n");
            return bloc.toString();
        }

        SimulationEquite simulation = new SimulationEquite(cas);
        for (PenibiliteType categorie : PenibiliteType.values()) {
            bloc.append(bascules(simulation, categorie, base));
        }

        return bloc.toString();
    }

    private static String classement(CasDeCalibration cas, CoefficientsPenibilite base) {
        StringBuilder bloc = new StringBuilder();
        bloc.append("### Classement sous l'echelle de reference\n\n")
                .append("| Rang | Ressource | Heures ponderees | Ecart au contrat | ")
                .append("Nuit | Dimanche | Ferie |\n")
                .append("|---:|---|---:|---:|---:|---:|---:|\n");

        int rang = 1;
        for (SimulationEquite.LigneClassement ligne : new SimulationEquite(cas).classement(base)) {
            ChargeObservee charge = cas.charges().stream()
                    .filter(c -> c.ressourceId().equals(ligne.ressourceId()))
                    .findFirst().orElseThrow();

            bloc.append(String.format(Locale.ROOT,
                    "| %d | %s | %s h | %s %% | %s h | %s h | %s h |%n",
                    rang++, ligne.ressourceId(),
                    heures(ligne.minutesPonderees()),
                    pourcentage(ligne.ecartPourcent()),
                    heures(charge.minutesNuit()),
                    heures(charge.minutesDimanche()),
                    heures(charge.minutesFerie())));
        }
        return bloc.append("\n").toString();
    }

    private static String bascules(SimulationEquite simulation, PenibiliteType categorie,
                                   CoefficientsPenibilite base) {
        List<SimulationEquite.Bascule> bascules = simulation.bascules(categorie, base);

        StringBuilder bloc = new StringBuilder();
        bloc.append("### Coefficient ").append(categorie).append("\n\n");

        if (bascules.isEmpty()) {
            bloc.append("Aucune bascule : sur ce cas, ce coefficient ne departage personne, ")
                    .append("quelle que soit sa valeur. Le classement est fixe par le reste.\n\n");
            return bloc.toString();
        }

        bloc.append("| Bascule a | En dessous, le plus sollicite | Au-dessus |\n")
                .append("|---:|---|---|\n");

        for (SimulationEquite.Bascule bascule : bascules) {
            bloc.append(String.format(Locale.ROOT, "| %s | %s | %s |%n",
                    nombre(bascule.valeur()),
                    bascule.plusSollicitEnDessous(),
                    bascule.plusSollicitAuDessus()));
        }

        return bloc.append("\n").toString();
    }

    private static String pourquoiMuet(CasDeCalibration.Mutisme mutisme) {
        return switch (mutisme) {
            case PERSONNE_A_COMPARER -> "Moins de deux personnes comparables : l'equite est une "
                    + "question sur un groupe, et il n'y a pas de groupe ici. A corriger en "
                    + "elargissant le jeu transmis, pas en changeant d'echelle.";
            case AUCUNE_PENIBILITE -> "Aucune penibilite ne distingue les personnes comparables : "
                    + "toutes les echelles y donnent le meme classement. A corriger en choisissant "
                    + "une periode qui en contient.";
            case AUCUN -> "";
        };
    }

    private static String echelle(CoefficientsPenibilite base) {
        return String.format(Locale.ROOT, "nuit = %s, dimanche = %s, ferie = %s, ordinaire = %s",
                nombre(base.getNuit()), nombre(base.getDimanche()), nombre(base.getFerie()),
                nombre(CoefficientsPenibilite.ORDINAIRE));
    }

    private static String heures(double minutes) {
        return String.format(Locale.ROOT, "%.2f", minutes / 60.0);
    }

    /** Au centieme, comme le moteur le publie — pour que les deux chiffres se recoupent a l'oeil. */
    private static String pourcentage(double valeur) {
        return String.format(Locale.ROOT, "%+.2f", valeur);
    }

    private static String nombre(double valeur) {
        return String.format(Locale.ROOT, "%.3f", valeur);
    }
}
