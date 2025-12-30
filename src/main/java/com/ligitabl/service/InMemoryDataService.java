package com.ligitabl.service;

import com.ligitabl.domain.Team;
import com.ligitabl.dto.Responses.*;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

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
            new Team("LUT", "Luton Town", "/images/crests/lut.png"),
            new Team("BUR", "Burnley", "/images/crests/bur.png"),
            new Team("SHU", "Sheffield United", "/images/crests/shu.png")
        );
    }
    
    private List<PredictionRow> initializeMyPrediction() {
        List<PredictionRow> prediction = new ArrayList<>();
        int position = 1;
        for (Team team : teams) {
            prediction.add(new PredictionRow(position++, team.getCode(), team.getName(), team.getCrestUrl()));
        }
        return prediction;
    }
    
    public List<LeaderboardEntry> getLeaderboard(String phase) {
        return List.of(
            new LeaderboardEntry(1, "Alice Wonder", 1850, 45, 23, 198, 0),
            new LeaderboardEntry(2, "Bob Smith", 1850, 45, 28, 195, -1),
            new LeaderboardEntry(3, "Carol Jones", 1850, 44, 20, 200, 2),
            new LeaderboardEntry(4, "Dave Brown", 1840, 50, 15, 195, -1),
            new LeaderboardEntry(5, "Eve Davis", 1825, 42, 31, 192, 1),
            new LeaderboardEntry(6, "Frank Miller", 1810, 38, 29, 190, -2),
            new LeaderboardEntry(7, "Grace Wilson", 1805, 41, 25, 188, 3),
            new LeaderboardEntry(8, "Henry Moore", 1795, 37, 33, 186, 0),
            new LeaderboardEntry(9, "Ivy Taylor", 1780, 35, 27, 184, -3),
            new LeaderboardEntry(10, "Jack Anderson", 1775, 40, 22, 182, 1)
        );
    }
    
    public List<PredictionRow> getMyPrediction() {
        return new ArrayList<>(myPrediction);
    }
    
    public void swapTeams(String teamA, String teamB) {
        // Find positions
        int posA = -1, posB = -1;
        for (int i = 0; i < myPrediction.size(); i++) {
            if (myPrediction.get(i).getTeamCode().equals(teamA)) posA = i;
            if (myPrediction.get(i).getTeamCode().equals(teamB)) posB = i;
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
                "Make your initial prediction! You can make unlimited changes before submitting."
            );
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
                "🎁 Bonus: You get one free change after your initial prediction!"
            );
        }
        
        // After first swap - 24 hour cooldown applies
        long hoursSinceLastSwap = ChronoUnit.HOURS.between(lastSwapTime, now);
        long minutesSinceLastSwap = ChronoUnit.MINUTES.between(lastSwapTime, now);
        boolean canSwap = hoursSinceLastSwap >= 24;
        
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMM dd, yyyy HH:mm")
            .withZone(ZoneId.systemDefault());
        
        if (canSwap) {
            return new SwapStatusResponse(
                "OPEN",
                true,
                formatter.format(lastSwapTime),
                "Now",
                0.0,
                "You can make changes now!"
            );
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
                "Next change in " + timeDisplay
            );
        }
    }
    
    public LatestResultResponse getLatestResult() {
        HitDistribution dist = new HitDistribution(
            3, 5, 8, 4,
            "Arsenal", 1,
            "Chelsea", 8
        );
        return new LatestResultResponse(10, 367, 5, 2, dist);
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
            new StandingsRow(18, "LUT", "Luton Town", "/images/crests/lut.png", 19, 3, 4, 12, 13, 19, 39, -20),
            new StandingsRow(19, "BUR", "Burnley", "/images/crests/bur.png", 19, 2, 4, 13, 10, 17, 42, -25),
            new StandingsRow(20, "SHU", "Sheffield United", "/images/crests/shu.png", 19, 1, 3, 15, 6, 14, 48, -34)
        );
    }
    
    public List<Match> getMatches() {
        return List.of(
            new Match("Manchester City", "Arsenal", "2", "1", "Sat, Dec 28, 12:30", "FINISHED"),
            new Match("Liverpool", "Aston Villa", "3", "3", "Sat, Dec 28, 15:00", "FINISHED"),
            new Match("Chelsea", "Tottenham", "2", "1", "Sat, Dec 28, 17:30", "LIVE", "67'"),
            new Match("Newcastle", "Man United", "", "", "Sun, Dec 29, 14:00", "SCHEDULED"),
            new Match("West Ham", "Brighton", "", "", "Sun, Dec 29, 16:30", "SCHEDULED")
        );
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
}
