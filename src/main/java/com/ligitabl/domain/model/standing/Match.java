package com.ligitabl.domain.model.standing;

import java.util.Objects;
import java.util.Optional;

/**
 * Domain entity representing a football match.
 *
 * <p>Contains match details including teams, scores, and status.</p>
 */
public class Match {

    private final String homeTeam;
    private final String awayTeam;
    private final String homeScore;
    private final String awayScore;
    private final String kickOff;
    private final MatchStatus status;
    private final String matchTime;

    private Match(
        String homeTeam,
        String awayTeam,
        String homeScore,
        String awayScore,
        String kickOff,
        MatchStatus status,
        String matchTime
    ) {
        this.homeTeam = Objects.requireNonNull(homeTeam, "homeTeam is required");
        this.awayTeam = Objects.requireNonNull(awayTeam, "awayTeam is required");
        this.homeScore = homeScore;
        this.awayScore = awayScore;
        this.kickOff = kickOff;
        this.status = Objects.requireNonNull(status, "status is required");
        this.matchTime = matchTime;
    }

    /**
     * Factory method to create a match.
     */
    public static Match create(
        String homeTeam,
        String awayTeam,
        String homeScore,
        String awayScore,
        String kickOff,
        String statusStr,
        String matchTime
    ) {
        MatchStatus status = MatchStatus.fromString(statusStr);
        return new Match(homeTeam, awayTeam, homeScore, awayScore, kickOff, status, matchTime);
    }

    /**
     * Factory method for scheduled matches.
     */
    public static Match scheduled(String homeTeam, String awayTeam, String kickOff) {
        return new Match(homeTeam, awayTeam, "", "", kickOff, MatchStatus.SCHEDULED, null);
    }

    /**
     * Factory method for finished matches.
     */
    public static Match finished(String homeTeam, String awayTeam, String homeScore, String awayScore, String kickOff) {
        return new Match(homeTeam, awayTeam, homeScore, awayScore, kickOff, MatchStatus.FINISHED, null);
    }

    /**
     * Factory method for live matches.
     */
    public static Match live(String homeTeam, String awayTeam, String homeScore, String awayScore, String matchTime) {
        return new Match(homeTeam, awayTeam, homeScore, awayScore, null, MatchStatus.LIVE, matchTime);
    }

    // Getters
    public String getHomeTeam() { return homeTeam; }
    public String getAwayTeam() { return awayTeam; }
    public String getHomeScore() { return homeScore; }
    public String getAwayScore() { return awayScore; }
    public String getKickOff() { return kickOff; }
    public MatchStatus getStatus() { return status; }
    public Optional<String> getMatchTime() { return Optional.ofNullable(matchTime); }

    /**
     * Get status as string for backward compatibility.
     */
    public String getStatusString() {
        return status.name();
    }

    /**
     * Check if the match is currently live.
     */
    public boolean isLive() {
        return status == MatchStatus.LIVE;
    }

    /**
     * Check if the match has finished.
     */
    public boolean isFinished() {
        return status == MatchStatus.FINISHED;
    }

    /**
     * Check if the match is scheduled.
     */
    public boolean isScheduled() {
        return status == MatchStatus.SCHEDULED;
    }

    /**
     * Get the home team score as integer, or null if not available.
     */
    public Integer getHomeScoreInt() {
        try {
            return homeScore != null && !homeScore.isEmpty() ? Integer.parseInt(homeScore) : null;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * Get the away team score as integer, or null if not available.
     */
    public Integer getAwayScoreInt() {
        try {
            return awayScore != null && !awayScore.isEmpty() ? Integer.parseInt(awayScore) : null;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Match match = (Match) o;
        return homeTeam.equals(match.homeTeam) && awayTeam.equals(match.awayTeam);
    }

    @Override
    public int hashCode() {
        return Objects.hash(homeTeam, awayTeam);
    }

    @Override
    public String toString() {
        return "Match{" +
            homeTeam + " " + homeScore + " - " + awayScore + " " + awayTeam +
            " (" + status + ")" +
            '}';
    }

    /**
     * Match status enum.
     */
    public enum MatchStatus {
        SCHEDULED,
        LIVE,
        FINISHED,
        POSTPONED,
        CANCELLED;

        public static MatchStatus fromString(String value) {
            if (value == null || value.isBlank()) {
                return SCHEDULED;
            }
            try {
                return MatchStatus.valueOf(value.toUpperCase());
            } catch (IllegalArgumentException e) {
                return SCHEDULED;
            }
        }
    }
}
