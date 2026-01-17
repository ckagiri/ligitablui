package com.ligitabl.application.usecase.standing;

import com.ligitabl.api.shared.Either;
import com.ligitabl.application.command.GetStandingsCommand;
import com.ligitabl.application.error.ErrorMapper;
import com.ligitabl.application.error.UseCaseError;
import com.ligitabl.domain.model.standing.TeamStanding;
import com.ligitabl.domain.repository.StandingRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;

/**
 * Use case for retrieving league standings.
 */
@Service
public class GetStandingsUseCase {

    private final StandingRepository standingRepository;

    public GetStandingsUseCase(StandingRepository standingRepository) {
        this.standingRepository = Objects.requireNonNull(
            standingRepository,
            "standingRepository is required"
        );
    }

    /**
     * Execute the use case to retrieve standings.
     *
     * @param command the get standings command
     * @return Either containing UseCaseError (left) or StandingsResult (right)
     */
    public Either<UseCaseError, StandingsResult> execute(GetStandingsCommand command) {
        return Either.catching(
            () -> buildStandingsResult(command),
            ErrorMapper::toUseCaseError
        );
    }

    private StandingsResult buildStandingsResult(GetStandingsCommand command) {
        List<TeamStanding> standings = standingRepository.findBySeasonAndRound(
            command.seasonId(),
            command.round()
        );

        return new StandingsResult(
            command.seasonId().value(),
            command.round().value(),
            standings
        );
    }

    /**
     * Result object containing standings data.
     */
    public record StandingsResult(
        String seasonId,
        int round,
        List<TeamStanding> standings
    ) {
        public StandingsResult {
            Objects.requireNonNull(seasonId, "seasonId is required");
            Objects.requireNonNull(standings, "standings are required");
            standings = List.copyOf(standings);
        }
    }
}
