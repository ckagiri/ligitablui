package com.ligitabl.domain.model.season;

import java.util.UUID;

/**
 * Value object representing a season's unique identifier.
 *
 * <p>This is a type-safe wrapper around a UUID to prevent mixing up different types of IDs.</p>
 *
 * <p>Example usage:</p>
 * <pre>
 * SeasonId id = SeasonId.generate();
 * SeasonId existing = SeasonId.of("123e4567-e89b-12d3-a456-426614174000");
 * </pre>
 *
 * @throws IllegalArgumentException if the value is null or not a valid UUID
 */
public record SeasonId(String value) {

    /**
     * Compact constructor with validation.
     *
     * @throws IllegalArgumentException if the value is null or not a valid UUID
     */
    public SeasonId {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("SeasonId value cannot be null or blank");
        }
        // Validate UUID format
        try {
            UUID.fromString(value);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("SeasonId must be a valid UUID: " + value, e);
        }
    }

    /**
     * Factory method to create a SeasonId from a string.
     *
     * @param id the season ID string (must be a valid UUID)
     * @return a validated SeasonId instance
     * @throws IllegalArgumentException if the id is invalid
     */
    public static SeasonId of(String id) {
        return new SeasonId(id);
    }

    /**
     * Generate a new random SeasonId.
     *
     * @return a new SeasonId with a randomly generated UUID
     */
    public static SeasonId generate() {
        return new SeasonId(UUID.randomUUID().toString());
    }
}
