package com.ligitabl.application.command;

import com.ligitabl.domain.model.season.RoundNumber;
import com.ligitabl.domain.model.season.SeasonId;

import java.util.Objects;

/**
 * Command object for the GetStandings use case.
 */
public record GetStandingsCommand(
    SeasonId seasonId,
    RoundNumber round
) {
    public GetStandingsCommand {
        Objects.requireNonNull(seasonId, "seasonId is required");
        Objects.requireNonNull(round, "round is required");
    }

    /**
     * Factory method to create from raw values.
     */
    public static GetStandingsCommand of(String seasonId, int round) {
        return new GetStandingsCommand(
            SeasonId.of(seasonId),
            RoundNumber.of(round)
        );
    }
}
