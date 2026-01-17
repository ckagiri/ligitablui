package com.ligitabl.application.usecase.seasonprediction;

import com.ligitabl.api.shared.Either;
import com.ligitabl.application.command.SwapTeamsCommand;
import com.ligitabl.application.error.ErrorMapper;
import com.ligitabl.application.error.UseCaseError;
import com.ligitabl.domain.model.season.RoundNumber;
import com.ligitabl.domain.model.seasonprediction.SeasonPrediction;
import com.ligitabl.domain.model.seasonprediction.SwapPair;
import com.ligitabl.domain.repository.SeasonPredictionRepository;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Objects;

/**
 * Use case for swapping exactly two teams in an existing season prediction.
 *
 * <p>CRITICAL Business Rule: Only ONE swap per request.
 * This is enforced by the SwapPair value object which validates that exactly
 * one pair of teams has changed positions.</p>
 *
 * <p>The use case implements optimistic locking - if the team positions have
 * changed since the user's UI was rendered, the swap will be rejected.</p>
 *
 * <p>Business Rules Enforced:
 * - User must already have a prediction (404 if not found)
 * - Exactly one team pair swap
 * - Current positions must match actual positions
 * - Cannot swap same team with itself</p>
 *
 * <p>This is the Railway-Oriented Programming boundary - domain operations
 * are wrapped in Either monad to provide type-safe error handling.</p>
 */
@Service
public class SwapTeamsUseCase {

    private final SeasonPredictionRepository seasonPredictionRepository;
    private final RoundNumberProvider roundNumberProvider;

    public SwapTeamsUseCase(
        SeasonPredictionRepository seasonPredictionRepository,
        RoundNumberProvider roundNumberProvider
    ) {
        this.seasonPredictionRepository = Objects.requireNonNull(
            seasonPredictionRepository,
            "seasonPredictionRepository is required"
        );
        this.roundNumberProvider = Objects.requireNonNull(
            roundNumberProvider,
            "roundNumberProvider is required"
        );
    }

    /**
     * Execute the use case.
     *
     * @param command the swap teams command
     * @return Either containing UseCaseError (left) or updated SeasonPrediction (right)
     */
    public Either<UseCaseError, SeasonPrediction> execute(SwapTeamsCommand command) {
        return loadPrediction(command)
            .flatMap(prediction -> performSwap(prediction, command));
    }

    /**
     * Load the existing season prediction.
     *
     * @param command the command with user and season info
     * @return Either containing NotFoundError (left) or SeasonPrediction (right)
     */
    private Either<UseCaseError, SeasonPrediction> loadPrediction(SwapTeamsCommand command) {
        return Either.catching(
            () -> seasonPredictionRepository
                .findByUserIdAndSeasonId(command.userId(), command.seasonId())
                .orElseThrow(() -> new IllegalArgumentException(
                    "Season prediction not found for userId=" + command.userId().value() +
                    ", seasonId=" + command.seasonId().value()
                )),
            ex -> {
                if (ex instanceof IllegalArgumentException) {
                    return new UseCaseError.NotFoundError(
                        "Season prediction not found",
                        "SeasonPrediction",
                        "userId=" + command.userId().value() +
                        ", seasonId=" + command.seasonId().value()
                    );
                }
                return ErrorMapper.toUseCaseError(ex);
            }
        );
    }

    /**
     * Perform the swap operation on the prediction.
     *
     * <p>This is where we wrap the domain operation in Either.catching()
     * to convert exceptions into UseCaseErrors.</p>
     *
     * @param prediction the existing prediction
     * @param command the swap command
     * @return Either containing UseCaseError (left) or updated SeasonPrediction (right)
     */
    private Either<UseCaseError, SeasonPrediction> performSwap(
        SeasonPrediction prediction,
        SwapTeamsCommand command
    ) {
        return Either.catching(
            () -> {
                // Create SwapPair value object (validates single swap)
                SwapPair swapPair = SwapPair.create(
                    command.teamAId(),
                    command.teamACurrentPosition(),
                    command.teamBId(),
                    command.teamBCurrentPosition()
                );

                // Get current round
                RoundNumber currentRound = roundNumberProvider.getCurrentRound(command.seasonId());
                Instant now = Instant.now();

                // Perform swap (validates position mismatch for optimistic locking)
                SeasonPrediction updatedPrediction = prediction.swapTeams(
                    swapPair,
                    currentRound,
                    now
                );

                // Save and return
                return seasonPredictionRepository.save(updatedPrediction);
            },
            ErrorMapper::toUseCaseError
        );
    }

    /**
     * Provider interface for getting current round number.
     * Allows dependency injection and testing.
     */
    public interface RoundNumberProvider {
        RoundNumber getCurrentRound(com.ligitabl.domain.model.season.SeasonId seasonId);
    }
}
