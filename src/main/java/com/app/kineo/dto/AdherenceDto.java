package com.app.kineo.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Data
@Builder
public class AdherenceDto {

    private Long   planId;
    private String planName;
    private int    totalSessions;
    private int    completedSessions;
    private int    pendingSessions;
    /** 0–100 */
    private int    adherencePercent;

    /** Desglose por sesión para mostrar el calendario de adherencia. */
    private List<SessionAdherence> sessions;

    @Data
    @Builder
    public static class SessionAdherence {
        private Long      sessionId;
        private String    sessionName;
        private LocalDate scheduledDate;
        private boolean   completed;
        /** Porcentaje de series completadas dentro de la sesión (0–100). */
        private int       setsCompletionPercent;
    }
}
