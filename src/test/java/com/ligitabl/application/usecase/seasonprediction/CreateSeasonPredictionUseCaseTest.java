package com.ligitabl.application.usecase.seasonprediction;

import com.ligitabl.api.shared.Either;
import com.ligitabl.application.command.CreateSeasonPredictionCommand;
import com.ligitabl.application.error.UseCaseError;
import com.ligitabl.domain.model.contest.ContestId;
import com.ligitabl.domain.model.contest.MainContestEntry;
import com.ligitabl.domain.model.season.RoundNumber;
import com.ligitabl.domain.model.season.SeasonId;
import com.ligitabl.domain.model.seasonprediction.SeasonPrediction;
import com.ligitabl.domain.model.seasonprediction.TeamRanking;
import com.ligitabl.domain.model.team.TeamId;
import com.ligitabl.domain.model.user.UserId;
import com.ligitabl.domain.repository.MainContestEntryRepository;
import com.ligitabl.domain.repository.SeasonPredictionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for CreateSeasonPredictionUseCase.
 * Tests the creation of season prediction with auto-creation of main contest entry.
 */
class CreateSeasonPredictionUseCaseTest {

    private SeasonPredictionRepository seasonPredictionRepository;
    private MainContestEntryRepository mainContestEntryRepository;
    private CreateSeasonPredictionUseCase.RoundNumberProvider roundNumberProvider;
    private CreateSeasonPredictionUseCase.ContestIdProvider contestIdProvider;
    private CreateSeasonPredictionUseCase useCase;

    private final UserId userId = UserId.generate();
    private final SeasonId seasonId = SeasonId.generate();
    private final RoundNumber currentRound = RoundNumber.of(5);
    private final ContestId mainContestId = ContestId.generate();

    @BeforeEach
    void setUp() {
        seasonPredictionRepository = mock(SeasonPredictionRepository.class);
        mainContestEntryRepository = mock(MainContestEntryRepository.class);
        roundNumberProvider = mock(CreateSeasonPredictionUseCase.RoundNumberProvider.class);
        contestIdProvider = mock(CreateSeasonPredictionUseCase.ContestIdProvider.class);

        when(roundNumberProvider.getCurrentRound(any())).thenReturn(currentRound);
        when(contestIdProvider.getMainContestId(any())).thenReturn(mainContestId);

        useCase = new CreateSeasonPredictionUseCase(
            seasonPredictionRepository,
            mainContestEntryRepository,
            roundNumberProvider,
            contestIdProvider
        );
    }

    @Test
    void shouldCreateSeasonPredictionAndMainContestEntry() {
        // Given: User doesn't have prediction yet
        List<TeamRanking> rankings = create20Rankings();
        when(seasonPredictionRepository.existsByUserIdAndSeasonId(userId, seasonId))
            .thenReturn(false);
        when(seasonPredictionRepository.save(any(SeasonPrediction.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));
        when(mainContestEntryRepository.save(any(MainContestEntry.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        CreateSeasonPredictionCommand command = CreateSeasonPredictionCommand.of(
            userId, seasonId, rankings
        );

        // When
        Either<UseCaseError, CreateSeasonPredictionUseCase.CreatedResult> result =
            useCase.execute(command);

        // Then
        assertTrue(result.isRight(), "Should succeed");
        CreateSeasonPredictionUseCase.CreatedResult createdResult = result.get();

        assertNotNull(createdResult.prediction());
        assertNotNull(createdResult.contestEntry());
        assertEquals(userId, createdResult.prediction().getUserId());
        assertEquals(seasonId, createdResult.prediction().getSeasonId());
        assertEquals(userId, createdResult.contestEntry().getUserId());

        // Verify both repositories were called
        verify(seasonPredictionRepository).save(any(SeasonPrediction.class));
        verify(mainContestEntryRepository).save(any(MainContestEntry.class));
    }

    @Test
    void shouldReturnConflictErrorWhenPredictionAlreadyExists() {
        // Given: User already has a prediction
        List<TeamRanking> rankings = create20Rankings();
        when(seasonPredictionRepository.existsByUserIdAndSeasonId(userId, seasonId))
            .thenReturn(true);

        CreateSeasonPredictionCommand command = CreateSeasonPredictionCommand.of(
            userId, seasonId, rankings
        );

        // When
        Either<UseCaseError, CreateSeasonPredictionUseCase.CreatedResult> result =
            useCase.execute(command);

        // Then
        assertTrue(result.isLeft(), "Should fail");
        UseCaseError error = result.getLeft();

        assertEquals(UseCaseError.ErrorType.CONFLICT, error.type());
        assertTrue(error.message().contains("already exists"));

        // Verify save was NOT called
        verify(seasonPredictionRepository, never()).save(any());
        verify(mainContestEntryRepository, never()).save(any());
    }

    @Test
    void shouldReturnValidationErrorForInvalidRankings() {
        // Given: Invalid rankings (only 19 teams)
        List<TeamRanking> invalidRankings = create19Rankings();
        when(seasonPredictionRepository.existsByUserIdAndSeasonId(userId, seasonId))
            .thenReturn(false);

        CreateSeasonPredictionCommand command = CreateSeasonPredictionCommand.of(
            userId, seasonId, invalidRankings
        );

        // When
        Either<UseCaseError, CreateSeasonPredictionUseCase.CreatedResult> result =
            useCase.execute(command);

        // Then
        assertTrue(result.isLeft(), "Should fail");
        UseCaseError error = result.getLeft();

        assertEquals(UseCaseError.ErrorType.VALIDATION, error.type());
        assertTrue(error.message().contains("Invalid season prediction"));
    }

    @Test
    void shouldReturnValidationErrorForDuplicatePositions() {
        // Given: Rankings with duplicate positions
        List<TeamRanking> rankings = create20Rankings();
        rankings.set(5, TeamRanking.create(TeamId.generate(), 1)); // Duplicate position 1

        when(seasonPredictionRepository.existsByUserIdAndSeasonId(userId, seasonId))
            .thenReturn(false);

        CreateSeasonPredictionCommand command = CreateSeasonPredictionCommand.of(
            userId, seasonId, rankings
        );

        // When
        Either<UseCaseError, CreateSeasonPredictionUseCase.CreatedResult> result =
            useCase.execute(command);

        // Then
        assertTrue(result.isLeft(), "Should fail");
        UseCaseError error = result.getLeft();

        assertEquals(UseCaseError.ErrorType.VALIDATION, error.type());
    }

    @Test
    void shouldReturnValidationErrorForDuplicateTeams() {
        // Given: Rankings with duplicate teams
        TeamId duplicateTeam = TeamId.generate();
        List<TeamRanking> rankings = create20Rankings();
        rankings.set(0, TeamRanking.create(duplicateTeam, 1));
        rankings.set(1, TeamRanking.create(duplicateTeam, 2)); // Duplicate team

        when(seasonPredictionRepository.existsByUserIdAndSeasonId(userId, seasonId))
            .thenReturn(false);

        CreateSeasonPredictionCommand command = CreateSeasonPredictionCommand.of(
            userId, seasonId, rankings
        );

        // When
        Either<UseCaseError, CreateSeasonPredictionUseCase.CreatedResult> result =
            useCase.execute(command);

        // Then
        assertTrue(result.isLeft(), "Should fail");
        UseCaseError error = result.getLeft();

        assertEquals(UseCaseError.ErrorType.VALIDATION, error.type());
    }

    @Test
    void shouldUseCurrentRoundFromProvider() {
        // Given
        List<TeamRanking> rankings = create20Rankings();
        when(seasonPredictionRepository.existsByUserIdAndSeasonId(userId, seasonId))
            .thenReturn(false);
        when(seasonPredictionRepository.save(any(SeasonPrediction.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));
        when(mainContestEntryRepository.save(any(MainContestEntry.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        CreateSeasonPredictionCommand command = CreateSeasonPredictionCommand.of(
            userId, seasonId, rankings
        );

        // When
        Either<UseCaseError, CreateSeasonPredictionUseCase.CreatedResult> result =
            useCase.execute(command);

        // Then
        assertTrue(result.isRight());
        assertEquals(currentRound, result.get().prediction().getAtRound());

        verify(roundNumberProvider).getCurrentRound(seasonId);
    }

    @Test
    void shouldUseMainContestIdFromProvider() {
        // Given
        List<TeamRanking> rankings = create20Rankings();
        when(seasonPredictionRepository.existsByUserIdAndSeasonId(userId, seasonId))
            .thenReturn(false);
        when(seasonPredictionRepository.save(any(SeasonPrediction.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));
        when(mainContestEntryRepository.save(any(MainContestEntry.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        CreateSeasonPredictionCommand command = CreateSeasonPredictionCommand.of(
            userId, seasonId, rankings
        );

        // When
        Either<UseCaseError, CreateSeasonPredictionUseCase.CreatedResult> result =
            useCase.execute(command);

        // Then
        assertTrue(result.isRight());
        assertEquals(mainContestId, result.get().contestEntry().getContestId());

        verify(contestIdProvider).getMainContestId(seasonId);
    }

    @Test
    void shouldLinkContestEntryToCreatedPrediction() {
        // Given
        List<TeamRanking> rankings = create20Rankings();
        when(seasonPredictionRepository.existsByUserIdAndSeasonId(userId, seasonId))
            .thenReturn(false);
        when(seasonPredictionRepository.save(any(SeasonPrediction.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));
        when(mainContestEntryRepository.save(any(MainContestEntry.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        CreateSeasonPredictionCommand command = CreateSeasonPredictionCommand.of(
            userId, seasonId, rankings
        );

        // When
        Either<UseCaseError, CreateSeasonPredictionUseCase.CreatedResult> result =
            useCase.execute(command);

        // Then
        assertTrue(result.isRight());
        CreateSeasonPredictionUseCase.CreatedResult createdResult = result.get();

        // Verify contest entry references the prediction
        assertEquals(
            createdResult.prediction().getId(),
            createdResult.contestEntry().getSeasonPredictionId()
        );
    }

    @Test
    void shouldThrowNullPointerExceptionForNullRepositories() {
        // When/Then
        assertThrows(NullPointerException.class, () ->
            new CreateSeasonPredictionUseCase(
                null, mainContestEntryRepository, roundNumberProvider, contestIdProvider
            )
        );
        assertThrows(NullPointerException.class, () ->
            new CreateSeasonPredictionUseCase(
                seasonPredictionRepository, null, roundNumberProvider, contestIdProvider
            )
        );
        assertThrows(NullPointerException.class, () ->
            new CreateSeasonPredictionUseCase(
                seasonPredictionRepository, mainContestEntryRepository, null, contestIdProvider
            )
        );
        assertThrows(NullPointerException.class, () ->
            new CreateSeasonPredictionUseCase(
                seasonPredictionRepository, mainContestEntryRepository, roundNumberProvider, null
            )
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

    private List<TeamRanking> create19Rankings() {
        List<TeamRanking> rankings = new ArrayList<>();
        for (int i = 1; i <= 19; i++) {
            rankings.add(TeamRanking.create(TeamId.generate(), i));
        }
        return rankings;
    }
}
