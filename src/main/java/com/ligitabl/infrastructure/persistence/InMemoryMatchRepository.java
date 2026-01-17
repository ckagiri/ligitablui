package com.ligitabl.infrastructure.persistence;

import com.ligitabl.domain.model.season.RoundNumber;
import com.ligitabl.domain.model.season.SeasonId;
import com.ligitabl.domain.model.standing.Match;
import com.ligitabl.domain.repository.MatchRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * In-memory implementation of MatchRepository.
 *
 * <p>Returns stub data for demonstration purposes.
 * In production, this would be replaced with a database-backed implementation.</p>
 */
@Repository
public class InMemoryMatchRepository implements MatchRepository {

    // Stub matches data
    private static final List<Match> STUB_MATCHES = List.of(
        Match.create("Manchester City", "Arsenal", "2", "1", "Sat, Dec 28, 12:30", "FINISHED", null),
        Match.create("Liverpool", "Aston Villa", "3", "3", "Sat, Dec 28, 15:00", "FINISHED", null),
        Match.create("Chelsea", "Tottenham", "2", "1", "Sat, Dec 28, 17:30", "LIVE", "67'"),
        Match.create("Newcastle", "Man United", "", "", "Sun, Dec 29, 14:00", "SCHEDULED", null),
        Match.create("West Ham", "Brighton", "", "", "Sun, Dec 29, 16:30", "SCHEDULED", null)
    );

    @Override
    public List<Match> findBySeasonAndRound(SeasonId seasonId, RoundNumber round) {
        // For stub, return same matches regardless of season/round
        return STUB_MATCHES;
    }

    @Override
    public List<Match> findCurrentRoundMatches() {
        return STUB_MATCHES;
    }

    @Override
    public List<Match> findLiveMatches() {
        return STUB_MATCHES.stream()
            .filter(Match::isLive)
            .toList();
    }

    @Override
    public List<Match> findByStatus(Match.MatchStatus status) {
        return STUB_MATCHES.stream()
            .filter(match -> match.getStatus() == status)
            .toList();
    }
}
