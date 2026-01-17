package com.ligitabl.infrastructure.persistence;

import com.ligitabl.domain.model.season.SeasonId;
import com.ligitabl.domain.model.seasonprediction.TeamRanking;
import com.ligitabl.domain.model.team.TeamId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Initialize game invariants on application startup.
 *
 * <p>This component ensures that critical baseline data exists:
 * - Active season
 * - Main contest
 * - Season baseline rankings (20 teams)</p>
 *
 * <p>IMPORTANT: Season baseline rankings are a game invariant.
 * They MUST exist for the fallback hierarchy to work correctly.</p>
 */
@Component
@Profile("!test") // Don't run during tests (tests set up their own data)
public class DataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    // Game invariant IDs (fixed UUIDs for consistency)
    public static final SeasonId ACTIVE_SEASON_ID = SeasonId.of("550e8400-e29b-41d4-a716-446655440000");

    private final InMemorySeasonTeamRankingsRepository seasonTeamRankingsRepository;

    public DataInitializer(InMemorySeasonTeamRankingsRepository seasonTeamRankingsRepository) {
        this.seasonTeamRankingsRepository = seasonTeamRankingsRepository;
    }

    @Override
    public void run(String... args) {
        log.info("Initializing game invariants...");

        initializeSeasonBaselineRankings();

        log.info("Game invariants initialized successfully");
    }

    /**
     * Initialize baseline rankings for the active season.
     *
     * <p>Uses the 20 Premier League teams in a default order.
     * This is the final fallback in the smart fallback hierarchy.</p>
     */
    private void initializeSeasonBaselineRankings() {
        if (seasonTeamRankingsRepository.existsBySeasonId(ACTIVE_SEASON_ID)) {
            log.info("Season baseline rankings already exist, skipping initialization");
            return;
        }

        log.info("Creating baseline rankings for active season: {}", ACTIVE_SEASON_ID.value());

        // Create baseline rankings for 20 teams
        // Team codes match those in InMemoryDataService
        List<TeamRanking> baselineRankings = createBaselineRankings();

        seasonTeamRankingsRepository.save(ACTIVE_SEASON_ID, baselineRankings);

        log.info("Created baseline rankings with {} teams", baselineRankings.size());
    }

    /**
     * Create baseline rankings for all 20 teams.
     *
     * <p>Uses fixed UUID-based team IDs for consistency.
     * Order represents last season's finish or pre-season predictions.</p>
     *
     * @return list of 20 team rankings
     */
    private List<TeamRanking> createBaselineRankings() {
        List<TeamRanking> rankings = new ArrayList<>();

        // Create 20 teams with fixed UUIDs (for consistency across restarts)
        // Format: 00000000-0000-0000-0000-00000000000X where X is the position
        rankings.add(TeamRanking.create(TeamId.of("00000000-0000-0000-0000-000000000001"), 1));  // Manchester City
        rankings.add(TeamRanking.create(TeamId.of("00000000-0000-0000-0000-000000000002"), 2));  // Arsenal
        rankings.add(TeamRanking.create(TeamId.of("00000000-0000-0000-0000-000000000003"), 3));  // Liverpool
        rankings.add(TeamRanking.create(TeamId.of("00000000-0000-0000-0000-000000000004"), 4));  // Aston Villa
        rankings.add(TeamRanking.create(TeamId.of("00000000-0000-0000-0000-000000000005"), 5));  // Tottenham
        rankings.add(TeamRanking.create(TeamId.of("00000000-0000-0000-0000-000000000006"), 6));  // Chelsea
        rankings.add(TeamRanking.create(TeamId.of("00000000-0000-0000-0000-000000000007"), 7));  // Newcastle
        rankings.add(TeamRanking.create(TeamId.of("00000000-0000-0000-0000-000000000008"), 8));  // Man United
        rankings.add(TeamRanking.create(TeamId.of("00000000-0000-0000-0000-000000000009"), 9));  // West Ham
        rankings.add(TeamRanking.create(TeamId.of("00000000-0000-0000-0000-000000000010"), 10)); // Brighton
        rankings.add(TeamRanking.create(TeamId.of("00000000-0000-0000-0000-000000000011"), 11)); // Wolves
        rankings.add(TeamRanking.create(TeamId.of("00000000-0000-0000-0000-000000000012"), 12)); // Fulham
        rankings.add(TeamRanking.create(TeamId.of("00000000-0000-0000-0000-000000000013"), 13)); // Bournemouth
        rankings.add(TeamRanking.create(TeamId.of("00000000-0000-0000-0000-000000000014"), 14)); // Crystal Palace
        rankings.add(TeamRanking.create(TeamId.of("00000000-0000-0000-0000-000000000015"), 15)); // Brentford
        rankings.add(TeamRanking.create(TeamId.of("00000000-0000-0000-0000-000000000016"), 16)); // Everton
        rankings.add(TeamRanking.create(TeamId.of("00000000-0000-0000-0000-000000000017"), 17)); // Nottingham Forest
        rankings.add(TeamRanking.create(TeamId.of("00000000-0000-0000-0000-000000000018"), 18)); // Leeds United
        rankings.add(TeamRanking.create(TeamId.of("00000000-0000-0000-0000-000000000019"), 19)); // Burnley
        rankings.add(TeamRanking.create(TeamId.of("00000000-0000-0000-0000-000000000020"), 20)); // Sunderland

        return rankings;
    }
}
