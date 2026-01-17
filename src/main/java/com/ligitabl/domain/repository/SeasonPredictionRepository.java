package com.ligitabl.domain.repository;

import com.ligitabl.domain.model.season.SeasonId;
import com.ligitabl.domain.model.seasonprediction.SeasonPrediction;
import com.ligitabl.domain.model.seasonprediction.SeasonPredictionId;
import com.ligitabl.domain.model.user.UserId;

import java.util.Optional;

/**
 * Repository interface for SeasonPrediction aggregate.
 *
 * <p>This is a domain interface (port) that will be implemented by the infrastructure layer (adapter).
 * This follows the Dependency Inversion Principle - the domain defines what it needs,
 * infrastructure provides the implementation.</p>
 *
 * <p>Business Rule: One season prediction per user per season.</p>
 */
public interface SeasonPredictionRepository {

    /**
     * Save a season prediction (create or update).
     *
     * @param prediction the season prediction to save
     * @return the saved season prediction
     */
    SeasonPrediction save(SeasonPrediction prediction);

    /**
     * Find a season prediction by its ID.
     *
     * @param id the prediction ID
     * @return an Optional containing the prediction if found, empty otherwise
     */
    Optional<SeasonPrediction> findById(SeasonPredictionId id);

    /**
     * Find a season prediction by user ID and season ID.
     *
     * <p>This enforces the business rule: one prediction per user per season.</p>
     *
     * @param userId the user ID
     * @param seasonId the season ID
     * @return an Optional containing the prediction if found, empty otherwise
     */
    Optional<SeasonPrediction> findByUserIdAndSeasonId(UserId userId, SeasonId seasonId);

    /**
     * Check if a season prediction exists for a user in a season.
     *
     * @param userId the user ID
     * @param seasonId the season ID
     * @return true if a prediction exists, false otherwise
     */
    boolean existsByUserIdAndSeasonId(UserId userId, SeasonId seasonId);

    /**
     * Delete a season prediction.
     *
     * @param id the prediction ID to delete
     */
    void deleteById(SeasonPredictionId id);
}
