package com.ligitabl.domain.model.team;

import java.util.UUID;

/**
 * Value object representing a team's unique identifier.
 *
 * <p>This is a type-safe wrapper around a string ID to prevent mixing up different types of IDs.</p>
 *
 * <p>Supports multiple ID formats:</p>
 * <ul>
 *   <li>Standard UUID: "123e4567-e89b-12d3-a456-426614174000"</li>
 *   <li>Team code format: "team-mci-000000000001"</li>
 * </ul>
 *
 * <p>Example usage:</p>
 * <pre>
 * TeamId id = TeamId.generate(); // Generates UUID format
 * TeamId existing = TeamId.of("team-mci-000000000001"); // Team code format
 * </pre>
 *
 * @throws IllegalArgumentException if the value is null or blank
 */
public record TeamId(String value) {

    /**
     * Compact constructor with validation.
     *
     * @throws IllegalArgumentException if the value is null or blank
     */
    public TeamId {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("TeamId value cannot be null or blank");
        }
        // Allow both UUID format and team-{code}-{uuid} format
        // No strict validation to support flexible ID formats
    }

    /**
     * Factory method to create a TeamId from a string.
     *
     * @param id the team ID string (supports UUID or team-{code}-{uuid} format)
     * @return a validated TeamId instance
     * @throws IllegalArgumentException if the id is null or blank
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
