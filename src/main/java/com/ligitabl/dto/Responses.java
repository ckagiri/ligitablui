package com.ligitabl.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import java.util.List;

public class Responses {

  @Data
  @AllArgsConstructor
  public static class LeaderboardEntry {
    private Integer position;
    private String displayName;
    private Integer totalScore;
    private Integer totalZeroes;
    private Integer totalSwaps;
    private Integer maxScore;
    private Integer movement;
  }

  @Data
  @AllArgsConstructor
  public static class PredictionRow {
    private Integer position;
    private String teamCode;
    private String teamName;
    private String crestUrl;
    private Integer hit; // NEW: points lost (0 = perfect)
    private Integer actualPosition; // NEW: actual standings position

    // Backward compatibility constructor
    public PredictionRow(Integer position, String teamCode, String teamName, String crestUrl) {
      this(position, teamCode, teamName, crestUrl, null, null);
    }
  }

  @Data
  @AllArgsConstructor
  public static class SwapStatusResponse {
    private String roundStatus;
    private Boolean canSwap;
    private String lastSwapAt;
    private String nextSwapAt;
    private Double hoursRemaining;
    private String message;
  }

  @Data
  @AllArgsConstructor
  public static class LatestResultResponse {
    private Integer round;
    private Integer score;
    private Integer position;
    private Integer movement;
    private HitDistribution hitDistribution;
  }

  @Data
  @AllArgsConstructor
  public static class HitDistribution {
    private Integer perfect;
    private Integer closeCalls;
    private Integer nearMisses;
    private Integer bigMisses;
    private String bestTeam;
    private Integer bestHit;
    private String worstTeam;
    private Integer worstHit;
  }

  @Data
  @AllArgsConstructor
  public static class StandingsRow {
    private Integer position;
    private String teamCode;
    private String teamName;
    private String crestUrl;
    private Integer played;
    private Integer won;
    private Integer drawn;
    private Integer lost;
    private Integer points;
    private Integer goalsFor;
    private Integer goalsAgainst;
    private Integer goalDifference;
  }

  @Data
  @AllArgsConstructor
  public static class Match {
    private String homeTeam;
    private String awayTeam;
    private String homeScore;
    private String awayScore;
    private String kickOff;
    private String status;
    private String matchTime; // For LIVE matches (e.g., "67'", "45'+2'")

    // Constructor without matchTime for backward compatibility
    public Match(String homeTeam, String awayTeam, String homeScore, String awayScore,
        String kickOff, String status) {
      this(homeTeam, awayTeam, homeScore, awayScore, kickOff, status, null);
    }
  }
}
