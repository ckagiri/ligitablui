package com.ligitabl.domain.repository;

import com.ligitabl.domain.model.season.RoundNumber;
import com.ligitabl.domain.model.season.SeasonId;
import com.ligitabl.domain.model.standing.Match;

import java.util.List;

/**
 * Repository interface for match data access.
 *
 * <p>This is a port in the hexagonal architecture - implementations
 * live in the infrastructure layer.</p>
 */
public interface MatchRepository {

    /**
     * Find all matches for a season and round.
     *
     * @param seasonId the season identifier
     * @param round the round number
     * @return list of matches
     */
    List<Match> findBySeasonAndRound(SeasonId seasonId, RoundNumber round);

    /**
     * Find matches for the current round.
     *
     * @return list of matches for current round
     */
    List<Match> findCurrentRoundMatches();

    /**
     * Find live matches.
     *
     * @return list of currently live matches
     */
    List<Match> findLiveMatches();

    /**
     * Find matches by status.
     *
     * @param status the match status
     * @return list of matches with the given status
     */
    List<Match> findByStatus(Match.MatchStatus status);
}
