package com.ligitabl.application.usecase.standing;

import com.ligitabl.api.shared.Either;
import com.ligitabl.application.command.GetMatchesCommand;
import com.ligitabl.application.error.ErrorMapper;
import com.ligitabl.application.error.UseCaseError;
import com.ligitabl.domain.model.standing.Match;
import com.ligitabl.domain.repository.MatchRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;

/**
 * Use case for retrieving matches for a round.
 */
@Service
public class GetMatchesUseCase {

    private final MatchRepository matchRepository;

    public GetMatchesUseCase(MatchRepository matchRepository) {
        this.matchRepository = Objects.requireNonNull(
            matchRepository,
            "matchRepository is required"
        );
    }

    /**
     * Execute the use case to retrieve matches.
     *
     * @param command the get matches command
     * @return Either containing UseCaseError (left) or MatchesResult (right)
     */
    public Either<UseCaseError, MatchesResult> execute(GetMatchesCommand command) {
        return Either.catching(
            () -> buildMatchesResult(command),
            ErrorMapper::toUseCaseError
        );
    }

    private MatchesResult buildMatchesResult(GetMatchesCommand command) {
        List<Match> matches = matchRepository.findBySeasonAndRound(
            command.seasonId(),
            command.round()
        );

        long liveCount = matches.stream().filter(Match::isLive).count();
        long finishedCount = matches.stream().filter(Match::isFinished).count();
        long scheduledCount = matches.stream().filter(Match::isScheduled).count();

        return new MatchesResult(
            command.seasonId().value(),
            command.round().value(),
            matches,
            liveCount,
            finishedCount,
            scheduledCount
        );
    }

    /**
     * Result object containing matches data.
     */
    public record MatchesResult(
        String seasonId,
        int round,
        List<Match> matches,
        long liveCount,
        long finishedCount,
        long scheduledCount
    ) {
        public MatchesResult {
            Objects.requireNonNull(seasonId, "seasonId is required");
            Objects.requireNonNull(matches, "matches are required");
            matches = List.copyOf(matches);
        }

        /**
         * Check if there are any live matches.
         */
        public boolean hasLiveMatches() {
            return liveCount > 0;
        }

        /**
         * Check if all matches are finished.
         */
        public boolean allMatchesFinished() {
            return finishedCount == matches.size();
        }
    }
}
