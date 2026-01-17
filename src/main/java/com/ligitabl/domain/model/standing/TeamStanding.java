package com.ligitabl.domain.model.standing;

import java.util.Objects;

/**
 * Domain entity representing a team's standing in the league table.
 *
 * <p>Contains all statistical information about a team's performance.</p>
 */
public class TeamStanding {

    private final int position;
    private final String teamCode;
    private final String teamName;
    private final String crestUrl;
    private final int played;
    private final int won;
    private final int drawn;
    private final int lost;
    private final int points;
    private final int goalsFor;
    private final int goalsAgainst;
    private final int goalDifference;

    private TeamStanding(
        int position,
        String teamCode,
        String teamName,
        String crestUrl,
        int played,
        int won,
        int drawn,
        int lost,
        int points,
        int goalsFor,
        int goalsAgainst,
        int goalDifference
    ) {
        this.position = position;
        this.teamCode = Objects.requireNonNull(teamCode, "teamCode is required");
        this.teamName = Objects.requireNonNull(teamName, "teamName is required");
        this.crestUrl = crestUrl;
        this.played = played;
        this.won = won;
        this.drawn = drawn;
        this.lost = lost;
        this.points = points;
        this.goalsFor = goalsFor;
        this.goalsAgainst = goalsAgainst;
        this.goalDifference = goalDifference;
    }

    /**
     * Factory method to create a team standing.
     */
    public static TeamStanding create(
        int position,
        String teamCode,
        String teamName,
        String crestUrl,
        int played,
        int won,
        int drawn,
        int lost,
        int points,
        int goalsFor,
        int goalsAgainst,
        int goalDifference
    ) {
        if (position < 1 || position > 20) {
            throw new IllegalArgumentException("Position must be between 1 and 20");
        }
        if (played < 0 || won < 0 || drawn < 0 || lost < 0) {
            throw new IllegalArgumentException("Match counts cannot be negative");
        }
        if (won + drawn + lost != played) {
            throw new IllegalArgumentException("Won + drawn + lost must equal played");
        }
        return new TeamStanding(
            position, teamCode, teamName, crestUrl, played,
            won, drawn, lost, points, goalsFor, goalsAgainst, goalDifference
        );
    }

    // Getters
    public int getPosition() { return position; }
    public String getTeamCode() { return teamCode; }
    public String getTeamName() { return teamName; }
    public String getCrestUrl() { return crestUrl; }
    public int getPlayed() { return played; }
    public int getWon() { return won; }
    public int getDrawn() { return drawn; }
    public int getLost() { return lost; }
    public int getPoints() { return points; }
    public int getGoalsFor() { return goalsFor; }
    public int getGoalsAgainst() { return goalsAgainst; }
    public int getGoalDifference() { return goalDifference; }

    /**
     * Check if team is in top 4 (Champions League places).
     */
    public boolean isInChampionsLeague() {
        return position <= 4;
    }

    /**
     * Check if team is in relegation zone.
     */
    public boolean isInRelegationZone() {
        return position >= 18;
    }

    /**
     * Check if team is in Europa League places.
     */
    public boolean isInEuropaLeague() {
        return position >= 5 && position <= 6;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TeamStanding that = (TeamStanding) o;
        return position == that.position && teamCode.equals(that.teamCode);
    }

    @Override
    public int hashCode() {
        return Objects.hash(position, teamCode);
    }

    @Override
    public String toString() {
        return "TeamStanding{" +
            "position=" + position +
            ", teamCode='" + teamCode + '\'' +
            ", teamName='" + teamName + '\'' +
            ", points=" + points +
            ", goalDifference=" + goalDifference +
            '}';
    }
}
