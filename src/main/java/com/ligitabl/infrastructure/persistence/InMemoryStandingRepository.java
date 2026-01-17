package com.ligitabl.infrastructure.persistence;

import com.ligitabl.domain.model.season.RoundNumber;
import com.ligitabl.domain.model.season.SeasonId;
import com.ligitabl.domain.model.standing.TeamStanding;
import com.ligitabl.domain.repository.StandingRepository;
import org.springframework.stereotype.Repository;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * In-memory implementation of StandingRepository.
 *
 * <p>Returns stub data for demonstration purposes.
 * In production, this would be replaced with a database-backed implementation.</p>
 */
@Repository
public class InMemoryStandingRepository implements StandingRepository {

    // Stub standings data
    private static final List<TeamStanding> STUB_STANDINGS = List.of(
        TeamStanding.create(1, "MCI", "Manchester City", "/images/crests/mci.png", 19, 14, 3, 2, 45, 42, 15, 27),
        TeamStanding.create(2, "ARS", "Arsenal", "/images/crests/ars.png", 19, 13, 4, 2, 43, 39, 16, 23),
        TeamStanding.create(3, "LIV", "Liverpool", "/images/crests/liv.png", 19, 13, 3, 3, 42, 41, 18, 23),
        TeamStanding.create(4, "AVL", "Aston Villa", "/images/crests/avl.png", 19, 12, 4, 3, 40, 38, 21, 17),
        TeamStanding.create(5, "TOT", "Tottenham", "/images/crests/tot.png", 19, 11, 3, 5, 36, 40, 28, 12),
        TeamStanding.create(6, "CHE", "Chelsea", "/images/crests/che.png", 19, 10, 4, 5, 34, 35, 24, 11),
        TeamStanding.create(7, "NEW", "Newcastle", "/images/crests/new.png", 19, 10, 3, 6, 33, 34, 26, 8),
        TeamStanding.create(8, "MUN", "Man United", "/images/crests/mun.png", 19, 9, 4, 6, 31, 30, 25, 5),
        TeamStanding.create(9, "WHU", "West Ham", "/images/crests/whu.png", 19, 8, 5, 6, 29, 32, 31, 1),
        TeamStanding.create(10, "BHA", "Brighton", "/images/crests/bha.png", 19, 7, 7, 5, 28, 31, 30, 1),
        TeamStanding.create(11, "WOL", "Wolves", "/images/crests/wol.png", 19, 7, 5, 7, 26, 25, 28, -3),
        TeamStanding.create(12, "FUL", "Fulham", "/images/crests/ful.png", 19, 6, 6, 7, 24, 24, 29, -5),
        TeamStanding.create(13, "BOU", "Bournemouth", "/images/crests/bou.png", 19, 6, 5, 8, 23, 26, 32, -6),
        TeamStanding.create(14, "CRY", "Crystal Palace", "/images/crests/cry.png", 19, 5, 6, 8, 21, 22, 30, -8),
        TeamStanding.create(15, "BRE", "Brentford", "/images/crests/bre.png", 19, 5, 5, 9, 20, 24, 33, -9),
        TeamStanding.create(16, "EVE", "Everton", "/images/crests/eve.png", 19, 4, 6, 9, 18, 20, 31, -11),
        TeamStanding.create(17, "NFO", "Nottingham Forest", "/images/crests/nfo.png", 19, 4, 5, 10, 17, 21, 35, -14),
        TeamStanding.create(18, "LEE", "Leeds United", "/images/crests/lee.png", 19, 3, 4, 12, 13, 19, 39, -20),
        TeamStanding.create(19, "BUR", "Burnley", "/images/crests/bur.png", 19, 2, 4, 13, 10, 17, 42, -25),
        TeamStanding.create(20, "SUN", "Sunderland", "/images/crests/sun.png", 19, 1, 3, 15, 6, 14, 48, -34)
    );

    @Override
    public List<TeamStanding> findBySeasonAndRound(SeasonId seasonId, RoundNumber round) {
        // For stub, return same standings regardless of season/round
        return STUB_STANDINGS;
    }

    @Override
    public List<TeamStanding> findCurrent() {
        return STUB_STANDINGS;
    }

    @Override
    public Map<String, Integer> findPositionMap(SeasonId seasonId, RoundNumber round) {
        return buildPositionMap();
    }

    @Override
    public Map<String, Integer> findPointsMap(SeasonId seasonId, RoundNumber round) {
        return buildPointsMap();
    }

    @Override
    public Map<String, Integer> findCurrentPositionMap() {
        return buildPositionMap();
    }

    @Override
    public Map<String, Integer> findCurrentPointsMap() {
        return buildPointsMap();
    }

    private Map<String, Integer> buildPositionMap() {
        Map<String, Integer> positionMap = new HashMap<>();
        for (TeamStanding standing : STUB_STANDINGS) {
            positionMap.put(standing.getTeamCode(), standing.getPosition());
        }
        return positionMap;
    }

    private Map<String, Integer> buildPointsMap() {
        Map<String, Integer> pointsMap = new HashMap<>();
        for (TeamStanding standing : STUB_STANDINGS) {
            pointsMap.put(standing.getTeamCode(), standing.getPoints());
        }
        return pointsMap;
    }
}
