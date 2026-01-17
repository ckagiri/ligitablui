package com.ligitabl.domain.model.prediction;

import java.util.Objects;

/**
 * Value object representing a fixture (upcoming match) for a team.
 */
public record Fixture(
    String opponent,
    boolean isHome
) {
    public Fixture {
        Objects.requireNonNull(opponent, "opponent is required");
    }

    /**
     * Factory method for home fixture.
     */
    public static Fixture home(String opponent) {
        return new Fixture(opponent, true);
    }

    /**
     * Factory method for away fixture.
     */
    public static Fixture away(String opponent) {
        return new Fixture(opponent, false);
    }

    /**
     * Get display string like "vs ARS (H)" or "vs LIV (A)".
     */
    public String getDisplayString() {
        return "vs " + opponent + " (" + (isHome ? "H" : "A") + ")";
    }
}
