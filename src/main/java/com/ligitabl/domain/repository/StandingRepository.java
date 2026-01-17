package com.ligitabl.domain.repository;

import com.ligitabl.domain.model.season.RoundNumber;
import com.ligitabl.domain.model.season.SeasonId;
import com.ligitabl.domain.model.standing.TeamStanding;

import java.util.List;
import java.util.Map;

/**
 * Repository interface for league standings data access.
 *
 * <p>This is a port in the hexagonal architecture - implementations
 * live in the infrastructure layer.</p>
 */
public interface StandingRepository {

    /**
     * Find all standings for a season and round.
     *
     * @param seasonId the season identifier
     * @param round the round number
     * @return list of team standings sorted by position
     */
    List<TeamStanding> findBySeasonAndRound(SeasonId seasonId, RoundNumber round);

    /**
     * Find current standings for the active season.
     *
     * @return list of team standings sorted by position
     */
    List<TeamStanding> findCurrent();

    /**
     * Get a map of team code to position.
     *
     * @param seasonId the season identifier
     * @param round the round number
     * @return map of team code to league position
     */
    Map<String, Integer> findPositionMap(SeasonId seasonId, RoundNumber round);

    /**
     * Get a map of team code to points.
     *
     * @param seasonId the season identifier
     * @param round the round number
     * @return map of team code to points
     */
    Map<String, Integer> findPointsMap(SeasonId seasonId, RoundNumber round);

    /**
     * Get the current position map for the active season.
     *
     * @return map of team code to league position
     */
    Map<String, Integer> findCurrentPositionMap();

    /**
     * Get the current points map for the active season.
     *
     * @return map of team code to points
     */
    Map<String, Integer> findCurrentPointsMap();
}
