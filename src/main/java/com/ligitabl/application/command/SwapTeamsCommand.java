package com.ligitabl.application.command;

import com.ligitabl.domain.model.season.SeasonId;
import com.ligitabl.domain.model.team.TeamId;
import com.ligitabl.domain.model.user.UserId;

import java.util.Objects;

/**
 * Command to swap exactly two teams in a season prediction.
 *
 * <p>This command is used by existing participants to update their prediction.
 * CRITICAL Business Rule: Only ONE swap per request.</p>
 *
 * <p>The command includes current positions for optimistic locking - if the positions
 * have changed since the user's UI was rendered, the swap will be rejected.</p>
 *
 * <p>Business Rules:
 * - User must already have a season prediction (404 if not found)
 * - Exactly one team pair swap (validated by SwapPair value object)
 * - Current positions must match actual positions (optimistic locking)
 * - Cannot swap same team with itself</p>
 */
public record SwapTeamsCommand(
    UserId userId,
    SeasonId seasonId,
    TeamId teamAId,
    int teamACurrentPosition,
    TeamId teamBId,
    int teamBCurrentPosition
) {
    public SwapTeamsCommand {
        Objects.requireNonNull(userId, "userId is required");
        Objects.requireNonNull(seasonId, "seasonId is required");
        Objects.requireNonNull(teamAId, "teamAId is required");
        Objects.requireNonNull(teamBId, "teamBId is required");

        // Basic validation (detailed validation happens in domain)
        if (teamACurrentPosition < 1 || teamACurrentPosition > 20) {
            throw new IllegalArgumentException(
                "teamACurrentPosition must be between 1 and 20, got: " + teamACurrentPosition
            );
        }
        if (teamBCurrentPosition < 1 || teamBCurrentPosition > 20) {
            throw new IllegalArgumentException(
                "teamBCurrentPosition must be between 1 and 20, got: " + teamBCurrentPosition
            );
        }
        if (teamAId.equals(teamBId)) {
            throw new IllegalArgumentException("Cannot swap a team with itself");
        }
        if (teamACurrentPosition == teamBCurrentPosition) {
            throw new IllegalArgumentException("Teams cannot be at the same position");
        }
    }

    /**
     * Factory method for creating command.
     *
     * @param userId the user ID
     * @param seasonId the season ID
     * @param teamAId the first team's ID
     * @param teamACurrentPosition the first team's current position
     * @param teamBId the second team's ID
     * @param teamBCurrentPosition the second team's current position
     * @return a new SwapTeamsCommand
     */
    public static SwapTeamsCommand of(
        UserId userId,
        SeasonId seasonId,
        TeamId teamAId,
        int teamACurrentPosition,
        TeamId teamBId,
        int teamBCurrentPosition
    ) {
        return new SwapTeamsCommand(
            userId,
            seasonId,
            teamAId,
            teamACurrentPosition,
            teamBId,
            teamBCurrentPosition
        );
    }
}
