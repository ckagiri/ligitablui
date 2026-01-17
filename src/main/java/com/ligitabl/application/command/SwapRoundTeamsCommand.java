package com.ligitabl.application.command;

import com.ligitabl.domain.model.user.UserId;

import java.util.Objects;

/**
 * Command object for the SwapRoundTeams use case (round predictions).
 *
 * <p>This is different from SwapTeamsCommand which is for season predictions.</p>
 */
public record SwapRoundTeamsCommand(
    UserId userId,
    String teamA,
    String teamB
) {
    public SwapRoundTeamsCommand {
        Objects.requireNonNull(userId, "userId is required");
        Objects.requireNonNull(teamA, "teamA is required");
        Objects.requireNonNull(teamB, "teamB is required");
        if (teamA.equals(teamB)) {
            throw new IllegalArgumentException("Cannot swap a team with itself");
        }
    }

    /**
     * Factory method to create from raw values.
     */
    public static SwapRoundTeamsCommand of(String userId, String teamA, String teamB) {
        return new SwapRoundTeamsCommand(
            UserId.of(userId),
            teamA,
            teamB
        );
    }
}
