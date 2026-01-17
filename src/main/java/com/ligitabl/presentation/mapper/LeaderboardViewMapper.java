package com.ligitabl.presentation.mapper;

import com.ligitabl.domain.model.leaderboard.LeaderboardEntry;
import com.ligitabl.domain.model.leaderboard.UserDetail;
import com.ligitabl.dto.Responses;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Mapper for converting domain leaderboard models to legacy DTOs.
 *
 * <p>This maintains backward compatibility with existing Thymeleaf templates
 * while using the new domain models internally.</p>
 */
@Component
public class LeaderboardViewMapper {

    /**
     * Convert domain LeaderboardEntry to legacy DTO.
     */
    public Responses.LeaderboardEntry toLegacyDTO(LeaderboardEntry entry) {
        return new Responses.LeaderboardEntry(
            entry.getPosition(),
            entry.getUserId().value(),
            entry.getDisplayName(),
            entry.getTotalScore(),
            entry.getRoundScore(),
            entry.getTotalZeroes(),
            entry.getTotalSwaps(),
            entry.getTotalPoints(),
            entry.getMovement()
        );
    }

    /**
     * Convert list of domain LeaderboardEntry to list of legacy DTOs.
     */
    public List<Responses.LeaderboardEntry> toLegacyDTOList(List<LeaderboardEntry> entries) {
        return entries.stream()
            .map(this::toLegacyDTO)
            .toList();
    }

    /**
     * Convert domain UserDetail to legacy UserDetailResponse DTO.
     */
    public Responses.UserDetailResponse toLegacyDTO(UserDetail userDetail) {
        List<Responses.PredictionRow> predictions = userDetail.getPredictions().stream()
            .map(this::toLegacyPredictionRow)
            .toList();

        return new Responses.UserDetailResponse(
            userDetail.getUserId().value(),
            userDetail.getDisplayName(),
            userDetail.getPosition(),
            userDetail.getTotalScore(),
            userDetail.getRoundScore(),
            userDetail.getTotalZeroes(),
            userDetail.getMovement(),
            predictions
        );
    }

    /**
     * Convert domain PredictionDetail to legacy PredictionRow DTO.
     */
    private Responses.PredictionRow toLegacyPredictionRow(UserDetail.PredictionDetail detail) {
        return new Responses.PredictionRow(
            detail.position(),
            detail.teamCode(),
            detail.teamName(),
            detail.crestUrl(),
            detail.hit(),
            detail.actualPosition()
        );
    }
}
