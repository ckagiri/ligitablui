package com.ligitabl.domain.model.roundresult;

import com.ligitabl.domain.model.season.RoundNumber;
import com.ligitabl.domain.model.user.UserId;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

/**
 * Represents a user's scored prediction results for a completed round.
 *
 * <p>Created when a round is finalized, containing the user's predictions
 * compared against actual standings with calculated hits.</p>
 */
public record RoundResult(
    RoundResultId id,
    UserId userId,
    RoundNumber roundNumber,
    List<ResultTeamRank> rankings,
    int totalScore,
    int zeroesCount,
    int swapCount,
    boolean userViewed,
    Instant createdAt
) {
    private static final int MAX_HIT_POINTS = 200; // Maximum possible hit points (sum of max hits)

    public RoundResult {
        Objects.requireNonNull(id, "id is required");
        Objects.requireNonNull(userId, "userId is required");
        Objects.requireNonNull(roundNumber, "roundNumber is required");
        Objects.requireNonNull(rankings, "rankings is required");
        Objects.requireNonNull(createdAt, "createdAt is required");
        rankings = List.copyOf(rankings);
        if (totalScore < 0) {
            throw new IllegalArgumentException("totalScore cannot be negative");
        }
        if (zeroesCount < 0) {
            throw new IllegalArgumentException("zeroesCount cannot be negative");
        }
        if (swapCount < 0) {
            throw new IllegalArgumentException("swapCount cannot be negative");
        }
    }

    /**
     * Create a round result from rankings with calculated score.
     */
    public static RoundResult create(
        UserId userId,
        RoundNumber roundNumber,
        List<ResultTeamRank> rankings,
        int swapCount
    ) {
        int totalHits = rankings.stream().mapToInt(ResultTeamRank::hit).sum();
        int totalScore = MAX_HIT_POINTS - totalHits;
        int zeroesCount = (int) rankings.stream().filter(ResultTeamRank::isPerfect).count();

        return new RoundResult(
            RoundResultId.generate(),
            userId,
            roundNumber,
            rankings,
            totalScore,
            zeroesCount,
            swapCount,
            false,
            Instant.now()
        );
    }

    /**
     * Get total hit points (sum of all hits).
     */
    public int getTotalHits() {
        return rankings.stream().mapToInt(ResultTeamRank::hit).sum();
    }

    /**
     * Mark result as viewed by user.
     */
    public RoundResult markAsViewed() {
        return new RoundResult(
            id, userId, roundNumber, rankings, totalScore,
            zeroesCount, swapCount, true, createdAt
        );
    }

    /**
     * Get the maximum possible score.
     */
    public static int getMaxScore() {
        return MAX_HIT_POINTS;
    }
}
