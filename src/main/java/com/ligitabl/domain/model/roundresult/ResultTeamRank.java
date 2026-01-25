package com.ligitabl.domain.model.roundresult;

import java.util.Objects;

/**
 * Represents a single team's result in a round.
 *
 * <p>Contains the predicted position, actual standings position,
 * and the hit (points lost).</p>
 */
public record ResultTeamRank(
    String teamCode,
    String teamName,
    int predictedPosition,
    int standingsPosition,
    int hit
) {
    public ResultTeamRank {
        Objects.requireNonNull(teamCode, "teamCode is required");
        Objects.requireNonNull(teamName, "teamName is required");
        if (predictedPosition < 1 || predictedPosition > 20) {
            throw new IllegalArgumentException("predictedPosition must be between 1 and 20");
        }
        if (standingsPosition < 1 || standingsPosition > 20) {
            throw new IllegalArgumentException("standingsPosition must be between 1 and 20");
        }
        if (hit < 0) {
            throw new IllegalArgumentException("hit cannot be negative");
        }
    }

    /**
     * Create a result team rank with calculated hit.
     */
    public static ResultTeamRank create(
        String teamCode,
        String teamName,
        int predictedPosition,
        int standingsPosition
    ) {
        int hit = Math.abs(predictedPosition - standingsPosition);
        return new ResultTeamRank(teamCode, teamName, predictedPosition, standingsPosition, hit);
    }

    /**
     * Check if this is a perfect prediction (hit = 0).
     */
    public boolean isPerfect() {
        return hit == 0;
    }

    /**
     * Check if this is a close prediction (1-2 positions off).
     */
    public boolean isClose() {
        return hit >= 1 && hit <= 2;
    }

    /**
     * Check if this is a near miss (3-5 positions off).
     */
    public boolean isNearMiss() {
        return hit >= 3 && hit <= 5;
    }

    /**
     * Check if this is a big miss (>5 positions off).
     */
    public boolean isBigMiss() {
        return hit > 5;
    }
}
