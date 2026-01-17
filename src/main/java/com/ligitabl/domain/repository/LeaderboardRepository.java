package com.ligitabl.domain.repository;

import com.ligitabl.domain.model.leaderboard.LeaderboardEntry;
import com.ligitabl.domain.model.leaderboard.Phase;
import com.ligitabl.domain.model.leaderboard.UserDetail;
import com.ligitabl.domain.model.season.RoundNumber;
import com.ligitabl.domain.model.user.UserId;

import java.util.List;
import java.util.Optional;

/**
 * Repository interface for leaderboard data access.
 *
 * <p>This is a port in the hexagonal architecture - implementations
 * live in the infrastructure layer.</p>
 */
public interface LeaderboardRepository {

    /**
     * Find leaderboard entries for a specific phase.
     *
     * @param phase the scoring phase
     * @param page the page number (1-indexed)
     * @param pageSize the number of entries per page
     * @return list of leaderboard entries sorted by position
     */
    List<LeaderboardEntry> findByPhase(Phase phase, int page, int pageSize);

    /**
     * Find all leaderboard entries for a phase (no pagination).
     *
     * @param phase the scoring phase
     * @return list of all leaderboard entries sorted by position
     */
    List<LeaderboardEntry> findAllByPhase(Phase phase);

    /**
     * Find a specific user's position in the leaderboard.
     *
     * @param userId the user's ID
     * @param phase the scoring phase
     * @return the user's leaderboard entry, or empty if not found
     */
    Optional<LeaderboardEntry> findUserPosition(UserId userId, Phase phase);

    /**
     * Find detailed user information including their prediction.
     *
     * @param userId the user's ID
     * @param round the round number to get prediction for
     * @return the user's details, or empty if not found
     */
    Optional<UserDetail> findUserDetails(UserId userId, RoundNumber round);

    /**
     * Get the total number of entries in the leaderboard for a phase.
     *
     * @param phase the scoring phase
     * @return the total count of entries
     */
    int countByPhase(Phase phase);
}
