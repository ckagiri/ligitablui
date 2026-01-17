package com.ligitabl.application.command;

import com.ligitabl.domain.model.season.RoundNumber;
import com.ligitabl.domain.model.season.SeasonId;

import java.util.Objects;

/**
 * Command object for the GetMatches use case.
 */
public record GetMatchesCommand(
    SeasonId seasonId,
    RoundNumber round
) {
    public GetMatchesCommand {
        Objects.requireNonNull(seasonId, "seasonId is required");
        Objects.requireNonNull(round, "round is required");
    }

    /**
     * Factory method to create from raw values.
     */
    public static GetMatchesCommand of(String seasonId, int round) {
        return new GetMatchesCommand(
            SeasonId.of(seasonId),
            RoundNumber.of(round)
        );
    }
}
