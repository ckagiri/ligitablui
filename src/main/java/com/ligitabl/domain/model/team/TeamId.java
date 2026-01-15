package com.ligitabl.domain.model.team;

import java.util.UUID;

/**
 * Value object representing a team's unique identifier.
 *
 * <p>This is a type-safe wrapper around a UUID to prevent mixing up different types of IDs.</p>
 *
 * <p>Example usage:</p>
 * <pre>
 * TeamId id = TeamId.generate();
 * TeamId existing = TeamId.of("123e4567-e89b-12d3-a456-426614174000");
 * </pre>
 *
 * @throws IllegalArgumentException if the value is null or not a valid UUID
 */
public record TeamId(String value) {

    /**
     * Compact constructor with validation.
     *
     * @throws IllegalArgumentException if the value is null or not a valid UUID
     */
    public TeamId {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("TeamId value cannot be null or blank");
        }
        // Validate UUID format
        try {
            UUID.fromString(value);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("TeamId must be a valid UUID: " + value, e);
        }
    }

    /**
     * Factory method to create a TeamId from a string.
     *
     * @param id the team ID string (must be a valid UUID)
     * @return a validated TeamId instance
     * @throws IllegalArgumentException if the id is invalid
     */
    public static TeamId of(String id) {
        return new TeamId(id);
    }

    /**
     * Generate a new random TeamId.
     *
     * @return a new TeamId with a randomly generated UUID
     */
    public static TeamId generate() {
        return new TeamId(UUID.randomUUID().toString());
    }
}
