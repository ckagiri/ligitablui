package com.ligitabl.domain.model.contest;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for ContestId value object.
 */
class ContestIdTest {

    @Test
    void shouldCreateContestIdWithValidUUID() {
        // Given
        String validUuid = UUID.randomUUID().toString();

        // When
        ContestId contestId = ContestId.of(validUuid);

        // Then
        assertEquals(validUuid, contestId.value());
    }

    @Test
    void shouldGenerateRandomContestId() {
        // When
        ContestId id1 = ContestId.generate();
        ContestId id2 = ContestId.generate();

        // Then
        assertNotEquals(id1, id2);
    }

    @Test
    void shouldThrowExceptionForNullValue() {
        // When/Then
        assertThrows(IllegalArgumentException.class, () -> new ContestId(null));
    }

    @Test
    void shouldThrowExceptionForInvalidUUID() {
        // When/Then
        assertThrows(IllegalArgumentException.class, () -> ContestId.of("invalid"));
    }

    @Test
    void shouldBeEqualWhenUUIDsMatch() {
        // Given
        String uuid = UUID.randomUUID().toString();

        // When/Then
        assertEquals(ContestId.of(uuid), ContestId.of(uuid));
    }
}
