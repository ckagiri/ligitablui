package com.ligitabl.application.usecase.seasonprediction;

import com.ligitabl.api.shared.Either;
import com.ligitabl.application.command.GetSeasonPredictionCommand;
import com.ligitabl.application.error.ErrorMapper;
import com.ligitabl.application.error.UseCaseError;
import com.ligitabl.domain.model.seasonprediction.RankingSource;
import com.ligitabl.domain.model.seasonprediction.TeamRanking;
import com.ligitabl.domain.repository.RoundStandingsRepository;
import com.ligitabl.domain.repository.SeasonPredictionRepository;
import com.ligitabl.domain.repository.SeasonTeamRankingsRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;

/**
 * Use case for retrieving season prediction with smart fallback.
 *
 * <p>Implements the three-tier fallback hierarchy:
 * 1. User's own prediction (if exists)
 * 2. Current round standings (if available)
 * 3. Season baseline rankings (guaranteed fallback)</p>
 *
 * <p>This is the Railway-Oriented Programming boundary - domain operations
 * are wrapped in Either monad to provide type-safe error handling.</p>
 */
@Service
public class GetSeasonPredictionUseCase {

    private final SeasonPredictionRepository seasonPredictionRepository;
    private final RoundStandingsRepository roundStandingsRepository;
    private final SeasonTeamRankingsRepository seasonTeamRankingsRepository;

    public GetSeasonPredictionUseCase(
        SeasonPredictionRepository seasonPredictionRepository,
        RoundStandingsRepository roundStandingsRepository,
        SeasonTeamRankingsRepository seasonTeamRankingsRepository
    ) {
        this.seasonPredictionRepository = Objects.requireNonNull(
            seasonPredictionRepository,
            "seasonPredictionRepository is required"
        );
        this.roundStandingsRepository = Objects.requireNonNull(
            roundStandingsRepository,
            "roundStandingsRepository is required"
        );
        this.seasonTeamRankingsRepository = Objects.requireNonNull(
            seasonTeamRankingsRepository,
            "seasonTeamRankingsRepository is required"
        );
    }

    /**
     * Execute the use case with smart fallback.
     *
     * @param command the get season prediction command
     * @return Either containing UseCaseError (left) or RankingsWithSource (right)
     */
    public Either<UseCaseError, RankingsWithSource> execute(GetSeasonPredictionCommand command) {
        return Either.catching(
            () -> determineRankingsSource(command),
            ErrorMapper::toUseCaseError
        );
    }

    /**
     * Determine which rankings source to use via fallback hierarchy.
     *
     * @param command the command with user and season info
     * @return RankingsWithSource containing rankings and their source
     */
    private RankingsWithSource determineRankingsSource(GetSeasonPredictionCommand command) {
        // 1. Try user's own prediction first
        var userPrediction = seasonPredictionRepository
            .findByUserIdAndSeasonId(command.userId(), command.seasonId());

        if (userPrediction.isPresent()) {
            return new RankingsWithSource(
                RankingSource.USER_PREDICTION,
                userPrediction.get().getRankings()
            );
        }

        // 2. Fallback to current round standings
        // Note: In a real implementation, we'd get the current round from a service
        // For now, we'll try to get the latest round standings
        var roundStandings = roundStandingsRepository
            .findLatestBySeasonId(command.seasonId());

        if (roundStandings.isPresent() && !roundStandings.get().isEmpty()) {
            return new RankingsWithSource(
                RankingSource.ROUND_STANDINGS,
                roundStandings.get()
            );
        }

        // 3. Final fallback: season baseline (guaranteed to exist)
        var baseline = seasonTeamRankingsRepository
            .findBySeasonId(command.seasonId())
            .orElseThrow(() -> new IllegalStateException(
                "Season baseline rankings not found for season: " + command.seasonId().value() +
                ". This is a critical system invariant violation."
            ));

        return new RankingsWithSource(
            RankingSource.SEASON_BASELINE,
            baseline
        );
    }

    /**
     * Result object containing rankings and their source.
     *
     * <p>The source indicates which tier of the fallback hierarchy was used.</p>
     */
    public record RankingsWithSource(
        RankingSource source,
        List<TeamRanking> rankings
    ) {
        public RankingsWithSource {
            Objects.requireNonNull(source, "source is required");
            Objects.requireNonNull(rankings, "rankings are required");
            rankings = List.copyOf(rankings); // Defensive copy
        }
    }
}
