package com.ligitabl.application.usecase.prediction;

import com.ligitabl.api.shared.Either;
import com.ligitabl.application.command.GetUserPredictionCommand;
import com.ligitabl.application.error.ErrorMapper;
import com.ligitabl.application.error.UseCaseError;
import com.ligitabl.domain.model.prediction.Fixture;
import com.ligitabl.domain.model.prediction.PredictionAccessMode;
import com.ligitabl.domain.model.prediction.SwapCooldown;
import com.ligitabl.domain.model.season.RoundNumber;
import com.ligitabl.domain.model.seasonprediction.RankingSource;
import com.ligitabl.domain.model.seasonprediction.TeamRanking;
import com.ligitabl.domain.model.user.UserContext;
import com.ligitabl.domain.repository.*;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Use case for retrieving user predictions with access mode resolution.
 *
 * <p>Handles different user contexts:
 * <ul>
 *   <li><b>Guest</b>: Returns fallback rankings as READONLY_GUEST</li>
 *   <li><b>Authenticated with prediction</b>: Returns user's prediction as EDITABLE or READONLY_COOLDOWN</li>
 *   <li><b>Authenticated without prediction</b>: Returns fallback as CAN_CREATE_ENTRY</li>
 *   <li><b>Viewing other user</b>: Returns their prediction as READONLY_VIEWING_OTHER</li>
 *   <li><b>User not found</b>: Returns fallback as READONLY_USER_NOT_FOUND</li>
 * </ul>
 *
 * <p>This is the Railway-Oriented Programming boundary - domain operations
 * are wrapped in Either monad to provide type-safe error handling.</p>
 */
@Service
public class GetUserPredictionUseCase {

    private final SeasonPredictionRepository seasonPredictionRepository;
    private final RoundStandingsRepository roundStandingsRepository;
    private final SeasonTeamRankingsRepository seasonTeamRankingsRepository;
    private final RoundPredictionRepository roundPredictionRepository;
    private final FixtureRepository fixtureRepository;
    private final StandingRepository standingRepository;

    public GetUserPredictionUseCase(
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
     * Execute the use case with user context resolution.
     *
     * @param command the get user prediction command with user context
     * @return Either containing UseCaseError (left) or UserPredictionViewData (right)
     */
    public Either<UseCaseError, UserPredictionViewData> execute(GetUserPredictionCommand command) {
        return Either.catching(
            () -> buildViewData(command),
            ErrorMapper::toUseCaseError
        );
    }

    /**
     * Build complete prediction view data based on user context.
     */
    private UserPredictionViewData buildViewData(GetUserPredictionCommand command) {
        UserContext ctx = command.userContext();
        int currentRound = roundPredictionRepository.getCurrentRound();
        int maxRounds = 38; // TODO: Get from season config
        int viewingRound = command.resolveRound(currentRound, maxRounds);
        boolean isCurrentRound = viewingRound == currentRound;

        // Determine access mode and rankings based on user type
        return switch (ctx.userType()) {
            case GUEST -> buildGuestView(command, currentRound, viewingRound, isCurrentRound);
            case AUTHENTICATED -> buildAuthenticatedView(command, currentRound, viewingRound, isCurrentRound);
            case VIEWING_OTHER -> buildViewingOtherView(command, currentRound, viewingRound, isCurrentRound);
            case USER_NOT_FOUND -> buildUserNotFoundView(command, currentRound, viewingRound, isCurrentRound);
        };
    }

    /**
     * Build view for guest users (not logged in).
     * Always returns fallback rankings with READONLY_GUEST access mode.
     */
    private UserPredictionViewData buildGuestView(
        GetUserPredictionCommand command,
        int currentRound,
        int viewingRound,
        boolean isCurrentRound
    ) {
        RankingsWithSource rankingsWithSource = getFallbackRankings(command);

        return new UserPredictionViewData(
            rankingsWithSource.rankings(),
            rankingsWithSource.source(),
            PredictionAccessMode.READONLY_GUEST,
            null, // swapCooldown not applicable
            isCurrentRound ? getFixtures(currentRound) : Map.of(),
            isCurrentRound ? standingRepository.findCurrentPositionMap() : Map.of(),
            isCurrentRound ? standingRepository.findCurrentPointsMap() : Map.of(),
            currentRound,
            viewingRound,
            isCurrentRound ? "OPEN" : "COMPLETED",
            "Log in to create your prediction",
            null // no target display name
        );
    }

    /**
     * Build view for authenticated users viewing their own predictions.
     */
    private UserPredictionViewData buildAuthenticatedView(
        GetUserPredictionCommand command,
        int currentRound,
        int viewingRound,
        boolean isCurrentRound
    ) {
        UserContext ctx = command.userContext();

        // If user has a prediction, show it
        if (ctx.hasSeasonPrediction()) {
            var prediction = seasonPredictionRepository
                .findByUserIdAndSeasonId(ctx.userId(), command.seasonId())
                .orElseThrow(() -> new IllegalStateException(
                    "User context indicates prediction exists but not found"
                ));

            // Determine access mode based on swap cooldown
            SwapCooldown swapCooldown = roundPredictionRepository.getSwapCooldown(ctx.userId());
            PredictionAccessMode accessMode = determineAccessMode(swapCooldown, isCurrentRound);

            return new UserPredictionViewData(
                prediction.getRankings(),
                RankingSource.USER_PREDICTION,
                accessMode,
                swapCooldown,
                isCurrentRound ? getFixtures(currentRound) : Map.of(),
                isCurrentRound ? standingRepository.findCurrentPositionMap() : Map.of(),
                isCurrentRound ? standingRepository.findCurrentPointsMap() : Map.of(),
                currentRound,
                viewingRound,
                isCurrentRound ? "OPEN" : "COMPLETED",
                accessMode == PredictionAccessMode.READONLY_COOLDOWN
                    ? "Swap cooldown active"
                    : null,
                null
            );
        }

        // User is authenticated but has no prediction - show fallback with CAN_CREATE_ENTRY
        RankingsWithSource rankingsWithSource = getFallbackRankings(command);

        return new UserPredictionViewData(
            rankingsWithSource.rankings(),
            rankingsWithSource.source(),
            PredictionAccessMode.CAN_CREATE_ENTRY,
            null, // no swap cooldown yet
            isCurrentRound ? getFixtures(currentRound) : Map.of(),
            isCurrentRound ? standingRepository.findCurrentPositionMap() : Map.of(),
            isCurrentRound ? standingRepository.findCurrentPointsMap() : Map.of(),
            currentRound,
            viewingRound,
            isCurrentRound ? "OPEN" : "COMPLETED",
            "Arrange teams and submit to join the competition",
            null
        );
    }

    /**
     * Build view for viewing another user's predictions.
     */
    private UserPredictionViewData buildViewingOtherView(
        GetUserPredictionCommand command,
        int currentRound,
        int viewingRound,
        boolean isCurrentRound
    ) {
        UserContext ctx = command.userContext();

        // If target user has a prediction, show it
        if (ctx.hasSeasonPrediction()) {
            var prediction = seasonPredictionRepository
                .findByUserIdAndSeasonId(ctx.userId(), command.seasonId())
                .orElseThrow(() -> new IllegalStateException(
                    "User context indicates prediction exists but not found"
                ));

            return new UserPredictionViewData(
                prediction.getRankings(),
                RankingSource.USER_PREDICTION,
                PredictionAccessMode.READONLY_VIEWING_OTHER,
                null, // no swap cooldown - readonly
                isCurrentRound ? getFixtures(currentRound) : Map.of(),
                isCurrentRound ? standingRepository.findCurrentPositionMap() : Map.of(),
                isCurrentRound ? standingRepository.findCurrentPointsMap() : Map.of(),
                currentRound,
                viewingRound,
                isCurrentRound ? "OPEN" : "COMPLETED",
                "Viewing " + (command.targetDisplayName() != null ? command.targetDisplayName() : "user") + "'s prediction",
                command.targetDisplayName()
            );
        }

        // Target user exists but has no prediction - show fallback
        RankingsWithSource rankingsWithSource = getFallbackRankings(command);

        return new UserPredictionViewData(
            rankingsWithSource.rankings(),
            rankingsWithSource.source(),
            PredictionAccessMode.READONLY_VIEWING_OTHER,
            null,
            isCurrentRound ? getFixtures(currentRound) : Map.of(),
            isCurrentRound ? standingRepository.findCurrentPositionMap() : Map.of(),
            isCurrentRound ? standingRepository.findCurrentPointsMap() : Map.of(),
            currentRound,
            viewingRound,
            isCurrentRound ? "OPEN" : "COMPLETED",
            (command.targetDisplayName() != null ? command.targetDisplayName() : "This user") + " hasn't made a prediction yet",
            command.targetDisplayName()
        );
    }

    /**
     * Build view when the target user was not found.
     */
    private UserPredictionViewData buildUserNotFoundView(
        GetUserPredictionCommand command,
        int currentRound,
        int viewingRound,
        boolean isCurrentRound
    ) {
        RankingsWithSource rankingsWithSource = getFallbackRankings(command);

        return new UserPredictionViewData(
            rankingsWithSource.rankings(),
            rankingsWithSource.source(),
            PredictionAccessMode.READONLY_USER_NOT_FOUND,
            null,
            isCurrentRound ? getFixtures(currentRound) : Map.of(),
            isCurrentRound ? standingRepository.findCurrentPositionMap() : Map.of(),
            isCurrentRound ? standingRepository.findCurrentPointsMap() : Map.of(),
            currentRound,
            viewingRound,
            isCurrentRound ? "OPEN" : "COMPLETED",
            "User not found",
            null
        );
    }

    /**
     * Determine access mode based on swap cooldown status.
     */
    private PredictionAccessMode determineAccessMode(SwapCooldown swapCooldown, boolean isCurrentRound) {
        if (!isCurrentRound) {
            return PredictionAccessMode.READONLY_COOLDOWN; // Historical rounds are always readonly
        }

        if (swapCooldown != null && swapCooldown.canSwap(Instant.now())) {
            return PredictionAccessMode.EDITABLE;
        }

        return PredictionAccessMode.READONLY_COOLDOWN;
    }

    /**
     * Get fallback rankings using the three-tier hierarchy:
     * 1. Current round standings
     * 2. Season baseline rankings
     */
    private RankingsWithSource getFallbackRankings(GetUserPredictionCommand command) {
        // Try current round standings first
        var roundStandings = roundStandingsRepository.findLatestBySeasonId(command.seasonId());
        if (roundStandings.isPresent() && !roundStandings.get().isEmpty()) {
            return new RankingsWithSource(RankingSource.ROUND_STANDINGS, roundStandings.get());
        }

        // Fallback to season baseline (guaranteed to exist)
        var baseline = seasonTeamRankingsRepository
            .findBySeasonId(command.seasonId())
            .orElseThrow(() -> new IllegalStateException(
                "Season baseline rankings not found for season: " + command.seasonId().value()
            ));

        return new RankingsWithSource(RankingSource.SEASON_BASELINE, baseline);
    }

    /**
     * Get fixtures for a round.
     */
    private Map<String, List<Fixture>> getFixtures(int round) {
        return fixtureRepository.findByRound(RoundNumber.of(round));
    }

    /**
     * Internal record for rankings with source.
     */
    private record RankingsWithSource(
        RankingSource source,
        List<TeamRanking> rankings
    ) {}

    /**
     * Complete view data returned by this use case.
     *
     * <p>Contains all information needed by the template to render the prediction view,
     * including access mode for UI control rendering.</p>
     */
    public record UserPredictionViewData(
        List<TeamRanking> rankings,
        RankingSource source,
        PredictionAccessMode accessMode,
        SwapCooldown swapCooldown,
        Map<String, List<Fixture>> fixtures,
        Map<String, Integer> standingsMap,
        Map<String, Integer> pointsMap,
        int currentRound,
        int viewingRound,
        String roundState,
        String message,
        String targetDisplayName
    ) {
        public UserPredictionViewData {
            Objects.requireNonNull(rankings, "rankings are required");
            Objects.requireNonNull(source, "source is required");
            Objects.requireNonNull(accessMode, "accessMode is required");
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
            return accessMode == PredictionAccessMode.EDITABLE;
        }

        /**
         * Check if user can create a new entry (initial prediction).
         */
        public boolean canCreateEntry() {
            return accessMode == PredictionAccessMode.CAN_CREATE_ENTRY;
        }

        /**
         * Check if the view is readonly.
         */
        public boolean isReadonly() {
            return accessMode.isReadonly();
        }

        /**
         * Check if this is a guest user.
         */
        public boolean isGuest() {
            return accessMode == PredictionAccessMode.READONLY_GUEST;
        }

        /**
         * Check if viewing another user's prediction.
         */
        public boolean isViewingOther() {
            return accessMode == PredictionAccessMode.READONLY_VIEWING_OTHER;
        }

        /**
         * Check if target user was not found.
         */
        public boolean isUserNotFound() {
            return accessMode == PredictionAccessMode.READONLY_USER_NOT_FOUND;
        }
    }
}
