package com.ligitabl.domain.model.team;

import java.util.Set;

/**
 * Value object representing a team's unique code.
 *
 * <p>Valid codes are the Premier League team abbreviations for the 2023/24 season.
 * This value object is self-validating and immutable.</p>
 *
 * <p>Example codes: MCI (Manchester City), ARS (Arsenal), LIV (Liverpool)</p>
 *
 * @throws IllegalArgumentException if an invalid team code is provided
 */
public record TeamCode(String value) {

    /**
     * Set of valid Premier League team codes for the 2023/24 season.
     */
    private static final Set<String> VALID_CODES = Set.of(
        "MCI", "ARS", "LIV", "AVL", "TOT", "CHE", "NEW", "MUN",
        "WHU", "BHA", "WOL", "FUL", "BOU", "CRY", "BRE", "EVE",
        "NFO", "LEE", "BUR", "SUN"
    );

    /**
     * Compact constructor with validation.
     *
     * @throws IllegalArgumentException if the code is null or not in VALID_CODES
     */
    public TeamCode {
        if (value == null) {
            throw new IllegalArgumentException("Team code cannot be null");
        }
        if (!VALID_CODES.contains(value)) {
            throw new IllegalArgumentException(
                "Invalid team code: '" + value + "'. Must be one of: " + VALID_CODES
            );
        }
    }

    /**
     * Factory method to create a TeamCode from a string.
     *
     * @param code the team code string
     * @return a validated TeamCode instance
     * @throws IllegalArgumentException if the code is invalid
     */
    public static TeamCode of(String code) {
        return new TeamCode(code);
    }

    /**
     * Get all valid team codes.
     *
     * @return an immutable set of all valid team codes
     */
    public static Set<String> validCodes() {
        return VALID_CODES;
    }
}
