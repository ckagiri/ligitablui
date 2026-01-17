package com.ligitabl.domain.model.leaderboard;

import com.ligitabl.domain.model.user.UserId;

import java.util.List;
import java.util.Objects;

/**
 * Domain entity representing detailed user information for leaderboard drill-down.
 *
 * <p>Contains the user's prediction and scoring details for a specific round.</p>
 */
public class UserDetail {

    private final UserId userId;
    private final String displayName;
    private final int position;
    private final int totalScore;
    private final int roundScore;
    private final int totalZeroes;
    private final int movement;
    private final List<PredictionDetail> predictions;

    private UserDetail(
        UserId userId,
        String displayName,
        int position,
        int totalScore,
        int roundScore,
        int totalZeroes,
        int movement,
        List<PredictionDetail> predictions
    ) {
        this.userId = Objects.requireNonNull(userId, "userId is required");
        this.displayName = Objects.requireNonNull(displayName, "displayName is required");
        this.position = position;
        this.totalScore = totalScore;
        this.roundScore = roundScore;
        this.totalZeroes = totalZeroes;
        this.movement = movement;
        this.predictions = List.copyOf(Objects.requireNonNull(predictions, "predictions are required"));
    }

    /**
     * Factory method to create user details.
     */
    public static UserDetail create(
        UserId userId,
        String displayName,
        int position,
        int totalScore,
        int roundScore,
        int totalZeroes,
        int movement,
        List<PredictionDetail> predictions
    ) {
        return new UserDetail(
            userId, displayName, position, totalScore,
            roundScore, totalZeroes, movement, predictions
        );
    }

    // Getters
    public UserId getUserId() {
        return userId;
    }

    public String getDisplayName() {
        return displayName;
    }

    public int getPosition() {
        return position;
    }

    public int getTotalScore() {
        return totalScore;
    }

    public int getRoundScore() {
        return roundScore;
    }

    public int getTotalZeroes() {
        return totalZeroes;
    }

    public int getMovement() {
        return movement;
    }

    public List<PredictionDetail> getPredictions() {
        return predictions;
    }

    /**
     * Nested record representing a single prediction row in user details.
     */
    public record PredictionDetail(
        int position,
        String teamCode,
        String teamName,
        String crestUrl,
        Integer hit,
        Integer actualPosition
    ) {
        public PredictionDetail {
            Objects.requireNonNull(teamCode, "teamCode is required");
            Objects.requireNonNull(teamName, "teamName is required");
        }

        /**
         * Check if this prediction was a perfect hit (0 difference).
         */
        public boolean isPerfect() {
            return hit != null && hit == 0;
        }

        /**
         * Check if this prediction was close (1-2 positions off).
         */
        public boolean isClose() {
            return hit != null && hit >= 1 && hit <= 2;
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        UserDetail that = (UserDetail) o;
        return userId.equals(that.userId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(userId);
    }
}
