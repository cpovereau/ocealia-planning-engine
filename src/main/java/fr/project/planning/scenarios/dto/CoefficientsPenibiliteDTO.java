package fr.project.planning.scenarios.dto;

/**
 * CoefficientsPenibiliteDTO — ce que vaut une heure selon quand elle est travaillée (équité L1).
 *
 * <p>Sert à rendre comparables des heures qui ne le sont pas : <em>on ne juge l'équité qu'à
 * pénibilité équivalente</em>. L'heure ordinaire est l'unité, son coefficient vaut 1 et ne se
 * transmet pas.</p>
 *
 * <p><strong>Chaque coefficient est facultatif ; absent, il vaut 1</strong> — donc aucune
 * pondération. Le bloc entier absent laisse la mesure pondérée égale aux heures brutes, et le
 * moteur le signale : il n'invente pas une échelle que seule la simulation peut établir.</p>
 *
 * <p>Une minute ne relève que d'une catégorie — celle que la dominance retient — de sorte que ces
 * coefficients ne se multiplient jamais entre eux. Une nuit du dimanche est pondérée comme une
 * nuit, une fois.</p>
 */
public class CoefficientsPenibiliteDTO {

    /** Ce que vaut une heure de nuit, en heures ordinaires. */
    private Double nuit;

    /** Ce que vaut une heure de dimanche non nocturne. */
    private Double dimanche;

    /** Ce que vaut une heure de jour férié ni nocturne ni dominicale. */
    private Double ferie;

    public Double getNuit() {
        return nuit;
    }

    public void setNuit(Double nuit) {
        this.nuit = nuit;
    }

    public Double getDimanche() {
        return dimanche;
    }

    public void setDimanche(Double dimanche) {
        this.dimanche = dimanche;
    }

    public Double getFerie() {
        return ferie;
    }

    public void setFerie(Double ferie) {
        this.ferie = ferie;
    }
}
