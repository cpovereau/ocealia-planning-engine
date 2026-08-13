package fr.project.planning.domain.creneau;

import fr.project.planning.domain.ressource.Ressource;
import org.optaplanner.core.api.domain.entity.PlanningEntity;
import org.optaplanner.core.api.domain.entity.PlanningPin;
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

    /**
     * Créneau figé : son affectation est un fait acquis, pas une décision.
     *
     * <p>[Lot S1] Un créneau figé est retiré de l'espace de recherche du solveur, qui ne peut
     * ni le déplacer ni le désaffecter. Il reste pleinement visible des contraintes : c'est
     * précisément ce qui permet à un planning existant de peser sur les décisions restantes,
     * sans être lui-même remis en cause.</p>
     *
     * <p>Vaut {@code false} par défaut — SC-01 et SC-03 ne figent rien et conservent donc leur
     * comportement, tous leurs créneaux restant des variables de décision.</p>
     *
     * <p><strong>Invariant</strong> : un créneau figé porte toujours une ressource. Figer un
     * créneau sans affectation laisserait la solution éternellement non initialisée, le solveur
     * n'ayant pas le droit de lui en attribuer une. {@link #figerSur(Ressource)} est l'unique
     * moyen de figer, et rend cet état impossible à construire.</p>
     */
    @PlanningPin
    private boolean fige;

    /**
     * Créneau dont celui-ci est un morceau, ou {@code null} s'il n'en est pas un.
     *
     * <p>[Lot S2 de SC-02] OptaPlanner affecte des valeurs à un ensemble d'entités fixé à la
     * construction du problème : découper un créneau ne peut donc pas être une décision du
     * solveur. Les segments sont préparés en amont, et ce champ est ce qui permet ensuite de les
     * reconnaître — pour mesurer le bloc réellement confié à quelqu'un, pour pénaliser
     * l'éparpillement d'un même besoin entre plusieurs personnes, et pour recombiner les segments
     * contigus au moment de restituer.</p>
     *
     * <p>Nul pour tout créneau reçu tel quel, donc pour l'intégralité de SC-01, SC-03 et SC-06.</p>
     */
    private String creneauOrigineId;

    /* =========================
       Bornes absolues du créneau
       ========================= */

    /**
     * Instant de début du créneau : sa {@link #date}, à son {@link #heureDebut}.
     */
    public LocalDateTime getDebutEffectif() {
        return LocalDateTime.of(this.date, this.heureDebut);
    }

    /**
     * Instant de fin du créneau, <strong>minuit franchi compris</strong>.
     *
     * <p>Convention du contrat d'entrée : {@code date} désigne le jour de <em>début</em>. Une
     * heure de fin qui n'est pas postérieure à l'heure de début tombe donc le lendemain — un
     * créneau 22:00–06:00 du 3 mars se termine le 4 mars à 06:00.</p>
     *
     * <p>[Lot S8.3] Cette convention était réécrite partout où l'on en avait besoin :
     * {@link #getMinutesDansIntervalle}, {@code ReposQuotidienMinimum.totalDeficitMinutes},
     * {@code AmplitudeJournaliere.calculerAmplitudeMinutes}. {@code LimitePhysique} l'avait
     * omise et comparait des {@link LocalTime} nus : deux créneaux se chevauchant de part et
     * d'autre de minuit ne produisaient aucun point HARD. Deux méthodes portent désormais la
     * règle, et les contraintes les appellent au lieu de la redécrire.</p>
     */
    public LocalDateTime getFinEffectif() {
        LocalDateTime debut = getDebutEffectif();
        LocalDateTime fin = LocalDateTime.of(this.date, this.heureFin);
        return fin.isAfter(debut) ? fin : fin.plusDays(1);
    }

    /**
     * Indique si le créneau couvre effectivement le jour civil fourni — au moins une minute.
     *
     * <p>Un créneau ne peut toucher que deux jours civils au plus, sa date et le lendemain. Un
     * créneau 14:00–00:00 se termine à minuit pile : il ne couvre pas le lendemain, et la
     * comparaison stricte le dit sans cas particulier.</p>
     */
    public boolean couvre(LocalDate jour) {
        LocalDateTime debutJour = jour.atStartOfDay();
        return getDebutEffectif().isBefore(debutJour.plusDays(1))
                && getFinEffectif().isAfter(debutJour);
    }

    /**
     * Indique si le créneau empiète sur la période fournie, bornes comprises et prises en
     * <strong>jours entiers</strong> — au moins une minute suffit.
     *
     * <p>[Lot S0 de SC-02] Une période d'absence est déclarée en dates, pas en horaires : elle
     * couvre donc du premier jour à 00:00 au lendemain du dernier jour à 00:00. Comparer la seule
     * {@link #date} du créneau à ces bornes laissait passer le créneau qui traverse minuit — un
     * 3 mars 22:00–06:00 échappait à une absence déclarée le 4 mars, alors qu'il fait travailler
     * six heures pendant celle-ci.</p>
     *
     * <p>Même famille de défaut que les quatre calculs réparés au lot S8.3, sur deux lecteurs que
     * ce lot n'avait pas couverts : {@code IndisponibiliteSalarie} et le filtre d'éligibilité de
     * SC-06. Ils appellent désormais cette méthode au lieu de redécrire la règle.</p>
     *
     * @param debut premier jour de la période, incluse ; {@code false} si null
     * @param fin   dernier jour de la période, inclus ; {@code false} si null
     */
    public boolean chevauchePeriode(LocalDate debut, LocalDate fin) {
        if (debut == null || fin == null || this.date == null) {
            return false;
        }
        LocalDateTime debutPeriode = debut.atStartOfDay();
        LocalDateTime finPeriode = fin.plusDays(1).atStartOfDay();
        return getDebutEffectif().isBefore(finPeriode)
                && getFinEffectif().isAfter(debutPeriode);
    }

    /* =========================
       Calcul des intersections
       ========================= */

    public long getMinutesDansIntervalle(LocalTime debutPlage, LocalTime finPlage) {

    LocalDateTime creneauDebut = getDebutEffectif();
    LocalDateTime creneauFin   = getFinEffectif();

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

    /**
     * Code activité effectif : {@link #codeActiviteId} en priorité, {@link #activite} en repli.
     *
     * <p>Seul point d'entrée pour lire l'activité d'un créneau — contraintes, calcul des
     * métriques et restitution API passent tous par ici, de sorte que le score et la réponse
     * exposent la même clé. La règle elle-même vit dans {@link CodeActivite}, partagée avec
     * le DTO d'entrée.</p>
     *
     * @return le code activité effectif, ou null si aucun des deux champs n'est renseigné
     */
    public String getCodeActiviteEffectif() {
        return CodeActivite.effectif(codeActiviteId, activite);
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

    // Lot S1 — figement

    /**
     * Indique si ce créneau est figé, c'est-à-dire soustrait aux décisions du solveur.
     */
    public boolean isFige() {
        return fige;
    }

    /** Créneau dont celui-ci est un morceau, ou {@code null} s'il n'en est pas un. */
    public String getCreneauOrigineId() {
        return creneauOrigineId;
    }

    public void setCreneauOrigineId(String creneauOrigineId) {
        this.creneauOrigineId = creneauOrigineId;
    }

    /**
     * Identifiant du besoin auquel ce créneau répond : le sien s'il est entier, celui de son
     * origine s'il n'en est qu'un morceau.
     */
    public String getIdBesoin() {
        return creneauOrigineId != null ? creneauOrigineId : id;
    }

    /**
     * Fige ce créneau sur une ressource : l'affectation devient un fait d'entrée.
     *
     * <p>Unique moyen de figer un créneau. Aucun {@code setFige(boolean)} n'est exposé :
     * figer et affecter sont indissociables, et les séparer permettrait de construire un
     * créneau figé sans ressource — état qui bloquerait la résolution (voir {@link #fige}).</p>
     *
     * @param ressource ressource sur laquelle figer le créneau, jamais {@code null}
     * @throws NullPointerException si {@code ressource} est {@code null}
     */
    public void figerSur(Ressource ressource) {
        this.ressourceAffectee = java.util.Objects.requireNonNull(
                ressource, "Un créneau figé porte toujours une ressource.");
        this.fige = true;
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



