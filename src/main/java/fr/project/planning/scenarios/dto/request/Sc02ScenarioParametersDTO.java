package fr.project.planning.scenarios.dto.request;

import jakarta.validation.constraints.NotBlank;

/**
 * Sc02ScenarioParametersDTO — paramètres propres au remplacement d'un salarié absent.
 *
 * <h3>Ce bloc n'expose que ce que le lot S1 honore</h3>
 * <p>Le cadrage prévoit d'autres paramètres — liste de remplaçants autorisés, seuils de surcharge,
 * autorisation de découpage. Ils <strong>ne figurent pas ici</strong> tant qu'aucune règle ne les
 * lit : un champ transporté que personne n'exploite est pire qu'un champ absent, puisque
 * l'appelant le renseigne et croit l'avoir dit. Ils arriveront avec les lots qui les mettent en
 * œuvre. Voir {@code 92_CADRAGE_SCENARIO_SC-02.md} §6.2 et §8.</p>
 *
 * <p>Le bloc est <strong>strict</strong> — aucun {@code @JsonIgnoreProperties}. Un appelant qui
 * enverrait aujourd'hui un paramètre d'un lot à venir obtient une erreur explicite plutôt qu'un
 * silence : c'est ce qu'on veut pendant un déploiement par étapes.</p>
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

    /** Lecture décidée du drapeau : un vide ne suppose jamais que la chose est possible. */
    public boolean estPosteVirtuelAutorise() {
        return Boolean.TRUE.equals(posteVirtuelAutorise);
    }
}
