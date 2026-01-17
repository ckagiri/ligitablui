package com.ligitabl.application.usecase.prediction;

import com.ligitabl.api.shared.Either;
import com.ligitabl.application.command.GetMyPredictionCommand;
import com.ligitabl.application.error.ErrorMapper;
import com.ligitabl.application.error.UseCaseError;
import com.ligitabl.domain.model.prediction.Fixture;
import com.ligitabl.domain.model.prediction.PredictionRow;
import com.ligitabl.domain.model.prediction.PredictionStatus;
import com.ligitabl.domain.model.prediction.SwapCooldown;
import com.ligitabl.domain.model.season.RoundNumber;
import com.ligitabl.domain.repository.FixtureRepository;
import com.ligitabl.domain.repository.RoundPredictionRepository;
import com.ligitabl.domain.repository.StandingRepository;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Use case for retrieving user's prediction with all related data.
 *
 * <p>This is the most complex use case - it aggregates data from multiple sources:
 * predictions, swap status, fixtures, standings, and points.</p>
 */
@Service
public class GetMyPredictionUseCase {

    private final RoundPredictionRepository predictionRepository;
    private final FixtureRepository fixtureRepository;
    private final StandingRepository standingRepository;

    public GetMyPredictionUseCase(
        RoundPredictionRepository predictionRepository,
        FixtureRepository fixtureRepository,
        StandingRepository standingRepository
    ) {
        this.predictionRepository = Objects.requireNonNull(predictionRepository);
        this.fixtureRepository = Objects.requireNonNull(fixtureRepository);
        this.standingRepository = Objects.requireNonNull(standingRepository);
    }

    /**
     * Execute the use case to retrieve prediction data.
     */
    public Either<UseCaseError, PredictionViewData> execute(GetMyPredictionCommand command) {
        return Either.catching(
            () -> buildPredictionViewData(command),
            ErrorMapper::toUseCaseError
        );
    }

    private PredictionViewData buildPredictionViewData(GetMyPredictionCommand command) {
        int currentRound = predictionRepository.getCurrentRound();
        boolean isCurrentRound = command.round().value() == currentRound;
        PredictionStatus roundState = command.roundState();

        boolean isOpenOrLocked = roundState == PredictionStatus.OPEN ||
                                 roundState == PredictionStatus.LOCKED;

        if (isCurrentRound && isOpenOrLocked) {
            return buildCurrentRoundView(command);
        } else {
            return buildHistoricalView(command);
        }
    }

    private PredictionViewData buildCurrentRoundView(GetMyPredictionCommand command) {
        Instant now = Instant.now();

        // Get prediction
        List<PredictionRow> predictions = predictionRepository.findCurrentByUser(command.userId());

        // Get swap status
        SwapCooldown swapCooldown = predictionRepository.getSwapCooldown(command.userId());
        boolean canSwap = command.roundState() == PredictionStatus.OPEN && swapCooldown.canSwap(now);
        boolean isInitialPrediction = !swapCooldown.initialPredictionMade();

        // Get fixtures
        Map<String, List<Fixture>> fixtures = fixtureRepository.findByRound(command.round());

        // Get standings and points maps
        Map<String, Integer> standingsMap = standingRepository.findCurrentPositionMap();
        Map<String, Integer> pointsMap = standingRepository.findCurrentPointsMap();

        return new PredictionViewData(
            predictions,
            swapCooldown,
            fixtures,
            standingsMap,
            pointsMap,
            canSwap,
            isInitialPrediction,
            null, // roundScore
            null, // totalHits
            predictionRepository.getCurrentRound(),
            command.round().value(),
            command.roundState().name()
        );
    }

    private PredictionViewData buildHistoricalView(GetMyPredictionCommand command) {
        // Get scored predictions for historical round
        List<PredictionRow> predictions = predictionRepository.findByUserAndRound(
            command.userId(),
            command.round()
        );

        // Calculate score
        int totalHits = predictions.stream()
            .filter(p -> p.getHit() != null)
            .mapToInt(PredictionRow::getHit)
            .sum();
        int roundScore = 200 - totalHits;

        return new PredictionViewData(
            predictions,
            null, // no swap cooldown for historical
            Map.of(), // no fixtures
            Map.of(), // no standings map
            Map.of(), // no points map
            false, // cannot swap historical
            false, // not initial prediction
            roundScore,
            totalHits,
            predictionRepository.getCurrentRound(),
            command.round().value(),
            "COMPLETED"
        );
    }

    /**
     * Result object containing all prediction view data.
     */
    public record PredictionViewData(
        List<PredictionRow> predictions,
        SwapCooldown swapCooldown,
        Map<String, List<Fixture>> fixtures,
        Map<String, Integer> standingsMap,
        Map<String, Integer> pointsMap,
        boolean canSwap,
        boolean isInitialPrediction,
        Integer roundScore,
        Integer totalHits,
        int currentRound,
        int viewingRound,
        String roundState
    ) {
        public PredictionViewData {
            Objects.requireNonNull(predictions, "predictions are required");
            predictions = List.copyOf(predictions);
        }

        /**
         * Check if viewing the current round.
         */
        public boolean isCurrentRound() {
            return viewingRound == currentRound;
        }
    }
}
