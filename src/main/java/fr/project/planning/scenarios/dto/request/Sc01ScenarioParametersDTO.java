package fr.project.planning.scenarios.dto.request;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Set;

public class Sc01ScenarioParametersDTO {

    private ResourceRefDTO resourceRef;

    /** Amplitude journalière incluant la pause (ex : 7.5) */
    private double dailyAmplitudeHours;

    /** Heure de début de poste */
    private LocalTime shiftStart;

    /** Heure de fin de poste (borne d'alerte) */
    private LocalTime shiftEndAlert;

    /** Pause optionnelle */
    private LunchBreakDTO lunchBreak;

    /** Jours cochés MON..SUN */
    private Set<DayOfWeek> workedDays;

    /** Liste des jours fériés (non travaillés) */
    private Set<LocalDate> holidayDates;

    // =========================
    // Getters / Setters
    // =========================

    public ResourceRefDTO getResourceRef() {
        return resourceRef;
    }

    public void setResourceRef(ResourceRefDTO resourceRef) {
        this.resourceRef = resourceRef;
    }

    public double getDailyAmplitudeHours() {
        return dailyAmplitudeHours;
    }

    public void setDailyAmplitudeHours(double dailyAmplitudeHours) {
        this.dailyAmplitudeHours = dailyAmplitudeHours;
    }

    public LocalTime getShiftStart() {
        return shiftStart;
    }

    public void setShiftStart(LocalTime shiftStart) {
        this.shiftStart = shiftStart;
    }

    public LocalTime getShiftEndAlert() {
        return shiftEndAlert;
    }

    public void setShiftEndAlert(LocalTime shiftEndAlert) {
        this.shiftEndAlert = shiftEndAlert;
    }

    public LunchBreakDTO getLunchBreak() {
        return lunchBreak;
    }

    public void setLunchBreak(LunchBreakDTO lunchBreak) {
        this.lunchBreak = lunchBreak;
    }

    public Set<DayOfWeek> getWorkedDays() {
        return workedDays;
    }

    public void setWorkedDays(Set<DayOfWeek> workedDays) {
        this.workedDays = workedDays;
    }

    public Set<LocalDate> getHolidayDates() {
        return holidayDates;
    }

    public void setHolidayDates(Set<LocalDate> holidayDates) {
        this.holidayDates = holidayDates;
    }
}