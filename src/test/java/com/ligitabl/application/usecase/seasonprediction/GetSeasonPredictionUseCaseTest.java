package com.ligitabl.application.usecase.seasonprediction;

import com.ligitabl.api.shared.Either;
import com.ligitabl.application.command.GetSeasonPredictionCommand;
import com.ligitabl.application.error.UseCaseError;
import com.ligitabl.domain.model.prediction.SwapCooldown;
import com.ligitabl.domain.model.season.SeasonId;
import com.ligitabl.domain.model.seasonprediction.RankingSource;
import com.ligitabl.domain.model.seasonprediction.SeasonPrediction;
import com.ligitabl.domain.model.seasonprediction.TeamRanking;
import com.ligitabl.domain.model.team.TeamId;
import com.ligitabl.domain.model.user.UserId;
import com.ligitabl.domain.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for GetSeasonPredictionUseCase.
 * Tests the smart fallback hierarchy: User Prediction → Round Standings → Season Baseline.
 */
class GetSeasonPredictionUseCaseTest {

    private SeasonPredictionRepository seasonPredictionRepository;
    private RoundStandingsRepository roundStandingsRepository;
    private SeasonTeamRankingsRepository seasonTeamRankingsRepository;
    private RoundPredictionRepository roundPredictionRepository;
    private FixtureRepository fixtureRepository;
    private StandingRepository standingRepository;
    private GetSeasonPredictionUseCase useCase;

    private final UserId userId = UserId.generate();
    private final SeasonId seasonId = SeasonId.generate();

    @BeforeEach
    void setUp() {
        seasonPredictionRepository = mock(SeasonPredictionRepository.class);
        roundStandingsRepository = mock(RoundStandingsRepository.class);
        seasonTeamRankingsRepository = mock(SeasonTeamRankingsRepository.class);
        roundPredictionRepository = mock(RoundPredictionRepository.class);
        fixtureRepository = mock(FixtureRepository.class);
        standingRepository = mock(StandingRepository.class);

        // Default mock behavior for additional repositories
        when(roundPredictionRepository.getCurrentRound()).thenReturn(19);
        when(roundPredictionRepository.getSwapCooldown(any())).thenReturn(
            new SwapCooldown(Instant.now().minusSeconds(3600), true, 1, true)
        );
        when(fixtureRepository.findByRound(any())).thenReturn(Map.of());
        when(standingRepository.findCurrentPositionMap()).thenReturn(Map.of());
        when(standingRepository.findCurrentPointsMap()).thenReturn(Map.of());

        useCase = new GetSeasonPredictionUseCase(
            seasonPredictionRepository,
            roundStandingsRepository,
            seasonTeamRankingsRepository,
            roundPredictionRepository,
            fixtureRepository,
            standingRepository
        );
    }

    @Test
    void shouldReturnUserPredictionWhenExists() {
        // Given: User has their own prediction
        List<TeamRanking> userRankings = create20Rankings();
        SeasonPrediction userPrediction = createMockPrediction(userRankings);

        when(seasonPredictionRepository.findByUserIdAndSeasonId(userId, seasonId))
            .thenReturn(Optional.of(userPrediction));

        GetSeasonPredictionCommand command = GetSeasonPredictionCommand.forSeason(userId, seasonId);

        // When
        Either<UseCaseError, GetSeasonPredictionUseCase.PredictionViewData> result =
            useCase.execute(command);

        // Then
        assertTrue(result.isRight(), "Should succeed");
        GetSeasonPredictionUseCase.PredictionViewData viewData = result.get();

        assertEquals(RankingSource.USER_PREDICTION, viewData.source());
        assertEquals(20, viewData.rankings().size());

        // Verify fallbacks were NOT called
        verifyNoInteractions(roundStandingsRepository);
        verifyNoInteractions(seasonTeamRankingsRepository);
    }

    @Test
    void shouldFallbackToRoundStandingsWhenUserPredictionNotFound() {
        // Given: User has no prediction, but round standings exist
        List<TeamRanking> roundRankings = create20Rankings();

        when(seasonPredictionRepository.findByUserIdAndSeasonId(userId, seasonId))
            .thenReturn(Optional.empty());
        when(roundStandingsRepository.findLatestBySeasonId(seasonId))
            .thenReturn(Optional.of(roundRankings));

        GetSeasonPredictionCommand command = GetSeasonPredictionCommand.forSeason(userId, seasonId);

        // When
        Either<UseCaseError, GetSeasonPredictionUseCase.PredictionViewData> result =
            useCase.execute(command);

        // Then
        assertTrue(result.isRight(), "Should succeed");
        GetSeasonPredictionUseCase.PredictionViewData viewData = result.get();

        assertEquals(RankingSource.ROUND_STANDINGS, viewData.source());
        assertEquals(20, viewData.rankings().size());

        // Verify baseline was NOT called (round standings found)
        verifyNoInteractions(seasonTeamRankingsRepository);
    }

    @Test
    void shouldFallbackToSeasonBaselineWhenNoUserPredictionAndNoRoundStandings() {
        // Given: User has no prediction, no round standings, but baseline exists
        List<TeamRanking> baselineRankings = create20Rankings();

        when(seasonPredictionRepository.findByUserIdAndSeasonId(userId, seasonId))
            .thenReturn(Optional.empty());
        when(roundStandingsRepository.findLatestBySeasonId(seasonId))
            .thenReturn(Optional.empty());
        when(seasonTeamRankingsRepository.findBySeasonId(seasonId))
            .thenReturn(Optional.of(baselineRankings));

        GetSeasonPredictionCommand command = GetSeasonPredictionCommand.forSeason(userId, seasonId);

        // When
        Either<UseCaseError, GetSeasonPredictionUseCase.PredictionViewData> result =
            useCase.execute(command);

        // Then
        assertTrue(result.isRight(), "Should succeed");
        GetSeasonPredictionUseCase.PredictionViewData viewData = result.get();

        assertEquals(RankingSource.SEASON_BASELINE, viewData.source());
        assertEquals(20, viewData.rankings().size());
    }

    @Test
    void shouldSkipRoundStandingsIfEmpty() {
        // Given: User has no prediction, round standings exist but empty
        List<TeamRanking> baselineRankings = create20Rankings();

        when(seasonPredictionRepository.findByUserIdAndSeasonId(userId, seasonId))
            .thenReturn(Optional.empty());
        when(roundStandingsRepository.findLatestBySeasonId(seasonId))
            .thenReturn(Optional.of(List.of())); // Empty list
        when(seasonTeamRankingsRepository.findBySeasonId(seasonId))
            .thenReturn(Optional.of(baselineRankings));

        GetSeasonPredictionCommand command = GetSeasonPredictionCommand.forSeason(userId, seasonId);

        // When
        Either<UseCaseError, GetSeasonPredictionUseCase.PredictionViewData> result =
            useCase.execute(command);

        // Then
        assertTrue(result.isRight(), "Should succeed");
        GetSeasonPredictionUseCase.PredictionViewData viewData = result.get();

        assertEquals(RankingSource.SEASON_BASELINE, viewData.source());
        assertEquals(20, viewData.rankings().size());
    }

    @Test
    void shouldReturnErrorWhenNoBaselineExists() {
        // Given: Complete fallback failure - no baseline (critical system invariant violation)
        when(seasonPredictionRepository.findByUserIdAndSeasonId(userId, seasonId))
            .thenReturn(Optional.empty());
        when(roundStandingsRepository.findLatestBySeasonId(seasonId))
            .thenReturn(Optional.empty());
        when(seasonTeamRankingsRepository.findBySeasonId(seasonId))
            .thenReturn(Optional.empty());

        GetSeasonPredictionCommand command = GetSeasonPredictionCommand.forSeason(userId, seasonId);

        // When
        Either<UseCaseError, GetSeasonPredictionUseCase.PredictionViewData> result =
            useCase.execute(command);

        // Then
        assertTrue(result.isLeft(), "Should fail - got: " + result);
        UseCaseError error = result.getLeft();

        assertEquals(UseCaseError.ErrorType.BUSINESS_RULE, error.type());
        // Message should contain either "invariant" or "Invalid state"
        assertTrue(error.message().toLowerCase().contains("invariant") ||
                   error.message().toLowerCase().contains("invalid state"),
            "Message was: " + error.message());
    }

    @Test
    void shouldReturnErrorWhenRepositoryThrowsException() {
        // Given: Repository throws exception
        when(seasonPredictionRepository.findByUserIdAndSeasonId(userId, seasonId))
            .thenThrow(new RuntimeException("Database error"));

        GetSeasonPredictionCommand command = GetSeasonPredictionCommand.forSeason(userId, seasonId);

        // When
        Either<UseCaseError, GetSeasonPredictionUseCase.PredictionViewData> result =
            useCase.execute(command);

        // Then
        assertTrue(result.isLeft(), "Should fail");
        UseCaseError error = result.getLeft();

        assertEquals(UseCaseError.ErrorType.BUSINESS_RULE, error.type());
        assertTrue(error.message().contains("Operation failed"));
    }

    @Test
    void shouldThrowNullPointerExceptionForNullRepositories() {
        // When/Then
        assertThrows(NullPointerException.class, () ->
            new GetSeasonPredictionUseCase(
                null, roundStandingsRepository, seasonTeamRankingsRepository,
                roundPredictionRepository, fixtureRepository, standingRepository
            )
        );
        assertThrows(NullPointerException.class, () ->
            new GetSeasonPredictionUseCase(
                seasonPredictionRepository, null, seasonTeamRankingsRepository,
                roundPredictionRepository, fixtureRepository, standingRepository
            )
        );
        assertThrows(NullPointerException.class, () ->
            new GetSeasonPredictionUseCase(
                seasonPredictionRepository, roundStandingsRepository, null,
                roundPredictionRepository, fixtureRepository, standingRepository
            )
        );
        assertThrows(NullPointerException.class, () ->
            new GetSeasonPredictionUseCase(
                seasonPredictionRepository, roundStandingsRepository, seasonTeamRankingsRepository,
                null, fixtureRepository, standingRepository
            )
        );
        assertThrows(NullPointerException.class, () ->
            new GetSeasonPredictionUseCase(
                seasonPredictionRepository, roundStandingsRepository, seasonTeamRankingsRepository,
                roundPredictionRepository, null, standingRepository
            )
        );
        assertThrows(NullPointerException.class, () ->
            new GetSeasonPredictionUseCase(
                seasonPredictionRepository, roundStandingsRepository, seasonTeamRankingsRepository,
                roundPredictionRepository, fixtureRepository, null
            )
        );
    }

    @Test
    void shouldReturnImmutableRankingsList() {
        // Given: User has prediction
        List<TeamRanking> userRankings = create20Rankings();
        SeasonPrediction userPrediction = createMockPrediction(userRankings);

        when(seasonPredictionRepository.findByUserIdAndSeasonId(userId, seasonId))
            .thenReturn(Optional.of(userPrediction));

        GetSeasonPredictionCommand command = GetSeasonPredictionCommand.forSeason(userId, seasonId);

        // When
        Either<UseCaseError, GetSeasonPredictionUseCase.PredictionViewData> result =
            useCase.execute(command);

        // Then
        assertTrue(result.isRight());
        List<TeamRanking> rankings = result.get().rankings();

        // Verify immutability
        assertThrows(UnsupportedOperationException.class, () ->
            rankings.add(TeamRanking.create(TeamId.generate(), 1))
        );
    }

    @Test
    void shouldReturnCorrectViewDataForCurrentRound() {
        // Given: User has prediction, viewing current round
        List<TeamRanking> userRankings = create20Rankings();
        SeasonPrediction userPrediction = createMockPrediction(userRankings);

        when(seasonPredictionRepository.findByUserIdAndSeasonId(userId, seasonId))
            .thenReturn(Optional.of(userPrediction));

        GetSeasonPredictionCommand command = GetSeasonPredictionCommand.forSeason(userId, seasonId);

        // When
        Either<UseCaseError, GetSeasonPredictionUseCase.PredictionViewData> result =
            useCase.execute(command);

        // Then
        assertTrue(result.isRight());
        GetSeasonPredictionUseCase.PredictionViewData viewData = result.get();

        assertEquals(19, viewData.currentRound());
        assertEquals(19, viewData.viewingRound());
        assertTrue(viewData.isCurrentRound());
        assertEquals("OPEN", viewData.roundState());
    }

    @Test
    void shouldReturnCorrectViewDataForHistoricalRound() {
        // Given: User has prediction, viewing historical round
        List<TeamRanking> userRankings = create20Rankings();
        SeasonPrediction userPrediction = createMockPrediction(userRankings);

        when(seasonPredictionRepository.findByUserIdAndSeasonId(userId, seasonId))
            .thenReturn(Optional.of(userPrediction));

        GetSeasonPredictionCommand command = GetSeasonPredictionCommand.forRound(userId, seasonId, 15);

        // When
        Either<UseCaseError, GetSeasonPredictionUseCase.PredictionViewData> result =
            useCase.execute(command);

        // Then
        assertTrue(result.isRight());
        GetSeasonPredictionUseCase.PredictionViewData viewData = result.get();

        assertEquals(19, viewData.currentRound());
        assertEquals(15, viewData.viewingRound());
        assertFalse(viewData.isCurrentRound());
        assertEquals("COMPLETED", viewData.roundState());
    }

    // Helper methods

    private List<TeamRanking> create20Rankings() {
        List<TeamRanking> rankings = new ArrayList<>();
        for (int i = 1; i <= 20; i++) {
            rankings.add(TeamRanking.create(TeamId.generate(), i));
        }
        return rankings;
    }

    private SeasonPrediction createMockPrediction(List<TeamRanking> rankings) {
        SeasonPrediction mock = mock(SeasonPrediction.class);
        when(mock.getRankings()).thenReturn(rankings);
        return mock;
    }
}
