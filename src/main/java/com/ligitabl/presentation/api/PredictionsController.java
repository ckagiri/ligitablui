package com.ligitabl.presentation.api;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ligitabl.api.shared.Either;
import com.ligitabl.application.command.GetSeasonPredictionCommand;
import com.ligitabl.application.command.SwapRoundTeamsCommand;
import com.ligitabl.application.command.UpdatePredictionOrderCommand;
import com.ligitabl.application.error.UseCaseError;
import com.ligitabl.application.usecase.prediction.GetSwapStatusUseCase;
import com.ligitabl.application.usecase.prediction.ResetDemoUseCase;
import com.ligitabl.application.usecase.prediction.SwapRoundTeamsUseCase;
import com.ligitabl.application.usecase.prediction.UpdatePredictionOrderUseCase;
import com.ligitabl.application.usecase.seasonprediction.GetSeasonPredictionUseCase;
import com.ligitabl.domain.model.season.SeasonId;
import com.ligitabl.domain.model.seasonprediction.RankingSource;
import com.ligitabl.domain.model.seasonprediction.TeamRanking;
import com.ligitabl.domain.model.user.UserId;
import com.ligitabl.dto.Responses;
import com.ligitabl.presentation.dto.response.RankingDTO;
import com.ligitabl.presentation.mapper.ErrorViewMapper;
import com.ligitabl.presentation.mapper.PredictionViewMapper;
import com.ligitabl.presentation.mapper.SeasonPredictionViewMapper;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

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
 *
 * @deprecated Use {@link UserPredictionsController} at /predictions/user/* instead.
 *             This experimental controller (/predictions2) will be removed in a future version.
 *             Migration:
 *             - /predictions2/me → /predictions/user/me
 *             - /predictions2/swap → (use swap endpoints from new controller)
 *             - /predictions2/demo-reset → (use new demo reset endpoint)
 */
@Deprecated(since = "2026-01-23", forRemoval = true)
@Controller
@RequestMapping("/predictions2")  // Use /predictions2 initially, switch to /predictions once stable
public class PredictionsController {

    private static final Logger log = LoggerFactory.getLogger(PredictionsController.class);
    private static final int CURRENT_ROUND = 19;  // TODO: Get from config or service
    private static final String DEFAULT_USER_ID = "00000000-0000-0000-0000-000000000000";

    private final GetSeasonPredictionUseCase getSeasonPredictionUseCase;
    private final GetSwapStatusUseCase getSwapStatusUseCase;
    private final SwapRoundTeamsUseCase swapRoundTeamsUseCase;
    private final UpdatePredictionOrderUseCase updatePredictionOrderUseCase;
    private final ResetDemoUseCase resetDemoUseCase;
    private final SeasonPredictionViewMapper viewMapper;
    private final PredictionViewMapper predictionViewMapper;
    private final ErrorViewMapper errorMapper;
    private final ObjectMapper objectMapper;
    private final SeasonId activeSeasonId;

    public PredictionsController(
        GetSeasonPredictionUseCase getSeasonPredictionUseCase,
        GetSwapStatusUseCase getSwapStatusUseCase,
        SwapRoundTeamsUseCase swapRoundTeamsUseCase,
        UpdatePredictionOrderUseCase updatePredictionOrderUseCase,
        ResetDemoUseCase resetDemoUseCase,
        SeasonPredictionViewMapper viewMapper,
        PredictionViewMapper predictionViewMapper,
        ErrorViewMapper errorMapper,
        ObjectMapper objectMapper,
        SeasonId activeSeasonId
    ) {
        this.getSeasonPredictionUseCase = getSeasonPredictionUseCase;
        this.getSwapStatusUseCase = getSwapStatusUseCase;
        this.swapRoundTeamsUseCase = swapRoundTeamsUseCase;
        this.updatePredictionOrderUseCase = updatePredictionOrderUseCase;
        this.resetDemoUseCase = resetDemoUseCase;
        this.viewMapper = viewMapper;
        this.predictionViewMapper = predictionViewMapper;
        this.errorMapper = errorMapper;
        this.objectMapper = objectMapper;
        this.activeSeasonId = activeSeasonId;
    }

    /**
     * GET /predictions2/demo-reset - Reset demo state and redirect to predictions page.
     */
    @GetMapping("/demo-reset")
    public String resetDemo(RedirectAttributes redirectAttributes) {
        Either<UseCaseError, ResetDemoUseCase.ResetResult> result = resetDemoUseCase.execute();

        return result.fold(
            error -> {
                redirectAttributes.addFlashAttribute("message", error.message());
                redirectAttributes.addFlashAttribute("messageType", "error");
                return "redirect:/predictions2/me";
            },
            success -> {
                redirectAttributes.addFlashAttribute("message", success.message());
                redirectAttributes.addFlashAttribute("messageType", "success");
                return "redirect:/predictions2/me";
            }
        );
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

    /**
     * GET /predictions2/me/swap-status - Get swap status as JSON.
     */
    @GetMapping("/me/swap-status")
    @ResponseBody
    public Responses.SwapStatusResponse getSwapStatus() {
        UserId userId = UserId.of(DEFAULT_USER_ID);

        Either<UseCaseError, GetSwapStatusUseCase.SwapStatusResult> result =
            getSwapStatusUseCase.execute(userId);

        return result.fold(
            error -> new Responses.SwapStatusResponse(
                "ERROR", false, null, null, 0.0, error.message()
            ),
            predictionViewMapper::toSwapStatusResponse
        );
    }

    /**
     * POST /predictions2/swap - Swap two teams in prediction.
     */
    @PostMapping("/swap")
    public String makeSwap(
        @RequestParam String teamA,
        @RequestParam String teamB,
        Model model,
        HttpServletResponse response
    ) {
        SwapRoundTeamsCommand command = SwapRoundTeamsCommand.of(DEFAULT_USER_ID, teamA, teamB);

        Either<UseCaseError, SwapRoundTeamsUseCase.SwapResult> result =
            swapRoundTeamsUseCase.execute(command);

        return result.fold(
            error -> {
                response.setStatus(mapErrorToStatus(error));
                model.addAttribute("error", errorMapper.toResponse(error));
                return "fragments/error-banner :: banner";
            },
            swapResult -> {
                List<Responses.PredictionRow> predictions =
                    predictionViewMapper.toDTOs(swapResult.predictions());
                model.addAttribute("predictions", predictions);
                return "fragments/prediction-table";
            }
        );
    }

    /**
     * POST /predictions2/swap-multiple - Update full prediction order (multiple swaps in one request).
     */
    @PostMapping("/swap-multiple")
    @ResponseBody
    public java.util.Map<String, Object> makeMultipleSwaps(@RequestBody java.util.Map<String, List<String>> request) {
        List<String> teamCodes = request.get("teamCodes");
        UserId userId = UserId.of(DEFAULT_USER_ID);

        UpdatePredictionOrderCommand command = UpdatePredictionOrderCommand.of(
            userId.value(),
            teamCodes
        );

        Either<UseCaseError, UpdatePredictionOrderUseCase.UpdateResult> result =
            updatePredictionOrderUseCase.execute(command);

        return result.fold(
            error -> java.util.Map.of("success", false, "message", error.message()),
            success -> java.util.Map.of("success", true, "message", success.message())
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
            Responses.SwapStatusResponse swapStatus =
                predictionViewMapper.toSwapStatusResponse(data.swapCooldown(), data.roundState());
            model.addAttribute("swapStatus", swapStatus);
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

    private int mapErrorToStatus(UseCaseError error) {
        return switch (error.type()) {
            case VALIDATION -> 400;
            case NOT_FOUND -> 404;
            case CONFLICT -> 409;
            case BUSINESS_RULE -> 422;
        };
    }
}
