package fr.project.planning.equite.calibration;

import com.fasterxml.jackson.databind.JsonNode;
import fr.project.planning.domain.contexte.HorizonTemporel;
import fr.project.planning.domain.ressource.ContratSalarie;
import fr.project.planning.domain.ressource.Indisponibilite;
import fr.project.planning.domain.workmetrics.EcartAuContrat;
import fr.project.planning.domain.workmetrics.JoursDisponibles;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * LectureCasDeCalibration — reconstituer une charge observée à partir du contrat publié.
 *
 * <h3>Pourquoi passer par la réponse, et non par le calcul interne</h3>
 * <p>Le harnais aurait pu appeler {@code WorkMetricsCalculator} directement. Il lit la réponse
 * HTTP, ce qui coûte une résolution mais vérifie au passage une chose que l'appel interne aurait
 * masquée : <strong>ce que le moteur publie suffit à calibrer</strong>. Si un jour la restitution
 * cessait de porter de quoi reconstituer la répartition, la lecture échouerait ici plutôt que de
 * calibrer sur des chiffres que personne d'autre ne peut voir.</p>
 *
 * <h3>La reconstitution</h3>
 * <p>{@code partNuits}, {@code partDimanches} et {@code partFeries} sont des pourcentages du
 * volume contractuel ; multipliés par ce volume, ils redonnent les minutes attribuées à chaque
 * catégorie par la dominance. Le reste des heures travaillées est ordinaire.</p>
 *
 * <p>Ces parts sont arrondies au centième de pourcent à la publication. Sur une fenêtre d'une
 * semaine, l'erreur reste très en deçà de la minute : l'arrondi à la minute la plus proche redonne
 * donc les volumes exacts. Sur une fenêtre très longue, elle grandirait — et c'est une raison de
 * plus pour que le harnais n'invente rien qu'il ne puisse recouper.</p>
 */
public final class LectureCasDeCalibration {

    private LectureCasDeCalibration() {
    }

    /**
     * @param nom      de quoi ce cas est le cas
     * @param requete  la demande transmise — pour les contrats et l'horizon
     * @param reponse  la réponse produite — pour la charge et sa répartition
     */
    public static CasDeCalibration depuis(String nom, JsonNode requete, JsonNode reponse) {
        Map<String, Double> attenduesParRessource = attenduesParRessource(requete);

        List<ChargeObservee> charges = new ArrayList<>();
        for (JsonNode metrique : reponse.get("workMetrics").get("byRessource")) {
            String ressourceId = metrique.get("resourceId").asText();
            Double attendues = attenduesParRessource.get(ressourceId);

            long travaillees = Math.round(metrique.get("heuresTravaillees").asDouble() * 60.0);
            long nuit = minutesDepuisPart(metrique.get("partNuits"), attendues);
            long dimanche = minutesDepuisPart(metrique.get("partDimanches"), attendues);
            long ferie = minutesDepuisPart(metrique.get("partFeries"), attendues);

            charges.add(new ChargeObservee(ressourceId, nuit, dimanche, ferie,
                    Math.max(0, travaillees - nuit - dimanche - ferie), attendues));
        }

        return new CasDeCalibration(nom, charges);
    }

    /**
     * Le volume contractuel attendu de chaque salarié, calculé comme le moteur le calcule.
     *
     * <p>Par la classe de production, et non par une formule recopiée : le harnais doit se tromper
     * exactement comme le moteur, ou pas du tout.</p>
     */
    private static Map<String, Double> attenduesParRessource(JsonNode requete) {
        JsonNode horizonJson = requete.get("planningContext").get("horizon");
        HorizonTemporel horizon = new HorizonTemporel(
                LocalDate.parse(horizonJson.get("dateDebut").asText()),
                LocalDate.parse(horizonJson.get("dateFin").asText()));

        /*
         * [Rang 14] Les indisponibilités déclarées sont déduites, comme le moteur les déduit.
         * Sans cela le harnais proratiserait sur l'horizon entier quand le moteur proratise sur
         * les jours disponibles, et l'écart se lirait comme une divergence de pondération — la
         * calibration porterait alors sur une mesure que le moteur ne produit pas.
         */
        List<Indisponibilite> indisponibilites = new ArrayList<>();
        for (JsonNode item : requete.path("dataSet").path("indisponibilites").path("items")) {
            JsonNode debut = item.path("dateDebut");
            JsonNode fin = item.path("dateFin");
            if (debut.isMissingNode() || fin.isMissingNode()) {
                continue;
            }
            indisponibilites.add(new Indisponibilite(
                    item.path("ressourceId").asText(null),
                    LocalDate.parse(debut.asText()),
                    LocalDate.parse(fin.asText()),
                    item.path("motif").asText(null)));
        }

        Map<String, Double> attendues = new HashMap<>();
        JsonNode salaries = requete.path("dataSet").path("ressources").path("salaries");

        for (JsonNode salarie : salaries) {
            JsonNode heures = salarie.path("contrat").path("heuresHebdomadairesHabituelles");
            ContratSalarie contrat = heures.isMissingNode() || heures.isNull()
                    ? null
                    : new ContratSalarie(null, heures.asDouble(), null, null);

            String id = salarie.get("id").asText();
            attendues.put(id, EcartAuContrat.minutesAttendues(contrat,
                    JoursDisponibles.pour(id, horizon, indisponibilites)));
        }
        return attendues;
    }

    private static long minutesDepuisPart(JsonNode part, Double minutesAttendues) {
        if (part == null || part.isNull() || minutesAttendues == null) {
            return 0;
        }
        return Math.round(part.asDouble() / 100.0 * minutesAttendues);
    }
}
