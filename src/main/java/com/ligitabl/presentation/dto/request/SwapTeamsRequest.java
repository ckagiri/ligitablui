package com.ligitabl.presentation.dto.request;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Objects;

/**
 * Request DTO for swapping exactly two teams.
 *
 * <p>Used by existing participants to update their prediction.
 * Includes current positions for optimistic locking.</p>
 *
 * <p>CRITICAL: Only ONE swap per request (enforced by domain).</p>
 */
public class SwapTeamsRequest {

    private final TeamSwapDTO teamA;
    private final TeamSwapDTO teamB;

    @JsonCreator
    public SwapTeamsRequest(
        @JsonProperty("teamA") TeamSwapDTO teamA,
        @JsonProperty("teamB") TeamSwapDTO teamB
    ) {
        this.teamA = Objects.requireNonNull(teamA, "teamA is required");
        this.teamB = Objects.requireNonNull(teamB, "teamB is required");
    }

    public TeamSwapDTO getTeamA() {
        return teamA;
    }

    public TeamSwapDTO getTeamB() {
        return teamB;
    }

    /**
     * Nested DTO for a team in the swap operation.
     */
    public static class TeamSwapDTO {
        private final String teamId;
        private final int currentPosition;

        @JsonCreator
        public TeamSwapDTO(
            @JsonProperty("teamId") String teamId,
            @JsonProperty("currentPosition") int currentPosition
        ) {
            this.teamId = Objects.requireNonNull(teamId, "teamId is required");
            this.currentPosition = currentPosition;
        }

        public String getTeamId() {
            return teamId;
        }

        public int getCurrentPosition() {
            return currentPosition;
        }

        @Override
        public String toString() {
            return "TeamSwapDTO{" +
                   "teamId='" + teamId + '\'' +
                   ", currentPosition=" + currentPosition +
                   '}';
        }
    }

    @Override
    public String toString() {
        return "SwapTeamsRequest{" +
               "teamA=" + teamA +
               ", teamB=" + teamB +
               '}';
    }
}
