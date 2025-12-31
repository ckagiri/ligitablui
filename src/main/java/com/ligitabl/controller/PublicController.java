package com.ligitabl.controller;

import com.ligitabl.dto.Responses.LeaderboardEntry;
import com.ligitabl.dto.Responses.UserDetailResponse;
import com.ligitabl.service.InMemoryDataService;

import java.util.List;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class PublicController {

  private final InMemoryDataService dataService;

  public PublicController(InMemoryDataService dataService) {
    this.dataService = dataService;
  }

  @GetMapping("/")
  public String home(Model model) {
    model.addAttribute("pageTitle", "Home");
    return "index";
  }

  @GetMapping("/leaderboard")
  public String leaderboard(
      @RequestParam(required = false, defaultValue = "FS") String phase,
      @RequestParam(required = false, defaultValue = "1") int page,
      @RequestHeader(value = "HX-Request", required = false) String hxRequest,
      Model model) {

    int pageSize = 10;
    List<LeaderboardEntry> leaderboard = dataService.getLeaderboard(phase);

    // Get current user's position
    String currentUserId = "current-user-id"; // TODO: Get from session/auth
    String currentUserName = "Deejay Wagz"; // TODO: Get from session/auth
    LeaderboardEntry userPosition = dataService.getUserPosition(currentUserId, phase);

    // Check if user is in current page
    boolean userInCurrentPage = false;
    if (userPosition != null) {
      int startPos = (page - 1) * pageSize + 1;
      int endPos = page * pageSize;
      userInCurrentPage = userPosition.getPosition() >= startPos &&
          userPosition.getPosition() <= endPos;
    }

    model.addAttribute("pageTitle", "Leaderboard");
    model.addAttribute("currentPhase", phase);
    model.addAttribute("leaderboard", leaderboard);
    model.addAttribute("userPosition", userPosition);
    model.addAttribute("userInCurrentPage", userInCurrentPage);
    model.addAttribute("currentUserName", currentUserName);
    model.addAttribute("phases", new String[] { "FS", "Q1", "Q2", "Q3", "Q4", "H1", "H2" });

    if (hxRequest != null && !hxRequest.isBlank()) {
      return "leaderboard :: leaderboardContent";
    }

    return "leaderboard";
  }

  @GetMapping("/leaderboard/user/{userId}/details")
  public String leaderboardUserDetails(@PathVariable String userId, Model model) {
    // Determine which round to show
    int currentRound = dataService.getCurrentRound();
    boolean isCurrentRoundOpen = dataService.isRoundOpen(currentRound);
    int roundToShow = isCurrentRoundOpen ? currentRound - 1 : currentRound;
    String roundStatus = isCurrentRoundOpen ? "Finalised" : "Locked";

    // Get user details for the appropriate round
    // Handle phase
    UserDetailResponse userDetails = dataService.getUserDetails(userId, roundToShow);

    model.addAttribute("user", userDetails);
    model.addAttribute("roundToShow", roundToShow);
    model.addAttribute("roundStatus", roundStatus);

    return "fragments/user-detail :: user-details(user=${user}, round=${roundToShow}, status=${roundStatus})";
  }

  @GetMapping("/seasons/{id}/standings")
  public String standings(
      @PathVariable String id,
      @RequestParam(required = false, defaultValue = "19") Integer round,
      Model model) {
    model.addAttribute("pageTitle", "Standings");
    model.addAttribute("seasonId", id);
    model.addAttribute("currentRound", round);
    model.addAttribute("standings", dataService.getStandings());

    return "standings";
  }

  @GetMapping("/seasons/{id}/matches")
  public String matches(
      @PathVariable String id,
      @RequestParam(required = false, defaultValue = "19") Integer round,
      Model model) {
    model.addAttribute("pageTitle", "Matches");
    model.addAttribute("seasonId", id);
    model.addAttribute("currentRound", round);
    model.addAttribute("matches", dataService.getMatches());

    return "matches";
  }
}
