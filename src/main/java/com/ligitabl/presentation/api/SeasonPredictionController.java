package com.ligitabl.presentation.api;

import com.ligitabl.api.shared.Either;
import com.ligitabl.application.command.CreateSeasonPredictionCommand;
import com.ligitabl.application.command.GetSeasonPredictionCommand;
import com.ligitabl.application.command.SwapTeamsCommand;
import com.ligitabl.application.error.UseCaseError;
import com.ligitabl.application.usecase.seasonprediction.CreateSeasonPredictionUseCase;
import com.ligitabl.application.usecase.seasonprediction.GetSeasonPredictionUseCase;
import com.ligitabl.application.usecase.seasonprediction.SwapTeamsUseCase;
import com.ligitabl.domain.model.season.SeasonId;
import com.ligitabl.domain.model.seasonprediction.RankingSource;
import com.ligitabl.domain.model.seasonprediction.SeasonPrediction;
import com.ligitabl.domain.model.seasonprediction.TeamRanking;
import com.ligitabl.domain.model.team.TeamId;
import com.ligitabl.domain.model.user.UserId;
import com.ligitabl.presentation.dto.request.CreateSeasonPredictionRequest;
import com.ligitabl.presentation.dto.request.SwapTeamsRequest;
import com.ligitabl.presentation.dto.response.ErrorResponse;
import com.ligitabl.presentation.dto.response.SeasonPredictionResponse;
import com.ligitabl.presentation.mapper.ErrorViewMapper;
import com.ligitabl.presentation.mapper.SeasonPredictionViewMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

/**
 * Controller for Season Prediction endpoints.
 *
 * <p>Implements Railway-Oriented Programming with the fold pattern.
 * All use cases return Either<UseCaseError, Result>, and this controller
 * uses .fold() to switch between error and success tracks.</p>
 *
 * <p>Three endpoints:
 * 1. GET /seasonprediction - Smart fallback retrieval
 * 2. POST /seasonprediction - Initial join (creates prediction + contest entry)
 * 3. POST /seasonprediction/swap - Single swap update</p>
 */
@Controller
@RequestMapping("/seasonprediction")
public class SeasonPredictionController {

    private static final Logger log = LoggerFactory.getLogger(SeasonPredictionController.class);

    private final GetSeasonPredictionUseCase getSeasonPredictionUseCase;
    private final CreateSeasonPredictionUseCase createSeasonPredictionUseCase;
    private final SwapTeamsUseCase swapTeamsUseCase;
    private final SeasonPredictionViewMapper viewMapper;
    private final ErrorViewMapper errorMapper;
    private final SeasonId activeSeasonId;
    private final ObjectMapper objectMapper;

    public SeasonPredictionController(
        GetSeasonPredictionUseCase getSeasonPredictionUseCase,
        CreateSeasonPredictionUseCase createSeasonPredictionUseCase,
        SwapTeamsUseCase swapTeamsUseCase,
        SeasonPredictionViewMapper viewMapper,
        ErrorViewMapper errorMapper,
        SeasonId activeSeasonId,
        ObjectMapper objectMapper
    ) {
        this.getSeasonPredictionUseCase = getSeasonPredictionUseCase;
        this.createSeasonPredictionUseCase = createSeasonPredictionUseCase;
        this.swapTeamsUseCase = swapTeamsUseCase;
        this.viewMapper = viewMapper;
        this.errorMapper = errorMapper;
        this.activeSeasonId = activeSeasonId;
        this.objectMapper = objectMapper;
    }

    /**
     * GET /seasonprediction - Smart fallback retrieval.
     *
     * <p>Returns rankings with source indicator:
     * 1. User's own prediction (if exists)
     * 2. Current round standings (fallback)
     * 3. Season baseline (final fallback)</p>
     *
     * @param principal the authenticated user (can be null for guests)
     * @param model the view model
     * @param response the HTTP response (for setting status codes)
     * @param hxRequest HTMX request header (if present, return fragment)
     * @return view name or fragment
     */
    @GetMapping
    public String getSeasonPrediction(
        Principal principal,
        Model model,
        HttpServletResponse response,
        @RequestHeader(value = "HX-Request", required = false) String hxRequest
    ) {
        log.info("GET /seasonprediction - user: {}", principal != null ? principal.getName() : "guest");

        // For guests, return baseline (will be handled by fallback)
        UserId userId = principal != null
            ? UserId.of(principal.getName())
            : UserId.generate(); // Temp ID for guest

        GetSeasonPredictionCommand command = GetSeasonPredictionCommand.of(userId, activeSeasonId);

        Either<UseCaseError, GetSeasonPredictionUseCase.RankingsWithSource> result =
            getSeasonPredictionUseCase.execute(command);

        // Railway-Oriented fold pattern
        return result.fold(
            error -> handleError(error, model, response, hxRequest),
            rankings -> handleGetSuccess(rankings, model, hxRequest)
        );
    }

    /**
     * POST /seasonprediction - Create initial season prediction.
     *
     * <p>Used when user first submits their prediction (joins the competition).
     * Auto-creates MainContestEntry as side-effect.</p>
     *
     * <p>Business Rules:
     * - User must be authenticated
     * - User must not already have a prediction (409 if exists)
     * - Must provide exactly 20 teams with valid positions</p>
     *
     * @param request the create request with team rankings
     * @param principal the authenticated user
     * @param model the view model
     * @param response the HTTP response
     * @param hxRequest HTMX request header
     * @return view name or fragment
     */
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public SeasonPredictionResponse createSeasonPrediction(
        @RequestBody CreateSeasonPredictionRequest request,
        Principal principal,
        Model model,
        HttpServletResponse response,
        @RequestHeader(value = "HX-Request", required = false) String hxRequest
    ) {
        if (principal == null) {
            response.setStatus(401);
            throw new IllegalStateException("Authentication required");
        }

        log.info("POST /seasonprediction - user: {}, teams: {}",
            principal.getName(), request.getTeamRankings().size());

        UserId userId = UserId.of(principal.getName());
        List<TeamRanking> rankings = viewMapper.toTeamRankings(request);

        CreateSeasonPredictionCommand command = CreateSeasonPredictionCommand.of(
            userId,
            activeSeasonId,
            rankings
        );

        Either<UseCaseError, CreateSeasonPredictionUseCase.CreatedResult> result =
            createSeasonPredictionUseCase.execute(command);

        // Railway-Oriented fold pattern
        return result.fold(
            error -> {
                response.setStatus(errorMapper.toHttpStatus(error));
                log.warn("Create prediction failed: {}", error.message());
                throw new IllegalStateException(error.message()); // Will be caught by exception handler
            },
            created -> {
                log.info("Created season prediction: {}", created.prediction().getId());
                return viewMapper.toResponse(created);
            }
        );
    }

    /**
     * POST /seasonprediction/swap - Swap exactly two teams.
     *
     * <p>Used by existing participants to update their prediction.
     * CRITICAL: Only ONE swap per request.</p>
     *
     * <p>Business Rules:
     * - User must be authenticated
     * - User must already have a prediction (404 if not found)
     * - Exactly one team pair swap
     * - Current positions must match (optimistic locking)</p>
     *
     * @param request the swap request with two teams
     * @param principal the authenticated user
     * @param model the view model
     * @param response the HTTP response
     * @param hxRequest HTMX request header
     * @return view name or fragment
     */
    @PostMapping(value = "/swap", consumes = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public SeasonPredictionResponse swapTeams(
        @RequestBody SwapTeamsRequest request,
        Principal principal,
        Model model,
        HttpServletResponse response,
        @RequestHeader(value = "HX-Request", required = false) String hxRequest
    ) {
        if (principal == null) {
            response.setStatus(401);
            throw new IllegalStateException("Authentication required");
        }

        log.info("POST /seasonprediction/swap - user: {}, teamA: {}, teamB: {}",
            principal.getName(),
            request.getTeamA().getTeamId(),
            request.getTeamB().getTeamId());

        UserId userId = UserId.of(principal.getName());

        SwapTeamsCommand command = SwapTeamsCommand.of(
            userId,
            activeSeasonId,
            TeamId.of(request.getTeamA().getTeamId()),
            request.getTeamA().getCurrentPosition(),
            TeamId.of(request.getTeamB().getTeamId()),
            request.getTeamB().getCurrentPosition()
        );

        Either<UseCaseError, SeasonPrediction> result = swapTeamsUseCase.execute(command);

        // Railway-Oriented fold pattern
        return result.fold(
            error -> {
                response.setStatus(errorMapper.toHttpStatus(error));
                log.warn("Swap failed: {}", error.message());
                throw new IllegalStateException(error.message()); // Will be caught by exception handler
            },
            updated -> {
                log.info("Swapped teams successfully: {}", updated.getId());
                return viewMapper.toResponse(updated);
            }
        );
    }

    /**
     * Handle error track - map to error response.
     *
     * @param error the use case error
     * @param model the view model
     * @param response the HTTP response
     * @param hxRequest HTMX header
     * @return view name or fragment
     */
    private String handleError(
        UseCaseError error,
        Model model,
        HttpServletResponse response,
        String hxRequest
    ) {
        int statusCode = errorMapper.toHttpStatus(error);
        response.setStatus(statusCode);

        ErrorResponse errorResponse = errorMapper.toResponse(error);
        model.addAttribute("error", errorResponse);

        log.warn("Request failed with {}: {}", error.type(), error.message());

        // Return fragment for HTMX, full page otherwise
        return hxRequest != null
            ? "fragments/error-banner :: banner"
            : "predictions/index";
    }

    /**
     * Handle success track for GET - map to success response.
     *
     * @param rankings the rankings with source
     * @param model the view model
     * @param hxRequest HTMX header
     * @return view name or fragment
     */
    private String handleGetSuccess(
        GetSeasonPredictionUseCase.RankingsWithSource rankings,
        Model model,
        String hxRequest
    ) {
        SeasonPredictionResponse predictionResponse = viewMapper.toResponse(rankings);
        model.addAttribute("prediction", predictionResponse);
        model.addAttribute("source", rankings.source().name());

        // Serialize rankings to JSON for Alpine.js
        try {
            String rankingsJson = objectMapper.writeValueAsString(predictionResponse.getRankings());
            model.addAttribute("rankingsJson", rankingsJson);
        } catch (Exception e) {
            log.error("Failed to serialize rankings to JSON", e);
            model.addAttribute("rankingsJson", "[]");
        }

        // Flag to indicate if user has their own prediction
        boolean hasSeasonPrediction = rankings.source() == RankingSource.USER_PREDICTION;
        model.addAttribute("hasSeasonPrediction", hasSeasonPrediction);

        log.info("Retrieved prediction from source: {}", rankings.source());

        // Return fragment for HTMX, full page otherwise
        return hxRequest != null
            ? "seasonprediction/index :: rankings-table"
            : "seasonprediction/index";
    }

    /**
     * Global exception handler for this controller.
     *
     * @param ex the exception
     * @param response the HTTP response
     * @return error response
     */
    @ExceptionHandler(Exception.class)
    @ResponseBody
    public ErrorResponse handleException(Exception ex, HttpServletResponse response) {
        log.error("Unhandled exception", ex);
        response.setStatus(500);
        return new ErrorResponse(
            "INTERNAL_ERROR",
            "An unexpected error occurred",
            List.of(ex.getMessage())
        );
    }
}
