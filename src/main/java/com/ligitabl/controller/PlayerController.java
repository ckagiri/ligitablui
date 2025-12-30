package com.ligitabl.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
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
    private final ObjectMapper objectMapper;

    public PlayerController(InMemoryDataService dataService, ObjectMapper objectMapper) {
        this.dataService = dataService;
        this.objectMapper = objectMapper;
    }

    @GetMapping("/predictions/me")
    public String myPredictions(
            @RequestParam(required = false, defaultValue = "19") Integer round,
            Model model) {

        int currentRound = dataService.getCurrentRound();
        boolean isCurrentRound = (round == currentRound);

        model.addAttribute("pageTitle", "My Predictions");
        model.addAttribute("currentRound", currentRound);
        model.addAttribute("viewingRound", round);
        model.addAttribute("isCurrentRound", isCurrentRound);

        if (isCurrentRound) {
            // Current round - editable
            var predictions = dataService.getMyPrediction();
            var swapStatus = dataService.getSwapStatus();
            boolean canSwap = swapStatus != null && Boolean.TRUE.equals(swapStatus.getCanSwap());
            boolean isInitialPrediction = swapStatus != null && "Never".equals(swapStatus.getLastSwapAt());

            model.addAttribute("predictions", predictions);
            model.addAttribute("swapStatus", swapStatus);
            model.addAttribute("canSwap", canSwap);
            model.addAttribute("isInitialPrediction", isInitialPrediction);
            model.addAttribute("roundScore", null);

            try {
                model.addAttribute("predictionsJson", objectMapper.writeValueAsString(predictions));
            } catch (JsonProcessingException e) {
                throw new IllegalStateException("Failed to serialize predictions", e);
            }
        } else {
            // Historical round - read-only
            var predictions = dataService.getMyPredictionForRound(round);
            model.addAttribute("predictions", predictions);
            model.addAttribute("roundScore", dataService.getScoreForRound(round));
            model.addAttribute("swapStatus", null); // No editing allowed

            model.addAttribute("canSwap", false);
            model.addAttribute("isInitialPrediction", false);

            try {
                model.addAttribute("predictionsJson", objectMapper.writeValueAsString(predictions));
            } catch (JsonProcessingException e) {
                throw new IllegalStateException("Failed to serialize predictions", e);
            }
        }

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
