package com.ligitabl.presentation.api;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ligitabl.api.shared.Either;
import com.ligitabl.application.command.GetUserPredictionCommand;
import com.ligitabl.application.error.UseCaseError;
import com.ligitabl.application.usecase.prediction.GetUserPredictionUseCase;
import com.ligitabl.domain.model.contest.ContestId;
import com.ligitabl.domain.model.season.SeasonId;
import com.ligitabl.domain.model.user.UserId;
import com.ligitabl.domain.repository.MainContestEntryRepository;
import com.ligitabl.domain.repository.RoundPredictionRepository;
import com.ligitabl.domain.repository.SeasonPredictionRepository;
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
 * Controller for user-centric prediction endpoints.
 *
 * <p>Provides endpoints:
 * <ul>
 *   <li><b>GET /predictions/user/me</b> - Current user's predictions (redirects to /guest if not logged in)</li>
 *   <li><b>GET /predictions/user/guest</b> - Guest view with localStorage swaps (always fallback, readonly)</li>
 *   <li><b>GET /predictions/user/{userId}</b> - View specific user's predictions</li>
 * </ul>
 *
 * <p>User resolution:
 * <ul>
 *   <li><b>/me</b> with no Principal → Redirects to /predictions/user/guest</li>
 *   <li><b>/me</b> with Principal + prediction → Own prediction (EDITABLE or READONLY_COOLDOWN)</li>
 *   <li><b>/me</b> with Principal + no prediction → Fallback (CAN_CREATE_ENTRY)</li>
 *   <li><b>/{userId}</b> matching own ID → Same as /me</li>
 *   <li><b>/{userId}</b> for existing user → Their prediction (READONLY_VIEWING_OTHER)</li>
 *   <li><b>/{userId}</b> for non-existent user → Fallback (READONLY_USER_NOT_FOUND)</li>
 * </ul>
 */
@Controller
@RequestMapping("/predictions/user")
public class UserPredictionsController {

    private static final Logger log = LoggerFactory.getLogger(UserPredictionsController.class);

    private final GetUserPredictionUseCase getUserPredictionUseCase;
    private final SeasonPredictionRepository seasonPredictionRepository;
    private final RoundPredictionRepository roundPredictionRepository;
    private final MainContestEntryRepository mainContestEntryRepository;
    private final SeasonPredictionViewMapper viewMapper;
    private final ErrorViewMapper errorMapper;
    private final ObjectMapper objectMapper;
    private final SeasonId activeSeasonId;
    private final ContestId mainContestId;

    public UserPredictionsController(
        GetUserPredictionUseCase getUserPredictionUseCase,
        SeasonPredictionRepository seasonPredictionRepository,
        RoundPredictionRepository roundPredictionRepository,
        MainContestEntryRepository mainContestEntryRepository,
        SeasonPredictionViewMapper viewMapper,
        ErrorViewMapper errorMapper,
        ObjectMapper objectMapper,
        SeasonId activeSeasonId,
        ContestId mainContestId
    ) {
        this.getUserPredictionUseCase = getUserPredictionUseCase;
        this.seasonPredictionRepository = seasonPredictionRepository;
        this.roundPredictionRepository = roundPredictionRepository;
        this.mainContestEntryRepository = mainContestEntryRepository;
        this.viewMapper = viewMapper;
        this.errorMapper = errorMapper;
        this.objectMapper = objectMapper;
        this.activeSeasonId = activeSeasonId;
        this.mainContestId = mainContestId;
    }

    /**
     * GET /predictions/user/me - View current user's predictions.
     *
     * <p>Resolves user from Principal:
     * <ul>
     *   <li>Not logged in → Redirects to /predictions/user/guest</li>
     *   <li>Logged in + has prediction → Returns user's prediction (EDITABLE or READONLY_COOLDOWN)</li>
     *   <li>Logged in + no prediction → Returns fallback as CAN_CREATE_ENTRY</li>
     * </ul>
     */
    @GetMapping("/me")
    public String myPredictions(
        @RequestParam(required = false) Integer round,
        Principal principal,
        Model model,
        HttpServletResponse response,
        @RequestHeader(value = "HX-Request", required = false) String hxRequest
    ) {
        log.info("GET /predictions/user/me - round: {}, user: {}",
            round, principal != null ? principal.getName() : "guest");

        // Redirect guests to /guest endpoint - /me implies "my account"
        if (principal == null) {
            String redirect = "redirect:/predictions/user/guest";
            if (round != null) {
                redirect += "?round=" + round;
            }
            return redirect;
        }

        GetUserPredictionCommand command = buildCommandForMe(principal, round);

        Either<UseCaseError, GetUserPredictionUseCase.UserPredictionViewData> result =
            getUserPredictionUseCase.execute(command);

        return result.fold(
            error -> handleError(error, model, response, hxRequest),
            data -> handleSuccess(data, model, hxRequest)
        );
    }

    /**
     * GET /predictions/user/guest - Explicit guest view.
     *
     * <p>Always returns fallback rankings as READONLY_GUEST, regardless of authentication.</p>
     */
    @GetMapping("/guest")
    public String guestPredictions(
        @RequestParam(required = false) Integer round,
        Model model,
        HttpServletResponse response,
        @RequestHeader(value = "HX-Request", required = false) String hxRequest
    ) {
        log.info("GET /predictions/user/guest - round: {}", round);

        GetUserPredictionCommand command = GetUserPredictionCommand.forGuest(activeSeasonId, round);

        Either<UseCaseError, GetUserPredictionUseCase.UserPredictionViewData> result =
            getUserPredictionUseCase.execute(command);

        return result.fold(
            error -> handleError(error, model, response, hxRequest),
            data -> handleSuccess(data, model, hxRequest)
        );
    }

    /**
     * GET /predictions/user/{userId} - View specific user's predictions.
     *
     * <p>Resolution logic:
     * <ul>
     *   <li>If userId matches logged-in user → Treat as /me</li>
     *   <li>If userId is valid existing user → Show their prediction (READONLY_VIEWING_OTHER)</li>
     *   <li>If userId doesn't exist → Show fallback (READONLY_USER_NOT_FOUND)</li>
     * </ul>
     */
    @GetMapping("/{userId}")
    public String userPredictions(
        @PathVariable String userId,
        @RequestParam(required = false) Integer round,
        Principal principal,
        Model model,
        HttpServletResponse response,
        @RequestHeader(value = "HX-Request", required = false) String hxRequest
    ) {
        log.info("GET /predictions/user/{} - round: {}, viewer: {}",
            userId, round, principal != null ? principal.getName() : "guest");

        // Check if viewing own predictions
        if (principal != null && principal.getName().equals(userId)) {
            return myPredictions(round, principal, model, response, hxRequest);
        }

        GetUserPredictionCommand command = buildCommandForUser(userId, round);

        Either<UseCaseError, GetUserPredictionUseCase.UserPredictionViewData> result =
            getUserPredictionUseCase.execute(command);

        return result.fold(
            error -> handleError(error, model, response, hxRequest),
            data -> handleSuccess(data, model, hxRequest)
        );
    }

    // --- Private Helper Methods ---

    /**
     * Build command for /me endpoint based on Principal.
     */
    private GetUserPredictionCommand buildCommandForMe(Principal principal, Integer round) {
        if (principal == null) {
            return GetUserPredictionCommand.forGuest(activeSeasonId, round);
        }

        UserId userId = UserId.of(principal.getName());
        boolean hasEntry = mainContestEntryRepository.existsByUserIdAndContestId(userId, mainContestId);

        // Check both season prediction and round prediction
        // User has prediction if they made either a season prediction OR an initial round prediction
        boolean hasSeasonPrediction = seasonPredictionRepository.existsByUserIdAndSeasonId(userId, activeSeasonId);
        boolean hasRoundPrediction = roundPredictionRepository.hasInitialPrediction(userId);
        boolean hasPrediction = hasSeasonPrediction || hasRoundPrediction;

        return GetUserPredictionCommand.forAuthenticatedUser(
            userId, activeSeasonId, hasEntry, hasPrediction, round
        );
    }

    /**
     * Build command for /{userId} endpoint.
     */
    private GetUserPredictionCommand buildCommandForUser(String userIdStr, Integer round) {
        // Validate UUID format
        UserId targetUserId;
        try {
            targetUserId = UserId.of(userIdStr);
        } catch (IllegalArgumentException e) {
            // Invalid UUID format - treat as user not found
            return GetUserPredictionCommand.forNonExistentUser(activeSeasonId, round);
        }

        // Check if user has a prediction (as proxy for user existence)
        // In a real system, we'd check a UserRepository
        boolean hasPrediction = seasonPredictionRepository.existsByUserIdAndSeasonId(targetUserId, activeSeasonId);

        // For now, assume user exists if they have a prediction
        // TODO: Add proper user existence check when UserRepository is available
        if (hasPrediction) {
            // User exists and has prediction
            String displayName = "User"; // TODO: Get from UserRepository
            return GetUserPredictionCommand.forViewingOtherUser(
                targetUserId, activeSeasonId, true, displayName, round
            );
        }

        // Check if user has a contest entry (another proxy for existence)
        boolean hasEntry = mainContestEntryRepository.existsByUserIdAndContestId(targetUserId, mainContestId);

        if (hasEntry) {
            // User exists but has no prediction yet
            String displayName = "User"; // TODO: Get from UserRepository
            return GetUserPredictionCommand.forViewingOtherUser(
                targetUserId, activeSeasonId, false, displayName, round
            );
        }

        // No prediction and no entry - treat as user not found
        // In a real system, we'd check UserRepository here
        return GetUserPredictionCommand.forNonExistentUser(activeSeasonId, round);
    }

    /**
     * Handle use case error.
     */
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

    /**
     * Handle successful use case result.
     */
    private String handleSuccess(
        GetUserPredictionUseCase.UserPredictionViewData data,
        Model model,
        String hxRequest
    ) {
        // Convert rankings to DTOs
        List<RankingDTO> predictions = data.rankings().stream()
            .map(viewMapper::toRankingDTO)
            .toList();

        // Set model attributes for template
        model.addAttribute("pageTitle", getPageTitle(data));
        model.addAttribute("currentRound", data.currentRound());
        model.addAttribute("viewingRound", data.viewingRound());
        model.addAttribute("isCurrentRound", data.isCurrentRound());
        model.addAttribute("roundState", data.roundState().toLowerCase());
        model.addAttribute("predictions", predictions);

        // Access mode attributes
        model.addAttribute("accessMode", data.accessMode().name());
        model.addAttribute("canSwap", data.canSwap());
        model.addAttribute("canCreateEntry", data.canCreateEntry());
        model.addAttribute("isReadonly", data.isReadonly());
        model.addAttribute("isGuest", data.isGuest());
        model.addAttribute("isViewingOther", data.isViewingOther());
        model.addAttribute("isUserNotFound", data.isUserNotFound());

        // Message for UI banners
        model.addAttribute("message", data.message());
        model.addAttribute("targetDisplayName", data.targetDisplayName());

        // Swap status for cooldown banners
        if (data.swapCooldown() != null) {
            var cooldown = data.swapCooldown();
            var now = java.time.Instant.now();
            model.addAttribute("swapStatus", new SwapStatusDTO(
                cooldown.canSwap(now),
                cooldown.getStatusMessage(now),
                cooldown.getLastSwapAtFormatted(),
                cooldown.initialPredictionMade(),
                cooldown.swapCount()
            ));
        }

        // Source information
        model.addAttribute("source", data.source().name());
        model.addAttribute("sourceLabel", getSourceLabel(data));

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
            return "predictions/user/me :: predictionPage";
        }
        return "predictions/user/me";
    }

    /**
     * Get page title based on access mode.
     */
    private String getPageTitle(GetUserPredictionUseCase.UserPredictionViewData data) {
        return switch (data.accessMode()) {
            case EDITABLE, READONLY_COOLDOWN -> "My Predictions";
            case CAN_CREATE_ENTRY -> "Create Prediction";
            case READONLY_GUEST -> "Predictions";
            case READONLY_VIEWING_OTHER -> data.targetDisplayName() != null
                ? data.targetDisplayName() + "'s Predictions"
                : "User Predictions";
            case READONLY_USER_NOT_FOUND -> "User Not Found";
        };
    }

    /**
     * Get source label for UI display.
     */
    private String getSourceLabel(GetUserPredictionUseCase.UserPredictionViewData data) {
        return switch (data.source()) {
            case USER_PREDICTION -> "Your Prediction";
            case ROUND_STANDINGS -> "Current Standings";
            case SEASON_BASELINE -> "Season Baseline";
        };
    }

    /**
     * Map use case error to HTTP status code.
     */
    private int mapErrorToStatus(UseCaseError error) {
        return switch (error.type()) {
            case VALIDATION -> 400;
            case NOT_FOUND -> 404;
            case CONFLICT -> 409;
            case BUSINESS_RULE -> 422;
        };
    }

    /**
     * DTO for swap status information displayed in templates.
     */
    public record SwapStatusDTO(
        boolean canSwap,
        String message,
        String lastSwapAt,
        boolean initialPredictionMade,
        int swapCount
    ) {
        /**
         * Check if this is the first swap bonus (can swap without cooldown).
         */
        public boolean isFirstSwapBonus() {
            return initialPredictionMade && swapCount == 0 && canSwap;
        }
    }
}
