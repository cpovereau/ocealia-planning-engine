package fr.project.planning.domain.ressource;

/**
 * ContratSalarie — objet valeur domaine, lot S2.
 *
 * <p>Décrit ce que le salarié <strong>fait normalement</strong> : volumes habituels et régime
 * d'annualisation. Pendant descriptif de {@link ContraintesReglementairesSalarie}, qui porte les
 * plafonds prescriptifs.</p>
 *
 * <p>Immuable, comme {@link ContraintesReglementairesSalarie}. Tous les champs sont facultatifs :
 * le moteur doit fonctionner sans eux.</p>
 *
 * <h3>Valeurs absentes</h3>
 * <p>Les valeurs brutes restent accessibles telles que reçues — {@code null} compris — afin que
 * l'absence d'information demeure distinguable d'une valeur transmise. Les méthodes
 * {@code …Effectif()} exposent en regard la valeur réellement appliquée par le moteur.</p>
 *
 * <p>Lot S2 : mappé, non exploité. Aucune contrainte ne lit cet objet.</p>
 */
public class ContratSalarie {

    /**
     * Nombre de jours travaillés par semaine appliqué à défaut d'information.
     * Valeur métier arbitrée : un temps plein sur cinq jours.
     */
    public static final int JOURS_TRAVAILLES_PAR_SEMAINE_DEFAUT = 5;

    private final Double heuresMoyennesParJour;
    private final Double heuresHebdomadairesHabituelles;
    private final Integer joursTravaillesParSemaine;
    private final Boolean estAnnualise;

    public ContratSalarie(
            Double heuresMoyennesParJour,
            Double heuresHebdomadairesHabituelles,
            Integer joursTravaillesParSemaine,
            Boolean estAnnualise
    ) {
        this.heuresMoyennesParJour = heuresMoyennesParJour;
        this.heuresHebdomadairesHabituelles = heuresHebdomadairesHabituelles;
        this.joursTravaillesParSemaine = joursTravaillesParSemaine;
        this.estAnnualise = estAnnualise;
    }

    // =========================
    // Valeurs brutes — telles que reçues
    // =========================

    public Double getHeuresMoyennesParJour() { return heuresMoyennesParJour; }

    public Double getHeuresHebdomadairesHabituelles() { return heuresHebdomadairesHabituelles; }

    public Integer getJoursTravaillesParSemaine() { return joursTravaillesParSemaine; }

    public Boolean getEstAnnualise() { return estAnnualise; }

    // =========================
    // Valeurs effectives — appliquées par le moteur
    // =========================

    /**
     * Nombre de jours travaillés par semaine réellement appliqué.
     *
     * @return la valeur transmise, ou {@link #JOURS_TRAVAILLES_PAR_SEMAINE_DEFAUT} si absente
     */
    public int joursTravaillesParSemaineEffectif() {
        return joursTravaillesParSemaine != null
                ? joursTravaillesParSemaine
                : JOURS_TRAVAILLES_PAR_SEMAINE_DEFAUT;
    }

    /**
     * Indique si le salarié est annualisé.
     *
     * <p>Une information absente ne vaut jamais autorisation : {@code null} est lu comme
     * « non annualisé ». Principe posé au cadrage — un vide ne suppose jamais que la chose
     * est possible.</p>
     */
    public boolean estAnnualise() {
        return Boolean.TRUE.equals(estAnnualise);
    }
}
