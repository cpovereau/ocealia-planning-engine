package fr.project.planning.scenarios.dto.input;

/**
 * ContraintesReglementairesDTO — transport des contraintes réglementaires individuelles.
 *
 * <p>Correspondance avec le schéma (ScenarioContract.schema.json §salarie.contraintesReglementaires) :</p>
 * <pre>
 *   heuresMinimumParJour              → borne basse journalière (h)
 *   heuresMaximumParJour              → borne haute journalière (h)
 *   amplitudeJournaliereMaximum       → amplitude max (début 1er → fin dernier créneau) (h)
 *   reposQuotidienMinimum             → repos entre deux journées travaillées (h)
 *   heuresMinimumParSemaine           → borne basse hebdomadaire (h)
 *   heuresMaximumParSemaine           → borne haute hebdomadaire (h)
 *   nuitsMaximumParSemaine            → nuits autorisées par semaine
 *   joursConsecutifsMaximum           → jours consécutifs travaillés max
 * </pre>
 *
 * <p>Ajoutés au lot S7.0, rapatriés depuis les seuils globaux :</p>
 * <pre>
 *   nuitsConsecutivesMaximum          → nuits consécutives autorisées
 *   joursReposMinimumApresNuits       → jours calendaires de repos après une séquence de nuits
 *   dimanchesTravaillesMaximum        → dimanches travaillés autorisés sur la période
 *   reposHebdomadaireFenetreJours     → largeur de la fenêtre glissante (j)
 *   reposHebdomadaireJoursOffMinimum  → jours non travaillés exigés dans cette fenêtre
 * </pre>
 *
 * <p>Les deux derniers champs forment une paire : la fenêtre sans le minimum de jours off ne
 * décrit aucune règle. Le mapper les transporte tels quels ; c'est la contrainte qui exige les
 * deux pour s'activer.</p>
 *
 * <p>Tous les champs sont facultatifs. <strong>Omettre</strong> un champ désactive la contrainte
 * correspondante ; envoyer {@code 0} produit le même effet mais est signalé en WARN — le contrat
 * demande l'omission.</p>
 */
public class ContraintesReglementairesDTO {

    private Double heuresMinimumParJour;
    private Double heuresMaximumParJour;
    private Double amplitudeJournaliereMaximum;
    private Double reposQuotidienMinimum;
    private Double heuresMinimumParSemaine;
    private Double heuresMaximumParSemaine;
    private Integer nuitsMaximumParSemaine;
    private Integer joursConsecutifsMaximum;

    private Integer nuitsConsecutivesMaximum;
    private Integer joursReposMinimumApresNuits;
    private Integer dimanchesTravaillesMaximum;
    private Integer reposHebdomadaireFenetreJours;
    private Integer reposHebdomadaireJoursOffMinimum;

    public Double getHeuresMinimumParJour() { return heuresMinimumParJour; }
    public void setHeuresMinimumParJour(Double heuresMinimumParJour) { this.heuresMinimumParJour = heuresMinimumParJour; }

    public Double getHeuresMaximumParJour() { return heuresMaximumParJour; }
    public void setHeuresMaximumParJour(Double heuresMaximumParJour) { this.heuresMaximumParJour = heuresMaximumParJour; }

    public Double getAmplitudeJournaliereMaximum() { return amplitudeJournaliereMaximum; }
    public void setAmplitudeJournaliereMaximum(Double amplitudeJournaliereMaximum) { this.amplitudeJournaliereMaximum = amplitudeJournaliereMaximum; }

    public Double getReposQuotidienMinimum() { return reposQuotidienMinimum; }
    public void setReposQuotidienMinimum(Double reposQuotidienMinimum) { this.reposQuotidienMinimum = reposQuotidienMinimum; }

    public Double getHeuresMinimumParSemaine() { return heuresMinimumParSemaine; }
    public void setHeuresMinimumParSemaine(Double heuresMinimumParSemaine) { this.heuresMinimumParSemaine = heuresMinimumParSemaine; }

    public Double getHeuresMaximumParSemaine() { return heuresMaximumParSemaine; }
    public void setHeuresMaximumParSemaine(Double heuresMaximumParSemaine) { this.heuresMaximumParSemaine = heuresMaximumParSemaine; }

    public Integer getNuitsMaximumParSemaine() { return nuitsMaximumParSemaine; }
    public void setNuitsMaximumParSemaine(Integer nuitsMaximumParSemaine) { this.nuitsMaximumParSemaine = nuitsMaximumParSemaine; }

    public Integer getJoursConsecutifsMaximum() { return joursConsecutifsMaximum; }
    public void setJoursConsecutifsMaximum(Integer joursConsecutifsMaximum) { this.joursConsecutifsMaximum = joursConsecutifsMaximum; }

    public Integer getNuitsConsecutivesMaximum() { return nuitsConsecutivesMaximum; }
    public void setNuitsConsecutivesMaximum(Integer nuitsConsecutivesMaximum) { this.nuitsConsecutivesMaximum = nuitsConsecutivesMaximum; }

    public Integer getJoursReposMinimumApresNuits() { return joursReposMinimumApresNuits; }
    public void setJoursReposMinimumApresNuits(Integer joursReposMinimumApresNuits) { this.joursReposMinimumApresNuits = joursReposMinimumApresNuits; }

    public Integer getDimanchesTravaillesMaximum() { return dimanchesTravaillesMaximum; }
    public void setDimanchesTravaillesMaximum(Integer dimanchesTravaillesMaximum) { this.dimanchesTravaillesMaximum = dimanchesTravaillesMaximum; }

    public Integer getReposHebdomadaireFenetreJours() { return reposHebdomadaireFenetreJours; }
    public void setReposHebdomadaireFenetreJours(Integer reposHebdomadaireFenetreJours) { this.reposHebdomadaireFenetreJours = reposHebdomadaireFenetreJours; }

    public Integer getReposHebdomadaireJoursOffMinimum() { return reposHebdomadaireJoursOffMinimum; }
    public void setReposHebdomadaireJoursOffMinimum(Integer reposHebdomadaireJoursOffMinimum) { this.reposHebdomadaireJoursOffMinimum = reposHebdomadaireJoursOffMinimum; }
}
