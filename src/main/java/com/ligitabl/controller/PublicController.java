package com.ligitabl.controller;

import com.ligitabl.service.InMemoryDataService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
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
            Model model) {
        model.addAttribute("pageTitle", "Leaderboard");
        model.addAttribute("currentPhase", phase);
        model.addAttribute("leaderboard", dataService.getLeaderboard(phase));
        model.addAttribute("phases", new String[]{"FS", "Q1", "Q2", "Q3", "Q4", "H1", "H2"});
        
        return "leaderboard";
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
