package com.ligitabl.domain.repository;

import com.ligitabl.domain.model.prediction.PredictionRow;
import com.ligitabl.domain.model.prediction.SwapCooldown;
import com.ligitabl.domain.model.season.RoundNumber;
import com.ligitabl.domain.model.user.UserId;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Repository interface for round prediction data access.
 *
 * <p>This is a port in the hexagonal architecture - implementations
 * live in the infrastructure layer.</p>
 */
public interface RoundPredictionRepository {

    /**
     * Find prediction for a user in the current round.
     *
     * @param userId the user's ID
     * @return list of prediction rows, or empty list if no prediction
     */
    List<PredictionRow> findCurrentByUser(UserId userId);

    /**
     * Find prediction for a user in a specific round.
     *
     * @param userId the user's ID
     * @param round the round number
     * @return list of prediction rows, or empty list if no prediction
     */
    List<PredictionRow> findByUserAndRound(UserId userId, RoundNumber round);

    /**
     * Get the swap cooldown state for a user.
     *
     * @param userId the user's ID
     * @return the swap cooldown state
     */
    SwapCooldown getSwapCooldown(UserId userId);

    /**
     * Save updated prediction order.
     *
     * @param userId the user's ID
     * @param predictions the new prediction order
     */
    void savePredictionOrder(UserId userId, List<PredictionRow> predictions);

    /**
     * Perform a swap between two teams.
     *
     * @param userId the user's ID
     * @param teamA first team code
     * @param teamB second team code
     */
    void swapTeams(UserId userId, String teamA, String teamB);

    /**
     * Update the swap cooldown after a swap.
     *
     * @param userId the user's ID
     * @param swapTime the time of the swap
     */
    void recordSwap(UserId userId, Instant swapTime);

    /**
     * Check if a user has made their initial prediction.
     *
     * @param userId the user's ID
     * @return true if initial prediction has been made
     */
    boolean hasInitialPrediction(UserId userId);

    /**
     * Get the current round number.
     *
     * @return the current round number
     */
    int getCurrentRound();

    /**
     * Check if a round is open for predictions.
     *
     * @param round the round number
     * @return true if round is open
     */
    boolean isRoundOpen(int round);

    /**
     * Reset the demo state (for testing/demo purposes).
     */
    void resetDemoState();

    /**
     * Get the actual standings for a round (for scoring).
     *
     * @param round the round number
     * @return map of team code to actual position
     */
    Map<String, Integer> getActualStandings(int round);
}
