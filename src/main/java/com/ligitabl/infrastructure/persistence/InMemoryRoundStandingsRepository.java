package com.ligitabl.infrastructure.persistence;

import com.ligitabl.domain.model.season.RoundNumber;
import com.ligitabl.domain.model.season.SeasonId;
import com.ligitabl.domain.model.seasonprediction.TeamRanking;
import com.ligitabl.domain.repository.RoundStandingsRepository;
import org.springframework.stereotype.Repository;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory implementation of RoundStandingsRepository.
 *
 * <p>Stores actual league standings for each round in a season.
 * Used as the second tier in the fallback hierarchy (after user prediction).</p>
 */
@Repository
public class InMemoryRoundStandingsRepository implements RoundStandingsRepository {

    // Storage: seasonId+roundNumber -> List<TeamRanking>
    private final Map<String, List<TeamRanking>> standingsBySeasonAndRound = new ConcurrentHashMap<>();

    // Track latest round per season: seasonId -> roundNumber
    private final Map<String, RoundNumber> latestRoundBySeason = new ConcurrentHashMap<>();

    @Override
    public Optional<List<TeamRanking>> findBySeasonIdAndRoundNumber(SeasonId seasonId, RoundNumber roundNumber) {
        Objects.requireNonNull(seasonId, "SeasonId cannot be null");
        Objects.requireNonNull(roundNumber, "RoundNumber cannot be null");

        String key = buildKey(seasonId, roundNumber);
        List<TeamRanking> standings = standingsBySeasonAndRound.get(key);

        return standings != null ? Optional.of(List.copyOf(standings)) : Optional.empty();
    }

    @Override
    public Optional<List<TeamRanking>> findLatestBySeasonId(SeasonId seasonId) {
        Objects.requireNonNull(seasonId, "SeasonId cannot be null");

        RoundNumber latestRound = latestRoundBySeason.get(seasonId.value());

        if (latestRound == null) {
            return Optional.empty();
        }

        return findBySeasonIdAndRoundNumber(seasonId, latestRound);
    }

    /**
     * Save round standings.
     *
     * @param seasonId the season ID
     * @param roundNumber the round number
     * @param rankings the team rankings for this round
     */
    public void save(SeasonId seasonId, RoundNumber roundNumber, List<TeamRanking> rankings) {
        Objects.requireNonNull(seasonId, "SeasonId cannot be null");
        Objects.requireNonNull(roundNumber, "RoundNumber cannot be null");
        Objects.requireNonNull(rankings, "Rankings cannot be null");

        if (rankings.size() != 20) {
            throw new IllegalArgumentException("Round standings must have exactly 20 teams");
        }

        String key = buildKey(seasonId, roundNumber);
        standingsBySeasonAndRound.put(key, List.copyOf(rankings));

        // Update latest round if this is newer
        RoundNumber currentLatest = latestRoundBySeason.get(seasonId.value());
        if (currentLatest == null || roundNumber.value() > currentLatest.value()) {
            latestRoundBySeason.put(seasonId.value(), roundNumber);
        }
    }

    /**
     * Build composite key for season+round.
     *
     * @param seasonId the season ID
     * @param roundNumber the round number
     * @return composite key string
     */
    private String buildKey(SeasonId seasonId, RoundNumber roundNumber) {
        return seasonId.value() + ":" + roundNumber.value();
    }

    /**
     * Clear all data (useful for testing).
     */
    public void clear() {
        standingsBySeasonAndRound.clear();
        latestRoundBySeason.clear();
    }

    /**
     * Get total count of stored round standings (useful for testing/monitoring).
     *
     * @return total number of stored round standings
     */
    public int count() {
        return standingsBySeasonAndRound.size();
    }
}
