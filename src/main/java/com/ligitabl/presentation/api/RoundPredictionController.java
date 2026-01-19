package com.ligitabl.presentation.api;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ligitabl.api.shared.Either;
import com.ligitabl.application.command.GetMyPredictionCommand;
import com.ligitabl.application.command.SwapRoundTeamsCommand;
import com.ligitabl.application.command.UpdatePredictionOrderCommand;
import com.ligitabl.application.error.UseCaseError;
import com.ligitabl.application.usecase.prediction.GetMyPredictionUseCase;
import com.ligitabl.application.usecase.prediction.GetMyPredictionUseCase.PredictionViewData;
import com.ligitabl.application.usecase.prediction.GetSwapStatusUseCase;
import com.ligitabl.application.usecase.prediction.ResetDemoUseCase;
import com.ligitabl.application.usecase.prediction.SwapRoundTeamsUseCase;
import com.ligitabl.application.usecase.prediction.UpdatePredictionOrderUseCase;
import com.ligitabl.domain.model.prediction.PredictionStatus;
import com.ligitabl.domain.model.season.RoundNumber;
import com.ligitabl.domain.model.user.UserId;
import com.ligitabl.dto.Responses;
import com.ligitabl.presentation.mapper.ErrorViewMapper;
import com.ligitabl.presentation.mapper.PredictionViewMapper;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.Map;

/**
 * Controller for round prediction endpoints.
 * Migrated from PlayerController to use Clean Architecture patterns.
 */
@Controller
public class RoundPredictionController {

    private static final String DEFAULT_USER_ID = "00000000-0000-0000-0000-000000000000";

    private final GetMyPredictionUseCase getMyPredictionUseCase;
    private final GetSwapStatusUseCase getSwapStatusUseCase;
    private final SwapRoundTeamsUseCase swapRoundTeamsUseCase;
    private final UpdatePredictionOrderUseCase updatePredictionOrderUseCase;
    private final ResetDemoUseCase resetDemoUseCase;
    private final PredictionViewMapper viewMapper;
    private final ErrorViewMapper errorViewMapper;
    private final ObjectMapper objectMapper;

    public RoundPredictionController(
        GetMyPredictionUseCase getMyPredictionUseCase,
        GetSwapStatusUseCase getSwapStatusUseCase,
        SwapRoundTeamsUseCase swapRoundTeamsUseCase,
        UpdatePredictionOrderUseCase updatePredictionOrderUseCase,
        ResetDemoUseCase resetDemoUseCase,
        PredictionViewMapper viewMapper,
        ErrorViewMapper errorViewMapper,
        ObjectMapper objectMapper
    ) {
        this.getMyPredictionUseCase = getMyPredictionUseCase;
        this.getSwapStatusUseCase = getSwapStatusUseCase;
        this.swapRoundTeamsUseCase = swapRoundTeamsUseCase;
        this.updatePredictionOrderUseCase = updatePredictionOrderUseCase;
        this.resetDemoUseCase = resetDemoUseCase;
        this.viewMapper = viewMapper;
        this.errorViewMapper = errorViewMapper;
        this.objectMapper = objectMapper;
    }

    /**
     * Reset demo state and redirect to predictions page.
     */
    @GetMapping("/predictions/demo-reset")
    public String resetDemo(RedirectAttributes redirectAttributes) {
        Either<UseCaseError, ResetDemoUseCase.ResetResult> result = resetDemoUseCase.execute();

        return result.fold(
            error -> {
                redirectAttributes.addFlashAttribute("message", error.message());
                redirectAttributes.addFlashAttribute("messageType", "error");
                return "redirect:/predictions/me";
            },
            success -> {
                redirectAttributes.addFlashAttribute("message", success.message());
                redirectAttributes.addFlashAttribute("messageType", "success");
                return "redirect:/predictions/me";
            }
        );
    }

    /**
     * Display user's predictions page.
     */
    @GetMapping("/predictions/me")
    public String myPredictions(
        @RequestParam(required = false, defaultValue = "19") Integer round,
        @RequestParam(required = false, defaultValue = "open") String state,
        @RequestHeader(value = "HX-Request", required = false) String hxRequest,
        Model model,
        HttpServletResponse response
    ) {
        UserId userId = UserId.of(DEFAULT_USER_ID);
        PredictionStatus roundState = parseRoundState(state);
        RoundNumber roundNumber = RoundNumber.of(round);

        GetMyPredictionCommand command = GetMyPredictionCommand.builder()
            .userId(userId)
            .round(roundNumber)
            .roundState(roundState)
            .build();

        Either<UseCaseError, PredictionViewData> result = getMyPredictionUseCase.execute(command);

        return result.fold(
            error -> handlePredictionError(error, model, response, hxRequest),
            data -> handlePredictionSuccess(data, model, hxRequest)
        );
    }

    /**
     * Get swap status as JSON.
     */
    @GetMapping("/predictions/me/swap-status")
    @ResponseBody
    public Responses.SwapStatusResponse getSwapStatus() {
        UserId userId = UserId.of(DEFAULT_USER_ID);

        Either<UseCaseError, GetSwapStatusUseCase.SwapStatusResult> result =
            getSwapStatusUseCase.execute(userId);

        return result.fold(
            error -> new Responses.SwapStatusResponse(
                "ERROR", false, null, null, 0.0, error.message()
            ),
            viewMapper::toSwapStatusResponse
        );
    }

    /**
     * Swap two teams in prediction.
     */
    @PostMapping("/predictions/swap")
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
                model.addAttribute("error", errorViewMapper.toResponse(error));
                return "fragments/error-banner :: banner";
            },
            swapResult -> {
                List<Responses.PredictionRow> predictions =
                    viewMapper.toDTOs(swapResult.predictions());
                model.addAttribute("predictions", predictions);
                return "fragments/prediction-table";
            }
        );
    }

    /**
     * Update full prediction order (multiple swaps in one request).
     */
    @PostMapping("/predictions/swap-multiple")
    @ResponseBody
    public Map<String, Object> makeMultipleSwaps(@RequestBody Map<String, List<String>> request) {
        List<String> teamCodes = request.get("teamCodes");
        UserId userId = UserId.of(DEFAULT_USER_ID);

        UpdatePredictionOrderCommand command = UpdatePredictionOrderCommand.of(
            userId.value(),
            teamCodes
        );

        Either<UseCaseError, UpdatePredictionOrderUseCase.UpdateResult> result =
            updatePredictionOrderUseCase.execute(command);

        return result.fold(
            error -> Map.of("success", false, "message", error.message()),
            success -> Map.of("success", true, "message", success.message())
        );
    }

    // --- Private helper methods ---

    private String handlePredictionError(
        UseCaseError error,
        Model model,
        HttpServletResponse response,
        String hxRequest
    ) {
        response.setStatus(mapErrorToStatus(error));
        model.addAttribute("pageTitle", "My Predictions");
        model.addAttribute("error", errorViewMapper.toResponse(error));

        if (hxRequest != null && !hxRequest.isBlank()) {
            return "fragments/error-banner :: banner";
        }
        return "error";
    }

    private String handlePredictionSuccess(PredictionViewData data, Model model, String hxRequest) {
        // Convert domain predictions to DTOs
        List<Responses.PredictionRow> predictions = viewMapper.toDTOs(data.predictions());

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

        // Convert swap status if available
        if (data.swapCooldown() != null) {
            Responses.SwapStatusResponse swapStatus =
                viewMapper.toSwapStatusResponse(data.swapCooldown(), data.roundState());
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
            model.addAttribute("fixturesJson", "{}");
            model.addAttribute("predictionsJson", "[]");
            model.addAttribute("currentStandingsJson", "{}");
            model.addAttribute("currentPointsJson", "{}");
        }

        if (hxRequest != null && !hxRequest.isBlank()) {
            return "predictions/me :: predictionPage";
        }

        return "predictions/me";
    }

    private PredictionStatus parseRoundState(String state) {
        return switch (state.toLowerCase()) {
            case "locked" -> PredictionStatus.LOCKED;
            case "completed" -> PredictionStatus.COMPLETED;
            default -> PredictionStatus.OPEN;
        };
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
