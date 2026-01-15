package com.ligitabl.domain.model.seasonprediction;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for SeasonPredictionId value object.
 */
class SeasonPredictionIdTest {

    @Test
    void shouldCreateSeasonPredictionIdWithValidUUID() {
        // Given
        String validUuid = UUID.randomUUID().toString();

        // When
        SeasonPredictionId predictionId = SeasonPredictionId.of(validUuid);

        // Then
        assertEquals(validUuid, predictionId.value());
    }

    @Test
    void shouldGenerateRandomSeasonPredictionId() {
        // When
        SeasonPredictionId id1 = SeasonPredictionId.generate();
        SeasonPredictionId id2 = SeasonPredictionId.generate();

        // Then
        assertNotEquals(id1, id2);
    }

    @Test
    void shouldThrowExceptionForNullValue() {
        // When/Then
        assertThrows(IllegalArgumentException.class, () -> new SeasonPredictionId(null));
    }

    @Test
    void shouldThrowExceptionForInvalidUUID() {
        // When/Then
        assertThrows(IllegalArgumentException.class, () -> SeasonPredictionId.of("invalid"));
    }

    @Test
    void shouldBeEqualWhenUUIDsMatch() {
        // Given
        String uuid = UUID.randomUUID().toString();

        // When/Then
        assertEquals(SeasonPredictionId.of(uuid), SeasonPredictionId.of(uuid));
    }
}
