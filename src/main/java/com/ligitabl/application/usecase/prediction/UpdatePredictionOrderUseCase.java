package com.ligitabl.application.usecase.prediction;

import com.ligitabl.api.shared.Either;
import com.ligitabl.application.command.UpdatePredictionOrderCommand;
import com.ligitabl.application.error.ErrorMapper;
import com.ligitabl.application.error.UseCaseError;
import com.ligitabl.domain.model.prediction.PredictionRow;
import com.ligitabl.domain.model.prediction.SwapCooldown;
import com.ligitabl.domain.repository.RoundPredictionRepository;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Use case for updating the full prediction order (multiple swaps).
 */
@Service
public class UpdatePredictionOrderUseCase {

    private final RoundPredictionRepository predictionRepository;

    public UpdatePredictionOrderUseCase(RoundPredictionRepository predictionRepository) {
        this.predictionRepository = Objects.requireNonNull(predictionRepository);
    }

    /**
     * Execute the update prediction order use case.
     */
    public Either<UseCaseError, UpdateResult> execute(UpdatePredictionOrderCommand command) {
        return Either.catching(
            () -> performUpdate(command),
            ErrorMapper::toUseCaseError
        );
    }

    private UpdateResult performUpdate(UpdatePredictionOrderCommand command) {
        Instant now = Instant.now();

        // Check cooldown
        SwapCooldown cooldown = predictionRepository.getSwapCooldown(command.userId());
        if (!cooldown.canSwap(now)) {
            throw new IllegalStateException(cooldown.getStatusMessage(now));
        }

        // Get current predictions and create map by team code
        List<PredictionRow> currentPredictions = predictionRepository.findCurrentByUser(command.userId());
        Map<String, PredictionRow> predictionMap = currentPredictions.stream()
            .collect(Collectors.toMap(PredictionRow::getTeamCode, Function.identity()));

        // Build new prediction order
        List<PredictionRow> newPredictions = new ArrayList<>();
        for (int i = 0; i < command.teamCodes().size(); i++) {
            String teamCode = command.teamCodes().get(i);
            PredictionRow original = predictionMap.get(teamCode);
            if (original == null) {
                throw new IllegalArgumentException("Unknown team code: " + teamCode);
            }
            newPredictions.add(original.withPosition(i + 1));
        }

        // Save new order (repository will validate swap count)
        predictionRepository.savePredictionOrder(command.userId(), newPredictions);

        return new UpdateResult(newPredictions, true, "Prediction updated successfully");
    }

    /**
     * Result of an update operation.
     */
    public record UpdateResult(
        List<PredictionRow> predictions,
        boolean success,
        String message
    ) {
        public UpdateResult {
            Objects.requireNonNull(predictions, "predictions are required");
            predictions = List.copyOf(predictions);
        }
    }
}
