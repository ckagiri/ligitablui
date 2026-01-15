package com.ligitabl.domain.model.user;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for UserId value object.
 */
class UserIdTest {

    @Test
    void shouldCreateUserIdWithValidUUID() {
        // Given
        String validUuid = UUID.randomUUID().toString();

        // When
        UserId userId = new UserId(validUuid);

        // Then
        assertNotNull(userId);
        assertEquals(validUuid, userId.value());
    }

    @Test
    void shouldCreateUserIdUsingFactoryMethod() {
        // Given
        String validUuid = UUID.randomUUID().toString();

        // When
        UserId userId = UserId.of(validUuid);

        // Then
        assertEquals(validUuid, userId.value());
    }

    @Test
    void shouldGenerateRandomUserId() {
        // When
        UserId userId1 = UserId.generate();
        UserId userId2 = UserId.generate();

        // Then
        assertNotNull(userId1);
        assertNotNull(userId2);
        assertNotEquals(userId1, userId2);
    }

    @Test
    void shouldThrowExceptionForNullValue() {
        // When/Then
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> new UserId(null)
        );

        assertEquals("UserId value cannot be null or blank", exception.getMessage());
    }

    @ParameterizedTest
    @ValueSource(strings = {"", " ", "   "})
    void shouldThrowExceptionForBlankValue(String blankValue) {
        // When/Then
        assertThrows(IllegalArgumentException.class, () -> new UserId(blankValue));
    }

    @ParameterizedTest
    @ValueSource(strings = {"not-a-uuid", "12345", "invalid"})
    void shouldThrowExceptionForInvalidUUID(String invalidUuid) {
        // When/Then
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> new UserId(invalidUuid)
        );

        assertTrue(exception.getMessage().contains("UserId must be a valid UUID"));
    }

    @Test
    void shouldBeEqualWhenUUIDsMatch() {
        // Given
        String uuid = UUID.randomUUID().toString();
        UserId id1 = UserId.of(uuid);
        UserId id2 = UserId.of(uuid);

        // When/Then
        assertEquals(id1, id2);
        assertEquals(id1.hashCode(), id2.hashCode());
    }

    @Test
    void shouldNotBeEqualWhenUUIDsDiffer() {
        // Given
        UserId id1 = UserId.generate();
        UserId id2 = UserId.generate();

        // When/Then
        assertNotEquals(id1, id2);
    }
}
