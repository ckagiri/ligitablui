package com.ligitabl.domain.repository;

import com.ligitabl.domain.model.roundresult.RoundResult;
import com.ligitabl.domain.model.season.RoundNumber;
import com.ligitabl.domain.model.user.UserId;

import java.util.Optional;

/**
 * Repository interface for round result data access.
 *
 * <p>This is a port in the hexagonal architecture - implementations
 * live in the infrastructure layer.</p>
 */
public interface RoundResultRepository {

    /**
     * Find a round result for a user in a specific round.
     *
     * @param userId the user's ID
     * @param roundNumber the round number
     * @return the round result if it exists
     */
    Optional<RoundResult> findByUserAndRound(UserId userId, RoundNumber roundNumber);

    /**
     * Check if a user has a round result for a specific round.
     *
     * @param userId the user's ID
     * @param roundNumber the round number
     * @return true if result exists
     */
    boolean existsByUserAndRound(UserId userId, RoundNumber roundNumber);

    /**
     * Save a round result.
     *
     * @param result the round result to save
     */
    void save(RoundResult result);

    /**
     * Get the earliest round number that has a result for a user.
     *
     * @param userId the user's ID
     * @return the earliest round number, or empty if no results exist
     */
    Optional<RoundNumber> findEarliestRoundForUser(UserId userId);

    /**
     * Mark a round result as viewed by the user.
     *
     * @param userId the user's ID
     * @param roundNumber the round number
     */
    void markAsViewed(UserId userId, RoundNumber roundNumber);
}
