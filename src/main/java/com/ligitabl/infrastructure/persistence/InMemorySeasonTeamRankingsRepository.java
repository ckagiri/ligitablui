package com.ligitabl.infrastructure.persistence;

import com.ligitabl.domain.model.season.SeasonId;
import com.ligitabl.domain.model.seasonprediction.TeamRanking;
import com.ligitabl.domain.repository.SeasonTeamRankingsRepository;
import org.springframework.stereotype.Repository;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory implementation of SeasonTeamRankingsRepository.
 *
 * <p>Stores baseline/default team rankings for each season.
 * Used as the final fallback in the hierarchy (guaranteed to exist).</p>
 *
 * <p>IMPORTANT: Every active season MUST have baseline rankings.
 * This is a game invariant enforced by DataInitializer.</p>
 */
@Repository
public class InMemorySeasonTeamRankingsRepository implements SeasonTeamRankingsRepository {

    // Storage: seasonId -> List<TeamRanking>
    private final Map<String, List<TeamRanking>> baselineBySeasonId = new ConcurrentHashMap<>();

    @Override
    public Optional<List<TeamRanking>> findBySeasonId(SeasonId seasonId) {
        Objects.requireNonNull(seasonId, "SeasonId cannot be null");

        List<TeamRanking> baseline = baselineBySeasonId.get(seasonId.value());
        return baseline != null ? Optional.of(List.copyOf(baseline)) : Optional.empty();
    }

    @Override
    public void save(SeasonId seasonId, List<TeamRanking> rankings) {
        Objects.requireNonNull(seasonId, "SeasonId cannot be null");
        Objects.requireNonNull(rankings, "Rankings cannot be null");

        if (rankings.size() != 20) {
            throw new IllegalArgumentException(
                "Season baseline rankings must have exactly 20 teams. Provided: " + rankings.size()
            );
        }

        // Validate positions are 1-20
        Set<Integer> positions = rankings.stream()
            .map(TeamRanking::position)
            .collect(java.util.stream.Collectors.toSet());

        if (positions.size() != 20) {
            throw new IllegalArgumentException("Duplicate positions found in baseline rankings");
        }

        for (int i = 1; i <= 20; i++) {
            if (!positions.contains(i)) {
                throw new IllegalArgumentException("Missing position " + i + " in baseline rankings");
            }
        }

        baselineBySeasonId.put(seasonId.value(), List.copyOf(rankings));
    }

    @Override
    public boolean existsBySeasonId(SeasonId seasonId) {
        Objects.requireNonNull(seasonId, "SeasonId cannot be null");
        return baselineBySeasonId.containsKey(seasonId.value());
    }

    /**
     * Clear all data (useful for testing).
     */
    public void clear() {
        baselineBySeasonId.clear();
    }

    /**
     * Get total count of seasons with baselines (useful for testing/monitoring).
     *
     * @return total number of seasons with baseline rankings
     */
    public int count() {
        return baselineBySeasonId.size();
    }
}
