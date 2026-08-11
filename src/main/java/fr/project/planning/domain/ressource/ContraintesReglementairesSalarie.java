package fr.project.planning.domain.ressource;

/**
 * ContraintesReglementairesSalarie — objet valeur domaine.
 *
 * <p>Représente les contraintes réglementaires <strong>individuelles</strong> d'un salarié,
 * transportées depuis WinDev et mappées par {@code ScenarioResourceMapper}.</p>
 *
 * <h3>Lot S7.0 — les seuils sont individuels</h3>
 * <p>Cinq seuils vivaient jusqu'ici dans {@code SeuilsDeTolerance}, globaux à la résolution et
 * jamais alimentés : ils valaient donc 0 en production. Ils rejoignent ici le salarié, parce
 * qu'un plafond de nuits consécutives ou de dimanches travaillés relève du contrat de la
 * personne, pas du contexte de calcul. Trois salariés = trois jeux de seuils.</p>
 *
 * <h3>Règle d'activation — seule l'absence désactive</h3>
 * <p>Voir {@link #borneRenseignee(Number)}. Le moteur ne devine pas un plafond qu'on ne lui a pas
 * donné : il s'abstient de juger, conformément à l'invariant « un vide ne suppose jamais que la
 * chose est possible ». Une valeur <strong>0</strong> n'est pas un vide — elle est lue
 * littéralement : un maximum à 0 interdit tout, un minimum à 0 n'exige rien.</p>
 */
public class ContraintesReglementairesSalarie {

    private final Double heuresMinimumParJour;
    private final Double heuresMaximumParJour;
    private final Double amplitudeJournaliereMaximum;
    private final Double reposQuotidienMinimum;
    private final Double heuresMinimumParSemaine;
    private final Double heuresMaximumParSemaine;
    private final Integer nuitsMaximumParSemaine;
    private final Integer joursConsecutifsMaximum;

    // ---- Seuils rapatriés de SeuilsDeTolerance (lot S7.0) ----

    private final Integer nuitsConsecutivesMaximum;
    private final Integer joursReposMinimumApresNuits;
    private final Integer dimanchesTravaillesMaximum;
    private final Integer reposHebdomadaireFenetreJours;
    private final Integer reposHebdomadaireJoursOffMinimum;

    /**
     * Constructeur historique — les cinq seuils du lot S7.0 restent absents, donc inactifs.
     * Conservé pour ne pas réécrire les jeux de test antérieurs à S7.
     */
    public ContraintesReglementairesSalarie(
            Double heuresMinimumParJour,
            Double heuresMaximumParJour,
            Double amplitudeJournaliereMaximum,
            Double reposQuotidienMinimum,
            Double heuresMinimumParSemaine,
            Double heuresMaximumParSemaine,
            Integer nuitsMaximumParSemaine,
            Integer joursConsecutifsMaximum
    ) {
        this(heuresMinimumParJour, heuresMaximumParJour, amplitudeJournaliereMaximum,
                reposQuotidienMinimum, heuresMinimumParSemaine, heuresMaximumParSemaine,
                nuitsMaximumParSemaine, joursConsecutifsMaximum,
                null, null, null, null, null);
    }

    public ContraintesReglementairesSalarie(
            Double heuresMinimumParJour,
            Double heuresMaximumParJour,
            Double amplitudeJournaliereMaximum,
            Double reposQuotidienMinimum,
            Double heuresMinimumParSemaine,
            Double heuresMaximumParSemaine,
            Integer nuitsMaximumParSemaine,
            Integer joursConsecutifsMaximum,
            Integer nuitsConsecutivesMaximum,
            Integer joursReposMinimumApresNuits,
            Integer dimanchesTravaillesMaximum,
            Integer reposHebdomadaireFenetreJours,
            Integer reposHebdomadaireJoursOffMinimum
    ) {
        this.heuresMinimumParJour = heuresMinimumParJour;
        this.heuresMaximumParJour = heuresMaximumParJour;
        this.amplitudeJournaliereMaximum = amplitudeJournaliereMaximum;
        this.reposQuotidienMinimum = reposQuotidienMinimum;
        this.heuresMinimumParSemaine = heuresMinimumParSemaine;
        this.heuresMaximumParSemaine = heuresMaximumParSemaine;
        this.nuitsMaximumParSemaine = nuitsMaximumParSemaine;
        this.joursConsecutifsMaximum = joursConsecutifsMaximum;
        this.nuitsConsecutivesMaximum = nuitsConsecutivesMaximum;
        this.joursReposMinimumApresNuits = joursReposMinimumApresNuits;
        this.dimanchesTravaillesMaximum = dimanchesTravaillesMaximum;
        this.reposHebdomadaireFenetreJours = reposHebdomadaireFenetreJours;
        this.reposHebdomadaireJoursOffMinimum = reposHebdomadaireJoursOffMinimum;
    }

    public Double getHeuresMinimumParJour() { return heuresMinimumParJour; }
    public Double getHeuresMaximumParJour() { return heuresMaximumParJour; }
    public Double getAmplitudeJournaliereMaximum() { return amplitudeJournaliereMaximum; }
    public Double getReposQuotidienMinimum() { return reposQuotidienMinimum; }
    public Double getHeuresMinimumParSemaine() { return heuresMinimumParSemaine; }
    public Double getHeuresMaximumParSemaine() { return heuresMaximumParSemaine; }
    public Integer getNuitsMaximumParSemaine() { return nuitsMaximumParSemaine; }
    public Integer getJoursConsecutifsMaximum() { return joursConsecutifsMaximum; }

    public Integer getNuitsConsecutivesMaximum() { return nuitsConsecutivesMaximum; }
    public Integer getJoursReposMinimumApresNuits() { return joursReposMinimumApresNuits; }
    public Integer getDimanchesTravaillesMaximum() { return dimanchesTravaillesMaximum; }
    public Integer getReposHebdomadaireFenetreJours() { return reposHebdomadaireFenetreJours; }
    public Integer getReposHebdomadaireJoursOffMinimum() { return reposHebdomadaireJoursOffMinimum; }

    /**
     * Bloc entièrement vide : aucun seuil, donc aucune contrainte individuelle active.
     *
     * <p>Sert de valeur de repli pour les salariés qui n'ont transmis aucune contrainte, afin que
     * les contraintes du solveur interrogent un seuil plutôt qu'une référence nulle.
     * Voir {@code SalarieReel.contraintesOuAucune()}.</p>
     */
    public static final ContraintesReglementairesSalarie AUCUNE =
            new ContraintesReglementairesSalarie(null, null, null, null, null, null, null, null);

    /**
     * Une borne individuelle est prise en compte dès qu'elle est renseignée — <strong>zéro
     * compris</strong>.
     *
     * <h3>Lecture littérale du zéro (arbitrage du lot S7.7)</h3>
     * <p>Un maximum à 0 interdit tout ; un minimum à 0 n'exige rien. Dans les deux cas le chiffre
     * garde son sens arithmétique, et <strong>seule l'absence</strong> désactive une règle.</p>
     *
     * <p>Le lot S7.0 avait retenu l'inverse — 0 valant désactivation — par prudence de migration.
     * Les données ont tranché : le jeu de référence SC-03 transmet
     * {@code nuitsMaximumParSemaine: 0} pour un salarié et {@code 3} pour l'autre. L'intention est
     * limpide, et la lire comme une désactivation aurait autorisé le premier à travailler toutes
     * les nuits — l'exact contraire de ce qui était demandé, et une entorse à l'invariant
     * « un vide ne suppose jamais que la chose est possible ».</p>
     *
     * <p>Une valeur négative n'a pas de sens comme borne : elle est traitée comme non renseignée
     * plutôt qu'appliquée à la lettre.</p>
     *
     * <p>Source unique de la règle : toutes les contraintes réglementaires individuelles la
     * consultent, aucune ne réimplémente son propre test.</p>
     *
     * <p>Cette phrase n'était vraie que de sept contraintes sur douze jusqu'au lot S8.3.
     * {@code AmplitudeJournaliere}, {@code HeuresMaximumParSemaine}, {@code HeuresMinimumParJour},
     * {@code JoursConsecutifsMax} et {@code ReposQuotidienMinimum} testaient {@code != null} en
     * direct : une borne négative les <strong>activait</strong> avec un seuil négatif, là où les
     * sept autres l'auraient tenue pour absente. Un même {@code -1} produisait donc deux
     * comportements opposés selon la règle qui le lisait.</p>
     */
    public static boolean borneRenseignee(Number borne) {
        return borne != null && borne.doubleValue() >= 0d;
    }

    /**
     * Une largeur de fenêtre est prise en compte à partir de 1 jour.
     *
     * <p>Distinction avec {@link #borneRenseignee(Number)} : une fenêtre est une
     * <strong>taille</strong>, pas une borne. « Au moins 2 jours off sur 0 jour » ne décrit
     * aucune règle, là où « au plus 0 nuit » en décrit une parfaitement. Le zéro littéral n'a de
     * sens que sur ce qui se compare, pas sur ce qui se mesure.</p>
     */
    public static boolean largeurRenseignee(Integer largeur) {
        return largeur != null && largeur >= 1;
    }
}
