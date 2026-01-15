package com.ligitabl.domain.model.contest;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for ContestEntryId value object.
 */
class ContestEntryIdTest {

    @Test
    void shouldCreateContestEntryIdWithValidUUID() {
        // Given
        String validUuid = UUID.randomUUID().toString();

        // When
        ContestEntryId entryId = ContestEntryId.of(validUuid);

        // Then
        assertEquals(validUuid, entryId.value());
    }

    @Test
    void shouldGenerateRandomContestEntryId() {
        // When
        ContestEntryId id1 = ContestEntryId.generate();
        ContestEntryId id2 = ContestEntryId.generate();

        // Then
        assertNotEquals(id1, id2);
    }

    @Test
    void shouldThrowExceptionForNullValue() {
        // When/Then
        assertThrows(IllegalArgumentException.class, () -> new ContestEntryId(null));
    }

    @Test
    void shouldThrowExceptionForInvalidUUID() {
        // When/Then
        assertThrows(IllegalArgumentException.class, () -> ContestEntryId.of("invalid"));
    }

    @Test
    void shouldBeEqualWhenUUIDsMatch() {
        // Given
        String uuid = UUID.randomUUID().toString();

        // When/Then
        assertEquals(ContestEntryId.of(uuid), ContestEntryId.of(uuid));
    }
}
