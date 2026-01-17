package com.ligitabl.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Public controller for home page.
 *
 * <p>NOTE: The following endpoints have been migrated to Clean Architecture:
 * - Leaderboard: {@link com.ligitabl.presentation.api.LeaderboardController}
 * - Standings/Matches: {@link com.ligitabl.presentation.api.StandingsController}</p>
 */
@Controller
public class PublicController {

  @GetMapping("/")
  public String home(Model model) {
    model.addAttribute("pageTitle", "Home");
    return "index";
  }
}
