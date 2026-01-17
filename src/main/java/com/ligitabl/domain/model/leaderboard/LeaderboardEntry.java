package com.ligitabl.domain.model.leaderboard;

import com.ligitabl.domain.model.user.UserId;

import java.util.Objects;

/**
 * Domain entity representing a single entry in the leaderboard.
 *
 * <p>Contains all scoring and ranking information for a user in a specific phase.</p>
 */
public class LeaderboardEntry {

    private final int position;
    private final UserId userId;
    private final String displayName;
    private final int totalScore;
    private final int roundScore;
    private final int totalZeroes;
    private final int totalSwaps;
    private final int totalPoints;
    private final int movement;

    private LeaderboardEntry(
        int position,
        UserId userId,
        String displayName,
        int totalScore,
        int roundScore,
        int totalZeroes,
        int totalSwaps,
        int totalPoints,
        int movement
    ) {
        this.position = position;
        this.userId = Objects.requireNonNull(userId, "userId is required");
        this.displayName = Objects.requireNonNull(displayName, "displayName is required");
        this.totalScore = totalScore;
        this.roundScore = roundScore;
        this.totalZeroes = totalZeroes;
        this.totalSwaps = totalSwaps;
        this.totalPoints = totalPoints;
        this.movement = movement;
    }

    /**
     * Factory method to create a leaderboard entry.
     */
    public static LeaderboardEntry create(
        int position,
        UserId userId,
        String displayName,
        int totalScore,
        int roundScore,
        int totalZeroes,
        int totalSwaps,
        int totalPoints,
        int movement
    ) {
        if (position < 1) {
            throw new IllegalArgumentException("Position must be at least 1");
        }
        if (displayName.isBlank()) {
            throw new IllegalArgumentException("Display name cannot be blank");
        }
        return new LeaderboardEntry(
            position, userId, displayName, totalScore, roundScore,
            totalZeroes, totalSwaps, totalPoints, movement
        );
    }

    /**
     * Factory method to create from raw values (for repository mapping).
     */
    public static LeaderboardEntry fromRaw(
        int position,
        String userIdValue,
        String displayName,
        int totalScore,
        int roundScore,
        int totalZeroes,
        int totalSwaps,
        int totalPoints,
        int movement
    ) {
        return create(
            position,
            UserId.of(userIdValue),
            displayName,
            totalScore,
            roundScore,
            totalZeroes,
            totalSwaps,
            totalPoints,
            movement
        );
    }

    // Getters
    public int getPosition() {
        return position;
    }

    public UserId getUserId() {
        return userId;
    }

    public String getDisplayName() {
        return displayName;
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

    public int getTotalSwaps() {
        return totalSwaps;
    }

    public int getTotalPoints() {
        return totalPoints;
    }

    public int getMovement() {
        return movement;
    }

    /**
     * Check if this entry represents an improvement from previous position.
     */
    public boolean isImproving() {
        return movement > 0;
    }

    /**
     * Check if this entry represents a decline from previous position.
     */
    public boolean isDeclining() {
        return movement < 0;
    }

    /**
     * Check if this entry maintained the same position.
     */
    public boolean isStable() {
        return movement == 0;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        LeaderboardEntry that = (LeaderboardEntry) o;
        return position == that.position && userId.equals(that.userId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(position, userId);
    }

    @Override
    public String toString() {
        return "LeaderboardEntry{" +
            "position=" + position +
            ", userId=" + userId +
            ", displayName='" + displayName + '\'' +
            ", totalScore=" + totalScore +
            ", movement=" + movement +
            '}';
    }
}
