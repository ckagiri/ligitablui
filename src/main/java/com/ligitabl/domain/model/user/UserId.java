package com.ligitabl.domain.model.user;

import java.util.UUID;

/**
 * Value object representing a user's unique identifier.
 *
 * <p>This is a type-safe wrapper around a UUID to prevent mixing up different types of IDs.</p>
 *
 * <p>Example usage:</p>
 * <pre>
 * UserId id = UserId.generate();
 * UserId existing = UserId.of("123e4567-e89b-12d3-a456-426614174000");
 * </pre>
 *
 * @throws IllegalArgumentException if the value is null or not a valid UUID
 */
public record UserId(String value) {

    /**
     * Compact constructor with validation.
     *
     * @throws IllegalArgumentException if the value is null or not a valid UUID
     */
    public UserId {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("UserId value cannot be null or blank");
        }
        // Validate UUID format
        try {
            UUID.fromString(value);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("UserId must be a valid UUID: " + value, e);
        }
    }

    /**
     * Factory method to create a UserId from a string.
     *
     * @param id the user ID string (must be a valid UUID)
     * @return a validated UserId instance
     * @throws IllegalArgumentException if the id is invalid
     */
    public static UserId of(String id) {
        return new UserId(id);
    }

    /**
     * Generate a new random UserId.
     *
     * @return a new UserId with a randomly generated UUID
     */
    public static UserId generate() {
        return new UserId(UUID.randomUUID().toString());
    }
}
