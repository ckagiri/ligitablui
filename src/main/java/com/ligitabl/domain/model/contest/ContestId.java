package com.ligitabl.domain.model.contest;

import java.util.UUID;

/**
 * Value object representing a contest's unique identifier.
 *
 * <p>This is a type-safe wrapper around a UUID to prevent mixing up different types of IDs.</p>
 *
 * <p>A contest represents a competition that users can enter with their season predictions.
 * The main contest is the default season-long competition.</p>
 *
 * <p>Example usage:</p>
 * <pre>
 * ContestId id = ContestId.generate();
 * ContestId existing = ContestId.of("123e4567-e89b-12d3-a456-426614174000");
 * </pre>
 *
 * @throws IllegalArgumentException if the value is null or not a valid UUID
 */
public record ContestId(String value) {

    /**
     * Compact constructor with validation.
     *
     * @throws IllegalArgumentException if the value is null or not a valid UUID
     */
    public ContestId {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("ContestId value cannot be null or blank");
        }
        // Validate UUID format
        try {
            UUID.fromString(value);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("ContestId must be a valid UUID: " + value, e);
        }
    }

    /**
     * Factory method to create a ContestId from a string.
     *
     * @param id the contest ID string (must be a valid UUID)
     * @return a validated ContestId instance
     * @throws IllegalArgumentException if the id is invalid
     */
    public static ContestId of(String id) {
        return new ContestId(id);
    }

    /**
     * Generate a new random ContestId.
     *
     * @return a new ContestId with a randomly generated UUID
     */
    public static ContestId generate() {
        return new ContestId(UUID.randomUUID().toString());
    }
}
