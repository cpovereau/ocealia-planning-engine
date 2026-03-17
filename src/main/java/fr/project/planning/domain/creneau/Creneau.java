package fr.project.planning.domain.creneau;

import fr.project.planning.domain.ressource.Ressource;
import org.optaplanner.core.api.domain.entity.PlanningEntity;
import org.optaplanner.core.api.domain.variable.PlanningVariable;
import org.optaplanner.core.api.domain.lookup.PlanningId;


import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.Duration;

/**
 * Creneau
 *
 * Représente un besoin de travail à couvrir.
 * Porte la variable de décision principale du moteur.
 */
@PlanningEntity
public class Creneau implements Serializable {

    /* =========================
       Identité
       ========================= */

    @PlanningId
    private String id;

    /* =========================
       Données métier (faites d'entrée)
       ========================= */

    private LocalDate date;
    private LocalTime heureDebut;
    private LocalTime heureFin;

    /**
     * Durée du créneau en minutes.
     * Calculée en amont, jamais recalculée par le moteur.
     */
    private int duree;

    private String lieu;

    /**
     * Identifiant du code activité (clé stable pour les restitutions / jointures référentiel).
     * Optionnel pendant la phase de transition : si null, on peut se replier sur {@link #activite}.
     */
    private String codeActiviteId;

    /**
     * Libellé de l'activité (affichage / compatibilité).
     */
    private String activite;
    private String posteComptable;

    private PrioriteCreneau priorite;
    private TypeCreneau type;

    /* =========================
       Qualification temporelle
       ========================= */

    private TypePlageHoraire typePlageHoraire;
    private boolean jourFerie;

    /* =========================
        Qualification réglementaire du jour
        =========================*/

    private QualificationJour qualificationJour;

    /* =========================
       Structuration des besoins (Phase 5)
       ========================= */

    /**
     * Identifiant du groupe de besoin auquel appartient ce créneau.
     * Permet de regrouper des créneaux logiquement liés (ex : tous les créneaux d'une même semaine de garde).
     * Transporté en Phase 1, mappé en Phase 5. Non exploité par le solveur jusqu'à Phase 7+.
     */
    private String groupeBesoinId;

    /**
     * Identifiant du bloc journalier auquel appartient ce créneau.
     * Permet de regrouper les créneaux d'une même journée (ex : matin / après-midi / nuit).
     * Transporté en Phase 1, mappé en Phase 5.
     */
    private String blocJourId;

    /**
     * Position ordinale du créneau dans son bloc journalier.
     * Permet d'ordonner les créneaux au sein d'un même {@link #blocJourId}.
     * Transporté en Phase 1, mappé en Phase 5.
     */
    private Integer ordreDansBloc;

    /**
     * Indique si ce créneau représente un segment de pause (non productif).
     * Transporté en Phase 1, mappé en Phase 5. Servira aux contraintes de fragmentation (Phase 7+).
     */
    private Boolean estSegmentDePause;

    /* =========================
       Variable de décision
       ========================= */

    @PlanningVariable(valueRangeProviderRefs = "ressourceRange")
    private Ressource ressourceAffectee;

    /* =========================
       Calcul des intersections
       ========================= */

    public long getMinutesDansIntervalle(LocalTime debutPlage, LocalTime finPlage) {

    LocalDateTime creneauDebut = LocalDateTime.of(this.date, this.heureDebut);
    LocalDateTime creneauFin   = LocalDateTime.of(this.date, this.heureFin);

    if (!creneauFin.isAfter(creneauDebut)) {
        creneauFin = creneauFin.plusDays(1);
    }

    LocalDateTime plageDebut = LocalDateTime.of(this.date, debutPlage);
    LocalDateTime plageFin   = LocalDateTime.of(this.date, finPlage);

    if (!plageFin.isAfter(plageDebut)) {
        plageFin = plageFin.plusDays(1);
    }

    LocalDateTime debutIntersection = creneauDebut.isAfter(plageDebut)
        ? creneauDebut
        : plageDebut;

    LocalDateTime finIntersection = creneauFin.isBefore(plageFin)
        ? creneauFin
        : plageFin;

    if (finIntersection.isAfter(debutIntersection)) {
        return Duration.between(debutIntersection, finIntersection).toMinutes();
    }

    return 0;
    }

    /* =========================
       Constructeurs
       ========================= */

    public Creneau() {
        // requis par OptaPlanner
    }

    public Creneau(
            String id,
            LocalDate date,
            LocalTime heureDebut,
            LocalTime heureFin,
            int duree,
            String lieu,
            String activite,
            String posteComptable,
            PrioriteCreneau priorite,
            TypeCreneau type,
            TypePlageHoraire typePlageHoraire,
            boolean jourFerie,
            QualificationJour qualificationJour
    ) {
        this(
                id,
                date,
                heureDebut,
                heureFin,
                duree,
                lieu,
                null,
                activite,
                posteComptable,
                priorite,
                type,
                typePlageHoraire,
                jourFerie,
                qualificationJour
        );
    }

    public Creneau(
            String id,
            LocalDate date,
            LocalTime heureDebut,
            LocalTime heureFin,
            int duree,
            String lieu,
            String codeActiviteId,
            String activite,
            String posteComptable,
            PrioriteCreneau priorite,
            TypeCreneau type,
            TypePlageHoraire typePlageHoraire,
            boolean jourFerie,
            QualificationJour qualificationJour
    ) {
        this.id = id;
        this.date = date;
        this.heureDebut = heureDebut;
        this.heureFin = heureFin;
        this.duree = duree;
        this.lieu = lieu;
        this.codeActiviteId = codeActiviteId;
        this.activite = activite;
        this.posteComptable = posteComptable;
        this.priorite = priorite;
        this.type = type;
        this.typePlageHoraire = typePlageHoraire;
        this.jourFerie = jourFerie;
        this.qualificationJour = qualificationJour;
    }

    /* =========================
       Getters
       ========================= */

    public String getId() {
        return id;
    }

    public LocalDate getDate() {
        return date;
    }

    public LocalTime getHeureDebut() {
        return heureDebut;
    }

    public LocalTime getHeureFin() {
        return heureFin;
    }

    public int getDuree() {
        return duree;
    }

    public String getLieu() {
        return lieu;
    }

    public String getCodeActiviteId() {
        return codeActiviteId;
    }

    public String getActivite() {
        return activite;
    }

    public String getPosteComptable() {
        return posteComptable;
    }

    public PrioriteCreneau getPriorite() {
        return priorite;
    }

    public TypeCreneau getType() {
        return type;
    }

    public TypePlageHoraire getTypePlageHoraire() {
        return typePlageHoraire;
    }

    public boolean isJourFerie() {
        return jourFerie;
    }

    public Ressource getRessourceAffectee() {
        return ressourceAffectee;
    }

    public QualificationJour getQualificationJour() {
        return qualificationJour;
    }
    
     /* =========================
       Setters
       ========================= */
       
    public void setRessourceAffectee(Ressource ressourceAffectee) {
        this.ressourceAffectee = ressourceAffectee;
    }

    // Phase 5 — structuration des besoins

    public String getGroupeBesoinId() {
        return groupeBesoinId;
    }

    public void setGroupeBesoinId(String groupeBesoinId) {
        this.groupeBesoinId = groupeBesoinId;
    }

    public String getBlocJourId() {
        return blocJourId;
    }

    public void setBlocJourId(String blocJourId) {
        this.blocJourId = blocJourId;
    }

    public Integer getOrdreDansBloc() {
        return ordreDansBloc;
    }

    public void setOrdreDansBloc(Integer ordreDansBloc) {
        this.ordreDansBloc = ordreDansBloc;
    }

    public Boolean getEstSegmentDePause() {
        return estSegmentDePause;
    }

    public void setEstSegmentDePause(Boolean estSegmentDePause) {
        this.estSegmentDePause = estSegmentDePause;
    }
}



