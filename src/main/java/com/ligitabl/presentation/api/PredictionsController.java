package com.ligitabl.presentation.api;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ligitabl.api.shared.Either;
import com.ligitabl.application.command.GetSeasonPredictionCommand;
import com.ligitabl.application.error.UseCaseError;
import com.ligitabl.application.usecase.seasonprediction.GetSeasonPredictionUseCase;
import com.ligitabl.domain.model.prediction.SwapCooldown;
import com.ligitabl.domain.model.season.SeasonId;
import com.ligitabl.domain.model.seasonprediction.RankingSource;
import com.ligitabl.domain.model.seasonprediction.TeamRanking;
import com.ligitabl.domain.model.user.UserId;
import com.ligitabl.presentation.dto.response.RankingDTO;
import com.ligitabl.presentation.mapper.ErrorViewMapper;
import com.ligitabl.presentation.mapper.SeasonPredictionViewMapper;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

/**
 * New unified controller for round predictions.
 * Uses GetSeasonPredictionUseCase for consistent ranking prediction logic.
 *
 * <p>Supports:
 * - Current round viewing (shows user's live prediction with swap capability)
 * - Historical round viewing (shows past round results, read-only)
 * - Guest users (shows baseline/fallback rankings)
 * - Authenticated users (shows their predictions)</p>
 */
@Controller
@RequestMapping("/predictions2")  // Use /predictions2 initially, switch to /predictions once stable
public class PredictionsController {

    private static final Logger log = LoggerFactory.getLogger(PredictionsController.class);
    private static final int CURRENT_ROUND = 19;  // TODO: Get from config or service

    private final GetSeasonPredictionUseCase getSeasonPredictionUseCase;
    private final SeasonPredictionViewMapper viewMapper;
    private final ErrorViewMapper errorMapper;
    private final ObjectMapper objectMapper;
    private final SeasonId activeSeasonId;

    public PredictionsController(
        GetSeasonPredictionUseCase getSeasonPredictionUseCase,
        SeasonPredictionViewMapper viewMapper,
        ErrorViewMapper errorMapper,
        ObjectMapper objectMapper,
        SeasonId activeSeasonId
    ) {
        this.getSeasonPredictionUseCase = getSeasonPredictionUseCase;
        this.viewMapper = viewMapper;
        this.errorMapper = errorMapper;
        this.objectMapper = objectMapper;
        this.activeSeasonId = activeSeasonId;
    }

    /**
     * GET /predictions2/me - View predictions for a specific round.
     *
     * <p>Logic:
     * - If viewing current round: Use GetSeasonPredictionUseCase (live, editable)
     * - If viewing past round: Use GetSeasonPredictionUseCase with round number (historical, read-only)
     * - Guest users: Get baseline/fallback rankings
     * - Authenticated users: Get their personal predictions</p>
     */
    @GetMapping("/me")
    public String myPredictions(
        @RequestParam(required = false, defaultValue = "" + CURRENT_ROUND) Integer round,
        @RequestParam(required = false) String source,  // Optional: force specific source for testing
        Principal principal,
        Model model,
        HttpServletResponse response,
        @RequestHeader(value = "HX-Request", required = false) String hxRequest
    ) {
        log.info("GET /predictions2/me - round: {}, user: {}", round,
            principal != null ? principal.getName() : "guest");

        // Determine user ID (guest or authenticated)
        UserId userId = principal != null
            ? UserId.of(principal.getName())
            : UserId.generate(); // Temp ID for guest - will trigger fallback

        boolean isCurrentRound = round == CURRENT_ROUND;
        boolean isGuestUser = principal == null;

        // Build command - use round number for historical rounds
        GetSeasonPredictionCommand command = isCurrentRound
            ? GetSeasonPredictionCommand.forSeason(userId, activeSeasonId)
            : GetSeasonPredictionCommand.forRound(userId, activeSeasonId, round);

        // Execute use case
        Either<UseCaseError, GetSeasonPredictionUseCase.PredictionViewData> result =
            getSeasonPredictionUseCase.execute(command);

        // Handle result with fold pattern
        return result.fold(
            error -> handleError(error, model, response, hxRequest),
            data -> handleSuccess(data, model, hxRequest)
        );
    }

    // --- Private Helper Methods ---

    private String handleError(
        UseCaseError error,
        Model model,
        HttpServletResponse response,
        String hxRequest
    ) {
        response.setStatus(mapErrorToStatus(error));
        model.addAttribute("error", errorMapper.toResponse(error));

        if (hxRequest != null && !hxRequest.isBlank()) {
            return "fragments/error-banner :: banner";
        }
        return "error";
    }

    private String handleSuccess(
        GetSeasonPredictionUseCase.PredictionViewData data,
        Model model,
        String hxRequest
    ) {
        List<TeamRanking> rankings = data.rankings();
        RankingSource source = data.source();

        // Convert TeamRanking to RankingDTO (similar to PredictionRow in RoundPredictionController)
        List<RankingDTO> predictions = rankings.stream()
            .map(viewMapper::toRankingDTO)
            .toList();

        // Set model attributes to match RoundPredictionController
        model.addAttribute("pageTitle", "My Predictions");
        model.addAttribute("currentRound", data.currentRound());
        model.addAttribute("viewingRound", data.viewingRound());
        model.addAttribute("isCurrentRound", data.isCurrentRound());
        model.addAttribute("roundState", data.roundState().toLowerCase());
        model.addAttribute("predictions", predictions);
        model.addAttribute("canSwap", data.canSwap());
        model.addAttribute("isInitialPrediction", data.isInitialPrediction());
        model.addAttribute("roundScore", data.roundScore());
        model.addAttribute("totalHits", data.totalHits());

        // Convert swap status if available (matching RoundPredictionController logic)
        if (data.swapCooldown() != null) {
            // For now, we'll set a simplified swap status
            // TODO: Create proper SwapStatusResponse DTO like RoundPredictionController
            model.addAttribute("swapStatus", createSwapStatusResponse(data.swapCooldown(), data.roundState()));
        } else {
            model.addAttribute("swapStatus", null);
        }

        // Serialize data for JavaScript
        try {
            model.addAttribute("fixturesJson", objectMapper.writeValueAsString(data.fixtures()));
            model.addAttribute("predictionsJson", objectMapper.writeValueAsString(predictions));
            model.addAttribute("currentStandingsJson", objectMapper.writeValueAsString(data.standingsMap()));
            model.addAttribute("currentPointsJson", objectMapper.writeValueAsString(data.pointsMap()));
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize data", e);
            model.addAttribute("fixturesJson", "{}");
            model.addAttribute("predictionsJson", "[]");
            model.addAttribute("currentStandingsJson", "{}");
            model.addAttribute("currentPointsJson", "{}");
        }

        // Return appropriate view
        if (hxRequest != null && !hxRequest.isBlank()) {
            return "predictions2/me :: predictionPage";
        }
        return "predictions2/me";
    }

    /**
     * Create a simple swap status response object.
     * TODO: Use proper mapper like RoundPredictionController does.
     */
    private Object createSwapStatusResponse(SwapCooldown swapCooldown, String roundState) {
        // For now, create a simple map
        // In a real implementation, we'd use the PredictionViewMapper
        return new java.util.HashMap<String, Object>() {{
            put("canSwap", swapCooldown.canSwap(java.time.Instant.now()));
            put("message", swapCooldown.canSwap(java.time.Instant.now())
                ? "Ready to swap"
                : "Cooldown active. Next change available soon.");
            put("lastSwapAt", swapCooldown.lastSwapAt() != null
                ? swapCooldown.lastSwapAt().toString()
                : "Never");
        }};
    }

    private int mapErrorToStatus(UseCaseError error) {
        return switch (error.type()) {
            case VALIDATION -> 400;
            case NOT_FOUND -> 404;
            case CONFLICT -> 409;
            case BUSINESS_RULE -> 422;
        };
    }
}
