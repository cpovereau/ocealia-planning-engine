package fr.project.planning.time;

public record TimeBreakdown(
        long minutesTravaillees,
        long minutesNuit,
        long minutesDimanche,
        long minutesFerie,
        long minutesNuitEtDimanche,
        long minutesNuitEtFerie,
        long minutesDimancheEtFerie,
        long minutesNuitEtDimancheEtFerie
) { }