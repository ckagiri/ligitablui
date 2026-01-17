package com.ligitabl.infrastructure.persistence;

import com.ligitabl.domain.model.season.RoundNumber;
import com.ligitabl.domain.model.season.SeasonId;
import com.ligitabl.domain.model.seasonprediction.SeasonPrediction;
import com.ligitabl.domain.model.seasonprediction.SeasonPredictionId;
import com.ligitabl.domain.model.seasonprediction.TeamRanking;
import com.ligitabl.domain.model.team.TeamId;
import com.ligitabl.domain.model.user.UserId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for InMemorySeasonPredictionRepository.
 * Validates CRUD operations and secondary index functionality.
 */
class InMemorySeasonPredictionRepositoryTest {

    private InMemorySeasonPredictionRepository repository;

    private final UserId userId = UserId.generate();
    private final SeasonId seasonId = SeasonId.generate();

    @BeforeEach
    void setUp() {
        repository = new InMemorySeasonPredictionRepository();
        repository.clear();
    }

    @Test
    void shouldSaveAndFindById() {
        // Given
        SeasonPrediction prediction = createPrediction();

        // When
        SeasonPrediction saved = repository.save(prediction);
        Optional<SeasonPrediction> found = repository.findById(prediction.getId());

        // Then
        assertNotNull(saved);
        assertTrue(found.isPresent());
        assertEquals(prediction.getId(), found.get().getId());
        assertEquals(prediction.getUserId(), found.get().getUserId());
    }

    @Test
    void shouldFindByUserIdAndSeasonId() {
        // Given
        SeasonPrediction prediction = createPrediction();
        repository.save(prediction);

        // When
        Optional<SeasonPrediction> found = repository.findByUserIdAndSeasonId(userId, seasonId);

        // Then
        assertTrue(found.isPresent());
        assertEquals(prediction.getId(), found.get().getId());
    }

    @Test
    void shouldReturnEmptyWhenNotFound() {
        // When
        Optional<SeasonPrediction> found = repository.findById(SeasonPredictionId.generate());

        // Then
        assertTrue(found.isEmpty());
    }

    @Test
    void shouldReturnEmptyWhenUserSeasonNotFound() {
        // When
        Optional<SeasonPrediction> found = repository.findByUserIdAndSeasonId(
            UserId.generate(),
            SeasonId.generate()
        );

        // Then
        assertTrue(found.isEmpty());
    }

    @Test
    void shouldCheckExistence() {
        // Given
        SeasonPrediction prediction = createPrediction();
        repository.save(prediction);

        // When
        boolean exists = repository.existsByUserIdAndSeasonId(userId, seasonId);
        boolean notExists = repository.existsByUserIdAndSeasonId(
            UserId.generate(),
            SeasonId.generate()
        );

        // Then
        assertTrue(exists);
        assertFalse(notExists);
    }

    @Test
    void shouldDeleteById() {
        // Given
        SeasonPrediction prediction = createPrediction();
        repository.save(prediction);

        // When
        repository.deleteById(prediction.getId());

        // Then
        Optional<SeasonPrediction> found = repository.findById(prediction.getId());
        assertTrue(found.isEmpty());

        // Secondary index should also be cleared
        boolean exists = repository.existsByUserIdAndSeasonId(userId, seasonId);
        assertFalse(exists);
    }

    @Test
    void shouldUpdateExistingPrediction() {
        // Given: Save initial prediction
        SeasonPrediction original = createPrediction();
        repository.save(original);

        // When: Update with new prediction for same user+season
        SeasonPrediction updated = SeasonPrediction.create(
            original.getId(), // Same ID
            userId,
            seasonId,
            RoundNumber.of(5), // Different round
            create20Rankings(),
            Instant.now()
        );
        repository.save(updated);

        // Then: Should have only one prediction
        assertEquals(1, repository.count());

        Optional<SeasonPrediction> found = repository.findByUserIdAndSeasonId(userId, seasonId);
        assertTrue(found.isPresent());
        assertEquals(RoundNumber.of(5), found.get().getAtRound());
    }

    @Test
    void shouldEnforceOnePerUserPerSeason() {
        // Given: Two predictions for same user+season
        SeasonPrediction pred1 = createPrediction();
        repository.save(pred1);

        SeasonPrediction pred2 = SeasonPrediction.create(
            SeasonPredictionId.generate(), // Different ID
            userId,
            seasonId,
            RoundNumber.of(1),
            create20Rankings(),
            Instant.now()
        );

        // When: Save second prediction (overwrites in secondary index)
        repository.save(pred2);

        // Then: Only one can be found via user+season lookup
        Optional<SeasonPrediction> found = repository.findByUserIdAndSeasonId(userId, seasonId);
        assertTrue(found.isPresent());
        assertEquals(pred2.getId(), found.get().getId());

        // But both exist by ID
        assertTrue(repository.findById(pred1.getId()).isPresent());
        assertTrue(repository.findById(pred2.getId()).isPresent());
        assertEquals(2, repository.count());
    }

    @Test
    void shouldHandleMultipleUsersAndSeasons() {
        // Given: Different users and seasons
        UserId user1 = UserId.generate();
        UserId user2 = UserId.generate();
        SeasonId season1 = SeasonId.generate();
        SeasonId season2 = SeasonId.generate();

        SeasonPrediction pred1 = createPredictionFor(user1, season1);
        SeasonPrediction pred2 = createPredictionFor(user1, season2);
        SeasonPrediction pred3 = createPredictionFor(user2, season1);

        // When
        repository.save(pred1);
        repository.save(pred2);
        repository.save(pred3);

        // Then
        assertEquals(3, repository.count());
        assertTrue(repository.existsByUserIdAndSeasonId(user1, season1));
        assertTrue(repository.existsByUserIdAndSeasonId(user1, season2));
        assertTrue(repository.existsByUserIdAndSeasonId(user2, season1));
        assertFalse(repository.existsByUserIdAndSeasonId(user2, season2));
    }

    @Test
    void shouldClearAllData() {
        // Given
        repository.save(createPrediction());
        repository.save(createPredictionFor(UserId.generate(), SeasonId.generate()));
        assertEquals(2, repository.count());

        // When
        repository.clear();

        // Then
        assertEquals(0, repository.count());
    }

    @Test
    void shouldThrowNullPointerForNullInputs() {
        // When/Then
        assertThrows(NullPointerException.class, () ->
            repository.save(null)
        );
        assertThrows(NullPointerException.class, () ->
            repository.findById(null)
        );
        assertThrows(NullPointerException.class, () ->
            repository.findByUserIdAndSeasonId(null, seasonId)
        );
        assertThrows(NullPointerException.class, () ->
            repository.findByUserIdAndSeasonId(userId, null)
        );
        assertThrows(NullPointerException.class, () ->
            repository.existsByUserIdAndSeasonId(null, seasonId)
        );
        assertThrows(NullPointerException.class, () ->
            repository.deleteById(null)
        );
    }

    // Helper methods

    private SeasonPrediction createPrediction() {
        return createPredictionFor(userId, seasonId);
    }

    private SeasonPrediction createPredictionFor(UserId userId, SeasonId seasonId) {
        return SeasonPrediction.create(
            SeasonPredictionId.generate(),
            userId,
            seasonId,
            RoundNumber.of(1),
            create20Rankings(),
            Instant.now()
        );
    }

    private List<TeamRanking> create20Rankings() {
        List<TeamRanking> rankings = new ArrayList<>();
        for (int i = 1; i <= 20; i++) {
            rankings.add(TeamRanking.create(TeamId.generate(), i));
        }
        return rankings;
    }
}
