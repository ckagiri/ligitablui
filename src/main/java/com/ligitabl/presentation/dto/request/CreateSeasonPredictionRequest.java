package com.ligitabl.presentation.dto.request;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.Objects;

/**
 * Request DTO for creating initial season prediction.
 *
 * <p>Used when a user first submits their season prediction (joins the competition).
 * Contains the full list of 20 team rankings.</p>
 */
public class CreateSeasonPredictionRequest {

    private final List<TeamRankingDTO> teamRankings;

    @JsonCreator
    public CreateSeasonPredictionRequest(
        @JsonProperty("teamRankings") List<TeamRankingDTO> teamRankings
    ) {
        this.teamRankings = Objects.requireNonNull(teamRankings, "teamRankings is required");
    }

    public List<TeamRankingDTO> getTeamRankings() {
        return teamRankings;
    }

    /**
     * Nested DTO for team ranking in the request.
     */
    public static class TeamRankingDTO {
        private final String teamId;
        private final int position;

        @JsonCreator
        public TeamRankingDTO(
            @JsonProperty("teamId") String teamId,
            @JsonProperty("position") int position
        ) {
            this.teamId = Objects.requireNonNull(teamId, "teamId is required");
            this.position = position;
        }

        public String getTeamId() {
            return teamId;
        }

        public int getPosition() {
            return position;
        }
    }

    @Override
    public String toString() {
        return "CreateSeasonPredictionRequest{" +
               "teamRankings=" + teamRankings.size() + " teams" +
               '}';
    }
}
