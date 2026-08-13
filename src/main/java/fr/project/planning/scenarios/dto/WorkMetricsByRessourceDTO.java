package fr.project.planning.scenarios.dto;

import java.time.LocalDate;

public class WorkMetricsByRessourceDTO {

    private String resourceId;
    private LocalDate periodeDebut;
    private LocalDate periodeFin;

    private double heuresTravaillees;
    private double heuresNuit;
    private double heuresJourFerie;
    private double heuresReposHebdoTravaille;

    private int nbDimanchesTravailles;
    private int maxJoursConsecutifsObservees;
    private int maxNuitsConsecutivesObservees;

    // Phase 8 — créneaux de nuit affectés à ce salarié alors qu'il n'est pas travailleur de nuit
    private int nbCreneauxNuitNonNuit;

    /**
     * [Équité L1] Charge ramenée à l'unité commune de l'heure ordinaire.
     *
     * <p>Chaque minute est pondérée par le coefficient de <strong>sa seule</strong> catégorie de
     * pénibilité — celle que la dominance retient — de sorte qu'une nuit du dimanche n'est jamais
     * comptée deux fois. C'est cette grandeur, et non {@code heuresTravaillees}, qui rend deux
     * personnes comparables : on ne juge l'équité qu'à pénibilité équivalente.</p>
     *
     * <p>⚠️ <strong>Sans coefficients transmis, elle vaut exactement {@code heuresTravaillees}</strong> :
     * le moteur n'invente pas d'échelle, il applique le neutre et le signale par une alerte
     * {@code COEFFICIENTS_PENIBILITE_PAR_DEFAUT}. Deux valeurs identiques ne veulent donc pas dire
     * « aucune pénibilité », mais « aucune pondération demandée ».</p>
     */
    private double heuresPonderees;

    /**
     * [Équité L2] Écart <strong>signé</strong> entre la charge pondérée et le volume contractuel
     * attendu sur la fenêtre observée, en pourcentage.
     *
     * <p>Positif : au-dessus de son contrat. Négatif : en dessous — et cela doit le rendre
     * <strong>préférable</strong>, pas seulement « non exclu ». Sans écart signé, le moteur se
     * contenterait d'éviter de surcharger : il ne rééquilibrerait jamais.</p>
     *
     * <p>La comparaison se fait au contrat de chacun, jamais à la moyenne du groupe : un salarié
     * à 50 % y serait perpétuellement « sous la moyenne ».</p>
     *
     * <p>{@code null} si le salarié ne déclare pas de volume hebdomadaire — rien n'est alors
     * comparable, et le moteur préfère ne rien dire plutôt que de supposer.</p>
     */
    private Double ecartContratPourcent;

    /** [Équité L2] Part des heures de nuit dans le volume contractuel attendu, en pourcentage. */
    private Double partNuits;

    /** [Équité L2] Part des heures de dimanche non nocturnes, en pourcentage du contrat. */
    private Double partDimanches;

    /** [Équité L2] Part des heures de jour férié ni nocturnes ni dominicales, idem. */
    private Double partFeries;

    /**
     * [Équité L2] Jours de l'<strong>horizon déclaré</strong> — le dénominateur de
     * {@code ecartContratPourcent}, qui rapporte le contrat hebdomadaire à cette durée.
     *
     * <p>À ne pas confondre avec {@code periodeDebut} / {@code periodeFin}, qui bornent les
     * créneaux effectivement reçus : l'horizon dit sur quelle fenêtre le moteur a jugé, ces deux
     * dates disent ce que les données couvraient. Plus la fenêtre est large, plus l'écart au
     * contrat a de sens.</p>
     */
    private int joursObserves;

    public WorkMetricsByRessourceDTO() {
    }

    public WorkMetricsByRessourceDTO(
            String resourceId,
            LocalDate periodeDebut,
            LocalDate periodeFin,
            double heuresTravaillees,
            double heuresNuit,
            double heuresJourFerie,
            double heuresReposHebdoTravaille,
            int nbDimanchesTravailles,
            int maxJoursConsecutifsObservees,
            int maxNuitsConsecutivesObservees
    ) {
        this.resourceId = resourceId;
        this.periodeDebut = periodeDebut;
        this.periodeFin = periodeFin;
        this.heuresTravaillees = heuresTravaillees;
        this.heuresNuit = heuresNuit;
        this.heuresJourFerie = heuresJourFerie;
        this.heuresReposHebdoTravaille = heuresReposHebdoTravaille;
        this.nbDimanchesTravailles = nbDimanchesTravailles;
        this.maxJoursConsecutifsObservees = maxJoursConsecutifsObservees;
        this.maxNuitsConsecutivesObservees = maxNuitsConsecutivesObservees;
    }

    public String getResourceId() {
        return resourceId;
    }

    public void setResourceId(String resourceId) {
        this.resourceId = resourceId;
    }

    public LocalDate getPeriodeDebut() {
        return periodeDebut;
    }

    public void setPeriodeDebut(LocalDate periodeDebut) {
        this.periodeDebut = periodeDebut;
    }

    public LocalDate getPeriodeFin() {
        return periodeFin;
    }

    public void setPeriodeFin(LocalDate periodeFin) {
        this.periodeFin = periodeFin;
    }

    public double getHeuresTravaillees() {
        return heuresTravaillees;
    }

    public void setHeuresTravaillees(double heuresTravaillees) {
        this.heuresTravaillees = heuresTravaillees;
    }

    public double getHeuresNuit() {
        return heuresNuit;
    }

    public void setHeuresNuit(double heuresNuit) {
        this.heuresNuit = heuresNuit;
    }

    public double getHeuresJourFerie() {
        return heuresJourFerie;
    }

    public void setHeuresJourFerie(double heuresJourFerie) {
        this.heuresJourFerie = heuresJourFerie;
    }

    public double getHeuresReposHebdoTravaille() {
        return heuresReposHebdoTravaille;
    }

    public void setHeuresReposHebdoTravaille(double heuresReposHebdoTravaille) {
        this.heuresReposHebdoTravaille = heuresReposHebdoTravaille;
    }

    public int getNbDimanchesTravailles() {
        return nbDimanchesTravailles;
    }

    public void setNbDimanchesTravailles(int nbDimanchesTravailles) {
        this.nbDimanchesTravailles = nbDimanchesTravailles;
    }

    public int getMaxJoursConsecutifsObservees() {
        return maxJoursConsecutifsObservees;
    }

    public void setMaxJoursConsecutifsObservees(int maxJoursConsecutifsObservees) {
        this.maxJoursConsecutifsObservees = maxJoursConsecutifsObservees;
    }

    public int getMaxNuitsConsecutivesObservees() {
        return maxNuitsConsecutivesObservees;
    }

    public void setMaxNuitsConsecutivesObservees(int maxNuitsConsecutivesObservees) {
        this.maxNuitsConsecutivesObservees = maxNuitsConsecutivesObservees;
    }

    public int getNbCreneauxNuitNonNuit() {
        return nbCreneauxNuitNonNuit;
    }

    public double getHeuresPonderees() {
        return heuresPonderees;
    }

    public void setHeuresPonderees(double heuresPonderees) {
        this.heuresPonderees = heuresPonderees;
    }

    public Double getEcartContratPourcent() { return ecartContratPourcent; }
    public Double getPartNuits() { return partNuits; }
    public Double getPartDimanches() { return partDimanches; }
    public Double getPartFeries() { return partFeries; }

    public int getJoursObserves() { return joursObserves; }

    public void setMesuresContractuelles(int joursObserves, Double ecartContratPourcent,
                                         Double partNuits, Double partDimanches,
                                         Double partFeries) {
        this.joursObserves = joursObserves;
        this.ecartContratPourcent = ecartContratPourcent;
        this.partNuits = partNuits;
        this.partDimanches = partDimanches;
        this.partFeries = partFeries;
    }

    public void setNbCreneauxNuitNonNuit(int nbCreneauxNuitNonNuit) {
        this.nbCreneauxNuitNonNuit = nbCreneauxNuitNonNuit;
    }
}