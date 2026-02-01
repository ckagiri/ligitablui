package com.ligitabl.presentation.dto.request;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.Objects;

/**
 * Request DTO for creating initial season prediction.
 *
 * <p>Used when a user first submits their season prediction (joins the competition).
 * Contains an ordered list of team codes — position is implied by array index
 * (index 0 = position 1, index 1 = position 2, etc.).</p>
 *
 * <p>Example JSON: {@code { "teamCodes": ["MCI", "ARS", "LIV", ...] }}</p>
 */
public record CreateSeasonPredictionRequest(
    @JsonProperty("teamCodes") List<String> teamCodes
) {
    @JsonCreator
    public CreateSeasonPredictionRequest(
        @JsonProperty("teamCodes") List<String> teamCodes
    ) {
        this.teamCodes = List.copyOf(Objects.requireNonNull(teamCodes, "teamCodes is required"));
    }
}
