package fr.project.planning.scenarios.mapper;

import fr.project.planning.domain.metier.ComptabiliteActivite;
import fr.project.planning.domain.metier.ReferentielComptabiliteActivite;
import fr.project.planning.domain.ressource.ContraintesReglementairesSalarie;
import fr.project.planning.domain.ressource.ContratSalarie;
import fr.project.planning.domain.ressource.Indisponibilite;
import fr.project.planning.domain.ressource.PosteVirtuel;
import fr.project.planning.domain.ressource.Ressource;
import fr.project.planning.domain.ressource.RessourceNonAffectee;
import fr.project.planning.domain.ressource.SalarieReel;
import fr.project.planning.domain.ressource.TypePosteVirtuel;
import fr.project.planning.scenarios.dto.DataSetDTO;
import fr.project.planning.scenarios.dto.input.ContraintesReglementairesDTO;
import fr.project.planning.scenarios.dto.input.ContratSalarieDTO;
import fr.project.planning.scenarios.dto.input.IndisponibilitesDTO;
import fr.project.planning.scenarios.dto.input.PosteVirtuelInputDTO;
import fr.project.planning.scenarios.dto.input.ReferentielsDTO;
import fr.project.planning.scenarios.dto.input.SalarieInputDTO;
import fr.project.planning.scenarios.dto.request.ResourceKind;
import fr.project.planning.scenarios.dto.request.ResourceRefDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * ScenarioResourceMapper
 *
 * Centralise toutes les conversions DTO → domaine pour les ressources.
 * Source de vérité unique pour le passage couche transport → couche domaine.
 *
 * Phase 1 : mapping minimal (id, statut, sitesAutorises, activitesCompatibles).
 * Phase 3 : ajout des champs nuit/férié et contraintesReglementaires.
 * Phase 4+ : les nouveaux champs commencent à être exploités par le solveur.
 */
@Service
public class ScenarioResourceMapper {

    private static final Logger log = LoggerFactory.getLogger(ScenarioResourceMapper.class);

    // =========================
    // Salarié
    // =========================

    public SalarieReel toSalarieReel(SalarieInputDTO dto) {
        SalarieReel salarie = new SalarieReel(
                dto.getId(),
                null,  // profilContractuel — absent du contrat WinDev Phase 1-3
                dto.getStatut(),
                dto.getSitesAutorises() != null ? dto.getSitesAutorises() : Set.of(),
                dto.getActivitesCompatibles() != null ? dto.getActivitesCompatibles() : Set.of(),
                dto.getPostesComptablesCompatibles() != null ? dto.getPostesComptablesCompatibles() : Set.of()
        );

        // Phase 3 : champs nuit/férié
        salarie.setTravailDeNuit(dto.getTravailDeNuit());
        salarie.setHeureDebutNuit(dto.getHeureDebutNuit());
        salarie.setHeureFinNuit(dto.getHeureFinNuit());
        salarie.setTravailleJourFerie(dto.getTravailleJourFerie());

        // Phase 3 : contraintes réglementaires individuelles
        if (dto.getContraintesReglementaires() != null) {
            salarie.setContraintesReglementaires(
                    toContraintesReglementaires(dto.getId(), dto.getContraintesReglementaires())
            );
        }

        // [Lot S2] contrat de travail — descriptif, non exploité par le solveur
        if (dto.getContrat() != null) {
            salarie.setContrat(toContrat(dto.getContrat()));
        }

        return salarie;
    }

    private ContraintesReglementairesSalarie toContraintesReglementaires(String salarieId,
                                                                        ContraintesReglementairesDTO dto) {
        signalerZeroInterdit(salarieId, "heuresMinimumParJour", dto.getHeuresMinimumParJour());
        signalerZeroInterdit(salarieId, "heuresMaximumParJour", dto.getHeuresMaximumParJour());
        signalerZeroInterdit(salarieId, "amplitudeJournaliereMaximum", dto.getAmplitudeJournaliereMaximum());
        signalerZeroInterdit(salarieId, "reposQuotidienMinimum", dto.getReposQuotidienMinimum());
        signalerZeroInterdit(salarieId, "heuresMinimumParSemaine", dto.getHeuresMinimumParSemaine());
        signalerZeroInterdit(salarieId, "heuresMaximumParSemaine", dto.getHeuresMaximumParSemaine());
        signalerZeroInterdit(salarieId, "nuitsMaximumParSemaine", dto.getNuitsMaximumParSemaine());
        signalerZeroInterdit(salarieId, "joursConsecutifsMaximum", dto.getJoursConsecutifsMaximum());

        return new ContraintesReglementairesSalarie(
                dto.getHeuresMinimumParJour(),
                dto.getHeuresMaximumParJour(),
                dto.getAmplitudeJournaliereMaximum(),
                dto.getReposQuotidienMinimum(),
                dto.getHeuresMinimumParSemaine(),
                dto.getHeuresMaximumParSemaine(),
                dto.getNuitsMaximumParSemaine(),
                dto.getJoursConsecutifsMaximum()
        );
    }

    /**
     * Signale une contrainte réglementaire transmise à 0.
     *
     * <p>Le moteur active une contrainte dès que son seuil est non nul — règle SC-03 conservée.
     * Une valeur 0 l'active donc avec un seuil nul, ce qui rend toute affectation fautive. Le
     * contrat impose d'<strong>omettre le champ</strong> pour désactiver une limite.</p>
     *
     * <p>Le moteur ne corrige pas la valeur : corriger reviendrait à deviner l'intention. Il
     * rend l'écart visible en intégration plutôt que silencieux.
     * Voir {@code 92_cadrage_scenario_sc-06.md} §4.7.</p>
     */
    private void signalerZeroInterdit(String salarieId, String champ, Number valeur) {
        if (valeur != null && valeur.doubleValue() == 0d) {
            log.warn("[ScenarioResourceMapper] salarié id='{}' : contraintesReglementaires.{}=0 — "
                    + "la contrainte est activée avec un seuil nul. Pour désactiver une limite, "
                    + "omettre le champ plutôt que d'envoyer 0.", salarieId, champ);
        }
    }

    /**
     * Convertit le bloc contrat du salarié.
     * [Lot S2] Transport et mapping uniquement — aucune contrainte ne lit cet objet.
     */
    private ContratSalarie toContrat(ContratSalarieDTO dto) {
        return new ContratSalarie(
                dto.getHeuresMoyennesParJour(),
                dto.getHeuresHebdomadairesHabituelles(),
                dto.getJoursTravaillesParSemaine(),
                dto.getEstAnnualise()
        );
    }

    // =========================
    // Poste virtuel
    // =========================

    public PosteVirtuel toPosteVirtuel(PosteVirtuelInputDTO dto) {
        TypePosteVirtuel type = TypePosteVirtuel.POTENTIEL;
        if (dto.getType() != null) {
            try {
                type = TypePosteVirtuel.valueOf(dto.getType());
            } catch (IllegalArgumentException ignored) {
                // type inconnu → POTENTIEL par défaut
                log.warn("[ScenarioResourceMapper] poste virtuel id='{}' : type='{}' inconnu — fallback sur POTENTIEL",
                        dto.getId(), dto.getType());
            }
        }
        return new PosteVirtuel(
                dto.getId(),
                type,
                dto.getCapaciteCible(),
                dto.getActivitesCompatibles() != null ? dto.getActivitesCompatibles() : Set.of(),
                dto.getSitesAutorises() != null ? dto.getSitesAutorises() : Set.of(),
                dto.getPostesComptablesCompatibles() != null ? dto.getPostesComptablesCompatibles() : Set.of()
        );
    }

    // =========================
    // Value range complet
    // =========================

    /**
     * Construit la liste complète de ressources pour le value range OptaPlanner.
     * Inclut automatiquement RessourceNonAffectee.INSTANCE.
     */
    public List<Ressource> toRessources(DataSetDTO dataSet) {
        List<Ressource> ressources = new ArrayList<>();
        dataSet.getRessources().getSalaries()
                .forEach(s -> ressources.add(toSalarieReel(s)));
        dataSet.getRessources().getPostesVirtuels()
                .forEach(p -> ressources.add(toPosteVirtuel(p)));
        ressources.add(RessourceNonAffectee.INSTANCE);
        return ressources;
    }

    // =========================
    // Résolution de ressource cible
    // =========================

    /**
     * Trouve et convertit la ressource référencée par resourceRef.
     * Lève IllegalArgumentException si la ressource est introuvable.
     */
    public Ressource resolveResource(DataSetDTO dataSet, ResourceRefDTO ref) {
        String id = ref.getId();
        ResourceKind kind = ref.getKind();

        if (kind == ResourceKind.SALARIE) {
            return dataSet.getRessources().getSalaries().stream()
                    .filter(s -> id.equals(s.getId()))
                    .findFirst()
                    .map(this::toSalarieReel)
                    .orElseThrow(() -> new IllegalArgumentException("Salarié introuvable : " + id));
        }

        if (kind == ResourceKind.POSTE_VIRTUEL) {
            return dataSet.getRessources().getPostesVirtuels().stream()
                    .filter(p -> id.equals(p.getId()))
                    .findFirst()
                    .map(this::toPosteVirtuel)
                    .orElseThrow(() -> new IllegalArgumentException("Poste virtuel introuvable : " + id));
        }

        throw new IllegalArgumentException("Type de ressource non supporté : " + kind);
    }

    // =========================
    // Référentiel activités
    // =========================

    /**
     * Convertit le bloc referentiels du dataSet en ReferentielComptabiliteActivite domaine.
     *
     * Champs absents du contrat WinDev Phase 7 (prioritaireSurConfort, typeImpact) :
     * valeurs par défaut appliquées — false et CHARGE_STANDARD.
     *
     * Si le bloc est null ou vide, retourne un référentiel neutre (map vide) :
     * toutes les contraintes qui font getByCode(...) retournent null et sont ignorées.
     */
    public ReferentielComptabiliteActivite toReferentiel(ReferentielsDTO dto) {
        if (dto == null || dto.getActivites() == null || dto.getActivites().isEmpty()) {
            return ReferentielComptabiliteActivite.neutre();
        }
        Map<String, ComptabiliteActivite> map = new HashMap<>();
        for (var a : dto.getActivites()) {
            // [Phase 4] a.getLibelle() est intentionnellement ignoré :
            // champ de présentation sans équivalent dans ComptabiliteActivite, sans incidence sur la résolution.
            map.put(a.getCodeActiviteId(), new ComptabiliteActivite(
                    a.getCodeActiviteId(),
                    Boolean.TRUE.equals(a.getCompteDansCharge()),
                    Boolean.TRUE.equals(a.getGenereDetteRepos()),
                    Boolean.TRUE.equals(a.getEstServiceCritique()),
                    false,                                              // prioritaireSurConfort — absent Phase 7
                    ComptabiliteActivite.TypeImpactActivite.CHARGE_STANDARD  // défaut
            ));
        }
        return new ReferentielComptabiliteActivite(map);
    }

    // =========================
    // Indisponibilités
    // =========================

    /**
     * Convertit le bloc indisponibilites du dataSet en objets domaine.
     * Phase 3 : transportées et mappées. Exploitées par le solveur en Phase 4.
     */
    public List<Indisponibilite> toIndisponibilites(IndisponibilitesDTO dto) {
        if (dto == null || dto.getItems() == null) return List.of();
        return dto.getItems().stream()
                .map(item -> new Indisponibilite(
                        item.getRessourceId(),
                        item.getDateDebut(),
                        item.getDateFin(),
                        item.getMotif()
                ))
                .toList();
    }
}