package com.ligitabl.presentation.dto.request;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Objects;

/**
 * Request DTO for swapping exactly two teams.
 *
 * <p>Used by existing participants to update their prediction.
 * Only team codes are required — current positions are resolved server-side.</p>
 *
 * <p>Example JSON: {@code { "teamACode": "MCI", "teamBCode": "ARS" }}</p>
 */
public record SwapTeamsRequest(
    @JsonProperty("teamACode") String teamACode,
    @JsonProperty("teamBCode") String teamBCode
) {
    @JsonCreator
    public SwapTeamsRequest(
        @JsonProperty("teamACode") String teamACode,
        @JsonProperty("teamBCode") String teamBCode
    ) {
        this.teamACode = Objects.requireNonNull(teamACode, "teamACode is required");
        this.teamBCode = Objects.requireNonNull(teamBCode, "teamBCode is required");
    }
}
