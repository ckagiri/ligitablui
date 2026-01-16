package com.ligitabl.domain.model.seasonprediction;

import com.ligitabl.domain.exception.InvalidSeasonPredictionException;
import com.ligitabl.domain.model.season.RoundNumber;
import com.ligitabl.domain.model.season.SeasonId;
import com.ligitabl.domain.model.team.TeamId;
import com.ligitabl.domain.model.user.UserId;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Aggregate root representing a user's season prediction.
 *
 * <p>A season prediction is the user's predicted final team rankings for the entire season.
 * It functions as a "current balance" that updates with each swap operation.</p>
 *
 * <p><strong>Business Rules:</strong></p>
 * <ul>
 *   <li>One prediction per user per season (enforced by repository unique constraint)</li>
 *   <li>Must contain exactly 20 teams (all Premier League teams)</li>
 *   <li>Positions must be 1-20 with no duplicates</li>
 *   <li>Existing participants can only swap ONE pair per request</li>
 *   <li>Tracks which round the prediction is valid for (atRound field)</li>
 * </ul>
 *
 * <p><strong>Key Operations:</strong></p>
 * <ul>
 *   <li>{@link #create} - Factory method for initial prediction creation</li>
 *   <li>{@link #swapTeams} - Swap exactly one pair of teams (immutable operation)</li>
 * </ul>
 *
 * <p>Example usage:</p>
 * <pre>
 * // Create initial prediction
 * SeasonPrediction prediction = SeasonPrediction.create(
 *     SeasonPredictionId.generate(),
 *     userId,
 *     seasonId,
 *     RoundNumber.of(1),
 *     initialRankings,
 *     Instant.now()
 * );
 *
 * // Perform a swap (returns new instance)
 * SwapPair swap = SwapPair.create(teamAId, 3, teamBId, 7);
 * SeasonPrediction updated = prediction.swapTeams(swap, RoundNumber.of(5), Instant.now());
 * </pre>
 */
public class SeasonPrediction {
    private static final int REQUIRED_TEAM_COUNT = 20;

    private final SeasonPredictionId id;
    private final UserId userId;
    private final SeasonId seasonId;
    private final RoundNumber atRound;
    private final List<TeamRanking> rankings;
    private final Instant createdAt;
    private final Instant updatedAt;

    private SeasonPrediction(
        SeasonPredictionId id,
        UserId userId,
        SeasonId seasonId,
        RoundNumber atRound,
        List<TeamRanking> rankings,
        Instant createdAt,
        Instant updatedAt
    ) {
        this.id = id;
        this.userId = userId;
        this.seasonId = seasonId;
        this.atRound = atRound;
        this.rankings = List.copyOf(rankings); // Immutable
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    /**
     * Factory method to create a new season prediction.
     *
     * <p>Validates all business rules:</p>
     * <ul>
     *   <li>Exactly 20 teams</li>
     *   <li>All positions 1-20 with no duplicates</li>
     *   <li>All teams are unique</li>
     * </ul>
     *
     * @param id unique prediction identifier
     * @param userId the user making the prediction
     * @param seasonId the season this prediction is for
     * @param atRound which round this prediction is valid for
     * @param rankings the predicted team rankings (must be 20 teams)
     * @param now the current timestamp
     * @return a new validated SeasonPrediction instance
     * @throws InvalidSeasonPredictionException if validation fails
     */
    public static SeasonPrediction create(
        SeasonPredictionId id,
        UserId userId,
        SeasonId seasonId,
        RoundNumber atRound,
        List<TeamRanking> rankings,
        Instant now
    ) {
        Objects.requireNonNull(id, "SeasonPredictionId cannot be null");
        Objects.requireNonNull(userId, "UserId cannot be null");
        Objects.requireNonNull(seasonId, "SeasonId cannot be null");
        Objects.requireNonNull(atRound, "RoundNumber cannot be null");
        Objects.requireNonNull(rankings, "Rankings cannot be null");
        Objects.requireNonNull(now, "Timestamp cannot be null");

        // Business Rule: Must have exactly 20 teams
        if (rankings.size() != REQUIRED_TEAM_COUNT) {
            throw new InvalidSeasonPredictionException(
                "Season prediction must contain exactly " + REQUIRED_TEAM_COUNT +
                " teams. Provided: " + rankings.size()
            );
        }

        // Business Rule: Positions must be 1-20 with no duplicates
        validatePositions(rankings);

        // Business Rule: All teams must be unique (no duplicate team IDs)
        validateUniqueTeams(rankings);

        return new SeasonPrediction(id, userId, seasonId, atRound, rankings, now, now);
    }

    /**
     * Swap exactly ONE pair of teams.
     *
     * <p><strong>CRITICAL:</strong> This enforces the single-swap business rule for existing
     * participants. The SwapPair parameter must be validated using
     * {@link SwapPair#fromRankingsChange} to ensure only one pair is swapped.</p>
     *
     * <p>This is an immutable operation that returns a new SeasonPrediction instance.</p>
     *
     * @param swapPair the validated swap pair
     * @param currentRound the current round number (for atRound tracking)
     * @param now the current timestamp
     * @return a new SeasonPrediction with the swap applied
     * @throws IllegalArgumentException if swap validation fails
     * @throws IllegalStateException if teams not found or positions don't match
     */
    public SeasonPrediction swapTeams(SwapPair swapPair, RoundNumber currentRound, Instant now) {
        Objects.requireNonNull(swapPair, "SwapPair cannot be null");
        Objects.requireNonNull(currentRound, "Current round cannot be null");
        Objects.requireNonNull(now, "Timestamp cannot be null");

        // Find the two teams to swap
        TeamRanking rankingA = findRankingByTeamId(swapPair.teamAId())
            .orElseThrow(() -> new IllegalStateException(
                "Team A (" + swapPair.teamAId() + ") not found in prediction"
            ));

        TeamRanking rankingB = findRankingByTeamId(swapPair.teamBId())
            .orElseThrow(() -> new IllegalStateException(
                "Team B (" + swapPair.teamBId() + ") not found in prediction"
            ));

        // Optimistic locking: Validate current positions match request
        if (rankingA.position() != swapPair.teamACurrentPosition()) {
            throw new IllegalStateException(
                "Team A position mismatch. Expected: " + swapPair.teamACurrentPosition() +
                ", Actual: " + rankingA.position() +
                ". Another update may have occurred."
            );
        }

        if (rankingB.position() != swapPair.teamBCurrentPosition()) {
            throw new IllegalStateException(
                "Team B position mismatch. Expected: " + swapPair.teamBCurrentPosition() +
                ", Actual: " + rankingB.position() +
                ". Another update may have occurred."
            );
        }

        // Perform the swap by creating new rankings with swapped positions
        List<TeamRanking> newRankings = rankings.stream()
            .map(ranking -> {
                if (ranking.teamId().equals(swapPair.teamAId())) {
                    return ranking.withPosition(rankingB.position());
                } else if (ranking.teamId().equals(swapPair.teamBId())) {
                    return ranking.withPosition(rankingA.position());
                }
                return ranking;
            })
            .sorted(Comparator.comparing(TeamRanking::position))
            .toList();

        return new SeasonPrediction(
            this.id,
            this.userId,
            this.seasonId,
            currentRound,  // Update atRound to current round
            newRankings,
            this.createdAt,
            now  // Update timestamp
        );
    }

    /**
     * Validate that all positions are 1-20 with no duplicates.
     */
    private static void validatePositions(List<TeamRanking> rankings) {
        Set<Integer> positions = rankings.stream()
            .map(TeamRanking::position)
            .collect(Collectors.toSet());

        if (positions.size() != REQUIRED_TEAM_COUNT) {
            throw new InvalidSeasonPredictionException(
                "Duplicate positions found. All positions from 1 to " + REQUIRED_TEAM_COUNT +
                " must be unique."
            );
        }

        for (int i = 1; i <= REQUIRED_TEAM_COUNT; i++) {
            if (!positions.contains(i)) {
                throw new InvalidSeasonPredictionException(
                    "Missing position " + i + ". All positions from 1 to " + REQUIRED_TEAM_COUNT +
                    " must be present."
                );
            }
        }
    }

    /**
     * Validate that all team IDs are unique (no duplicate teams).
     */
    private static void validateUniqueTeams(List<TeamRanking> rankings) {
        Set<TeamId> teamIds = rankings.stream()
            .map(TeamRanking::teamId)
            .collect(Collectors.toSet());

        if (teamIds.size() != REQUIRED_TEAM_COUNT) {
            throw new InvalidSeasonPredictionException(
                "Duplicate teams found. All " + REQUIRED_TEAM_COUNT + " teams must be unique."
            );
        }
    }

    /**
     * Find a ranking by team ID.
     */
    private Optional<TeamRanking> findRankingByTeamId(TeamId teamId) {
        return rankings.stream()
            .filter(r -> r.teamId().equals(teamId))
            .findFirst();
    }

    // Query methods

    /**
     * Get the ranking for a specific team.
     *
     * @param teamId the team to find
     * @return the team's ranking
     * @throws IllegalArgumentException if team not found
     */
    public TeamRanking getRankingForTeam(TeamId teamId) {
        return findRankingByTeamId(teamId)
            .orElseThrow(() -> new IllegalArgumentException("Team " + teamId + " not found"));
    }

    /**
     * Get the ranking at a specific position.
     *
     * @param position the position (1-20)
     * @return the team ranking at that position
     * @throws IllegalArgumentException if position not found
     */
    public TeamRanking getRankingAtPosition(int position) {
        return rankings.stream()
            .filter(r -> r.position() == position)
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException("No team at position " + position));
    }

    /**
     * Get all rankings as an immutable list, sorted by position.
     *
     * @return the complete ranking list
     */
    public List<TeamRanking> getRankings() {
        return rankings.stream()
            .sorted(Comparator.comparing(TeamRanking::position))
            .toList();
    }

    /**
     * Get the top N teams.
     *
     * @param n number of teams to return
     * @return the top N teams sorted by position
     */
    public List<TeamRanking> getTopN(int n) {
        return rankings.stream()
            .sorted(Comparator.comparing(TeamRanking::position))
            .limit(n)
            .toList();
    }

    /**
     * Get the bottom N teams.
     *
     * @param n number of teams to return
     * @return the bottom N teams sorted by position
     */
    public List<TeamRanking> getBottomN(int n) {
        return rankings.stream()
            .sorted(Comparator.comparing(TeamRanking::position).reversed())
            .limit(n)
            .toList();
    }

    // Getters

    public SeasonPredictionId getId() {
        return id;
    }

    public UserId getUserId() {
        return userId;
    }

    public SeasonId getSeasonId() {
        return seasonId;
    }

    public RoundNumber getAtRound() {
        return atRound;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SeasonPrediction that = (SeasonPrediction) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "SeasonPrediction{" +
                "id=" + id +
                ", userId=" + userId +
                ", seasonId=" + seasonId +
                ", atRound=" + atRound +
                ", teamsCount=" + rankings.size() +
                ", updatedAt=" + updatedAt +
                '}';
    }
}
