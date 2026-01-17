package com.ligitabl.infrastructure.persistence;

import com.ligitabl.domain.model.season.SeasonId;
import com.ligitabl.domain.model.seasonprediction.SeasonPrediction;
import com.ligitabl.domain.model.seasonprediction.SeasonPredictionId;
import com.ligitabl.domain.model.user.UserId;
import com.ligitabl.domain.repository.SeasonPredictionRepository;
import org.springframework.stereotype.Repository;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory implementation of SeasonPredictionRepository.
 *
 * <p>This implementation uses ConcurrentHashMap for thread-safe storage.
 * Data is stored in memory and will be lost on application restart.</p>
 *
 * <p>This is an infrastructure adapter implementing the domain repository interface (port).
 * It follows the Hexagonal Architecture pattern.</p>
 */
@Repository
public class InMemorySeasonPredictionRepository implements SeasonPredictionRepository {

    // Primary storage: ID -> SeasonPrediction
    private final Map<String, SeasonPrediction> predictionById = new ConcurrentHashMap<>();

    // Secondary index: userId+seasonId -> predictionId (for quick lookup)
    private final Map<String, String> predictionIdByUserAndSeason = new ConcurrentHashMap<>();

    @Override
    public SeasonPrediction save(SeasonPrediction prediction) {
        Objects.requireNonNull(prediction, "SeasonPrediction cannot be null");

        String id = prediction.getId().value();
        String userSeasonKey = buildUserSeasonKey(prediction.getUserId(), prediction.getSeasonId());

        // Store in primary storage
        predictionById.put(id, prediction);

        // Update secondary index
        predictionIdByUserAndSeason.put(userSeasonKey, id);

        return prediction;
    }

    @Override
    public Optional<SeasonPrediction> findById(SeasonPredictionId id) {
        Objects.requireNonNull(id, "SeasonPredictionId cannot be null");
        return Optional.ofNullable(predictionById.get(id.value()));
    }

    @Override
    public Optional<SeasonPrediction> findByUserIdAndSeasonId(UserId userId, SeasonId seasonId) {
        Objects.requireNonNull(userId, "UserId cannot be null");
        Objects.requireNonNull(seasonId, "SeasonId cannot be null");

        String userSeasonKey = buildUserSeasonKey(userId, seasonId);
        String predictionId = predictionIdByUserAndSeason.get(userSeasonKey);

        if (predictionId == null) {
            return Optional.empty();
        }

        return Optional.ofNullable(predictionById.get(predictionId));
    }

    @Override
    public boolean existsByUserIdAndSeasonId(UserId userId, SeasonId seasonId) {
        Objects.requireNonNull(userId, "UserId cannot be null");
        Objects.requireNonNull(seasonId, "SeasonId cannot be null");

        String userSeasonKey = buildUserSeasonKey(userId, seasonId);
        return predictionIdByUserAndSeason.containsKey(userSeasonKey);
    }

    @Override
    public void deleteById(SeasonPredictionId id) {
        Objects.requireNonNull(id, "SeasonPredictionId cannot be null");

        SeasonPrediction prediction = predictionById.remove(id.value());

        if (prediction != null) {
            // Remove from secondary index
            String userSeasonKey = buildUserSeasonKey(
                prediction.getUserId(),
                prediction.getSeasonId()
            );
            predictionIdByUserAndSeason.remove(userSeasonKey);
        }
    }

    /**
     * Build composite key for user+season index.
     *
     * @param userId the user ID
     * @param seasonId the season ID
     * @return composite key string
     */
    private String buildUserSeasonKey(UserId userId, SeasonId seasonId) {
        return userId.value() + ":" + seasonId.value();
    }

    /**
     * Clear all data (useful for testing).
     */
    public void clear() {
        predictionById.clear();
        predictionIdByUserAndSeason.clear();
    }

    /**
     * Get total count of predictions (useful for testing/monitoring).
     *
     * @return total number of stored predictions
     */
    public int count() {
        return predictionById.size();
    }
}
