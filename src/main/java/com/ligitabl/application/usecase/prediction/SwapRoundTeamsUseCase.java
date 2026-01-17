package com.ligitabl.application.usecase.prediction;

import com.ligitabl.api.shared.Either;
import com.ligitabl.application.command.SwapRoundTeamsCommand;
import com.ligitabl.application.error.ErrorMapper;
import com.ligitabl.application.error.UseCaseError;
import com.ligitabl.domain.model.prediction.PredictionRow;
import com.ligitabl.domain.model.prediction.SwapCooldown;
import com.ligitabl.domain.repository.RoundPredictionRepository;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

/**
 * Use case for swapping two teams in a prediction.
 */
@Service
public class SwapRoundTeamsUseCase {

    private final RoundPredictionRepository predictionRepository;

    public SwapRoundTeamsUseCase(RoundPredictionRepository predictionRepository) {
        this.predictionRepository = Objects.requireNonNull(predictionRepository);
    }

    /**
     * Execute the swap teams use case.
     */
    public Either<UseCaseError, SwapResult> execute(SwapRoundTeamsCommand command) {
        return Either.catching(
            () -> performSwap(command),
            ErrorMapper::toUseCaseError
        );
    }

    private SwapResult performSwap(SwapRoundTeamsCommand command) {
        Instant now = Instant.now();

        // Check cooldown
        SwapCooldown cooldown = predictionRepository.getSwapCooldown(command.userId());
        if (!cooldown.canSwap(now)) {
            throw new IllegalStateException(cooldown.getStatusMessage(now));
        }

        // Perform swap
        predictionRepository.swapTeams(command.userId(), command.teamA(), command.teamB());

        // Get updated prediction
        List<PredictionRow> updatedPredictions = predictionRepository.findCurrentByUser(command.userId());

        return new SwapResult(
            updatedPredictions,
            command.teamA(),
            command.teamB(),
            true
        );
    }

    /**
     * Result of a swap operation.
     */
    public record SwapResult(
        List<PredictionRow> predictions,
        String swappedTeamA,
        String swappedTeamB,
        boolean success
    ) {
        public SwapResult {
            Objects.requireNonNull(predictions, "predictions are required");
            predictions = List.copyOf(predictions);
        }
    }
}
