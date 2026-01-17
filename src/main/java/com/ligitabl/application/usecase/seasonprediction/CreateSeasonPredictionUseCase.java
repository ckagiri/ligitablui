package com.ligitabl.application.usecase.seasonprediction;

import com.ligitabl.api.shared.Either;
import com.ligitabl.application.command.CreateSeasonPredictionCommand;
import com.ligitabl.application.error.ErrorMapper;
import com.ligitabl.application.error.UseCaseError;
import com.ligitabl.domain.model.contest.ContestEntryId;
import com.ligitabl.domain.model.contest.ContestId;
import com.ligitabl.domain.model.contest.MainContestEntry;
import com.ligitabl.domain.model.season.RoundNumber;
import com.ligitabl.domain.model.seasonprediction.SeasonPrediction;
import com.ligitabl.domain.model.seasonprediction.SeasonPredictionId;
import com.ligitabl.domain.repository.MainContestEntryRepository;
import com.ligitabl.domain.repository.SeasonPredictionRepository;

import java.time.Instant;
import java.util.Objects;

/**
 * Use case for creating initial season prediction.
 *
 * <p>This use case handles the user's first submission when joining the competition.
 * It performs two critical operations in sequence:
 * 1. Creates the SeasonPrediction aggregate
 * 2. Auto-creates a MainContestEntry for the user</p>
 *
 * <p>Business Rules Enforced:
 * - User must not already have a prediction (409 Conflict)
 * - Must provide exactly 20 teams with valid positions
 * - Auto-creates contest entry as side-effect</p>
 *
 * <p>This is the Railway-Oriented Programming boundary - domain operations
 * are wrapped in Either monad to provide type-safe error handling.</p>
 */
public class CreateSeasonPredictionUseCase {

    private final SeasonPredictionRepository seasonPredictionRepository;
    private final MainContestEntryRepository mainContestEntryRepository;
    private final RoundNumberProvider roundNumberProvider;
    private final ContestIdProvider contestIdProvider;

    public CreateSeasonPredictionUseCase(
        SeasonPredictionRepository seasonPredictionRepository,
        MainContestEntryRepository mainContestEntryRepository,
        RoundNumberProvider roundNumberProvider,
        ContestIdProvider contestIdProvider
    ) {
        this.seasonPredictionRepository = Objects.requireNonNull(
            seasonPredictionRepository,
            "seasonPredictionRepository is required"
        );
        this.mainContestEntryRepository = Objects.requireNonNull(
            mainContestEntryRepository,
            "mainContestEntryRepository is required"
        );
        this.roundNumberProvider = Objects.requireNonNull(
            roundNumberProvider,
            "roundNumberProvider is required"
        );
        this.contestIdProvider = Objects.requireNonNull(
            contestIdProvider,
            "contestIdProvider is required"
        );
    }

    /**
     * Execute the use case.
     *
     * @param command the create season prediction command
     * @return Either containing UseCaseError (left) or CreatedResult (right)
     */
    public Either<UseCaseError, CreatedResult> execute(CreateSeasonPredictionCommand command) {
        return checkNotExists(command)
            .flatMap(valid -> createPrediction(command))
            .flatMap(this::autoCreateContestEntry);
    }

    /**
     * Check that user doesn't already have a prediction for this season.
     *
     * @param command the command
     * @return Either with void (right) if validation passes, ConflictError (left) if exists
     */
    private Either<UseCaseError, Void> checkNotExists(CreateSeasonPredictionCommand command) {
        boolean exists = seasonPredictionRepository.existsByUserIdAndSeasonId(
            command.userId(),
            command.seasonId()
        );

        if (exists) {
            return Either.left(new UseCaseError.ConflictError(
                "Season prediction already exists",
                "User " + command.userId().value() + " already has a prediction for season " +
                command.seasonId().value()
            ));
        }

        return Either.right(null);
    }

    /**
     * Create the season prediction aggregate.
     *
     * @param command the command with prediction data
     * @return Either containing UseCaseError (left) or created SeasonPrediction (right)
     */
    private Either<UseCaseError, SeasonPrediction> createPrediction(
        CreateSeasonPredictionCommand command
    ) {
        return Either.catching(
            () -> {
                RoundNumber currentRound = roundNumberProvider.getCurrentRound(command.seasonId());
                Instant now = Instant.now();

                SeasonPrediction prediction = SeasonPrediction.create(
                    SeasonPredictionId.generate(),
                    command.userId(),
                    command.seasonId(),
                    currentRound,
                    command.rankings(),
                    now
                );

                return seasonPredictionRepository.save(prediction);
            },
            ErrorMapper::toUseCaseError
        );
    }

    /**
     * Auto-create main contest entry as side-effect.
     *
     * @param prediction the created season prediction
     * @return Either containing UseCaseError (left) or CreatedResult (right)
     */
    private Either<UseCaseError, CreatedResult> autoCreateContestEntry(SeasonPrediction prediction) {
        return Either.catching(
            () -> {
                ContestId mainContestId = contestIdProvider.getMainContestId(prediction.getSeasonId());
                Instant now = Instant.now();

                MainContestEntry entry = MainContestEntry.create(
                    ContestEntryId.generate(),
                    prediction.getUserId(),
                    mainContestId,
                    prediction.getId(),
                    now
                );

                mainContestEntryRepository.save(entry);

                return new CreatedResult(prediction, entry);
            },
            ErrorMapper::toUseCaseError
        );
    }

    /**
     * Result object containing both created aggregates.
     */
    public record CreatedResult(
        SeasonPrediction prediction,
        MainContestEntry contestEntry
    ) {
        public CreatedResult {
            Objects.requireNonNull(prediction, "prediction is required");
            Objects.requireNonNull(contestEntry, "contestEntry is required");
        }
    }

    /**
     * Provider interface for getting current round number.
     * Allows dependency injection and testing.
     */
    public interface RoundNumberProvider {
        RoundNumber getCurrentRound(com.ligitabl.domain.model.season.SeasonId seasonId);
    }

    /**
     * Provider interface for getting main contest ID.
     * Allows dependency injection and testing.
     */
    public interface ContestIdProvider {
        ContestId getMainContestId(com.ligitabl.domain.model.season.SeasonId seasonId);
    }
}
