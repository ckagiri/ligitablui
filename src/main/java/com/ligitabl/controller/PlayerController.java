package com.ligitabl.controller;

import com.ligitabl.dto.Responses;
import com.ligitabl.service.InMemoryDataService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Controller
public class PlayerController {
    
    private final InMemoryDataService dataService;
    
    public PlayerController(InMemoryDataService dataService) {
        this.dataService = dataService;
    }

    @GetMapping("/predictions/me")
    public String myPredictions(Model model) {
        model.addAttribute("pageTitle", "My Predictions");
        model.addAttribute("predictions", dataService.getMyPrediction());
        model.addAttribute("swapStatus", dataService.getSwapStatus());
        return "predictions/me-improved";
    }

    @GetMapping("/predictions/me/swap-status")
    @ResponseBody
    public Responses.SwapStatusResponse getSwapStatus() {
        return dataService.getSwapStatus();
    }

    @PostMapping("/predictions/swap")
    public String makeSwap(
            @RequestParam String teamA,
            @RequestParam String teamB,
            Model model) {
        
        // Perform swap
        dataService.swapTeams(teamA, teamB);
        
        // Return updated prediction table fragment
        model.addAttribute("predictions", dataService.getMyPrediction());
        return "fragments/prediction-table";
    }

    @PostMapping("/predictions/swap-multiple")
    @ResponseBody
    public Map<String, Object> makeMultipleSwaps(@RequestBody Map<String, List<String>> request) {
        List<String> teamCodes = request.get("teamCodes");
        
        // Update the prediction with new order
        dataService.updatePredictionOrder(teamCodes);
        
        return Map.of("success", true, "message", "Prediction updated successfully");
    }

    @GetMapping("/predictions/me/latest-result")
    @ResponseBody
    public Responses.LatestResultResponse getLatestResult() {
        return dataService.getLatestResult();
    }

    @PutMapping("/predictions/me/results/{round}/mark-viewed")
    @ResponseBody
    public String markResultViewed(@PathVariable Integer round) {
        return "{\"success\": true}";
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
