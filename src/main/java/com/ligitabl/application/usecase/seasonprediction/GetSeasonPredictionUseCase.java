package com.ligitabl.application.usecase.seasonprediction;

import com.ligitabl.api.shared.Either;
import com.ligitabl.application.command.GetSeasonPredictionCommand;
import com.ligitabl.application.error.ErrorMapper;
import com.ligitabl.application.error.UseCaseError;
import com.ligitabl.domain.model.prediction.Fixture;
import com.ligitabl.domain.model.prediction.SwapCooldown;
import com.ligitabl.domain.model.season.RoundNumber;
import com.ligitabl.domain.model.seasonprediction.RankingSource;
import com.ligitabl.domain.model.seasonprediction.TeamRanking;
import com.ligitabl.domain.repository.*;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
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
    private final RoundPredictionRepository roundPredictionRepository;
    private final FixtureRepository fixtureRepository;
    private final StandingRepository standingRepository;

    public GetSeasonPredictionUseCase(
        SeasonPredictionRepository seasonPredictionRepository,
        RoundStandingsRepository roundStandingsRepository,
        SeasonTeamRankingsRepository seasonTeamRankingsRepository,
        RoundPredictionRepository roundPredictionRepository,
        FixtureRepository fixtureRepository,
        StandingRepository standingRepository
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
        this.roundPredictionRepository = Objects.requireNonNull(
            roundPredictionRepository,
            "roundPredictionRepository is required"
        );
        this.fixtureRepository = Objects.requireNonNull(
            fixtureRepository,
            "fixtureRepository is required"
        );
        this.standingRepository = Objects.requireNonNull(
            standingRepository,
            "standingRepository is required"
        );
    }

    /**
     * Execute the use case with smart fallback.
     *
     * @param command the get season prediction command
     * @return Either containing UseCaseError (left) or PredictionViewData (right)
     */
    public Either<UseCaseError, PredictionViewData> execute(GetSeasonPredictionCommand command) {
        return Either.catching(
            () -> buildPredictionViewData(command),
            ErrorMapper::toUseCaseError
        );
    }

    /**
     * Build complete prediction view data with all required information.
     */
    private PredictionViewData buildPredictionViewData(GetSeasonPredictionCommand command) {
        int currentRound = roundPredictionRepository.getCurrentRound();
        boolean isCurrentRound = command.roundNumber() == null || command.roundNumber() == currentRound;

        // Determine rankings source using fallback hierarchy
        RankingsWithSource rankingsWithSource = determineRankingsSource(command);

        // Fetch additional data based on whether viewing current or historical round
        if (isCurrentRound) {
            return buildCurrentRoundViewData(command, rankingsWithSource, currentRound);
        } else {
            return buildHistoricalRoundViewData(command, rankingsWithSource, currentRound);
        }
    }

    /**
     * Build view data for current round (live, potentially editable).
     */
    private PredictionViewData buildCurrentRoundViewData(
        GetSeasonPredictionCommand command,
        RankingsWithSource rankingsWithSource,
        int currentRound
    ) {
        // Get swap status
        SwapCooldown swapCooldown = roundPredictionRepository.getSwapCooldown(command.userId());
        boolean isInitialPrediction = !swapCooldown.initialPredictionMade();

        // Get fixtures for current round
        Map<String, List<Fixture>> fixtures = fixtureRepository.findByRound(RoundNumber.of(currentRound));

        // Get current standings and points
        Map<String, Integer> standingsMap = standingRepository.findCurrentPositionMap();
        Map<String, Integer> pointsMap = standingRepository.findCurrentPointsMap();

        return new PredictionViewData(
            rankingsWithSource.rankings(),
            rankingsWithSource.source(),
            swapCooldown,
            fixtures,
            standingsMap,
            pointsMap,
            isInitialPrediction,
            null, // roundScore - not applicable for current round
            null, // totalHits - not applicable for current round
            currentRound,
            currentRound,
            "OPEN" // Simplified - would need actual round state logic
        );
    }

    /**
     * Build view data for historical round (read-only, scored).
     */
    private PredictionViewData buildHistoricalRoundViewData(
        GetSeasonPredictionCommand command,
        RankingsWithSource rankingsWithSource,
        int currentRound
    ) {
        int viewingRound = command.roundNumber();

        // For historical rounds, we would calculate score based on actual results
        // For now, returning null as we don't have the scoring logic yet
        Integer roundScore = null;
        Integer totalHits = null;

        return new PredictionViewData(
            rankingsWithSource.rankings(),
            rankingsWithSource.source(),
            null, // no swap cooldown for historical
            Map.of(), // no fixtures for historical
            Map.of(), // no standings for historical
            Map.of(), // no points for historical
            false, // not initial prediction
            roundScore,
            totalHits,
            currentRound,
            viewingRound,
            "COMPLETED"
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

    /**
     * Complete prediction view data containing all information needed by the template.
     *
     * <p>This combines rankings with all supplementary data like fixtures, standings,
     * swap status, and scoring information.</p>
     */
    public record PredictionViewData(
        List<TeamRanking> rankings,
        RankingSource source,
        SwapCooldown swapCooldown,
        Map<String, List<Fixture>> fixtures,
        Map<String, Integer> standingsMap,
        Map<String, Integer> pointsMap,
        boolean isInitialPrediction,
        Integer roundScore,
        Integer totalHits,
        int currentRound,
        int viewingRound,
        String roundState
    ) {
        public PredictionViewData {
            Objects.requireNonNull(rankings, "rankings are required");
            Objects.requireNonNull(source, "source is required");
            rankings = List.copyOf(rankings);
            fixtures = fixtures != null ? Map.copyOf(fixtures) : Map.of();
            standingsMap = standingsMap != null ? Map.copyOf(standingsMap) : Map.of();
            pointsMap = pointsMap != null ? Map.copyOf(pointsMap) : Map.of();
        }

        /**
         * Check if viewing the current round.
         */
        public boolean isCurrentRound() {
            return viewingRound == currentRound;
        }

        /**
         * Check if user can swap teams.
         */
        public boolean canSwap() {
            return isCurrentRound() && swapCooldown != null && swapCooldown.canSwap(java.time.Instant.now());
        }
    }
}
