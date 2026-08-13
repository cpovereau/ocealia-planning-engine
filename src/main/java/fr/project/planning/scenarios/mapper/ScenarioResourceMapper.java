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
        signalerBorneNegative(salarieId, "heuresMinimumParJour", dto.getHeuresMinimumParJour());
        signalerBorneNegative(salarieId, "heuresMaximumParJour", dto.getHeuresMaximumParJour());
        signalerBorneNegative(salarieId, "amplitudeJournaliereMaximum", dto.getAmplitudeJournaliereMaximum());
        signalerBorneNegative(salarieId, "reposQuotidienMinimum", dto.getReposQuotidienMinimum());
        signalerBorneNegative(salarieId, "heuresMinimumParSemaine", dto.getHeuresMinimumParSemaine());
        signalerBorneNegative(salarieId, "heuresMaximumParSemaine", dto.getHeuresMaximumParSemaine());
        signalerBorneNegative(salarieId, "nuitsMaximumParSemaine", dto.getNuitsMaximumParSemaine());
        signalerBorneNegative(salarieId, "joursConsecutifsMaximum", dto.getJoursConsecutifsMaximum());
        signalerBorneNegative(salarieId, "nuitsConsecutivesMaximum", dto.getNuitsConsecutivesMaximum());
        signalerBorneNegative(salarieId, "joursReposMinimumApresNuits", dto.getJoursReposMinimumApresNuits());
        signalerBorneNegative(salarieId, "dimanchesTravaillesMaximum", dto.getDimanchesTravaillesMaximum());
        signalerBorneNegative(salarieId, "reposHebdomadaireFenetreJours", dto.getReposHebdomadaireFenetreJours());
        signalerBorneNegative(salarieId, "reposHebdomadaireJoursOffMinimum", dto.getReposHebdomadaireJoursOffMinimum());

        signalerPaireIncomplete(salarieId, dto);

        return new ContraintesReglementairesSalarie(
                dto.getHeuresMinimumParJour(),
                dto.getHeuresMaximumParJour(),
                dto.getAmplitudeJournaliereMaximum(),
                dto.getReposQuotidienMinimum(),
                dto.getHeuresMinimumParSemaine(),
                dto.getHeuresMaximumParSemaine(),
                dto.getNuitsMaximumParSemaine(),
                dto.getJoursConsecutifsMaximum(),
                dto.getNuitsConsecutivesMaximum(),
                dto.getJoursReposMinimumApresNuits(),
                dto.getDimanchesTravaillesMaximum(),
                dto.getReposHebdomadaireFenetreJours(),
                dto.getReposHebdomadaireJoursOffMinimum()
        );
    }

    /**
     * Signale une fenêtre de repos hebdomadaire déclarée à moitié.
     *
     * <p>{@code reposHebdomadaireFenetreJours} et {@code reposHebdomadaireJoursOffMinimum} ne
     * décrivent une règle qu'ensemble : une fenêtre sans minimum de jours off n'interdit rien,
     * un minimum sans fenêtre ne s'applique nulle part. La contrainte reste alors inactive, ce
     * qui est le comportement sûr — mais l'appelant croit probablement avoir posé une limite.</p>
     */
    private void signalerPaireIncomplete(String salarieId, ContraintesReglementairesDTO dto) {
        boolean fenetre = ContraintesReglementairesSalarie.largeurRenseignee(dto.getReposHebdomadaireFenetreJours());
        boolean joursOff = ContraintesReglementairesSalarie.borneRenseignee(dto.getReposHebdomadaireJoursOffMinimum());
        if (fenetre != joursOff) {
            log.warn("[ScenarioResourceMapper] salarié id='{}' : repos hebdomadaire glissant déclaré "
                    + "à moitié (fenetreJours={}, joursOffMinimum={}) — la contrainte reste inactive. "
                    + "Les deux champs doivent être renseignés ensemble.",
                    salarieId, dto.getReposHebdomadaireFenetreJours(), dto.getReposHebdomadaireJoursOffMinimum());
        }
    }

    /**
     * Signale une borne réglementaire négative.
     *
     * <p>Depuis l'arbitrage du lot S7.7, <strong>0 n'est plus une anomalie</strong> : il est lu
     * littéralement — un maximum à 0 interdit tout, un minimum à 0 n'exige rien — et seule
     * l'absence du champ désactive une règle. Voir
     * {@link ContraintesReglementairesSalarie#borneRenseignee(Number)}.</p>
     *
     * <p>Une valeur négative, elle, ne décrit rien : le moteur la traite comme non renseignée
     * plutôt que de l'appliquer à la lettre, et rend l'écart visible en intégration plutôt que
     * silencieux.</p>
     */
    private void signalerBorneNegative(String salarieId, String champ, Number valeur) {
        if (valeur != null && valeur.doubleValue() < 0d) {
            log.warn("[ScenarioResourceMapper] salarié id='{}' : contraintesReglementaires.{}={} — "
                    + "une borne négative ne décrit aucune règle. Le champ est ignoré ; "
                    + "l'omettre est la façon documentée de désactiver une limite.",
                    salarieId, champ, valeur);
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
     *
     * <p><strong>Les deux blocs sont facultatifs.</strong> Ni {@code salaries} ni
     * {@code postesVirtuels} n'est exigé par le contrat, et une demande qui n'en déclare qu'un
     * décrit quelque chose de parfaitement légitime — un dataset sans poste virtuel, par exemple,
     * dit simplement qu'aucun n'est mobilisable. Le moteur répondait jusqu'ici {@code 500
     * INTERNAL_ERROR} à ces demandes-là. Constaté au lot L4 du chantier équité, sur un jeu d'essai
     * qui ne déclarait pas de poste virtuel.</p>
     *
     * <p>Un dataset sans aucune ressource reste possible et reste signalé — par
     * {@code AUCUNE_RESSOURCE_DANS_DATASET}, à la préparation, où l'appelant peut le lire.</p>
     */
    public List<Ressource> toRessources(DataSetDTO dataSet) {
        List<Ressource> ressources = new ArrayList<>();
        listeOuVide(dataSet.getRessources().getSalaries())
                .forEach(s -> ressources.add(toSalarieReel(s)));
        listeOuVide(dataSet.getRessources().getPostesVirtuels())
                .forEach(p -> ressources.add(toPosteVirtuel(p)));
        ressources.add(RessourceNonAffectee.INSTANCE);
        return ressources;
    }

    private static <T> List<T> listeOuVide(List<T> liste) {
        return liste == null ? List.of() : liste;
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
        if (dto == null) {
            return ReferentielComptabiliteActivite.neutre();
        }
        // [S7.9b] Les codes de repos sont transmis même quand la liste d'activités est vide :
        // ils ne dépendent pas d'elle, et les perdre ferait retomber tout le calendrier sur le
        // repli samedi/dimanche sans que rien ne le signale.
        if (dto.getActivites() == null || dto.getActivites().isEmpty()) {
            return new ReferentielComptabiliteActivite(
                    Map.of(),
                    dto.getCodeActiviteReposHebdomadaire(),
                    dto.getCodeActiviteReposHebdomadaireDimanche());
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
        return new ReferentielComptabiliteActivite(
                map,
                dto.getCodeActiviteReposHebdomadaire(),
                dto.getCodeActiviteReposHebdomadaireDimanche());
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