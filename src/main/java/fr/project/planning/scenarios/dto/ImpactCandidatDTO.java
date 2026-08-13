package fr.project.planning.scenarios.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * ImpactCandidatDTO — ce que coûte le besoin à une ressource (SC-06, lot S5).
 *
 * <p>Une entrée par ressource <strong>réelle</strong> mobilisée par le candidat. Un poste virtuel
 * n'y figure pas : il ne porte ni contrat ni contraintes individuelles, et mesurer son amplitude
 * n'aurait aucun sens. Une solution entièrement portée par un poste virtuel a donc des impacts
 * vides — ce n'est pas une omission.</p>
 *
 * <p>Répond à l'exigence : « être en mesure de connaître, pour les solutions retenues, l'impact
 * sur l'amplitude journalière et hebdomadaire de la personne retenue ».</p>
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ImpactCandidatDTO {

    private String ressourceId;

    /** Amplitude du jour du besoin : du début du premier créneau à la fin du dernier. */
    private ImpactMesureDTO amplitudeJournaliere;

    /** Heures travaillées le jour du besoin. */
    private ImpactMesureDTO heuresJour;

    /** Heures travaillées sur la semaine calendaire lundi → dimanche du besoin. */
    private ImpactMesureDTO heuresSemaine;

    /**
     * Volume hebdomadaire habituel déclaré au contrat, en heures.
     * {@code null} si le contrat ne le déclare pas — l'écart n'est alors pas mesurable.
     */
    private Double heuresHabituellesSemaine;

    /**
     * [Équité L4] Série de jours travaillés d'affilée contenant le jour du besoin — <strong>en
     * jours</strong>, et non en heures comme les autres mesures de ce bloc.
     *
     * <p>Palier 5 du classement : ne pas rappeler qui enchaîne. {@code avant} vaut 0 quand la
     * personne ne travaille pas ce jour-là ; le besoin la fait alors entrer dans une série.
     * {@code plafond} reprend {@code joursConsecutifsMaximum} du salarié, s'il est déclaré.</p>
     */
    private ImpactMesureDTO joursConsecutifs;

    /**
     * [Équité L4] Écart <strong>signé</strong> au volume contractuel après affectation, en
     * pourcentage — palier 6 du classement.
     *
     * <p>Mesuré sur les heures <strong>pondérées</strong> par la pénibilité : on ne juge l'équité
     * qu'à pénibilité équivalente. Sans coefficients transmis, la pondération est neutre et cet
     * écart se recoupe avec {@code heuresSemaine.apres} et {@code heuresHabituellesSemaine}.</p>
     *
     * <p>Négatif : la personne est en dessous de son contrat, ce qui la rend <strong>préférable</strong>.
     * {@code null} quand le contrat ne déclare pas de volume — rien n'est alors comparable, et le
     * candidat est classé en dernier de ce palier plutôt que crédité d'un écart favorable.</p>
     */
    private Double ecartContratPourcent;

    public ImpactCandidatDTO() {
    }

    public ImpactCandidatDTO(String ressourceId,
                             ImpactMesureDTO amplitudeJournaliere,
                             ImpactMesureDTO heuresJour,
                             ImpactMesureDTO heuresSemaine,
                             Double heuresHabituellesSemaine) {
        this.ressourceId = ressourceId;
        this.amplitudeJournaliere = amplitudeJournaliere;
        this.heuresJour = heuresJour;
        this.heuresSemaine = heuresSemaine;
        this.heuresHabituellesSemaine = heuresHabituellesSemaine;
    }

    /** [Équité L4] Les deux grandeurs qui départagent aux paliers 5 et 6. */
    public void setCriteresEquite(ImpactMesureDTO joursConsecutifs, Double ecartContratPourcent) {
        this.joursConsecutifs = joursConsecutifs;
        this.ecartContratPourcent = ecartContratPourcent;
    }

    public ImpactMesureDTO getJoursConsecutifs() { return joursConsecutifs; }

    public Double getEcartContratPourcent() { return ecartContratPourcent; }

    public String getRessourceId() { return ressourceId; }
    public void setRessourceId(String ressourceId) { this.ressourceId = ressourceId; }

    public ImpactMesureDTO getAmplitudeJournaliere() { return amplitudeJournaliere; }
    public void setAmplitudeJournaliere(ImpactMesureDTO amplitudeJournaliere) { this.amplitudeJournaliere = amplitudeJournaliere; }

    public ImpactMesureDTO getHeuresJour() { return heuresJour; }
    public void setHeuresJour(ImpactMesureDTO heuresJour) { this.heuresJour = heuresJour; }

    public ImpactMesureDTO getHeuresSemaine() { return heuresSemaine; }
    public void setHeuresSemaine(ImpactMesureDTO heuresSemaine) { this.heuresSemaine = heuresSemaine; }

    public Double getHeuresHabituellesSemaine() { return heuresHabituellesSemaine; }
    public void setHeuresHabituellesSemaine(Double heuresHabituellesSemaine) { this.heuresHabituellesSemaine = heuresHabituellesSemaine; }
}
