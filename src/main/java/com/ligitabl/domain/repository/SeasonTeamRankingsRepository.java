package com.ligitabl.domain.repository;

import com.ligitabl.domain.model.season.SeasonId;
import com.ligitabl.domain.model.seasonprediction.TeamRanking;

import java.util.List;
import java.util.Optional;

/**
 * Repository interface for Season Team Rankings (baseline).
 *
 * <p>This is a domain interface (port) that will be implemented by the infrastructure layer (adapter).
 * Season baseline rankings represent the default/initial team rankings for a season,
 * typically set at the start based on previous season finish or predictions.</p>
 *
 * <p>Used as the final fallback in the smart fallback hierarchy:
 * User Prediction → Round Standings → Season Baseline</p>
 *
 * <p>This is a guaranteed fallback - every season must have baseline rankings.</p>
 */
public interface SeasonTeamRankingsRepository {

    /**
     * Find season baseline rankings by season ID.
     *
     * <p>Returns the baseline/default rankings for a season.
     * Used as final fallback when user has no prediction and no round standings available.</p>
     *
     * <p>Business Rule: Every active season MUST have baseline rankings.
     * This is a game invariant.</p>
     *
     * @param seasonId the season ID
     * @return an Optional containing the list of 20 team rankings if found, empty otherwise
     */
    Optional<List<TeamRanking>> findBySeasonId(SeasonId seasonId);

    /**
     * Save season baseline rankings.
     *
     * <p>Typically called once when initializing a new season.
     * Can also be used to update rankings if needed.</p>
     *
     * @param seasonId the season ID
     * @param rankings the list of 20 team rankings
     */
    void save(SeasonId seasonId, List<TeamRanking> rankings);

    /**
     * Check if baseline rankings exist for a season.
     *
     * @param seasonId the season ID
     * @return true if baseline rankings exist, false otherwise
     */
    boolean existsBySeasonId(SeasonId seasonId);
}
