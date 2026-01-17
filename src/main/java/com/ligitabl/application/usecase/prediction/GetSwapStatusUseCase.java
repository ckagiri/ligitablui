package com.ligitabl.application.usecase.prediction;

import com.ligitabl.api.shared.Either;
import com.ligitabl.application.error.ErrorMapper;
import com.ligitabl.application.error.UseCaseError;
import com.ligitabl.domain.model.prediction.SwapCooldown;
import com.ligitabl.domain.model.user.UserId;
import com.ligitabl.domain.repository.RoundPredictionRepository;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Objects;

/**
 * Use case for getting the swap status for a user.
 */
@Service
public class GetSwapStatusUseCase {

    private final RoundPredictionRepository predictionRepository;

    public GetSwapStatusUseCase(RoundPredictionRepository predictionRepository) {
        this.predictionRepository = Objects.requireNonNull(predictionRepository);
    }

    /**
     * Execute the use case to get swap status.
     */
    public Either<UseCaseError, SwapStatusResult> execute(UserId userId) {
        return Either.catching(
            () -> buildSwapStatus(userId),
            ErrorMapper::toUseCaseError
        );
    }

    private SwapStatusResult buildSwapStatus(UserId userId) {
        Instant now = Instant.now();
        SwapCooldown cooldown = predictionRepository.getSwapCooldown(userId);

        return new SwapStatusResult(
            "OPEN", // Round status - would come from a round service in production
            cooldown.canSwap(now),
            cooldown.getLastSwapAtFormatted(),
            cooldown.getNextSwapAtFormatted(now),
            cooldown.getRemainingCooldown(now).toHours() +
                (cooldown.getRemainingCooldown(now).toMinutesPart() / 60.0),
            cooldown.getStatusMessage(now)
        );
    }

    /**
     * Result containing swap status information.
     */
    public record SwapStatusResult(
        String roundStatus,
        boolean canSwap,
        String lastSwapAt,
        String nextSwapAt,
        double hoursRemaining,
        String message
    ) {}
}
