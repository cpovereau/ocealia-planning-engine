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
