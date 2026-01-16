package com.ligitabl.domain.model.seasonprediction;

import com.ligitabl.domain.exception.MultipleSwapException;
import com.ligitabl.domain.model.team.TeamId;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Value object representing a single team swap operation.
 *
 * <p><strong>CRITICAL BUSINESS RULE:</strong> Existing participants can only swap ONE pair of teams per request.
 * This value object enforces that constraint through validation.</p>
 *
 * <p>A swap pair consists of:</p>
 * <ul>
 *   <li>Team A: teamId and current position</li>
 *   <li>Team B: teamId and current position</li>
 * </ul>
 *
 * <p>Example usage:</p>
 * <pre>
 * // Direct construction
 * SwapPair swap = SwapPair.create(
 *     teamAId, 3,  // Team A at position 3
 *     teamBId, 7   // Team B at position 7
 * );
 *
 * // Validation from full rankings change (CRITICAL)
 * SwapPair swap = SwapPair.fromRankingsChange(oldRankings, newRankings);
 * // Throws MultipleSwapException if more than one pair changed
 * </pre>
 *
 * <p>The {@link #fromRankingsChange(List, List)} factory method is critical for the
 * POST /seasonprediction/swap endpoint to reject requests with multiple swaps.</p>
 */
public record SwapPair(
    TeamId teamAId,
    int teamACurrentPosition,
    TeamId teamBId,
    int teamBCurrentPosition
) {
    /**
     * Compact constructor with validation.
     *
     * @throws IllegalArgumentException if any parameter is invalid
     */
    public SwapPair {
        Objects.requireNonNull(teamAId, "Team A ID cannot be null");
        Objects.requireNonNull(teamBId, "Team B ID cannot be null");

        validatePosition(teamACurrentPosition, "Team A");
        validatePosition(teamBCurrentPosition, "Team B");

        if (teamAId.equals(teamBId)) {
            throw new IllegalArgumentException("Cannot swap a team with itself");
        }

        if (teamACurrentPosition == teamBCurrentPosition) {
            throw new IllegalArgumentException(
                "Teams cannot be in the same position. Team A: " + teamACurrentPosition +
                ", Team B: " + teamBCurrentPosition
            );
        }
    }

    /**
     * Factory method to create a SwapPair.
     *
     * @param teamAId team A's identifier
     * @param teamACurrentPosition team A's current position (1-20)
     * @param teamBId team B's identifier
     * @param teamBCurrentPosition team B's current position (1-20)
     * @return a validated SwapPair instance
     * @throws IllegalArgumentException if validation fails
     */
    public static SwapPair create(
        TeamId teamAId,
        int teamACurrentPosition,
        TeamId teamBId,
        int teamBCurrentPosition
    ) {
        return new SwapPair(teamAId, teamACurrentPosition, teamBId, teamBCurrentPosition);
    }

    /**
     * CRITICAL: Validate that only ONE pair of teams changed positions.
     *
     * <p>This factory method enforces the single-swap business rule by:</p>
     * <ol>
     *   <li>Comparing old and new rankings to detect position changes</li>
     *   <li>Counting how many teams have different positions</li>
     *   <li>Rejecting if count != 2 (one swap = exactly 2 teams change)</li>
     * </ol>
     *
     * <p><strong>Use case:</strong> POST /seasonprediction/swap endpoint receives full rankings
     * from client. This method validates that only one pair was swapped, preventing
     * multiple simultaneous swaps which violate the business rule.</p>
     *
     * @param oldRankings the previous ranking list
     * @param newRankings the proposed new ranking list
     * @return a validated SwapPair representing the single swap
     * @throws MultipleSwapException if more or fewer than 2 teams changed positions
     * @throws IllegalArgumentException if rankings are invalid or don't match
     */
    public static SwapPair fromRankingsChange(
        List<TeamRanking> oldRankings,
        List<TeamRanking> newRankings
    ) {
        Objects.requireNonNull(oldRankings, "Old rankings cannot be null");
        Objects.requireNonNull(newRankings, "New rankings cannot be null");

        if (oldRankings.size() != 20 || newRankings.size() != 20) {
            throw new IllegalArgumentException(
                "Rankings must contain exactly 20 teams. Old: " + oldRankings.size() +
                ", New: " + newRankings.size()
            );
        }

        // Create maps for quick lookup: teamId -> position
        Map<TeamId, Integer> oldPositions = oldRankings.stream()
            .collect(Collectors.toMap(TeamRanking::teamId, TeamRanking::position));

        Map<TeamId, Integer> newPositions = newRankings.stream()
            .collect(Collectors.toMap(TeamRanking::teamId, TeamRanking::position));

        // Detect position changes
        List<PositionChange> changes = new ArrayList<>();
        for (TeamId teamId : oldPositions.keySet()) {
            Integer oldPos = oldPositions.get(teamId);
            Integer newPos = newPositions.get(teamId);

            if (newPos == null) {
                throw new IllegalArgumentException(
                    "Team " + teamId + " exists in old rankings but not in new rankings"
                );
            }

            if (!oldPos.equals(newPos)) {
                changes.add(new PositionChange(teamId, oldPos, newPos));
            }
        }

        // CRITICAL: Enforce single swap rule
        if (changes.size() != 2) {
            throw new MultipleSwapException(
                "Only one swap allowed per request. Found " + changes.size() +
                " position changes. Existing participants can only swap ONE pair of teams."
            );
        }

        // Extract the two teams that swapped
        PositionChange change1 = changes.get(0);
        PositionChange change2 = changes.get(1);

        // Validate that it's truly a swap (positions exchanged)
        if (!change1.oldPosition().equals(change2.newPosition()) ||
            !change2.oldPosition().equals(change1.newPosition())) {
            throw new IllegalArgumentException(
                "Position changes do not represent a valid swap. " +
                "Team " + change1.teamId() + ": " + change1.oldPosition() + " -> " + change1.newPosition() + ", " +
                "Team " + change2.teamId() + ": " + change2.oldPosition() + " -> " + change2.newPosition()
            );
        }

        return new SwapPair(
            change1.teamId(), change1.oldPosition(),
            change2.teamId(), change2.oldPosition()
        );
    }

    /**
     * Validate position is in valid range (1-20).
     */
    private static void validatePosition(int position, String teamLabel) {
        if (position < 1 || position > 20) {
            throw new IllegalArgumentException(
                teamLabel + " position must be between 1 and 20. Provided: " + position
            );
        }
    }

    /**
     * Check if this swap involves a specific team.
     *
     * @param teamId the team to check
     * @return true if this swap involves the specified team
     */
    public boolean involvesTeam(TeamId teamId) {
        return this.teamAId.equals(teamId) || this.teamBId.equals(teamId);
    }

    /**
     * Get the other team in the swap.
     *
     * @param teamId one of the teams in the swap
     * @return the other team's ID
     * @throws IllegalArgumentException if teamId is not part of this swap
     */
    public TeamId getOtherTeam(TeamId teamId) {
        if (this.teamAId.equals(teamId)) {
            return this.teamBId;
        } else if (this.teamBId.equals(teamId)) {
            return this.teamAId;
        } else {
            throw new IllegalArgumentException("Team " + teamId + " is not part of this swap");
        }
    }

    /**
     * Internal record to track position changes.
     */
    private record PositionChange(TeamId teamId, Integer oldPosition, Integer newPosition) {}
}
