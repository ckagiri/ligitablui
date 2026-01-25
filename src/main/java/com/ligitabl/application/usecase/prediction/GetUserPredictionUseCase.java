package com.ligitabl.application.usecase.prediction;

import com.ligitabl.api.shared.Either;
import com.ligitabl.application.command.GetUserPredictionCommand;
import com.ligitabl.application.error.ErrorMapper;
import com.ligitabl.application.error.UseCaseError;
import com.ligitabl.domain.model.prediction.Fixture;
import com.ligitabl.domain.model.prediction.PredictionAccessMode;
import com.ligitabl.domain.model.prediction.SwapCooldown;
import com.ligitabl.domain.model.roundresult.RoundResult;
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
    private final RoundResultRepository roundResultRepository;
    private final FixtureRepository fixtureRepository;
    private final StandingRepository standingRepository;

    public GetUserPredictionUseCase(
        SeasonPredictionRepository seasonPredictionRepository,
        RoundStandingsRepository roundStandingsRepository,
        SeasonTeamRankingsRepository seasonTeamRankingsRepository,
        RoundPredictionRepository roundPredictionRepository,
        RoundResultRepository roundResultRepository,
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
        this.roundResultRepository = Objects.requireNonNull(
            roundResultRepository,
            "roundResultRepository is required"
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
        RoundNumber viewingRoundNumber = RoundNumber.of(viewingRound);

        // Get standings and points - use historical data for past rounds
        Map<String, Integer> standingsMap = isCurrentRound
            ? standingRepository.findCurrentPositionMap()
            : standingRepository.findPositionMap(command.seasonId(), viewingRoundNumber);
        Map<String, Integer> pointsMap = isCurrentRound
            ? standingRepository.findCurrentPointsMap()
            : standingRepository.findPointsMap(command.seasonId(), viewingRoundNumber);

        String message = isCurrentRound
            ? "Log in to create your prediction"
            : "Viewing Gameweek " + viewingRound + " results";

        return new UserPredictionViewData(
            rankingsWithSource.rankings(),
            rankingsWithSource.source(),
            PredictionAccessMode.READONLY_GUEST,
            null, // swapCooldown not applicable
            isCurrentRound ? getFixtures(currentRound) : Map.of(),
            standingsMap,
            pointsMap,
            currentRound,
            viewingRound,
            isCurrentRound ? "OPEN" : "COMPLETED",
            message,
            null, // no target display name
            null  // no round result for guest
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
        RoundNumber viewingRoundNumber = RoundNumber.of(viewingRound);

        // Get swap cooldown for this user
        SwapCooldown swapCooldown = roundPredictionRepository.getSwapCooldown(ctx.userId());

        // First try to load from SeasonPredictionRepository (the proper domain model)
        if (ctx.hasSeasonPrediction()) {
            var seasonPrediction = seasonPredictionRepository
                .findByUserIdAndSeasonId(ctx.userId(), command.seasonId());

            // For historical rounds, load RoundResult with scored data
            if (!isCurrentRound) {
                var roundResult = roundResultRepository.findByUserAndRound(ctx.userId(), viewingRoundNumber);
                if (roundResult.isPresent()) {
                    // Convert RoundResult rankings to TeamRanking for display
                    List<TeamRanking> rankings = convertResultRankingsToTeamRankings(roundResult.get());

                    return new UserPredictionViewData(
                        rankings,
                        RankingSource.USER_PREDICTION,
                        PredictionAccessMode.READONLY_COOLDOWN, // Historical is always readonly
                        null, // No swap cooldown for historical
                        Map.of(), // No fixtures for historical
                        Map.of(), // Standings come from RoundResult
                        Map.of(), // Points not needed for historical
                        currentRound,
                        viewingRound,
                        "COMPLETED",
                        "Viewing Gameweek " + viewingRound + " results",
                        null,
                        roundResult.get()
                    );
                }
            }

            if (seasonPrediction.isPresent()) {
                PredictionAccessMode accessMode = determineAccessMode(swapCooldown, isCurrentRound);

                // Get standings and points for current round
                Map<String, Integer> standingsMap = standingRepository.findCurrentPositionMap();
                Map<String, Integer> pointsMap = standingRepository.findCurrentPointsMap();

                String message = null;
                if (accessMode == PredictionAccessMode.READONLY_COOLDOWN) {
                    message = "Swap cooldown active";
                }

                return new UserPredictionViewData(
                    seasonPrediction.get().getRankings(),
                    RankingSource.USER_PREDICTION,
                    accessMode,
                    swapCooldown,
                    getFixtures(currentRound),
                    standingsMap,
                    pointsMap,
                    currentRound,
                    viewingRound,
                    "OPEN",
                    message,
                    null,
                    null // No round result for current round
                );
            }

            // User context says they have a prediction, but not in SeasonPredictionRepository
            // This means they made an initial prediction via the demo RoundPredictionRepository
            // Load from there and convert to TeamRanking format
            var roundPredictions = roundPredictionRepository.findCurrentByUser(ctx.userId());
            if (!roundPredictions.isEmpty()) {
                List<TeamRanking> rankings = convertPredictionRowsToRankings(roundPredictions);
                PredictionAccessMode accessMode = determineAccessMode(swapCooldown, isCurrentRound);

                Map<String, Integer> standingsMap = standingRepository.findCurrentPositionMap();
                Map<String, Integer> pointsMap = standingRepository.findCurrentPointsMap();

                String message = null;
                if (accessMode == PredictionAccessMode.READONLY_COOLDOWN) {
                    message = "Swap cooldown active";
                }

                return new UserPredictionViewData(
                    rankings,
                    RankingSource.USER_PREDICTION,
                    accessMode,
                    swapCooldown,
                    getFixtures(currentRound),
                    standingsMap,
                    pointsMap,
                    currentRound,
                    viewingRound,
                    "OPEN",
                    message,
                    null,
                    null // No round result for current round
                );
            }
        }

        // User is authenticated but has no prediction - show fallback with CAN_CREATE_ENTRY
        RankingsWithSource rankingsWithSource = getFallbackRankings(command);

        // For past rounds without prediction, still show historical standings
        Map<String, Integer> standingsMap = isCurrentRound
            ? standingRepository.findCurrentPositionMap()
            : standingRepository.findPositionMap(command.seasonId(), viewingRoundNumber);
        Map<String, Integer> pointsMap = isCurrentRound
            ? standingRepository.findCurrentPointsMap()
            : standingRepository.findPointsMap(command.seasonId(), viewingRoundNumber);

        // Can only create entry in current round
        PredictionAccessMode accessMode = isCurrentRound
            ? PredictionAccessMode.CAN_CREATE_ENTRY
            : PredictionAccessMode.READONLY_COOLDOWN;

        String message = isCurrentRound
            ? "Arrange teams and submit to join the competition"
            : "Viewing Gameweek " + viewingRound + " results";

        return new UserPredictionViewData(
            rankingsWithSource.rankings(),
            rankingsWithSource.source(),
            accessMode,
            null, // no swap cooldown yet
            isCurrentRound ? getFixtures(currentRound) : Map.of(),
            standingsMap,
            pointsMap,
            currentRound,
            viewingRound,
            isCurrentRound ? "OPEN" : "COMPLETED",
            message,
            null,
            null // No round result
        );
    }

    /**
     * Convert RoundResult rankings to TeamRanking list for template display.
     */
    private List<TeamRanking> convertResultRankingsToTeamRankings(RoundResult result) {
        return result.rankings().stream()
            .map(r -> {
                String teamIdStr = "team-" + r.teamCode().toLowerCase() + "-" +
                    String.format("%012d", getTeamNumber(r.teamCode()));
                return TeamRanking.create(
                    com.ligitabl.domain.model.team.TeamId.of(teamIdStr),
                    r.predictedPosition()
                );
            })
            .toList();
    }

    /**
     * Convert PredictionRow list to TeamRanking list.
     * Used to bridge between the demo RoundPredictionRepository and the domain model.
     */
    private List<TeamRanking> convertPredictionRowsToRankings(List<com.ligitabl.domain.model.prediction.PredictionRow> rows) {
        return rows.stream()
            .map(row -> {
                // Convert team code to TeamId format: "team-{code}-{padded number}"
                String teamIdStr = "team-" + row.getTeamCode().toLowerCase() + "-" +
                    String.format("%012d", getTeamNumber(row.getTeamCode()));
                return TeamRanking.create(
                    com.ligitabl.domain.model.team.TeamId.of(teamIdStr),
                    row.getPosition()
                );
            })
            .toList();
    }

    /**
     * Get a unique number for a team code (used for TeamId generation).
     */
    private int getTeamNumber(String teamCode) {
        return switch (teamCode) {
            case "MCI" -> 1;
            case "ARS" -> 2;
            case "LIV" -> 3;
            case "AVL" -> 4;
            case "TOT" -> 5;
            case "CHE" -> 6;
            case "NEW" -> 7;
            case "MUN" -> 8;
            case "WHU" -> 9;
            case "BHA" -> 10;
            case "WOL" -> 11;
            case "FUL" -> 12;
            case "BOU" -> 13;
            case "CRY" -> 14;
            case "BRE" -> 15;
            case "EVE" -> 16;
            case "NFO" -> 17;
            case "LEE" -> 18;
            case "BUR" -> 19;
            case "SUN" -> 20;
            default -> 0;
        };
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
        RoundNumber viewingRoundNumber = RoundNumber.of(viewingRound);

        // For historical rounds, load RoundResult with scored data
        if (!isCurrentRound && ctx.hasSeasonPrediction()) {
            var roundResult = roundResultRepository.findByUserAndRound(ctx.userId(), viewingRoundNumber);
            if (roundResult.isPresent()) {
                List<TeamRanking> rankings = convertResultRankingsToTeamRankings(roundResult.get());

                return new UserPredictionViewData(
                    rankings,
                    RankingSource.USER_PREDICTION,
                    PredictionAccessMode.READONLY_VIEWING_OTHER,
                    null,
                    Map.of(),
                    Map.of(),
                    Map.of(),
                    currentRound,
                    viewingRound,
                    "COMPLETED",
                    "Viewing " + (command.targetDisplayName() != null ? command.targetDisplayName() : "user") + "'s Gameweek " + viewingRound + " result",
                    command.targetDisplayName(),
                    roundResult.get()
                );
            }
        }

        // If target user has a prediction, show it (current round)
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
                getFixtures(currentRound),
                standingRepository.findCurrentPositionMap(),
                standingRepository.findCurrentPointsMap(),
                currentRound,
                viewingRound,
                "OPEN",
                "Viewing " + (command.targetDisplayName() != null ? command.targetDisplayName() : "user") + "'s prediction",
                command.targetDisplayName(),
                null
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
            command.targetDisplayName(),
            null
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
            null,
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
        String targetDisplayName,
        RoundResult roundResult  // Present for historical views with scored results
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
         * Check if this view has historical round result data.
         */
        public boolean hasRoundResult() {
            return roundResult != null;
        }

        /**
         * Check if viewing the current round.
         */
        public boolean isCurrentRound() {
            return viewingRound == currentRound;
        }

        /**
         * Check if user can swap teams.
         * Returns true for EDITABLE or CAN_CREATE_ENTRY modes.
         */
        public boolean canSwap() {
            return accessMode == PredictionAccessMode.EDITABLE ||
                   accessMode == PredictionAccessMode.CAN_CREATE_ENTRY;
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
