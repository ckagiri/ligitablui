package com.ligitabl.application.command;

import com.ligitabl.domain.model.season.SeasonId;
import com.ligitabl.domain.model.user.UserId;

import java.util.Objects;

/**
 * Command to get season prediction with smart fallback.
 *
 * <p>This command triggers the smart fallback hierarchy:
 * 1. Try to find user's own season prediction (or round prediction if roundNumber is specified)
 * 2. If not found, fallback to current round standings
 * 3. If no round standings, fallback to season baseline rankings</p>
 *
 * <p>The response will include a source indicator showing which fallback was used.</p>
 *
 * <p>Can be used for both:
 * - Season-long predictions (roundNumber = null)
 * - Round-specific predictions (roundNumber specified)</p>
 */
public record GetSeasonPredictionCommand(
    UserId userId,
    SeasonId seasonId,
    Integer roundNumber  // null for season prediction, specific value for round prediction
) {
    public GetSeasonPredictionCommand {
        Objects.requireNonNull(userId, "userId is required");
        Objects.requireNonNull(seasonId, "seasonId is required");
        // roundNumber can be null
    }

    /**
     * Factory method for creating season prediction command.
     *
     * @param userId the user ID
     * @param seasonId the season ID
     * @return a new GetSeasonPredictionCommand for season prediction
     */
    public static GetSeasonPredictionCommand forSeason(UserId userId, SeasonId seasonId) {
        return new GetSeasonPredictionCommand(userId, seasonId, null);
    }

    /**
     * Factory method for creating round prediction command.
     *
     * @param userId the user ID
     * @param seasonId the season ID
     * @param roundNumber the round number
     * @return a new GetSeasonPredictionCommand for round prediction
     */
    public static GetSeasonPredictionCommand forRound(UserId userId, SeasonId seasonId, int roundNumber) {
        return new GetSeasonPredictionCommand(userId, seasonId, roundNumber);
    }

    /**
     * Legacy factory method (for season prediction).
     *
     * @param userId the user ID
     * @param seasonId the season ID
     * @return a new GetSeasonPredictionCommand
     */
    public static GetSeasonPredictionCommand of(UserId userId, SeasonId seasonId) {
        return forSeason(userId, seasonId);
    }

    /**
     * Check if this is a round-specific prediction.
     */
    public boolean isRoundPrediction() {
        return roundNumber != null;
    }
}
