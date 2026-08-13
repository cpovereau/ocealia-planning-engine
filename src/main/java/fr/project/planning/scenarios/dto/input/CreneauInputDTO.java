package fr.project.planning.scenarios.dto.input;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import fr.project.planning.domain.creneau.CodeActivite;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * CreneauInputDTO — DTO de transport pour un créneau entrant (WinDev → moteur).
 *
 * <p>Distinct de {@code CreneauPlanningDTO}, qui est un DTO de sortie.</p>
 *
 * <p>Phase 5 : {@code groupeBesoinId}, {@code blocJourId}, {@code ordreDansBloc},
 * {@code estSegmentDePause} exploités.</p>
 *
 * <p>Champ ⚠️ <strong>déprécié conservé</strong> : {@code activite}, repli si
 * {@code codeActiviteId} est absent.</p>
 *
 * <p>Créneau traversant minuit : {@code date} = jour de début, {@code heureFin < heureDebut}.</p>
 *
 * <h3>[Rang 11] Les quatre noms tolérés le sont nommément</h3>
 * <p>La phase 10C avait retiré le {@code @JsonIgnoreProperties} de cette classe en concluant à un
 * « contrat strict ». Le retrait ne rendait rien strict — Spring Boot désactive
 * {@code FAIL_ON_UNKNOWN_PROPERTIES} — mais il a supprimé la seule trace, dans le code, des quatre
 * champs que le contrat publié <strong>promet d'accepter et d'ignorer</strong> :
 * {@code priorite}, {@code type}, {@code isReposHebdo} et {@code axesOrganisationnels}. Voir
 * {@code 50_ScenarioContract.schema.json}, où les quatre sont marqués « encore accepté et
 * silencieusement ignoré ; ne plus émettre ».</p>
 *
 * <p>Maintenant que le contrat refuse réellement l'inconnu, ces quatre noms doivent être
 * <strong>déclarés</strong> : ce ne sont pas des inconnus, ce sont des retraités. Un intégrateur
 * qui n'a pas fini sa migration n'a pas à voir sa requête refusée pour un champ que le contrat lui
 * dit d'ignorer — il continuera de le lire, sans effet, jusqu'à ce que le contrat le retire
 * vraiment.</p>
 */
@JsonIgnoreProperties({"priorite", "type", "isReposHebdo", "axesOrganisationnels"})
public class CreneauInputDTO {

    private String id;
    private LocalDate date;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "HH:mm")
    private LocalTime heureDebut;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "HH:mm")
    private LocalTime heureFin;

    private String lieu;
    private String codeActiviteId;

    @Deprecated
    // TODO Phase X : suppression après migration WinDev
    private String activite;        // ⚠️ DÉPRÉCIÉ — fallback si codeActiviteId absent
    
    private String posteComptable;

    /**
     * [Lot S1] Identifiant de la ressource déjà affectée à ce créneau.
     *
     * Renseigné lorsque le créneau relève d'un planning existant transmis comme fait acquis.
     * Absent lorsque le créneau est une décision à prendre — cas de SC-01 et SC-03, dont le
     * comportement est inchangé.
     *
     * Le moteur ne résout ce champ que dans les scénarios qui figent leur planning d'entrée.
     */
    private String ressourceAffecteeId;

    private Boolean isJourFerie;
    private Boolean segmentNuit;

    // [Phase 5] structuration des besoins
    private String groupeBesoinId;
    private String blocJourId;
    private Integer ordreDansBloc;
    private Boolean estSegmentDePause;

    // =========================
    // Getters / Setters
    // =========================

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public LocalDate getDate() { return date; }
    public void setDate(LocalDate date) { this.date = date; }

    public LocalTime getHeureDebut() { return heureDebut; }
    public void setHeureDebut(LocalTime heureDebut) { this.heureDebut = heureDebut; }

    public LocalTime getHeureFin() { return heureFin; }
    public void setHeureFin(LocalTime heureFin) { this.heureFin = heureFin; }

    public String getLieu() { return lieu; }
    public void setLieu(String lieu) { this.lieu = lieu; }

    public String getCodeActiviteId() { return codeActiviteId; }
    public void setCodeActiviteId(String codeActiviteId) { this.codeActiviteId = codeActiviteId; }

    public String getActivite() { return activite; }
    public void setActivite(String activite) { this.activite = activite; }

    /**
     * Code activité effectif — même règle que {@code Creneau.getCodeActiviteEffectif()},
     * applicable dès la préparation, avant que l'entité ne soit construite.
     *
     * <p>{@code @JsonIgnore} : propriété dérivée, elle ne fait pas partie du contrat d'entrée
     * et ne doit pas élargir la surface acceptée par le désérialiseur strict.</p>
     */
    @JsonIgnore
    public String getCodeActiviteEffectif() {
        return CodeActivite.effectif(codeActiviteId, activite);
    }

    public String getPosteComptable() { return posteComptable; }
    public void setPosteComptable(String posteComptable) { this.posteComptable = posteComptable; }

    public String getRessourceAffecteeId() { return ressourceAffecteeId; }
    public void setRessourceAffecteeId(String ressourceAffecteeId) { this.ressourceAffecteeId = ressourceAffecteeId; }

    public Boolean getIsJourFerie() { return isJourFerie; }
    public void setIsJourFerie(Boolean isJourFerie) { this.isJourFerie = isJourFerie; }

    public Boolean getSegmentNuit() { return segmentNuit; }
    public void setSegmentNuit(Boolean segmentNuit) { this.segmentNuit = segmentNuit; }

    public String getGroupeBesoinId() { return groupeBesoinId; }
    public void setGroupeBesoinId(String groupeBesoinId) { this.groupeBesoinId = groupeBesoinId; }

    public String getBlocJourId() { return blocJourId; }
    public void setBlocJourId(String blocJourId) { this.blocJourId = blocJourId; }

    public Integer getOrdreDansBloc() { return ordreDansBloc; }
    public void setOrdreDansBloc(Integer ordreDansBloc) { this.ordreDansBloc = ordreDansBloc; }

    public Boolean getEstSegmentDePause() { return estSegmentDePause; }
    public void setEstSegmentDePause(Boolean estSegmentDePause) { this.estSegmentDePause = estSegmentDePause; }
}
