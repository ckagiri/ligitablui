package com.ligitabl.controller;

import com.ligitabl.dto.Responses;
import com.ligitabl.dto.Responses.LatestResultResponse;
import com.ligitabl.service.InMemoryDataService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

/**
 * Controller for player-related endpoints that haven't been migrated yet.
 *
 * <p>Note: Most prediction endpoints have been migrated to RoundPredictionController.
 * This controller now only handles:
 * - Latest result endpoints
 * - User predictions view
 * - My contests page
 * </p>
 */
@Controller
public class PlayerController {

  private final InMemoryDataService dataService;

  public PlayerController(InMemoryDataService dataService) {
    this.dataService = dataService;
  }

  @GetMapping("/predictions/me/latest-result")
  @ResponseBody
  public Responses.LatestResultResponse getLatestResult() {
    return dataService.getLatestResult();
  }

  @GetMapping("/predictions/me/latest-result-banner")
  public String getLatestResultBanner(Model model) {
    LatestResultResponse result = dataService.getLatestResult();

    if (result != null) {
      model.addAttribute("result", result);
      return "fragments/results-banner :: results-banner(result=${result})";
    }

    return "fragments/results-banner :: results-banner(result=null)";
  }

  @GetMapping("/users/{userId}/predictions")
  public String userPredictions(
      @PathVariable String userId,
      @RequestParam(required = false) Integer round,
      Model model) {
    model.addAttribute("pageTitle", "User Predictions");
    model.addAttribute("userId", userId);
    model.addAttribute("round", round);
    model.addAttribute("predictions", dataService.getUserPrediction(userId));

    return "predictions/user";
  }

  @GetMapping("/contests/me")
  public String myContests(Model model) {
    model.addAttribute("pageTitle", "My Contests");
    return "contests/me";
  }
}
