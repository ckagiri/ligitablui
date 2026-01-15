package com.ligitabl.domain.model.seasonprediction;

import java.util.UUID;

/**
 * Value object representing a season prediction's unique identifier.
 *
 * <p>This is a type-safe wrapper around a UUID to prevent mixing up different types of IDs.</p>
 *
 * <p>A season prediction is the core aggregate representing a user's predicted team rankings
 * for the entire season. Each user has at most one season prediction per season.</p>
 *
 * <p>Example usage:</p>
 * <pre>
 * SeasonPredictionId id = SeasonPredictionId.generate();
 * SeasonPredictionId existing = SeasonPredictionId.of("123e4567-e89b-12d3-a456-426614174000");
 * </pre>
 *
 * @throws IllegalArgumentException if the value is null or not a valid UUID
 */
public record SeasonPredictionId(String value) {

    /**
     * Compact constructor with validation.
     *
     * @throws IllegalArgumentException if the value is null or not a valid UUID
     */
    public SeasonPredictionId {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("SeasonPredictionId value cannot be null or blank");
        }
        // Validate UUID format
        try {
            UUID.fromString(value);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("SeasonPredictionId must be a valid UUID: " + value, e);
        }
    }

    /**
     * Factory method to create a SeasonPredictionId from a string.
     *
     * @param id the season prediction ID string (must be a valid UUID)
     * @return a validated SeasonPredictionId instance
     * @throws IllegalArgumentException if the id is invalid
     */
    public static SeasonPredictionId of(String id) {
        return new SeasonPredictionId(id);
    }

    /**
     * Generate a new random SeasonPredictionId.
     *
     * @return a new SeasonPredictionId with a randomly generated UUID
     */
    public static SeasonPredictionId generate() {
        return new SeasonPredictionId(UUID.randomUUID().toString());
    }
}
