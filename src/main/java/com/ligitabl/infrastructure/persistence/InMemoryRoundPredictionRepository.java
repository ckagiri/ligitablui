package com.ligitabl.infrastructure.persistence;

import com.ligitabl.domain.model.prediction.PredictionRow;
import com.ligitabl.domain.model.prediction.SwapCooldown;
import com.ligitabl.domain.model.season.RoundNumber;
import com.ligitabl.domain.model.user.UserId;
import com.ligitabl.domain.repository.RoundPredictionRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * In-memory implementation of RoundPredictionRepository.
 *
 * <p>Maintains state for demo/testing purposes.
 * In production, this would be replaced with a database-backed implementation.</p>
 */
@Repository
public class InMemoryRoundPredictionRepository implements RoundPredictionRepository {

    private static final int CURRENT_ROUND = 19;

    // User prediction state
    private final List<PredictionRow> myPrediction;
    private Instant lastSwapTime = null;
    private boolean initialPredictionMade = false;
    private int swapCount = 0;

    // Team data for initialization
    private static final List<TeamInfo> TEAMS = List.of(
        new TeamInfo("MCI", "Manchester City", "/images/crests/mci.png"),
        new TeamInfo("ARS", "Arsenal", "/images/crests/ars.png"),
        new TeamInfo("LIV", "Liverpool", "/images/crests/liv.png"),
        new TeamInfo("AVL", "Aston Villa", "/images/crests/avl.png"),
        new TeamInfo("TOT", "Tottenham", "/images/crests/tot.png"),
        new TeamInfo("CHE", "Chelsea", "/images/crests/che.png"),
        new TeamInfo("NEW", "Newcastle", "/images/crests/new.png"),
        new TeamInfo("MUN", "Man United", "/images/crests/mun.png"),
        new TeamInfo("WHU", "West Ham", "/images/crests/whu.png"),
        new TeamInfo("BHA", "Brighton", "/images/crests/bha.png"),
        new TeamInfo("WOL", "Wolves", "/images/crests/wol.png"),
        new TeamInfo("FUL", "Fulham", "/images/crests/ful.png"),
        new TeamInfo("BOU", "Bournemouth", "/images/crests/bou.png"),
        new TeamInfo("CRY", "Crystal Palace", "/images/crests/cry.png"),
        new TeamInfo("BRE", "Brentford", "/images/crests/bre.png"),
        new TeamInfo("EVE", "Everton", "/images/crests/eve.png"),
        new TeamInfo("NFO", "Nottingham Forest", "/images/crests/nfo.png"),
        new TeamInfo("LEE", "Leeds United", "/images/crests/lee.png"),
        new TeamInfo("BUR", "Burnley", "/images/crests/bur.png"),
        new TeamInfo("SUN", "Sunderland", "/images/crests/sun.png")
    );

    public InMemoryRoundPredictionRepository() {
        this.myPrediction = new ArrayList<>(initializePrediction());
    }

    private List<PredictionRow> initializePrediction() {
        List<PredictionRow> prediction = new ArrayList<>();
        int position = 1;
        for (TeamInfo team : TEAMS) {
            prediction.add(PredictionRow.pending(position++, team.code, team.name, team.crestUrl));
        }
        return prediction;
    }

    @Override
    public List<PredictionRow> findCurrentByUser(UserId userId) {
        return new ArrayList<>(myPrediction);
    }

    @Override
    public List<PredictionRow> findByUserAndRound(UserId userId, RoundNumber round) {
        // Get actual standings for the round to calculate hits
        Map<String, Integer> actualStandings = getActualStandings(round.value());

        List<PredictionRow> scoredPrediction = new ArrayList<>();
        for (PredictionRow row : myPrediction) {
            Integer actualPos = actualStandings.get(row.getTeamCode());
            if (actualPos != null) {
                scoredPrediction.add(row.withResult(actualPos));
            } else {
                scoredPrediction.add(row);
            }
        }
        return scoredPrediction;
    }

    @Override
    public SwapCooldown getSwapCooldown(UserId userId) {
        return new SwapCooldown(lastSwapTime, initialPredictionMade, swapCount, true);
    }

    @Override
    public void savePredictionOrder(UserId userId, List<PredictionRow> predictions) {
        // Count changed teams
        int changedTeams = 0;
        for (int i = 0; i < predictions.size(); i++) {
            PredictionRow newRow = predictions.get(i);
            PredictionRow oldRow = myPrediction.stream()
                .filter(p -> p.getTeamCode().equals(newRow.getTeamCode()))
                .findFirst()
                .orElse(null);

            if (oldRow != null && oldRow.getPosition() != newRow.getPosition()) {
                changedTeams++;
            }
        }

        // Validate: After initial prediction, only 1 swap (2 teams) allowed
        int swapsAttempted = changedTeams / 2;
        if (initialPredictionMade && swapsAttempted > 1) {
            throw new IllegalArgumentException(
                "Only 1 swap allowed per period. You tried " + swapsAttempted + " swaps."
            );
        }

        // Update prediction
        myPrediction.clear();
        myPrediction.addAll(predictions);

        recordSwap(userId, Instant.now());
    }

    @Override
    public void swapTeams(UserId userId, String teamA, String teamB) {
        int posA = -1, posB = -1;
        for (int i = 0; i < myPrediction.size(); i++) {
            if (myPrediction.get(i).getTeamCode().equals(teamA)) posA = i;
            if (myPrediction.get(i).getTeamCode().equals(teamB)) posB = i;
        }

        if (posA != -1 && posB != -1) {
            PredictionRow rowA = myPrediction.get(posA);
            PredictionRow rowB = myPrediction.get(posB);

            myPrediction.set(posA, rowB.withPosition(posA + 1));
            myPrediction.set(posB, rowA.withPosition(posB + 1));

            recordSwap(userId, Instant.now());
        }
    }

    @Override
    public void recordSwap(UserId userId, Instant swapTime) {
        if (initialPredictionMade) {
            swapCount++;
        }
        lastSwapTime = swapTime;
        initialPredictionMade = true;
    }

    @Override
    public boolean hasInitialPrediction(UserId userId) {
        return initialPredictionMade;
    }

    @Override
    public int getCurrentRound() {
        return CURRENT_ROUND;
    }

    @Override
    public boolean isRoundOpen(int round) {
        return round == CURRENT_ROUND;
    }

    @Override
    public void resetDemoState() {
        this.lastSwapTime = null;
        this.initialPredictionMade = false;
        this.swapCount = 0;
        this.myPrediction.clear();
        this.myPrediction.addAll(initializePrediction());
    }

    @Override
    public Map<String, Integer> getActualStandings(int round) {
        // Stub: slight differences from prediction
        return Map.ofEntries(
            Map.entry("MCI", 1),
            Map.entry("ARS", 3),
            Map.entry("LIV", 2),
            Map.entry("AVL", 4),
            Map.entry("TOT", 5),
            Map.entry("CHE", 7),
            Map.entry("NEW", 6),
            Map.entry("MUN", 8),
            Map.entry("WHU", 9),
            Map.entry("BHA", 10),
            Map.entry("WOL", 11),
            Map.entry("FUL", 12),
            Map.entry("BOU", 13),
            Map.entry("CRY", 14),
            Map.entry("BRE", 15),
            Map.entry("EVE", 16),
            Map.entry("NFO", 17),
            Map.entry("LEE", 18),
            Map.entry("BUR", 19),
            Map.entry("SUN", 20)
        );
    }

    private record TeamInfo(String code, String name, String crestUrl) {}
}
