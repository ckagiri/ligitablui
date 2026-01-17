package com.ligitabl.application.command;

import com.ligitabl.domain.model.season.SeasonId;
import com.ligitabl.domain.model.seasonprediction.TeamRanking;
import com.ligitabl.domain.model.user.UserId;

import java.util.List;
import java.util.Objects;

/**
 * Command to create initial season prediction.
 *
 * <p>This command is used when a user first submits their season prediction (joins the competition).
 * It will:
 * 1. Create the SeasonPrediction aggregate
 * 2. Auto-create a MainContestEntry for the user</p>
 *
 * <p>Business Rules:
 * - User must not already have a season prediction (409 Conflict if exists)
 * - Must provide exactly 20 team rankings
 * - Positions must be 1-20 with no duplicates
 * - No duplicate teams</p>
 */
public record CreateSeasonPredictionCommand(
    UserId userId,
    SeasonId seasonId,
    List<TeamRanking> rankings
) {
    public CreateSeasonPredictionCommand {
        Objects.requireNonNull(userId, "userId is required");
        Objects.requireNonNull(seasonId, "seasonId is required");
        Objects.requireNonNull(rankings, "rankings are required");

        // Defensive copy to ensure immutability
        rankings = List.copyOf(rankings);

        // Basic validation (detailed validation happens in domain)
        if (rankings.isEmpty()) {
            throw new IllegalArgumentException("Rankings cannot be empty");
        }
    }

    /**
     * Factory method for creating command.
     *
     * @param userId the user ID
     * @param seasonId the season ID
     * @param rankings the list of 20 team rankings
     * @return a new CreateSeasonPredictionCommand
     */
    public static CreateSeasonPredictionCommand of(
        UserId userId,
        SeasonId seasonId,
        List<TeamRanking> rankings
    ) {
        return new CreateSeasonPredictionCommand(userId, seasonId, rankings);
    }
}
