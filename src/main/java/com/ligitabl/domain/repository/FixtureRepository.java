package com.ligitabl.domain.repository;

import com.ligitabl.domain.model.prediction.Fixture;
import com.ligitabl.domain.model.season.RoundNumber;

import java.util.List;
import java.util.Map;

/**
 * Repository interface for fixture data access.
 *
 * <p>This is a port in the hexagonal architecture - implementations
 * live in the infrastructure layer.</p>
 */
public interface FixtureRepository {

    /**
     * Find fixtures for all teams in a round.
     *
     * @param round the round number
     * @return map of team code to list of fixtures
     */
    Map<String, List<Fixture>> findByRound(RoundNumber round);

    /**
     * Find fixtures for a specific team in a round.
     *
     * @param teamCode the team code
     * @param round the round number
     * @return list of fixtures for the team
     */
    List<Fixture> findByTeamAndRound(String teamCode, RoundNumber round);
}
