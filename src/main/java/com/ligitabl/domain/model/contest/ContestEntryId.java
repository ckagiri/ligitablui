package com.ligitabl.domain.model.contest;

import java.util.UUID;

/**
 * Value object representing a contest entry's unique identifier.
 *
 * <p>This is a type-safe wrapper around a UUID to prevent mixing up different types of IDs.</p>
 *
 * <p>A contest entry represents a user's participation in a specific contest.
 * When a user first submits their season prediction, a MainContestEntry is automatically created.</p>
 *
 * <p>Example usage:</p>
 * <pre>
 * ContestEntryId id = ContestEntryId.generate();
 * ContestEntryId existing = ContestEntryId.of("123e4567-e89b-12d3-a456-426614174000");
 * </pre>
 *
 * @throws IllegalArgumentException if the value is null or not a valid UUID
 */
public record ContestEntryId(String value) {

    /**
     * Compact constructor with validation.
     *
     * @throws IllegalArgumentException if the value is null or not a valid UUID
     */
    public ContestEntryId {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("ContestEntryId value cannot be null or blank");
        }
        // Validate UUID format
        try {
            UUID.fromString(value);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("ContestEntryId must be a valid UUID: " + value, e);
        }
    }

    /**
     * Factory method to create a ContestEntryId from a string.
     *
     * @param id the contest entry ID string (must be a valid UUID)
     * @return a validated ContestEntryId instance
     * @throws IllegalArgumentException if the id is invalid
     */
    public static ContestEntryId of(String id) {
        return new ContestEntryId(id);
    }

    /**
     * Generate a new random ContestEntryId.
     *
     * @return a new ContestEntryId with a randomly generated UUID
     */
    public static ContestEntryId generate() {
        return new ContestEntryId(UUID.randomUUID().toString());
    }
}
