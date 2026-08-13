package fr.project.planning.domain.workmetrics;

public class WorkMetrics {

    private final String ressourceId;

    private int minutesTravaillees;
    private int minutesNuit;
    private int minutesJourFerie;
    private int minutesReposHebdoTravaille;
    private int nbDimanchesTravailles;

    // V1 ciblée : pour explicabilité future (sans analyse RH encore)
    private int nbCreneauxReposHebdoDetteRepos;

    // V3-B : Séquences observées (post-résolution, descriptif)
    private int maxJoursConsecutifsObservees;
    private int maxNuitsConsecutivesObservees;

    // Phase 8 — inadéquation nuit : créneaux de nuit affectés à un salarié non-nuit
    private int nbCreneauxNuitNonNuit;

    /*
     * [Équité L1] Minutes réparties par dominance — une minute n'appartient qu'à une catégorie.
     *
     * Distinctes de minutesNuit et minutesJourFerie ci-dessus, qui comptent les intersections
     * brutes et se recouvrent : une nuit du dimanche y figure dans les deux. Ici elle ne compte
     * qu'une fois, dans la dominante. Les quatre volumes s'additionnent exactement à
     * minutesTravaillees : toute minute est quelque part, et nulle part deux fois.
     */
    private int minutesNuitDominante;
    private int minutesDimancheDominante;
    private int minutesFerieDominante;
    private int minutesOrdinaires;

    /**
     * [Équité L1] Minutes ramenées à l'unité commune de l'heure ordinaire.
     *
     * <p>Fractionnaire par nature : un coefficient de 1,5 sur 47 minutes ne tombe pas juste. La
     * mesure sert à comparer, pas à facturer.</p>
     */
    private double minutesPonderees;

    /*
     * [Équité L2] Mesures rapportées au contrat, en pourcentage — nulles quand le salarié ne
     * déclare pas de volume hebdomadaire. Rien n'est alors comparable, et le moteur préfère ne
     * rien dire plutôt que de supposer.
     */
    private Double ecartContratPourcent;
    private Double partNuits;
    private Double partDimanches;
    private Double partFeries;

    /** [Équité L2] Jours de l'horizon déclaré — le dénominateur de {@code ecartContratPourcent}. */
    private int joursObserves;

    public WorkMetrics(String ressourceId) {
        this.ressourceId = ressourceId;
    }

    public String getRessourceId() { return ressourceId; }

    public int getMinutesTravaillees() { return minutesTravaillees; }
    public int getMinutesNuit() { return minutesNuit; }
    public int getMinutesJourFerie() { return minutesJourFerie; }
    public int getMinutesReposHebdoTravaille() { return minutesReposHebdoTravaille; }
    public int getNbCreneauxReposHebdoDetteRepos() { return nbCreneauxReposHebdoDetteRepos; }

    // Méthodes d'accumulation (utilisées par le calcul post-solve)
    public void addTravail(int minutes) { this.minutesTravaillees += minutes; }
    public void addNuit(int minutes) { this.minutesNuit += minutes; }
    public void addJourFerie(int minutes) { this.minutesJourFerie += minutes; }
    public void addReposHebdoTravaille(int minutes) { this.minutesReposHebdoTravaille += minutes; }
    public void incReposHebdoDetteRepos() { this.nbCreneauxReposHebdoDetteRepos++; }

    // V2 : méthodes de calcul dérivé
    public int getNbDimanchesTravailles() {
    return nbDimanchesTravailles;
    }

    public void incDimancheTravaille() {
    this.nbDimanchesTravailles++;
    }

    public int getMaxJoursConsecutifsObservees() { return maxJoursConsecutifsObservees; }
    public int getMaxNuitsConsecutivesObservees() { return maxNuitsConsecutivesObservees; }

    public void setMaxJoursConsecutifsObservees(int value) { this.maxJoursConsecutifsObservees = value; }
    public void setMaxNuitsConsecutivesObservees(int value) { this.maxNuitsConsecutivesObservees = value; }

    // Phase 8 — inadéquation nuit
    public int getNbCreneauxNuitNonNuit() { return nbCreneauxNuitNonNuit; }
    public void incNuitNonNuit() { this.nbCreneauxNuitNonNuit++; }

    // [Équité L1] Répartition par dominance et mesure pondérée
    public int getMinutesNuitDominante() { return minutesNuitDominante; }
    public int getMinutesDimancheDominante() { return minutesDimancheDominante; }
    public int getMinutesFerieDominante() { return minutesFerieDominante; }
    public int getMinutesOrdinaires() { return minutesOrdinaires; }
    public double getMinutesPonderees() { return minutesPonderees; }

    // [Équité L2] Mesures rapportées au contrat
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

    public void addPenibilites(int nuit, int dimanche, int ferie, int ordinaires, double ponderees) {
        this.minutesNuitDominante += nuit;
        this.minutesDimancheDominante += dimanche;
        this.minutesFerieDominante += ferie;
        this.minutesOrdinaires += ordinaires;
        this.minutesPonderees += ponderees;
    }
}