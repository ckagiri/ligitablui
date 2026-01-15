package com.ligitabl.domain.model.team;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for TeamId value object.
 * Target: 100% coverage on validation logic.
 */
class TeamIdTest {

    @Test
    void shouldCreateTeamIdWithValidUUID() {
        // Given
        String validUuid = UUID.randomUUID().toString();

        // When
        TeamId teamId = new TeamId(validUuid);

        // Then
        assertNotNull(teamId);
        assertEquals(validUuid, teamId.value());
    }

    @Test
    void shouldCreateTeamIdUsingFactoryMethod() {
        // Given
        String validUuid = UUID.randomUUID().toString();

        // When
        TeamId teamId = TeamId.of(validUuid);

        // Then
        assertNotNull(teamId);
        assertEquals(validUuid, teamId.value());
    }

    @Test
    void shouldGenerateRandomTeamId() {
        // When
        TeamId teamId1 = TeamId.generate();
        TeamId teamId2 = TeamId.generate();

        // Then
        assertNotNull(teamId1);
        assertNotNull(teamId2);
        assertNotEquals(teamId1, teamId2); // Should be different random IDs
        assertDoesNotThrow(() -> UUID.fromString(teamId1.value())); // Should be valid UUID
    }

    @Test
    void shouldThrowExceptionForNullValue() {
        // When/Then
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> new TeamId(null)
        );

        assertEquals("TeamId value cannot be null or blank", exception.getMessage());
    }

    @ParameterizedTest
    @ValueSource(strings = {"", " ", "   "})
    void shouldThrowExceptionForBlankValue(String blankValue) {
        // When/Then
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> new TeamId(blankValue)
        );

        assertEquals("TeamId value cannot be null or blank", exception.getMessage());
    }

    @ParameterizedTest
    @ValueSource(strings = {"not-a-uuid", "12345", "abc-def-ghi", "123e4567"})
    void shouldThrowExceptionForInvalidUUID(String invalidUuid) {
        // When/Then
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> new TeamId(invalidUuid)
        );

        assertTrue(exception.getMessage().contains("TeamId must be a valid UUID"));
        assertTrue(exception.getMessage().contains(invalidUuid));
    }

    @Test
    void shouldBeEqualWhenUUIDsMatch() {
        // Given
        String uuid = UUID.randomUUID().toString();
        TeamId id1 = TeamId.of(uuid);
        TeamId id2 = TeamId.of(uuid);

        // When/Then
        assertEquals(id1, id2);
        assertEquals(id1.hashCode(), id2.hashCode());
    }

    @Test
    void shouldNotBeEqualWhenUUIDsDiffer() {
        // Given
        TeamId id1 = TeamId.generate();
        TeamId id2 = TeamId.generate();

        // When/Then
        assertNotEquals(id1, id2);
    }

    @Test
    void shouldHaveUsefulToString() {
        // Given
        String uuid = "123e4567-e89b-12d3-a456-426614174000";
        TeamId id = TeamId.of(uuid);

        // When
        String toString = id.toString();

        // Then
        assertTrue(toString.contains(uuid));
    }
}
