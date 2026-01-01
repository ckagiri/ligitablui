package com.ligitabl.service;

import com.ligitabl.domain.Team;
import com.ligitabl.dto.Responses.*;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class InMemoryDataService {

  private final List<Team> teams;
  private final List<PredictionRow> myPrediction;
  private Instant lastSwapTime = null; // null = never swapped (initial prediction mode)
  private boolean initialPredictionMade = false;
  private int swapCount = 0; // Track number of swaps after initial prediction

  public InMemoryDataService() {
    this.teams = initializeTeams();
    this.myPrediction = initializeMyPrediction();
  }

  // ADD a reset method
  public void resetDemoState() {
    this.lastSwapTime = null;
    this.initialPredictionMade = false;
    this.swapCount = 0;
    // Reset prediction to original order
    this.myPrediction.clear();
    this.myPrediction.addAll(initializeMyPrediction());
  }

  private List<Team> initializeTeams() {
    return List.of(
        new Team("MCI", "Manchester City", "/images/crests/mci.png"),
        new Team("ARS", "Arsenal", "/images/crests/ars.png"),
        new Team("LIV", "Liverpool", "/images/crests/liv.png"),
        new Team("AVL", "Aston Villa", "/images/crests/avl.png"),
        new Team("TOT", "Tottenham", "/images/crests/tot.png"),
        new Team("CHE", "Chelsea", "/images/crests/che.png"),
        new Team("NEW", "Newcastle", "/images/crests/new.png"),
        new Team("MUN", "Man United", "/images/crests/mun.png"),
        new Team("WHU", "West Ham", "/images/crests/whu.png"),
        new Team("BHA", "Brighton", "/images/crests/bha.png"),
        new Team("WOL", "Wolves", "/images/crests/wol.png"),
        new Team("FUL", "Fulham", "/images/crests/ful.png"),
        new Team("BOU", "Bournemouth", "/images/crests/bou.png"),
        new Team("CRY", "Crystal Palace", "/images/crests/cry.png"),
        new Team("BRE", "Brentford", "/images/crests/bre.png"),
        new Team("EVE", "Everton", "/images/crests/eve.png"),
        new Team("NFO", "Nottingham Forest", "/images/crests/nfo.png"),
        new Team("LEE", "Leeds United", "/images/crests/lee.png"),
        new Team("BUR", "Burnley", "/images/crests/bur.png"),
        new Team("SUN", "Sunderland", "/images/crests/sun.png"));
  }

  private List<PredictionRow> initializeMyPrediction() {
    List<PredictionRow> prediction = new ArrayList<>();
    int position = 1;
    for (Team team : teams) {
      prediction.add(new PredictionRow(position++, team.getCode(), team.getName(), team.getCrestUrl()));
    }
    return prediction;
  }

  public void markResultAsViewed(int round) {
    // TODO: Update database to mark this result as viewed
    // For now, just log it
    System.out.println("Result for round " + round + " marked as viewed");
  }

  public boolean isRoundOpen(int round) {
    // TODO: Check database for round status
    // A round is "open" if matches haven't started yet
    // A round is "locked" once first match kicks off

    // For now, stub: current round (19) is open
    return round == getCurrentRound();
  }

  public UserDetailResponse getUserDetails(String userId, int round) {
    // TODO: Query database for user's prediction at specific round

    // Stub data
    List<PredictionRow> prediction = getMyPredictionForRound(round); // Get their prediction

    return new UserDetailResponse(
        userId,
        "Alice Wonder",
        1,
        1850,
        round == 19 ? 45 : 42, // Different score per round
        23,
        0,
        prediction // Their full 20-team prediction for this round
    );
  }

  public List<LeaderboardEntry> getLeaderboard(String phase) {
    return List.of(
        new LeaderboardEntry(1, "user1", "Alice Wonder", 1850, 45, 198, 23, 52, 0),
        new LeaderboardEntry(2, "user2", "Bob Smith", 1850, 45, 195, 28, 52, -1), // Same score as Alice, fewer zeroes
        new LeaderboardEntry(3, "user3", "Carol Jones", 1845, 44, 200, 20, 52, 2), // Best zeroes!
        new LeaderboardEntry(4, "user4", "Dave Brown", 1840, 50, 190, 15, 50, -1),
        new LeaderboardEntry(5, "user5", "Eve Davis", 1825, 42, 185, 31, 48, 1),
        new LeaderboardEntry(6, "user6", "Frank Miller", 1810, 38, 180, 29, 46, -2),
        new LeaderboardEntry(7, "user7", "Grace Wilson", 1805, 41, 178, 25, 45, 3),
        new LeaderboardEntry(8, "user8", "Henry Moore", 1795, 37, 175, 33, 44, 0),
        new LeaderboardEntry(9, "user9", "Ivy Taylor", 1780, 35, 172, 27, 42, -3),
        new LeaderboardEntry(10, "user10", "Jack Anderson", 1775, 40, 170, 22, 42, 1));
  }

  public LeaderboardEntry getUserPosition(String userId, String phase) {
    return new LeaderboardEntry(
        45, // position
        "current-user", // userId
        "Deejay Wagz", // displayName
        1702, // totalScore
        156, // roundScore
        175, // totalZeroes
        15, // totalSwaps
        168, // totalPoints (max round score)
        3 // movement
    );
  }

  public List<PredictionRow> getMyPrediction() {
    return new ArrayList<>(myPrediction);
  }

  public Integer getScoreForRound(int round) {
    // TODO: In real app, fetch actual score from database
    // Stub scores for demo purposes
    java.util.Map<Integer, Integer> scores = java.util.Map.of(
        18, 42,
        17, 38,
        16, 45,
        15, 40,
        14, 35);
    return scores.getOrDefault(round, null);
  }

  public int getCurrentRound() {
    // TODO: Get from database or configuration
    return 19;
  }

  public List<PredictionRow> getMyPredictionForRound(int round) {
    // Get actual standings for this round
    Map<String, Integer> actualStandings = getActualStandingsForRound(round);

    List<PredictionRow> historicalPrediction = new ArrayList<>();
    int pos = 1;
    for (PredictionRow pred : myPrediction) {
      Integer actualPos = actualStandings.get(pred.getTeamCode());
      Integer hit = actualPos != null ? Math.abs(pos - actualPos) : null;

      historicalPrediction.add(new PredictionRow(
          pos,
          pred.getTeamCode(),
          pred.getTeamName(),
          pred.getCrestUrl(),
          hit,
          actualPos));
      pos++;
    }

    return historicalPrediction;
  }

  public void swapTeams(String teamA, String teamB) {
    // Find positions
    int posA = -1, posB = -1;
    for (int i = 0; i < myPrediction.size(); i++) {
      if (myPrediction.get(i).getTeamCode().equals(teamA))
        posA = i;
      if (myPrediction.get(i).getTeamCode().equals(teamB))
        posB = i;
    }

    if (posA != -1 && posB != -1) {
      // Swap
      PredictionRow temp = myPrediction.get(posA);
      myPrediction.set(posA, myPrediction.get(posB));
      myPrediction.set(posB, temp);

      // Update positions
      myPrediction.get(posA).setPosition(posA + 1);
      myPrediction.get(posB).setPosition(posB + 1);

      // Only count swaps AFTER initial prediction
      if (initialPredictionMade) {
        swapCount++;
      }

      // Update last swap time
      lastSwapTime = Instant.now();
      initialPredictionMade = true;
    }
  }

  public void updatePredictionOrder(List<String> teamCodes) {
    // Count how many teams changed position
    int changedTeams = 0;
    for (int i = 0; i < teamCodes.size(); i++) {
      String code = teamCodes.get(i);
      PredictionRow originalTeam = myPrediction.stream()
          .filter(p -> p.getTeamCode().equals(code))
          .findFirst()
          .orElse(null);

      if (originalTeam != null && originalTeam.getPosition() != (i + 1)) {
        changedTeams++;
      }
    }

    // Validate: After initial prediction, only 1 swap (2 teams) allowed
    int swapsAttempted = changedTeams / 2;
    if (initialPredictionMade && swapsAttempted > 1) {
      throw new IllegalArgumentException("Only 1 swap allowed per period. You tried " + swapsAttempted + " swaps.");
    }

    // Reorder prediction based on new team code sequence
    List<PredictionRow> newPrediction = new ArrayList<>();

    for (int i = 0; i < teamCodes.size(); i++) {
      String code = teamCodes.get(i);
      // Find the team with this code
      PredictionRow team = myPrediction.stream()
          .filter(p -> p.getTeamCode().equals(code))
          .findFirst()
          .orElse(null);

      if (team != null) {
        // Create new row with updated position
        newPrediction.add(new PredictionRow(i + 1, team.getTeamCode(), team.getTeamName(), team.getCrestUrl()));
      }
    }

    // Replace old prediction
    myPrediction.clear();
    myPrediction.addAll(newPrediction);

    // Only count swaps AFTER initial prediction
    if (initialPredictionMade) {
      swapCount++;
    }

    // Update last swap time
    lastSwapTime = Instant.now();
    initialPredictionMade = true;
  }

  public Map<String, Integer> getCurrentStandingsMap() {
    List<StandingsRow> standings = getStandings();
    Map<String, Integer> standingsMap = new HashMap<>();

    for (StandingsRow row : standings) {
      standingsMap.put(row.getTeamCode(), row.getPosition());
    }

    return standingsMap;
  }

  public SwapStatusResponse getSwapStatus() {
    Instant now = Instant.now();

    // Initial prediction mode - unlimited changes
    if (!initialPredictionMade) {
      return new SwapStatusResponse(
          "OPEN",
          true,
          "Never",
          "Now",
          0.0,
          "Make your initial prediction! You can make unlimited changes before submitting.");
    }

    // First swap exception - one free swap after initial prediction
    if (swapCount == 0) {
      DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMM dd, yyyy HH:mm")
          .withZone(ZoneId.systemDefault());

      return new SwapStatusResponse(
          "OPEN",
          true,
          formatter.format(lastSwapTime),
          "Now",
          0.0,
          "Bonus: Make your first swap now—no 24-hour wait!");
    }

    // After first swap - 24 hour cooldown applies
    long hoursSinceLastSwap = ChronoUnit.HOURS.between(lastSwapTime, now);
    long minutesSinceLastSwap = ChronoUnit.MINUTES.between(lastSwapTime, now);
    boolean canSwap = hoursSinceLastSwap >= 0.033;

    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMM dd, yyyy HH:mm")
        .withZone(ZoneId.systemDefault());

    if (canSwap) {
      return new SwapStatusResponse(
          "OPEN",
          true,
          formatter.format(lastSwapTime),
          "Now",
          0.0,
          "You can make changes now!");
    } else {
      // Format time naturally
      long hoursRemaining = 24 - hoursSinceLastSwap;
      long minutesRemaining = (24 * 60) - minutesSinceLastSwap;

      String timeDisplay;
      if (hoursRemaining >= 2) {
        timeDisplay = hoursRemaining + "h";
      } else if (hoursRemaining == 1) {
        long mins = minutesRemaining - 60;
        if (mins > 0) {
          timeDisplay = "1h " + mins + "m";
        } else {
          timeDisplay = "1h";
        }
      } else {
        // Less than 1 hour
        if (minutesRemaining <= 1) {
          timeDisplay = "1m";
        } else {
          timeDisplay = minutesRemaining + "m";
        }
      }

      Instant nextSwapTime = lastSwapTime.plus(24, ChronoUnit.HOURS);
      return new SwapStatusResponse(
          "OPEN",
          false,
          formatter.format(lastSwapTime),
          formatter.format(nextSwapTime),
          (double) hoursRemaining,
          "Next change in " + timeDisplay);
    }
  }

  public LatestResultResponse getLatestResult() {
    // Check if there's a new unviewed result
    int currentRound = getCurrentRound();
    boolean isCurrentRoundOpen = isRoundOpen(currentRound);

    // If current round is open, show previous round result (if not viewed)
    // If current round is locked, show current round result (if not viewed)
    int resultRound = isCurrentRoundOpen ? currentRound - 1 : currentRound;

    // TODO: Check database if this result has been viewed
    boolean hasBeenViewed = false; // Stub

    if (hasBeenViewed) {
      return null; // No banner to show
    }

    // Get prediction for this round and calculate distribution
    List<PredictionRow> prediction = getMyPredictionForRound(resultRound);
    HitDistribution distribution = calculateHitDistribution(prediction);

    // Calculate score
    int totalHits = prediction.stream()
        .filter(p -> p.getHit() != null)
        .mapToInt(PredictionRow::getHit)
        .sum();
    int score = 200 - totalHits;

    // TODO: Get real position and movement from database
    return new LatestResultResponse(
        resultRound,
        score,
        45, // position (stub)
        3, // movement (stub)
        distribution,
        false // not viewed
    );
  }

  private HitDistribution calculateHitDistribution(List<PredictionRow> prediction) {
    int perfect = 0;
    int closeCalls = 0;
    int nearMisses = 0;
    int bigMisses = 0;

    for (PredictionRow pred : prediction) {
      if (pred.getHit() == null)
        continue;

      int hit = pred.getHit();
      if (hit == 0) {
        perfect++;
      } else if (hit >= 1 && hit <= 2) {
        closeCalls++;
      } else if (hit >= 3 && hit <= 5) {
        nearMisses++;
      } else {
        bigMisses++;
      }
    }

    return new HitDistribution(perfect, closeCalls, nearMisses, bigMisses);
  }

  public List<StandingsRow> getStandings() {
    return List.of(
        new StandingsRow(1, "MCI", "Manchester City", "/images/crests/mci.png", 19, 14, 3, 2, 45, 42, 15, 27),
        new StandingsRow(2, "ARS", "Arsenal", "/images/crests/ars.png", 19, 13, 4, 2, 43, 39, 16, 23),
        new StandingsRow(3, "LIV", "Liverpool", "/images/crests/liv.png", 19, 13, 3, 3, 42, 41, 18, 23),
        new StandingsRow(4, "AVL", "Aston Villa", "/images/crests/avl.png", 19, 12, 4, 3, 40, 38, 21, 17),
        new StandingsRow(5, "TOT", "Tottenham", "/images/crests/tot.png", 19, 11, 3, 5, 36, 40, 28, 12),
        new StandingsRow(6, "CHE", "Chelsea", "/images/crests/che.png", 19, 10, 4, 5, 34, 35, 24, 11),
        new StandingsRow(7, "NEW", "Newcastle", "/images/crests/new.png", 19, 10, 3, 6, 33, 34, 26, 8),
        new StandingsRow(8, "MUN", "Man United", "/images/crests/mun.png", 19, 9, 4, 6, 31, 30, 25, 5),
        new StandingsRow(9, "WHU", "West Ham", "/images/crests/whu.png", 19, 8, 5, 6, 29, 32, 31, 1),
        new StandingsRow(10, "BHA", "Brighton", "/images/crests/bha.png", 19, 7, 7, 5, 28, 31, 30, 1),
        new StandingsRow(11, "WOL", "Wolves", "/images/crests/wol.png", 19, 7, 5, 7, 26, 25, 28, -3),
        new StandingsRow(12, "FUL", "Fulham", "/images/crests/ful.png", 19, 6, 6, 7, 24, 24, 29, -5),
        new StandingsRow(13, "BOU", "Bournemouth", "/images/crests/bou.png", 19, 6, 5, 8, 23, 26, 32, -6),
        new StandingsRow(14, "CRY", "Crystal Palace", "/images/crests/cry.png", 19, 5, 6, 8, 21, 22, 30, -8),
        new StandingsRow(15, "BRE", "Brentford", "/images/crests/bre.png", 19, 5, 5, 9, 20, 24, 33, -9),
        new StandingsRow(16, "EVE", "Everton", "/images/crests/eve.png", 19, 4, 6, 9, 18, 20, 31, -11),
        new StandingsRow(17, "NFO", "Nottingham Forest", "/images/crests/nfo.png", 19, 4, 5, 10, 17, 21, 35, -14),
        new StandingsRow(18, "LEE", "Leeds United", "/images/crests/lee.png", 19, 3, 4, 12, 13, 19, 39, -20),
        new StandingsRow(19, "BUR", "Burnley", "/images/crests/bur.png", 19, 2, 4, 13, 10, 17, 42, -25),
        new StandingsRow(20, "SUN", "Sunderland", "/images/crests/sun.png", 19, 1, 3, 15, 6, 14, 48, -34));
  }

  public List<Match> getMatches() {
    return List.of(
        new Match("Manchester City", "Arsenal", "2", "1", "Sat, Dec 28, 12:30", "FINISHED"),
        new Match("Liverpool", "Aston Villa", "3", "3", "Sat, Dec 28, 15:00", "FINISHED"),
        new Match("Chelsea", "Tottenham", "2", "1", "Sat, Dec 28, 17:30", "LIVE", "67'"),
        new Match("Newcastle", "Man United", "", "", "Sun, Dec 29, 14:00", "SCHEDULED"),
        new Match("West Ham", "Brighton", "", "", "Sun, Dec 29, 16:30", "SCHEDULED"));
  }

  public List<PredictionRow> getUserPrediction(String userId) {
    // Return a slightly different prediction for demo
    List<PredictionRow> userPred = new ArrayList<>();
    int position = 1;
    // Shuffle the first 3 teams for variety
    List<Team> shuffled = new ArrayList<>(teams);
    Team temp = shuffled.get(0);
    shuffled.set(0, shuffled.get(1));
    shuffled.set(1, temp);

    for (Team team : shuffled) {
      userPred.add(new PredictionRow(position++, team.getCode(), team.getName(), team.getCrestUrl()));
    }
    return userPred;
  }

  private Map<String, Integer> getActualStandingsForRound(int round) {
    // TODO: Fetch from database
    // Stub: slight differences from prediction
    return Map.ofEntries(
        Map.entry("MCI", 1),
        Map.entry("ARS", 3), // Predicted 2nd, actually 3rd
        Map.entry("LIV", 2), // Predicted 3rd, actually 2nd
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
        Map.entry("SUN", 20));
  }
}
