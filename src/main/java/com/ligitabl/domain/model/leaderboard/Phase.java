package com.ligitabl.domain.model.leaderboard;

/**
 * Value object representing a scoring phase/period in the competition.
 *
 * <p>Phases divide the season into scoring periods:
 * - FS: Full Season (cumulative)
 * - Q1-Q4: Quarterly periods
 * - H1-H2: Half-year periods</p>
 */
public enum Phase {
    FS("Full Season"),
    Q1("Quarter 1"),
    Q2("Quarter 2"),
    Q3("Quarter 3"),
    Q4("Quarter 4"),
    H1("First Half"),
    H2("Second Half");

    private final String displayName;

    Phase(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    /**
     * Parse a phase from string, with fallback to FS.
     *
     * @param value the string value (case-insensitive)
     * @return the matching Phase, or FS if not found
     */
    public static Phase fromString(String value) {
        if (value == null || value.isBlank()) {
            return FS;
        }
        try {
            return Phase.valueOf(value.toUpperCase());
        } catch (IllegalArgumentException e) {
            return FS;
        }
    }
}
