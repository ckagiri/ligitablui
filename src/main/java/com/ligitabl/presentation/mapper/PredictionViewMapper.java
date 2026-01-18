package com.ligitabl.presentation.mapper;

import com.ligitabl.application.usecase.prediction.GetMyPredictionUseCase.PredictionViewData;
import com.ligitabl.application.usecase.prediction.GetSwapStatusUseCase.SwapStatusResult;
import com.ligitabl.domain.model.prediction.PredictionRow;
import com.ligitabl.domain.model.prediction.SwapCooldown;
import com.ligitabl.dto.Responses;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Maps prediction domain models to legacy DTOs for Thymeleaf template compatibility.
 */
@Component
public class PredictionViewMapper {

    private static final Map<String, String> TEAM_NAMES = Map.ofEntries(
        Map.entry("MCI", "Manchester City"),
        Map.entry("ARS", "Arsenal"),
        Map.entry("LIV", "Liverpool"),
        Map.entry("AVL", "Aston Villa"),
        Map.entry("TOT", "Tottenham"),
        Map.entry("CHE", "Chelsea"),
        Map.entry("NEW", "Newcastle"),
        Map.entry("MUN", "Manchester United"),
        Map.entry("WHU", "West Ham"),
        Map.entry("BHA", "Brighton"),
        Map.entry("WOL", "Wolves"),
        Map.entry("FUL", "Fulham"),
        Map.entry("BOU", "Bournemouth"),
        Map.entry("CRY", "Crystal Palace"),
        Map.entry("BRE", "Brentford"),
        Map.entry("EVE", "Everton"),
        Map.entry("NFO", "Nottingham Forest"),
        Map.entry("LEI", "Leicester City"),
        Map.entry("IPS", "Ipswich Town"),
        Map.entry("SOU", "Southampton")
    );

    /**
     * Convert domain PredictionRow to legacy DTO.
     */
    public Responses.PredictionRow toLegacyDTO(PredictionRow row) {
        return new Responses.PredictionRow(
            row.getPosition(),
            row.getTeamCode(),
            getTeamName(row.getTeamCode()),
            getCrestUrl(row.getTeamCode()),
            row.getHit(),
            row.getActualPosition()
        );
    }

    /**
     * Convert list of domain PredictionRows to legacy DTOs.
     */
    public List<Responses.PredictionRow> toLegacyDTOs(List<PredictionRow> rows) {
        return rows.stream()
            .map(this::toLegacyDTO)
            .toList();
    }

    /**
     * Convert SwapStatusResult to legacy DTO.
     */
    public Responses.SwapStatusResponse toLegacyDTO(SwapStatusResult status) {
        return new Responses.SwapStatusResponse(
            status.roundStatus(),
            status.canSwap(),
            status.lastSwapAt(),
            status.nextSwapAt(),
            status.hoursRemaining(),
            status.message()
        );
    }

    /**
     * Convert SwapCooldown to legacy SwapStatusResponse.
     */
    public Responses.SwapStatusResponse toLegacyDTO(SwapCooldown cooldown, String roundStatus) {
        Instant now = Instant.now();
        return new Responses.SwapStatusResponse(
            roundStatus,
            cooldown.canSwap(now),
            cooldown.getLastSwapAtFormatted(),
            cooldown.getNextSwapAtFormatted(now),
            cooldown.getRemainingCooldown(now).toHours() +
                (cooldown.getRemainingCooldown(now).toMinutesPart() / 60.0),
            cooldown.getStatusMessage(now)
        );
    }

    /**
     * Get team name from code.
     */
    private String getTeamName(String code) {
        return TEAM_NAMES.getOrDefault(code, code);
    }

    /**
     * Get crest URL from team code.
     */
    private String getCrestUrl(String code) {
        return "/images/crests/" + code.toLowerCase() + ".png";
    }
}
