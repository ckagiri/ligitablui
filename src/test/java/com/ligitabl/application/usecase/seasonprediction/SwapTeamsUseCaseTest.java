package com.ligitabl.application.usecase.seasonprediction;

import com.ligitabl.api.shared.Either;
import com.ligitabl.application.command.SwapTeamsCommand;
import com.ligitabl.application.error.UseCaseError;
import com.ligitabl.domain.model.season.RoundNumber;
import com.ligitabl.domain.model.season.SeasonId;
import com.ligitabl.domain.model.seasonprediction.SeasonPrediction;
import com.ligitabl.domain.model.seasonprediction.SeasonPredictionId;
import com.ligitabl.domain.model.seasonprediction.TeamRanking;
import com.ligitabl.domain.model.team.TeamId;
import com.ligitabl.domain.model.user.UserId;
import com.ligitabl.domain.repository.SeasonPredictionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for SwapTeamsUseCase.
 * Tests the single-swap operation with optimistic locking.
 */
class SwapTeamsUseCaseTest {

    private SeasonPredictionRepository seasonPredictionRepository;
    private SwapTeamsUseCase.RoundNumberProvider roundNumberProvider;
    private SwapTeamsUseCase useCase;

    private final UserId userId = UserId.generate();
    private final SeasonId seasonId = SeasonId.generate();
    private final RoundNumber currentRound = RoundNumber.of(5);

    @BeforeEach
    void setUp() {
        seasonPredictionRepository = mock(SeasonPredictionRepository.class);
        roundNumberProvider = mock(SwapTeamsUseCase.RoundNumberProvider.class);

        when(roundNumberProvider.getCurrentRound(any())).thenReturn(currentRound);

        useCase = new SwapTeamsUseCase(seasonPredictionRepository, roundNumberProvider);
    }

    @Test
    void shouldSwapTeamsSuccessfully() {
        // Given: Existing prediction with teams at positions 3 and 7
        TeamId teamA = TeamId.generate();
        TeamId teamB = TeamId.generate();

        List<TeamRanking> rankings = create20Rankings();
        rankings.set(2, TeamRanking.create(teamA, 3));  // Team A at position 3
        rankings.set(6, TeamRanking.create(teamB, 7));  // Team B at position 7

        SeasonPrediction existingPrediction = SeasonPrediction.create(
            SeasonPredictionId.generate(),
            userId,
            seasonId,
            RoundNumber.of(1),
            rankings,
            Instant.now()
        );

        when(seasonPredictionRepository.findByUserIdAndSeasonId(userId, seasonId))
            .thenReturn(Optional.of(existingPrediction));
        when(seasonPredictionRepository.save(any(SeasonPrediction.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        SwapTeamsCommand command = SwapTeamsCommand.of(
            userId, seasonId, teamA, 3, teamB, 7
        );

        // When
        Either<UseCaseError, SeasonPrediction> result = useCase.execute(command);

        // Then
        assertTrue(result.isRight(), "Should succeed");
        SeasonPrediction updatedPrediction = result.get();

        // Verify positions swapped
        assertEquals(7, updatedPrediction.getRankingForTeam(teamA).position());
        assertEquals(3, updatedPrediction.getRankingForTeam(teamB).position());
        assertEquals(currentRound, updatedPrediction.getAtRound());

        verify(seasonPredictionRepository).save(any(SeasonPrediction.class));
    }

    @Test
    void shouldReturnNotFoundErrorWhenPredictionDoesNotExist() {
        // Given: User has no prediction
        TeamId teamA = TeamId.generate();
        TeamId teamB = TeamId.generate();

        when(seasonPredictionRepository.findByUserIdAndSeasonId(userId, seasonId))
            .thenReturn(Optional.empty());

        SwapTeamsCommand command = SwapTeamsCommand.of(
            userId, seasonId, teamA, 3, teamB, 7
        );

        // When
        Either<UseCaseError, SeasonPrediction> result = useCase.execute(command);

        // Then
        assertTrue(result.isLeft(), "Should fail");
        UseCaseError error = result.getLeft();

        assertEquals(UseCaseError.ErrorType.NOT_FOUND, error.type());
        assertTrue(error.message().contains("not found"));

        verify(seasonPredictionRepository, never()).save(any());
    }

    @Test
    void shouldReturnBusinessRuleErrorForPositionMismatch() {
        // Given: Team A is at position 3, but command claims it's at 5 (stale data)
        TeamId teamA = TeamId.generate();
        TeamId teamB = TeamId.generate();

        List<TeamRanking> rankings = create20Rankings();
        rankings.set(2, TeamRanking.create(teamA, 3));  // Actually at position 3
        rankings.set(6, TeamRanking.create(teamB, 7));

        SeasonPrediction existingPrediction = SeasonPrediction.create(
            SeasonPredictionId.generate(),
            userId,
            seasonId,
            RoundNumber.of(1),
            rankings,
            Instant.now()
        );

        when(seasonPredictionRepository.findByUserIdAndSeasonId(userId, seasonId))
            .thenReturn(Optional.of(existingPrediction));

        SwapTeamsCommand command = SwapTeamsCommand.of(
            userId, seasonId, teamA, 5, teamB, 7  // Claims team A at 5 (wrong!)
        );

        // When
        Either<UseCaseError, SeasonPrediction> result = useCase.execute(command);

        // Then
        assertTrue(result.isLeft(), "Should fail");
        UseCaseError error = result.getLeft();

        assertEquals(UseCaseError.ErrorType.BUSINESS_RULE, error.type());
        assertEquals("Invalid state for operation", error.message());
        // The actual position mismatch detail is in the details list
        assertFalse(error.details().isEmpty());
        String detail = error.details().get(0);
        assertTrue(detail.contains("position mismatch") ||
                   detail.contains("Position mismatch"),
            "Detail was: " + detail);

        verify(seasonPredictionRepository, never()).save(any());
    }

    @Test
    void shouldThrowValidationErrorForSameTeamInCommand() {
        // Given: Command validation catches same team swap
        TeamId teamA = TeamId.generate();

        // When/Then: Command constructor throws IllegalArgumentException
        assertThrows(IllegalArgumentException.class, () ->
            SwapTeamsCommand.of(userId, seasonId, teamA, 3, teamA, 7)
        );
    }

    @Test
    void shouldReturnBusinessRuleErrorWhenTeamNotFound() {
        // Given: Team B doesn't exist in prediction
        TeamId teamA = TeamId.generate();
        TeamId nonExistentTeam = TeamId.generate();

        List<TeamRanking> rankings = create20Rankings();
        rankings.set(2, TeamRanking.create(teamA, 3));

        SeasonPrediction existingPrediction = SeasonPrediction.create(
            SeasonPredictionId.generate(),
            userId,
            seasonId,
            RoundNumber.of(1),
            rankings,
            Instant.now()
        );

        when(seasonPredictionRepository.findByUserIdAndSeasonId(userId, seasonId))
            .thenReturn(Optional.of(existingPrediction));

        SwapTeamsCommand command = SwapTeamsCommand.of(
            userId, seasonId, teamA, 3, nonExistentTeam, 7
        );

        // When
        Either<UseCaseError, SeasonPrediction> result = useCase.execute(command);

        // Then
        assertTrue(result.isLeft(), "Should fail");
        UseCaseError error = result.getLeft();

        assertEquals(UseCaseError.ErrorType.BUSINESS_RULE, error.type());
        assertEquals("Invalid state for operation", error.message());
        // The actual "not found" detail is in the details list
        assertFalse(error.details().isEmpty());
        String detail = error.details().get(0);
        assertTrue(detail.contains("not found"),
            "Detail was: " + detail);

        verify(seasonPredictionRepository, never()).save(any());
    }

    @Test
    void shouldUseCurrentRoundFromProvider() {
        // Given
        TeamId teamA = TeamId.generate();
        TeamId teamB = TeamId.generate();

        List<TeamRanking> rankings = create20Rankings();
        rankings.set(2, TeamRanking.create(teamA, 3));
        rankings.set(6, TeamRanking.create(teamB, 7));

        SeasonPrediction existingPrediction = SeasonPrediction.create(
            SeasonPredictionId.generate(),
            userId,
            seasonId,
            RoundNumber.of(1),
            rankings,
            Instant.now()
        );

        when(seasonPredictionRepository.findByUserIdAndSeasonId(userId, seasonId))
            .thenReturn(Optional.of(existingPrediction));
        when(seasonPredictionRepository.save(any(SeasonPrediction.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        SwapTeamsCommand command = SwapTeamsCommand.of(
            userId, seasonId, teamA, 3, teamB, 7
        );

        // When
        Either<UseCaseError, SeasonPrediction> result = useCase.execute(command);

        // Then
        assertTrue(result.isRight());
        assertEquals(currentRound, result.get().getAtRound());

        verify(roundNumberProvider).getCurrentRound(seasonId);
    }

    @Test
    void shouldPreserveOriginalPredictionImmutability() {
        // Given
        TeamId teamA = TeamId.generate();
        TeamId teamB = TeamId.generate();

        List<TeamRanking> rankings = create20Rankings();
        rankings.set(2, TeamRanking.create(teamA, 3));
        rankings.set(6, TeamRanking.create(teamB, 7));

        SeasonPrediction originalPrediction = SeasonPrediction.create(
            SeasonPredictionId.generate(),
            userId,
            seasonId,
            RoundNumber.of(1),
            rankings,
            Instant.now()
        );

        when(seasonPredictionRepository.findByUserIdAndSeasonId(userId, seasonId))
            .thenReturn(Optional.of(originalPrediction));
        when(seasonPredictionRepository.save(any(SeasonPrediction.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        SwapTeamsCommand command = SwapTeamsCommand.of(
            userId, seasonId, teamA, 3, teamB, 7
        );

        // When
        Either<UseCaseError, SeasonPrediction> result = useCase.execute(command);

        // Then
        assertTrue(result.isRight());
        SeasonPrediction updatedPrediction = result.get();

        // Verify original unchanged (immutability)
        assertEquals(3, originalPrediction.getRankingForTeam(teamA).position());
        assertEquals(7, originalPrediction.getRankingForTeam(teamB).position());

        // Verify updated has swapped positions
        assertEquals(7, updatedPrediction.getRankingForTeam(teamA).position());
        assertEquals(3, updatedPrediction.getRankingForTeam(teamB).position());

        // Verify different instances
        assertNotSame(originalPrediction, updatedPrediction);
    }

    @Test
    void shouldReturnErrorWhenRepositoryThrowsException() {
        // Given: Repository throws exception
        TeamId teamA = TeamId.generate();
        TeamId teamB = TeamId.generate();

        when(seasonPredictionRepository.findByUserIdAndSeasonId(userId, seasonId))
            .thenThrow(new RuntimeException("Database error"));

        SwapTeamsCommand command = SwapTeamsCommand.of(
            userId, seasonId, teamA, 3, teamB, 7
        );

        // When
        Either<UseCaseError, SeasonPrediction> result = useCase.execute(command);

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
            new SwapTeamsUseCase(null, roundNumberProvider)
        );
        assertThrows(NullPointerException.class, () ->
            new SwapTeamsUseCase(seasonPredictionRepository, null)
        );
    }

    // Helper methods

    private List<TeamRanking> create20Rankings() {
        List<TeamRanking> rankings = new ArrayList<>();
        for (int i = 1; i <= 20; i++) {
            rankings.add(TeamRanking.create(TeamId.generate(), i));
        }
        return rankings;
    }
}
