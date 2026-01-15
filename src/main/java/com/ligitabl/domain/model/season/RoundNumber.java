package com.ligitabl.domain.model.season;

/**
 * Value object representing a round number within a season.
 *
 * <p>A Premier League season has 38 rounds (matchweeks).
 * This value object ensures that only valid round numbers are used.</p>
 *
 * <p>Example usage:</p>
 * <pre>
 * RoundNumber round = RoundNumber.of(1);  // First round
 * RoundNumber round = RoundNumber.of(38); // Last round
 * </pre>
 *
 * @throws IllegalArgumentException if the round number is not between 1 and 38 (inclusive)
 */
public record RoundNumber(int value) {

    /**
     * Minimum valid round number (first round of the season).
     */
    public static final int MIN_ROUND = 1;

    /**
     * Maximum valid round number (last round of a 20-team Premier League season).
     */
    public static final int MAX_ROUND = 38;

    /**
     * Compact constructor with validation.
     *
     * @throws IllegalArgumentException if the round number is not between MIN_ROUND and MAX_ROUND
     */
    public RoundNumber {
        if (value < MIN_ROUND || value > MAX_ROUND) {
            throw new IllegalArgumentException(
                "Round number must be between " + MIN_ROUND + " and " + MAX_ROUND +
                ". Provided: " + value
            );
        }
    }

    /**
     * Factory method to create a RoundNumber from an int.
     *
     * @param round the round number (must be between 1 and 38)
     * @return a validated RoundNumber instance
     * @throws IllegalArgumentException if the round is invalid
     */
    public static RoundNumber of(int round) {
        return new RoundNumber(round);
    }

    /**
     * Check if this is the first round of the season.
     *
     * @return true if this is round 1, false otherwise
     */
    public boolean isFirstRound() {
        return value == MIN_ROUND;
    }

    /**
     * Check if this is the last round of the season.
     *
     * @return true if this is round 38, false otherwise
     */
    public boolean isLastRound() {
        return value == MAX_ROUND;
    }

    /**
     * Get the next round number.
     *
     * @return the next RoundNumber
     * @throws IllegalArgumentException if already at the last round
     */
    public RoundNumber next() {
        if (isLastRound()) {
            throw new IllegalArgumentException("Cannot get next round: already at last round (" + MAX_ROUND + ")");
        }
        return new RoundNumber(value + 1);
    }

    /**
     * Get the previous round number.
     *
     * @return the previous RoundNumber
     * @throws IllegalArgumentException if already at the first round
     */
    public RoundNumber previous() {
        if (isFirstRound()) {
            throw new IllegalArgumentException("Cannot get previous round: already at first round (" + MIN_ROUND + ")");
        }
        return new RoundNumber(value - 1);
    }
}
