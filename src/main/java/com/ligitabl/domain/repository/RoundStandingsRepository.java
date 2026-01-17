package com.ligitabl.domain.repository;

import com.ligitabl.domain.model.season.RoundNumber;
import com.ligitabl.domain.model.season.SeasonId;
import com.ligitabl.domain.model.seasonprediction.TeamRanking;

import java.util.List;
import java.util.Optional;

/**
 * Repository interface for Round Standings.
 *
 * <p>This is a domain interface (port) that will be implemented by the infrastructure layer (adapter).
 * Round standings represent the actual league table at a specific round in the season.</p>
 *
 * <p>Used in the smart fallback hierarchy:
 * User Prediction → Round Standings → Season Baseline</p>
 */
public interface RoundStandingsRepository {

    /**
     * Find round standings by season ID and round number.
     *
     * <p>Returns the actual league standings (team rankings) for a specific round.
     * Used as fallback when user has no prediction.</p>
     *
     * @param seasonId the season ID
     * @param roundNumber the round number
     * @return an Optional containing the list of 20 team rankings if found, empty otherwise
     */
    Optional<List<TeamRanking>> findBySeasonIdAndRoundNumber(SeasonId seasonId, RoundNumber roundNumber);

    /**
     * Find the most recent round standings for a season.
     *
     * <p>Returns standings for the latest completed round in the season.
     * Useful when current round hasn't finished yet.</p>
     *
     * @param seasonId the season ID
     * @return an Optional containing the list of 20 team rankings if found, empty otherwise
     */
    Optional<List<TeamRanking>> findLatestBySeasonId(SeasonId seasonId);
}
