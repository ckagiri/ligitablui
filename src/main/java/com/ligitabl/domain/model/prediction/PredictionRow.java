package com.ligitabl.domain.model.prediction;

import java.util.Objects;

/**
 * Domain entity representing a single row in a prediction.
 *
 * <p>Contains the predicted position for a team along with
 * actual results if the round is completed.</p>
 */
public class PredictionRow {

    private final int position;
    private final String teamCode;
    private final String teamName;
    private final String crestUrl;
    private final Integer hit;          // Points lost (0 = perfect, null = not yet scored)
    private final Integer actualPosition;  // Actual standings position (null = not yet known)

    private PredictionRow(
        int position,
        String teamCode,
        String teamName,
        String crestUrl,
        Integer hit,
        Integer actualPosition
    ) {
        this.position = position;
        this.teamCode = Objects.requireNonNull(teamCode, "teamCode is required");
        this.teamName = Objects.requireNonNull(teamName, "teamName is required");
        this.crestUrl = crestUrl;
        this.hit = hit;
        this.actualPosition = actualPosition;
    }

    /**
     * Factory method for pending prediction (no results yet).
     */
    public static PredictionRow pending(int position, String teamCode, String teamName, String crestUrl) {
        return new PredictionRow(position, teamCode, teamName, crestUrl, null, null);
    }

    /**
     * Factory method for scored prediction (with results).
     */
    public static PredictionRow scored(
        int position,
        String teamCode,
        String teamName,
        String crestUrl,
        int hit,
        int actualPosition
    ) {
        return new PredictionRow(position, teamCode, teamName, crestUrl, hit, actualPosition);
    }

    /**
     * Create a scored version of this prediction row.
     */
    public PredictionRow withResult(int actualPosition) {
        int calculatedHit = Math.abs(this.position - actualPosition);
        return new PredictionRow(position, teamCode, teamName, crestUrl, calculatedHit, actualPosition);
    }

    /**
     * Create a new row with updated position (for swaps).
     */
    public PredictionRow withPosition(int newPosition) {
        return new PredictionRow(newPosition, teamCode, teamName, crestUrl, hit, actualPosition);
    }

    // Getters
    public int getPosition() { return position; }
    public String getTeamCode() { return teamCode; }
    public String getTeamName() { return teamName; }
    public String getCrestUrl() { return crestUrl; }
    public Integer getHit() { return hit; }
    public Integer getActualPosition() { return actualPosition; }

    /**
     * Check if this is a perfect prediction (hit = 0).
     */
    public boolean isPerfect() {
        return hit != null && hit == 0;
    }

    /**
     * Check if this is a close prediction (1-2 positions off).
     */
    public boolean isClose() {
        return hit != null && hit >= 1 && hit <= 2;
    }

    /**
     * Check if this is a near miss (3-5 positions off).
     */
    public boolean isNearMiss() {
        return hit != null && hit >= 3 && hit <= 5;
    }

    /**
     * Check if this is a big miss (>5 positions off).
     */
    public boolean isBigMiss() {
        return hit != null && hit > 5;
    }

    /**
     * Check if results have been calculated.
     */
    public boolean hasResult() {
        return hit != null;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PredictionRow that = (PredictionRow) o;
        return position == that.position && teamCode.equals(that.teamCode);
    }

    @Override
    public int hashCode() {
        return Objects.hash(position, teamCode);
    }

    @Override
    public String toString() {
        return "PredictionRow{" +
            "position=" + position +
            ", teamCode='" + teamCode + '\'' +
            ", hit=" + hit +
            '}';
    }
}
