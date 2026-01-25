package com.ligitabl.infrastructure.persistence;

import com.ligitabl.domain.model.roundresult.ResultTeamRank;
import com.ligitabl.domain.model.roundresult.RoundResult;
import com.ligitabl.domain.model.season.RoundNumber;
import com.ligitabl.domain.model.user.UserId;
import com.ligitabl.domain.repository.RoundResultRepository;
import org.springframework.stereotype.Repository;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory implementation of RoundResultRepository.
 *
 * <p>Maintains sample historical data for demo/testing purposes.
 * In production, this would be replaced with a database-backed implementation.</p>
 */
@Repository
public class InMemoryRoundResultRepository implements RoundResultRepository {

    // Key: "userId:roundNumber"
    private final Map<String, RoundResult> results = new ConcurrentHashMap<>();

    // Team data for generating sample results
    private static final List<TeamInfo> TEAMS = List.of(
        new TeamInfo("MCI", "Manchester City"),
        new TeamInfo("ARS", "Arsenal"),
        new TeamInfo("LIV", "Liverpool"),
        new TeamInfo("AVL", "Aston Villa"),
        new TeamInfo("TOT", "Tottenham"),
        new TeamInfo("CHE", "Chelsea"),
        new TeamInfo("NEW", "Newcastle"),
        new TeamInfo("MUN", "Man United"),
        new TeamInfo("WHU", "West Ham"),
        new TeamInfo("BHA", "Brighton"),
        new TeamInfo("WOL", "Wolves"),
        new TeamInfo("FUL", "Fulham"),
        new TeamInfo("BOU", "Bournemouth"),
        new TeamInfo("CRY", "Crystal Palace"),
        new TeamInfo("BRE", "Brentford"),
        new TeamInfo("EVE", "Everton"),
        new TeamInfo("NFO", "Nottingham Forest"),
        new TeamInfo("LEE", "Leeds United"),
        new TeamInfo("BUR", "Burnley"),
        new TeamInfo("SUN", "Sunderland")
    );

    public InMemoryRoundResultRepository() {
        // Initialize with sample historical data for demo user
        // In a real app, this would be populated from a database
    }

    @Override
    public Optional<RoundResult> findByUserAndRound(UserId userId, RoundNumber roundNumber) {
        String key = makeKey(userId, roundNumber);

        // Check if we have stored result
        RoundResult stored = results.get(key);
        if (stored != null) {
            return Optional.of(stored);
        }

        // For demo purposes, generate sample data for any user viewing historical rounds
        // In production, this would only return actual stored results
        if (roundNumber.value() < 19) { // Current round is 19
            return Optional.of(generateSampleResult(userId, roundNumber));
        }

        return Optional.empty();
    }

    @Override
    public boolean existsByUserAndRound(UserId userId, RoundNumber roundNumber) {
        // For demo, any round before current (19) has results
        return roundNumber.value() < 19 || results.containsKey(makeKey(userId, roundNumber));
    }

    @Override
    public void save(RoundResult result) {
        String key = makeKey(result.userId(), result.roundNumber());
        results.put(key, result);
    }

    @Override
    public Optional<RoundNumber> findEarliestRoundForUser(UserId userId) {
        // For demo purposes, earliest round is always 1
        // In production, this would query actual data
        return Optional.of(RoundNumber.of(1));
    }

    @Override
    public void markAsViewed(UserId userId, RoundNumber roundNumber) {
        String key = makeKey(userId, roundNumber);
        RoundResult result = results.get(key);
        if (result != null) {
            results.put(key, result.markAsViewed());
        }
    }

    private String makeKey(UserId userId, RoundNumber roundNumber) {
        return userId.value() + ":" + roundNumber.value();
    }

    /**
     * Generate sample round result data for demo purposes.
     * Creates slightly varied predictions to make it interesting.
     */
    private RoundResult generateSampleResult(UserId userId, RoundNumber roundNumber) {
        // Use round number as seed for consistent results
        Random random = new Random(roundNumber.value() * 1000L + userId.value().hashCode());

        // Generate sample standings (actual positions) with some variation
        List<Integer> standings = generateStandings(random, roundNumber.value());

        // Generate predictions (user's predicted positions) with some accuracy
        List<ResultTeamRank> rankings = new ArrayList<>();
        for (int i = 0; i < TEAMS.size(); i++) {
            TeamInfo team = TEAMS.get(i);
            int predictedPosition = i + 1; // User predicted in order 1-20
            int standingsPosition = standings.get(i);

            rankings.add(ResultTeamRank.create(
                team.code,
                team.name,
                predictedPosition,
                standingsPosition
            ));
        }

        // Sort by predicted position
        rankings.sort(Comparator.comparingInt(ResultTeamRank::predictedPosition));

        // Calculate swap count (varies by round for demo)
        int swapCount = random.nextInt(3); // 0-2 swaps

        return RoundResult.create(userId, roundNumber, rankings, swapCount);
    }

    /**
     * Generate standings positions with some variation based on round.
     */
    private List<Integer> generateStandings(Random random, int round) {
        List<Integer> positions = new ArrayList<>();
        for (int i = 1; i <= 20; i++) {
            positions.add(i);
        }

        // Shuffle a bit based on round - more shuffling in later rounds
        int swaps = Math.min(round, 10);
        for (int i = 0; i < swaps; i++) {
            int idx1 = random.nextInt(20);
            int idx2 = random.nextInt(20);
            Collections.swap(positions, idx1, idx2);
        }

        return positions;
    }

    private record TeamInfo(String code, String name) {}
}
