package com.ligitabl.application.command;

import com.ligitabl.domain.model.season.SeasonId;
import com.ligitabl.domain.model.user.UserId;

import java.util.Objects;

/**
 * Command to get season prediction with smart fallback.
 *
 * <p>This command triggers the smart fallback hierarchy:
 * 1. Try to find user's own season prediction
 * 2. If not found, fallback to current round standings
 * 3. If no round standings, fallback to season baseline rankings</p>
 *
 * <p>The response will include a source indicator showing which fallback was used.</p>
 */
public record GetSeasonPredictionCommand(
    UserId userId,
    SeasonId seasonId
) {
    public GetSeasonPredictionCommand {
        Objects.requireNonNull(userId, "userId is required");
        Objects.requireNonNull(seasonId, "seasonId is required");
    }

    /**
     * Factory method for creating command.
     *
     * @param userId the user ID
     * @param seasonId the season ID
     * @return a new GetSeasonPredictionCommand
     */
    public static GetSeasonPredictionCommand of(UserId userId, SeasonId seasonId) {
        return new GetSeasonPredictionCommand(userId, seasonId);
    }
}
