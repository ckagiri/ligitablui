package com.ligitabl.domain.model.season;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for SeasonId value object.
 */
class SeasonIdTest {

    @Test
    void shouldCreateSeasonIdWithValidUUID() {
        // Given
        String validUuid = UUID.randomUUID().toString();

        // When
        SeasonId seasonId = SeasonId.of(validUuid);

        // Then
        assertEquals(validUuid, seasonId.value());
    }

    @Test
    void shouldGenerateRandomSeasonId() {
        // When
        SeasonId id1 = SeasonId.generate();
        SeasonId id2 = SeasonId.generate();

        // Then
        assertNotEquals(id1, id2);
    }

    @Test
    void shouldThrowExceptionForNullValue() {
        // When/Then
        assertThrows(IllegalArgumentException.class, () -> new SeasonId(null));
    }

    @Test
    void shouldThrowExceptionForInvalidUUID() {
        // When/Then
        assertThrows(IllegalArgumentException.class, () -> SeasonId.of("invalid"));
    }

    @Test
    void shouldBeEqualWhenUUIDsMatch() {
        // Given
        String uuid = UUID.randomUUID().toString();

        // When/Then
        assertEquals(SeasonId.of(uuid), SeasonId.of(uuid));
    }
}
