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
 * <h3>Règle d'activation — un seuil absent ou nul désactive la contrainte</h3>
 * <p>Voir {@link #seuilActif(Number)}. Cette règle est la contrepartie de l'invariant
 * « un vide ne suppose jamais que la chose est possible » : le moteur ne devine pas un plafond
 * qu'on ne lui a pas donné, il s'abstient de juger. Le contrat demande à l'appelant d'
 * <strong>omettre</strong> le champ pour désactiver, jamais d'envoyer 0 ; un 0 reçu est tracé en
 * WARN par le mapper mais traité comme une désactivation, faute de pouvoir deviner l'intention.</p>
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
     * Un seuil individuel est actif s'il est renseigné et strictement positif.
     *
     * <p>Source unique de la règle : toutes les contraintes réglementaires individuelles la
     * consultent, aucune ne réimplémente son propre test de nullité. C'est ce qui garantit
     * qu'« omettre » et « envoyer 0 » produisent le même comportement moteur — le seul écart
     * entre les deux est la trace WARN émise à la cartographie.</p>
     */
    public static boolean seuilActif(Number seuil) {
        return seuil != null && seuil.doubleValue() > 0d;
    }
}
