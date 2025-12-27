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

}
