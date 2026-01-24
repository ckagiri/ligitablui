package com.ligitabl.domain.model.contest;

import com.ligitabl.domain.model.seasonprediction.SeasonPredictionId;
import com.ligitabl.domain.model.user.UserId;

import java.time.Instant;
import java.util.Objects;

/**
 * Aggregate root representing a user's entry into the main contest.
 *
 * <p>A contest entry is automatically created when a user first submits their season prediction.
 * This represents the user's participation in the main season-long competition.</p>
 *
 * <p><strong>Business Rules:</strong></p>
 * <ul>
 *   <li>One entry per user per contest (enforced by repository unique constraint)</li>
 *   <li>Automatically created on first season prediction submission</li>
 *   <li>Links to the user's season prediction for scoring</li>
 *   <li>Once created, entry persists for the entire contest/season</li>
 * </ul>
 *
 * <p>Example usage:</p>
 * <pre>
 * MainContestEntry entry = MainContestEntry.create(
 *     ContestEntryId.generate(),
 *     userId,
 *     mainContestId,
 *     seasonPredictionId,
 *     Instant.now()
 * );
 * </pre>
 *
 * <p><strong>Lifecycle:</strong></p>
 * <ol>
 *   <li>User submits first season prediction (POST /seasonprediction)</li>
 *   <li>CreateSeasonPredictionUseCase auto-creates MainContestEntry</li>
 *   <li>Entry persists throughout season</li>
 *   <li>Used for scoring and leaderboard calculations</li>
 * </ol>
 */
public class MainContestEntry {
    private final ContestEntryId id;
    private final UserId userId;
    private final ContestId contestId;
    private final SeasonPredictionId seasonPredictionId;
    private final Instant joinedAt;

    private MainContestEntry(
        ContestEntryId id,
        UserId userId,
        ContestId contestId,
        SeasonPredictionId seasonPredictionId, // nullable - may not have season prediction yet
        Instant joinedAt
    ) {
        this.id = Objects.requireNonNull(id, "id cannot be null");
        this.userId = Objects.requireNonNull(userId, "userId cannot be null");
        this.contestId = Objects.requireNonNull(contestId, "contestId cannot be null");
        this.seasonPredictionId = seasonPredictionId; // nullable
        this.joinedAt = Objects.requireNonNull(joinedAt, "joinedAt cannot be null");
    }

    /**
     * Factory method to create a new main contest entry.
     *
     * <p>This is typically called automatically when a user first submits their season prediction.</p>
     *
     * @param id unique entry identifier
     * @param userId the user joining the contest
     * @param contestId the contest being joined (typically the main contest)
     * @param seasonPredictionId reference to the user's season prediction
     * @param joinedAt timestamp of contest entry creation
     * @return a new MainContestEntry instance
     * @throws NullPointerException if any parameter is null
     */
    public static MainContestEntry create(
        ContestEntryId id,
        UserId userId,
        ContestId contestId,
        SeasonPredictionId seasonPredictionId,
        Instant joinedAt
    ) {
        Objects.requireNonNull(id, "ContestEntryId cannot be null");
        Objects.requireNonNull(userId, "UserId cannot be null");
        Objects.requireNonNull(contestId, "ContestId cannot be null");
        Objects.requireNonNull(seasonPredictionId, "SeasonPredictionId cannot be null");
        Objects.requireNonNull(joinedAt, "Joined timestamp cannot be null");

        return new MainContestEntry(id, userId, contestId, seasonPredictionId, joinedAt);
    }

    /**
     * Convenience factory method to create a contest entry without a season prediction link.
     *
     * <p>Used when a user joins the contest via round predictions before making a season prediction.
     * The season prediction can be linked later when the user makes one.</p>
     *
     * @param userId the user joining the contest
     * @param contestId the contest being joined
     * @return a new MainContestEntry instance with generated ID and current timestamp
     */
    public static MainContestEntry createWithoutSeasonPrediction(UserId userId, ContestId contestId) {
        Objects.requireNonNull(userId, "UserId cannot be null");
        Objects.requireNonNull(contestId, "ContestId cannot be null");

        return new MainContestEntry(
            ContestEntryId.generate(),
            userId,
            contestId,
            null, // No season prediction yet
            Instant.now()
        );
    }

    /**
     * Check if this entry belongs to a specific user.
     *
     * @param userId the user ID to check
     * @return true if this entry belongs to the specified user
     */
    public boolean belongsToUser(UserId userId) {
        return this.userId.equals(userId);
    }

    /**
     * Check if this entry is for a specific contest.
     *
     * @param contestId the contest ID to check
     * @return true if this entry is for the specified contest
     */
    public boolean isForContest(ContestId contestId) {
        return this.contestId.equals(contestId);
    }

    // Getters

    public ContestEntryId getId() {
        return id;
    }

    public UserId getUserId() {
        return userId;
    }

    public ContestId getContestId() {
        return contestId;
    }

    public SeasonPredictionId getSeasonPredictionId() {
        return seasonPredictionId;
    }

    public Instant getJoinedAt() {
        return joinedAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MainContestEntry that = (MainContestEntry) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "MainContestEntry{" +
                "id=" + id +
                ", userId=" + userId +
                ", contestId=" + contestId +
                ", seasonPredictionId=" + seasonPredictionId +
                ", joinedAt=" + joinedAt +
                '}';
    }
}
