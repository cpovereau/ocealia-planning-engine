package fr.project.planning.scenarios.dto.request;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import jakarta.validation.constraints.NotBlank;

/**
 * Sc02ScenarioParametersDTO — paramètres propres au remplacement d'un salarié absent.
 *
 * <h3>Ce bloc n'expose que ce que le moteur honore</h3>
 * <p>Le cadrage prévoit d'autres paramètres — liste de remplaçants autorisés, autorisation de
 * découpage. Ils <strong>ne figurent pas ici</strong> tant qu'aucune règle ne les lit : un champ
 * transporté que personne n'exploite est pire qu'un champ absent, puisque l'appelant le renseigne
 * et croit l'avoir dit. Ils arriveront avec les lots qui les mettent en œuvre. Voir
 * {@code 92_CADRAGE_SCENARIO_SC-02.md} §6.2 et §8.</p>
 *
 * <h3>[S5] Le bloc est strict — et il l'est vraiment</h3>
 * <p>Il était annoncé strict au motif qu'il ne portait <em>pas</em> de
 * {@code @JsonIgnoreProperties}. C'était faux : Spring Boot désactive
 * {@code FAIL_ON_UNKNOWN_PROPERTIES}, si bien qu'un paramètre inconnu était <strong>silencieusement
 * ignoré</strong>. Le contrat promettait donc un refus que le moteur ne prononçait pas — le pire
 * cas exactement, puisqu'un appelant qui envoie {@code remplacantsAutorises} en repartait avec une
 * réponse 200 et la conviction d'avoir été entendu.</p>
 *
 * <p>Le refus est désormais explicite et local à ce bloc, et il nomme le paramètre en cause. Les
 * autres DTO du contrat gardent le comportement tolérant : les aligner est un chantier à part,
 * inscrit au backlog.</p>
 */
public class Sc02ScenarioParametersDTO {

    /**
     * Salarié dont l'absence motive le scénario.
     *
     * <p>Il ne sert pas à la contrainte : l'absence elle-même est portée par le bloc
     * {@code dataSet.indisponibilites}, déjà tenu par une contrainte HARD. Il sert à savoir
     * <strong>de quelle absence</strong> le moteur doit raconter les conséquences — donc quels
     * créneaux libérer, et quoi restituer.</p>
     */
    @NotBlank(message = "scenarioParameters.salarieAbsentId est obligatoire")
    private String salarieAbsentId;

    /**
     * Autorise le report des heures non couvertes sur un poste virtuel.
     *
     * <p>Absent ou {@code null} vaut <strong>refus</strong> : le poste virtuel ne s'invite jamais
     * de lui-même, il faut l'avoir demandé. Les heures non couvertes reviennent alors sans
     * ressource, et sont totalisées dans {@code remplacement.heuresAPourvoir}.</p>
     */
    private Boolean posteVirtuelAutorise;

    /**
     * Charge journalière au-delà de laquelle l'encadrement estime un remplaçant en surcharge,
     * en heures décimales (lot S3).
     *
     * <p>Borne de <strong>confort</strong>, propre à cette demande — à ne pas confondre avec les
     * bornes réglementaires individuelles du salarié, qui gardent leur rôle. Son dépassement est
     * pesé et signalé, <strong>jamais éliminatoire</strong> : le moteur ne refuse pas, il rend
     * visible ce qu'il en coûte.</p>
     *
     * <p>Absent ou {@code null} : aucun seuil journalier, donc aucun dépassement de ce côté. Une
     * borne absente n'est pas une borne à zéro.</p>
     */
    private Double surchargeMaxHeuresJour;

    /**
     * Même chose sur la semaine calendaire lundi → dimanche (lot S3).
     *
     * <p>Les deux seuils sont indépendants : une longue journée dans une semaine creuse ne
     * franchit que le premier, une semaine chargée faite de journées ordinaires que le second.</p>
     */
    private Double surchargeMaxHeuresSemaine;

    public String getSalarieAbsentId() {
        return salarieAbsentId;
    }

    public void setSalarieAbsentId(String salarieAbsentId) {
        this.salarieAbsentId = salarieAbsentId;
    }

    public Boolean getPosteVirtuelAutorise() {
        return posteVirtuelAutorise;
    }

    public void setPosteVirtuelAutorise(Boolean posteVirtuelAutorise) {
        this.posteVirtuelAutorise = posteVirtuelAutorise;
    }

    public Double getSurchargeMaxHeuresJour() {
        return surchargeMaxHeuresJour;
    }

    public void setSurchargeMaxHeuresJour(Double surchargeMaxHeuresJour) {
        this.surchargeMaxHeuresJour = surchargeMaxHeuresJour;
    }

    public Double getSurchargeMaxHeuresSemaine() {
        return surchargeMaxHeuresSemaine;
    }

    public void setSurchargeMaxHeuresSemaine(Double surchargeMaxHeuresSemaine) {
        this.surchargeMaxHeuresSemaine = surchargeMaxHeuresSemaine;
    }

    /** Lecture décidée du drapeau : un vide ne suppose jamais que la chose est possible. */
    public boolean estPosteVirtuelAutorise() {
        return Boolean.TRUE.equals(posteVirtuelAutorise);
    }

    /**
     * Refuse tout paramètre que ce lot n'honore pas, en le nommant.
     *
     * <p>C'est ce qu'on veut pendant un déploiement par étapes : un appelant en avance sur le
     * moteur doit l'apprendre de la réponse, pas le déduire d'un résultat qui ne tient pas compte
     * de ce qu'il a demandé. Les deux canaux — HTTP et FileAdapter — s'appuient sur la même
     * désérialisation et refusent donc pareil.</p>
     */
    @JsonAnySetter
    void refuserParametreInconnu(String nom, Object valeur) {
        throw new IllegalArgumentException(
                "[SC-02] paramètre inconnu dans scenarioParameters : '" + nom + "'. Le bloc est "
                        + "strict — seuls salarieAbsentId, posteVirtuelAutorise, "
                        + "surchargeMaxHeuresJour et surchargeMaxHeuresSemaine sont honorés. "
                        + "Voir 50_SCENARIO_CONTRACT.md, section SC-02.");
    }
}
