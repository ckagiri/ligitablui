package com.ligitabl.application.usecase.prediction;

import com.ligitabl.api.shared.Either;
import com.ligitabl.application.command.GetUserPredictionCommand;
import com.ligitabl.application.error.UseCaseError;
import com.ligitabl.domain.model.prediction.PredictionAccessMode;
import com.ligitabl.domain.model.prediction.SwapCooldown;
import com.ligitabl.domain.model.season.SeasonId;
import com.ligitabl.domain.model.seasonprediction.RankingSource;
import com.ligitabl.domain.model.seasonprediction.SeasonPrediction;
import com.ligitabl.domain.model.seasonprediction.TeamRanking;
import com.ligitabl.domain.model.team.TeamId;
import com.ligitabl.domain.model.user.UserId;
import com.ligitabl.domain.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
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
 * Unit tests for GetUserPredictionUseCase.
 * Tests user context resolution and access mode determination.
 */
class GetUserPredictionUseCaseTest {

    private SeasonPredictionRepository seasonPredictionRepository;
    private RoundStandingsRepository roundStandingsRepository;
    private SeasonTeamRankingsRepository seasonTeamRankingsRepository;
    private RoundPredictionRepository roundPredictionRepository;
    private FixtureRepository fixtureRepository;
    private StandingRepository standingRepository;
    private GetUserPredictionUseCase useCase;

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

        // Default mock behavior
        when(roundPredictionRepository.getCurrentRound()).thenReturn(19);
        when(fixtureRepository.findByRound(any())).thenReturn(Map.of());
        when(standingRepository.findCurrentPositionMap()).thenReturn(Map.of());
        when(standingRepository.findCurrentPointsMap()).thenReturn(Map.of());

        // Default baseline rankings (always available)
        when(seasonTeamRankingsRepository.findBySeasonId(any()))
            .thenReturn(Optional.of(create20Rankings()));

        useCase = new GetUserPredictionUseCase(
            seasonPredictionRepository,
            roundStandingsRepository,
            seasonTeamRankingsRepository,
            roundPredictionRepository,
            fixtureRepository,
            standingRepository
        );
    }

    @Nested
    @DisplayName("Guest User Tests")
    class GuestUserTests {

        @Test
        @DisplayName("Guest user should get READONLY_GUEST access mode")
        void guestUserShouldGetReadonlyGuestAccessMode() {
            // Given
            GetUserPredictionCommand command = GetUserPredictionCommand.forGuest(seasonId, null);

            // When
            Either<UseCaseError, GetUserPredictionUseCase.UserPredictionViewData> result =
                useCase.execute(command);

            // Then
            assertTrue(result.isRight(), "Should succeed");
            GetUserPredictionUseCase.UserPredictionViewData viewData = result.get();

            assertEquals(PredictionAccessMode.READONLY_GUEST, viewData.accessMode());
            assertTrue(viewData.isGuest());
            assertTrue(viewData.isReadonly());
            assertFalse(viewData.canSwap());
            assertFalse(viewData.canCreateEntry());
        }

        @Test
        @DisplayName("Guest user should get fallback rankings")
        void guestUserShouldGetFallbackRankings() {
            // Given
            List<TeamRanking> roundStandings = create20Rankings();
            when(roundStandingsRepository.findLatestBySeasonId(seasonId))
                .thenReturn(Optional.of(roundStandings));

            GetUserPredictionCommand command = GetUserPredictionCommand.forGuest(seasonId, null);

            // When
            Either<UseCaseError, GetUserPredictionUseCase.UserPredictionViewData> result =
                useCase.execute(command);

            // Then
            assertTrue(result.isRight());
            GetUserPredictionUseCase.UserPredictionViewData viewData = result.get();

            assertEquals(RankingSource.ROUND_STANDINGS, viewData.source());
            assertEquals(20, viewData.rankings().size());
        }

        @Test
        @DisplayName("Guest user should fall back to season baseline when no round standings")
        void guestUserShouldFallbackToSeasonBaseline() {
            // Given
            when(roundStandingsRepository.findLatestBySeasonId(seasonId))
                .thenReturn(Optional.empty());

            GetUserPredictionCommand command = GetUserPredictionCommand.forGuest(seasonId, null);

            // When
            Either<UseCaseError, GetUserPredictionUseCase.UserPredictionViewData> result =
                useCase.execute(command);

            // Then
            assertTrue(result.isRight());
            GetUserPredictionUseCase.UserPredictionViewData viewData = result.get();

            assertEquals(RankingSource.SEASON_BASELINE, viewData.source());
        }
    }

    @Nested
    @DisplayName("Authenticated User Tests")
    class AuthenticatedUserTests {

        @Test
        @DisplayName("Authenticated user with prediction should get USER_PREDICTION source")
        void authenticatedUserWithPredictionShouldGetUserPredictionSource() {
            // Given
            List<TeamRanking> userRankings = create20Rankings();
            SeasonPrediction userPrediction = createMockPrediction(userRankings);

            when(seasonPredictionRepository.findByUserIdAndSeasonId(userId, seasonId))
                .thenReturn(Optional.of(userPrediction));
            when(roundPredictionRepository.getSwapCooldown(userId))
                .thenReturn(SwapCooldown.initial());

            GetUserPredictionCommand command = GetUserPredictionCommand.forAuthenticatedUser(
                userId, seasonId, true, true, null
            );

            // When
            Either<UseCaseError, GetUserPredictionUseCase.UserPredictionViewData> result =
                useCase.execute(command);

            // Then
            assertTrue(result.isRight());
            GetUserPredictionUseCase.UserPredictionViewData viewData = result.get();

            assertEquals(RankingSource.USER_PREDICTION, viewData.source());
            assertEquals(20, viewData.rankings().size());
        }

        @Test
        @DisplayName("Authenticated user with prediction and no cooldown should get EDITABLE access")
        void authenticatedUserWithPredictionAndNoCooldownShouldGetEditableAccess() {
            // Given
            List<TeamRanking> userRankings = create20Rankings();
            SeasonPrediction userPrediction = createMockPrediction(userRankings);

            when(seasonPredictionRepository.findByUserIdAndSeasonId(userId, seasonId))
                .thenReturn(Optional.of(userPrediction));
            // Initial swap cooldown - can swap
            when(roundPredictionRepository.getSwapCooldown(userId))
                .thenReturn(new SwapCooldown(null, true, 0, true));

            GetUserPredictionCommand command = GetUserPredictionCommand.forAuthenticatedUser(
                userId, seasonId, true, true, null
            );

            // When
            Either<UseCaseError, GetUserPredictionUseCase.UserPredictionViewData> result =
                useCase.execute(command);

            // Then
            assertTrue(result.isRight());
            GetUserPredictionUseCase.UserPredictionViewData viewData = result.get();

            assertEquals(PredictionAccessMode.EDITABLE, viewData.accessMode());
            assertTrue(viewData.canSwap());
            assertFalse(viewData.isReadonly());
        }

        @Test
        @DisplayName("Authenticated user with prediction on cooldown should get READONLY_COOLDOWN access")
        void authenticatedUserWithPredictionOnCooldownShouldGetReadonlyCooldownAccess() {
            // Given
            List<TeamRanking> userRankings = create20Rankings();
            SeasonPrediction userPrediction = createMockPrediction(userRankings);

            when(seasonPredictionRepository.findByUserIdAndSeasonId(userId, seasonId))
                .thenReturn(Optional.of(userPrediction));
            // On cooldown - cannot swap (swapped recently with swap count > 0)
            when(roundPredictionRepository.getSwapCooldown(userId))
                .thenReturn(new SwapCooldown(Instant.now().minusSeconds(60), true, 1, true));

            GetUserPredictionCommand command = GetUserPredictionCommand.forAuthenticatedUser(
                userId, seasonId, true, true, null
            );

            // When
            Either<UseCaseError, GetUserPredictionUseCase.UserPredictionViewData> result =
                useCase.execute(command);

            // Then
            assertTrue(result.isRight());
            GetUserPredictionUseCase.UserPredictionViewData viewData = result.get();

            assertEquals(PredictionAccessMode.READONLY_COOLDOWN, viewData.accessMode());
            assertFalse(viewData.canSwap());
            assertTrue(viewData.isReadonly());
        }

        @Test
        @DisplayName("Authenticated user without prediction should get CAN_CREATE_ENTRY access")
        void authenticatedUserWithoutPredictionShouldGetCanCreateEntryAccess() {
            // Given
            when(seasonPredictionRepository.findByUserIdAndSeasonId(userId, seasonId))
                .thenReturn(Optional.empty());
            when(roundStandingsRepository.findLatestBySeasonId(seasonId))
                .thenReturn(Optional.of(create20Rankings()));

            GetUserPredictionCommand command = GetUserPredictionCommand.forAuthenticatedUser(
                userId, seasonId, false, false, null
            );

            // When
            Either<UseCaseError, GetUserPredictionUseCase.UserPredictionViewData> result =
                useCase.execute(command);

            // Then
            assertTrue(result.isRight());
            GetUserPredictionUseCase.UserPredictionViewData viewData = result.get();

            assertEquals(PredictionAccessMode.CAN_CREATE_ENTRY, viewData.accessMode());
            assertTrue(viewData.canCreateEntry());
            assertFalse(viewData.canSwap());
            assertFalse(viewData.isReadonly());
        }
    }

    @Nested
    @DisplayName("Viewing Other User Tests")
    class ViewingOtherUserTests {

        @Test
        @DisplayName("Viewing other user with prediction should get READONLY_VIEWING_OTHER access")
        void viewingOtherUserWithPredictionShouldGetReadonlyViewingOtherAccess() {
            // Given
            UserId targetUserId = UserId.generate();
            List<TeamRanking> userRankings = create20Rankings();
            SeasonPrediction userPrediction = createMockPrediction(userRankings);

            when(seasonPredictionRepository.findByUserIdAndSeasonId(targetUserId, seasonId))
                .thenReturn(Optional.of(userPrediction));

            GetUserPredictionCommand command = GetUserPredictionCommand.forViewingOtherUser(
                targetUserId, seasonId, true, "John Doe", null
            );

            // When
            Either<UseCaseError, GetUserPredictionUseCase.UserPredictionViewData> result =
                useCase.execute(command);

            // Then
            assertTrue(result.isRight());
            GetUserPredictionUseCase.UserPredictionViewData viewData = result.get();

            assertEquals(PredictionAccessMode.READONLY_VIEWING_OTHER, viewData.accessMode());
            assertTrue(viewData.isViewingOther());
            assertTrue(viewData.isReadonly());
            assertFalse(viewData.canSwap());
            assertEquals("John Doe", viewData.targetDisplayName());
        }

        @Test
        @DisplayName("Viewing other user without prediction should get fallback with READONLY_VIEWING_OTHER")
        void viewingOtherUserWithoutPredictionShouldGetFallbackWithReadonlyAccess() {
            // Given
            UserId targetUserId = UserId.generate();

            when(seasonPredictionRepository.findByUserIdAndSeasonId(targetUserId, seasonId))
                .thenReturn(Optional.empty());
            when(roundStandingsRepository.findLatestBySeasonId(seasonId))
                .thenReturn(Optional.of(create20Rankings()));

            GetUserPredictionCommand command = GetUserPredictionCommand.forViewingOtherUser(
                targetUserId, seasonId, false, "Jane Doe", null
            );

            // When
            Either<UseCaseError, GetUserPredictionUseCase.UserPredictionViewData> result =
                useCase.execute(command);

            // Then
            assertTrue(result.isRight());
            GetUserPredictionUseCase.UserPredictionViewData viewData = result.get();

            assertEquals(PredictionAccessMode.READONLY_VIEWING_OTHER, viewData.accessMode());
            assertEquals(RankingSource.ROUND_STANDINGS, viewData.source());
            assertTrue(viewData.isViewingOther());
        }
    }

    @Nested
    @DisplayName("User Not Found Tests")
    class UserNotFoundTests {

        @Test
        @DisplayName("Non-existent user should get READONLY_USER_NOT_FOUND access")
        void nonExistentUserShouldGetReadonlyUserNotFoundAccess() {
            // Given
            when(roundStandingsRepository.findLatestBySeasonId(seasonId))
                .thenReturn(Optional.of(create20Rankings()));

            GetUserPredictionCommand command = GetUserPredictionCommand.forNonExistentUser(seasonId, null);

            // When
            Either<UseCaseError, GetUserPredictionUseCase.UserPredictionViewData> result =
                useCase.execute(command);

            // Then
            assertTrue(result.isRight());
            GetUserPredictionUseCase.UserPredictionViewData viewData = result.get();

            assertEquals(PredictionAccessMode.READONLY_USER_NOT_FOUND, viewData.accessMode());
            assertTrue(viewData.isUserNotFound());
            assertTrue(viewData.isReadonly());
            assertNotNull(viewData.message());
        }
    }

    @Nested
    @DisplayName("Round Navigation Tests")
    class RoundNavigationTests {

        @Test
        @DisplayName("Viewing current round should return isCurrentRound = true")
        void viewingCurrentRoundShouldReturnIsCurrentRoundTrue() {
            // Given
            when(roundPredictionRepository.getCurrentRound()).thenReturn(19);

            GetUserPredictionCommand command = GetUserPredictionCommand.forGuest(seasonId, 19);

            // When
            Either<UseCaseError, GetUserPredictionUseCase.UserPredictionViewData> result =
                useCase.execute(command);

            // Then
            assertTrue(result.isRight());
            GetUserPredictionUseCase.UserPredictionViewData viewData = result.get();

            assertEquals(19, viewData.currentRound());
            assertEquals(19, viewData.viewingRound());
            assertTrue(viewData.isCurrentRound());
        }

        @Test
        @DisplayName("Viewing historical round should return isCurrentRound = false")
        void viewingHistoricalRoundShouldReturnIsCurrentRoundFalse() {
            // Given
            when(roundPredictionRepository.getCurrentRound()).thenReturn(19);

            GetUserPredictionCommand command = GetUserPredictionCommand.forGuest(seasonId, 15);

            // When
            Either<UseCaseError, GetUserPredictionUseCase.UserPredictionViewData> result =
                useCase.execute(command);

            // Then
            assertTrue(result.isRight());
            GetUserPredictionUseCase.UserPredictionViewData viewData = result.get();

            assertEquals(19, viewData.currentRound());
            assertEquals(15, viewData.viewingRound());
            assertFalse(viewData.isCurrentRound());
            assertEquals("COMPLETED", viewData.roundState());
        }
    }

    @Nested
    @DisplayName("Error Handling Tests")
    class ErrorHandlingTests {

        @Test
        @DisplayName("Should return error when baseline not found")
        void shouldReturnErrorWhenBaselineNotFound() {
            // Given
            when(roundStandingsRepository.findLatestBySeasonId(seasonId))
                .thenReturn(Optional.empty());
            when(seasonTeamRankingsRepository.findBySeasonId(seasonId))
                .thenReturn(Optional.empty());

            GetUserPredictionCommand command = GetUserPredictionCommand.forGuest(seasonId, null);

            // When
            Either<UseCaseError, GetUserPredictionUseCase.UserPredictionViewData> result =
                useCase.execute(command);

            // Then
            assertTrue(result.isLeft(), "Should fail when no fallback available");
        }

        @Test
        @DisplayName("Should return error when repository throws exception")
        void shouldReturnErrorWhenRepositoryThrowsException() {
            // Given
            when(roundStandingsRepository.findLatestBySeasonId(any()))
                .thenThrow(new RuntimeException("Database error"));

            GetUserPredictionCommand command = GetUserPredictionCommand.forGuest(seasonId, null);

            // When
            Either<UseCaseError, GetUserPredictionUseCase.UserPredictionViewData> result =
                useCase.execute(command);

            // Then
            assertTrue(result.isLeft(), "Should fail on repository exception");
        }
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
