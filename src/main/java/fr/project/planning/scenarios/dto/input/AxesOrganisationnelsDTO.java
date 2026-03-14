package fr.project.planning.scenarios.dto.input;

import java.util.List;

/**
 * AxesOrganisationnelsDTO — Phase 1 transport uniquement.
 * Transporte les axes organisationnels d'un salarié ou d'un créneau.
 * Non exploité par le builder avant Phase 3.
 */
public class AxesOrganisationnelsDTO {

    private List<String> directionIds;
    private List<String> serviceIds;
    private List<String> lieuIds;
    private List<String> posteComptableIds;

    public List<String> getDirectionIds() { return directionIds; }
    public void setDirectionIds(List<String> directionIds) { this.directionIds = directionIds; }

    public List<String> getServiceIds() { return serviceIds; }
    public void setServiceIds(List<String> serviceIds) { this.serviceIds = serviceIds; }

    public List<String> getLieuIds() { return lieuIds; }
    public void setLieuIds(List<String> lieuIds) { this.lieuIds = lieuIds; }

    public List<String> getPosteComptableIds() { return posteComptableIds; }
    public void setPosteComptableIds(List<String> posteComptableIds) { this.posteComptableIds = posteComptableIds; }
}
