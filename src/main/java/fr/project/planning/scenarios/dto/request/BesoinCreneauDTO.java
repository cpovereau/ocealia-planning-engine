package fr.project.planning.scenarios.dto.request;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalTime;

/**
 * BesoinCreneauDTO — un créneau du besoin à couvrir (SC-06, lot S4).
 *
 * <p>Volontairement distinct de {@code CreneauInputDTO}, pour deux raisons structurelles :</p>
 * <ul>
 *   <li><strong>pas de {@code date}</strong> — elle est portée une seule fois par
 *       {@link BesoinDTO}, ce qui rend l'invariant « tous les créneaux du besoin tombent le
 *       même jour » impossible à violer plutôt que simplement interdit ;</li>
 *   <li><strong>pas de {@code ressourceAffecteeId}</strong> — un besoin est la question posée,
 *       jamais une affectation. Le champ n'existe pas, donc ne peut pas être renseigné par
 *       mégarde.</li>
 * </ul>
 *
 * <p>Contrat strict : un champ inconnu est rejeté.</p>
 */
public class BesoinCreneauDTO {

    /**
     * Identifiant du créneau de besoin, fourni par l'appelant.
     * Convention WinDev : préfixe dédié ({@code BES-00X}), unique sur tout le scénario.
     * Le moteur le traite comme une chaîne opaque et le restitue à l'identique.
     */
    @NotBlank(message = "besoin.creneaux[].id est obligatoire")
    private String id;

    @NotNull(message = "besoin.creneaux[].heureDebut est obligatoire")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "HH:mm")
    private LocalTime heureDebut;

    @NotNull(message = "besoin.creneaux[].heureFin est obligatoire")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "HH:mm")
    private LocalTime heureFin;

    /** Code activité à servir, qui doit figurer au référentiel transmis. */
    @NotBlank(message = "besoin.creneaux[].codeActiviteId est obligatoire")
    private String codeActiviteId;

    /** Lieu où servir l'activité. Transporté et restitué ; ne restreint pas les candidats. */
    private String lieu;

    private String posteComptable;

    /** Créneau de nuit déclaré par l'appelant. */
    private Boolean segmentNuit;

    /** Jour férié déclaré par l'appelant. */
    private Boolean isJourFerie;

    // =========================
    // Getters / Setters
    // =========================

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public LocalTime getHeureDebut() { return heureDebut; }
    public void setHeureDebut(LocalTime heureDebut) { this.heureDebut = heureDebut; }

    public LocalTime getHeureFin() { return heureFin; }
    public void setHeureFin(LocalTime heureFin) { this.heureFin = heureFin; }

    public String getCodeActiviteId() { return codeActiviteId; }
    public void setCodeActiviteId(String codeActiviteId) { this.codeActiviteId = codeActiviteId; }

    public String getLieu() { return lieu; }
    public void setLieu(String lieu) { this.lieu = lieu; }

    public String getPosteComptable() { return posteComptable; }
    public void setPosteComptable(String posteComptable) { this.posteComptable = posteComptable; }

    public Boolean getSegmentNuit() { return segmentNuit; }
    public void setSegmentNuit(Boolean segmentNuit) { this.segmentNuit = segmentNuit; }

    public Boolean getIsJourFerie() { return isJourFerie; }
    public void setIsJourFerie(Boolean isJourFerie) { this.isJourFerie = isJourFerie; }
}
