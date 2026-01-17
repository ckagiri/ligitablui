package com.ligitabl.application.usecase.prediction;

import com.ligitabl.api.shared.Either;
import com.ligitabl.application.error.ErrorMapper;
import com.ligitabl.application.error.UseCaseError;
import com.ligitabl.domain.repository.RoundPredictionRepository;
import org.springframework.stereotype.Service;

import java.util.Objects;

/**
 * Use case for resetting the demo state.
 */
@Service
public class ResetDemoUseCase {

    private final RoundPredictionRepository predictionRepository;

    public ResetDemoUseCase(RoundPredictionRepository predictionRepository) {
        this.predictionRepository = Objects.requireNonNull(predictionRepository);
    }

    /**
     * Execute the demo reset.
     */
    public Either<UseCaseError, ResetResult> execute() {
        return Either.catching(
            () -> {
                predictionRepository.resetDemoState();
                return new ResetResult(true, "Demo reset! Start from initial prediction again.");
            },
            ErrorMapper::toUseCaseError
        );
    }

    /**
     * Result of demo reset.
     */
    public record ResetResult(
        boolean success,
        String message
    ) {}
}
