package com.ligitabl.domain.model.seasonprediction;

import com.ligitabl.domain.model.team.TeamId;

import java.util.Objects;

/**
 * Entity representing a team's position in a ranking list.
 *
 * <p>This is a core domain entity used in:</p>
 * <ul>
 *   <li>Season predictions - user's predicted final standings</li>
 *   <li>Round standings - actual results at a specific round</li>
 *   <li>Season baseline - default starting rankings</li>
 * </ul>
 *
 * <p>Business rules:</p>
 * <ul>
 *   <li>Position must be between 1 and 20 (Premier League has 20 teams)</li>
 *   <li>Each ranking list must have unique positions (no duplicates)</li>
 *   <li>TeamId and position are required</li>
 * </ul>
 *
 * <p>Example usage:</p>
 * <pre>
 * TeamRanking ranking = TeamRanking.create(TeamId.of("..."), 1);
 * TeamRanking updated = ranking.withPosition(5); // Immutable update
 * </pre>
 */
public class TeamRanking {
    private static final int MIN_POSITION = 1;
    private static final int MAX_POSITION = 20;

    private final TeamId teamId;
    private final int position;

    private TeamRanking(TeamId teamId, int position) {
        this.teamId = teamId;
        this.position = position;
    }

    /**
     * Factory method to create a TeamRanking.
     *
     * @param teamId the team's unique identifier
     * @param position the team's position in the rankings (1-20)
     * @return a new TeamRanking instance
     * @throws IllegalArgumentException if teamId is null or position is out of range
     */
    public static TeamRanking create(TeamId teamId, int position) {
        Objects.requireNonNull(teamId, "TeamId cannot be null");
        validatePosition(position);
        return new TeamRanking(teamId, position);
    }

    /**
     * Create a new TeamRanking with an updated position.
     * This is an immutable operation that returns a new instance.
     *
     * @param newPosition the new position (1-20)
     * @return a new TeamRanking with the updated position
     * @throws IllegalArgumentException if position is out of range
     */
    public TeamRanking withPosition(int newPosition) {
        validatePosition(newPosition);
        return new TeamRanking(this.teamId, newPosition);
    }

    /**
     * Validate that position is within valid range (1-20).
     *
     * @param position the position to validate
     * @throws IllegalArgumentException if position is out of range
     */
    private static void validatePosition(int position) {
        if (position < MIN_POSITION || position > MAX_POSITION) {
            throw new IllegalArgumentException(
                "Position must be between " + MIN_POSITION + " and " + MAX_POSITION +
                ". Provided: " + position
            );
        }
    }

    /**
     * Check if this team is ranked first.
     *
     * @return true if position is 1
     */
    public boolean isFirst() {
        return position == MIN_POSITION;
    }

    /**
     * Check if this team is ranked last.
     *
     * @return true if position is 20
     */
    public boolean isLast() {
        return position == MAX_POSITION;
    }

    /**
     * Check if this team is in the top 4 (Champions League positions).
     *
     * @return true if position is 1-4
     */
    public boolean isChampionsLeague() {
        return position <= 4;
    }

    /**
     * Check if this team is in relegation zone (bottom 3).
     *
     * @return true if position is 18-20
     */
    public boolean isRelegationZone() {
        return position >= 18;
    }

    // Getters

    public TeamId teamId() {
        return teamId;
    }

    public int position() {
        return position;
    }

    // Equals and hashCode

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TeamRanking that = (TeamRanking) o;
        return position == that.position && Objects.equals(teamId, that.teamId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(teamId, position);
    }

    @Override
    public String toString() {
        return "TeamRanking{" +
                "teamId=" + teamId +
                ", position=" + position +
                '}';
    }
}
