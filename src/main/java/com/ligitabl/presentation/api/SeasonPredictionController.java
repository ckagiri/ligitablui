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
import com.ligitabl.domain.model.seasonprediction.SeasonPrediction;
import com.ligitabl.domain.model.seasonprediction.TeamRanking;
import com.ligitabl.domain.model.team.TeamId;
import com.ligitabl.domain.model.user.UserId;
import com.ligitabl.domain.repository.SeasonPredictionRepository;
import com.ligitabl.presentation.dto.request.CreateSeasonPredictionRequest;
import com.ligitabl.presentation.dto.request.SwapTeamsRequest;
import com.ligitabl.presentation.dto.response.ErrorResponse;
import com.ligitabl.presentation.dto.response.SeasonPredictionResponse;
import com.ligitabl.presentation.mapper.ErrorViewMapper;
import com.ligitabl.presentation.mapper.SeasonPredictionViewMapper;
import com.ligitabl.presentation.mapper.TeamCodeResolver;
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
import java.util.Map;

/**
 * Controller for Season Prediction endpoints.
 *
 * <p>Implements Railway-Oriented Programming with the fold pattern.
 * All use cases return Either&lt;UseCaseError, Result&gt;, and this controller
 * uses .fold() to switch between error and success tracks.</p>
 *
 * <p>Three endpoints:
 * 1. GET /seasonprediction - Smart fallback retrieval
 * 2. POST /seasonprediction - Initial join (creates prediction + contest entry)
 * 3. POST /seasonprediction/swap - Single swap update (team codes only)</p>
 */
@Controller
@RequestMapping("/seasonprediction")
public class SeasonPredictionController {

    private static final Logger log = LoggerFactory.getLogger(SeasonPredictionController.class);

    private final GetSeasonPredictionUseCase getSeasonPredictionUseCase;
    private final CreateSeasonPredictionUseCase createSeasonPredictionUseCase;
    private final SwapTeamsUseCase swapTeamsUseCase;
    private final SeasonPredictionRepository seasonPredictionRepository;
    private final SeasonPredictionViewMapper viewMapper;
    private final ErrorViewMapper errorMapper;
    private final TeamCodeResolver teamCodeResolver;
    private final SeasonId activeSeasonId;
    private final ObjectMapper objectMapper;

    public SeasonPredictionController(
        GetSeasonPredictionUseCase getSeasonPredictionUseCase,
        CreateSeasonPredictionUseCase createSeasonPredictionUseCase,
        SwapTeamsUseCase swapTeamsUseCase,
        SeasonPredictionRepository seasonPredictionRepository,
        SeasonPredictionViewMapper viewMapper,
        ErrorViewMapper errorMapper,
        TeamCodeResolver teamCodeResolver,
        SeasonId activeSeasonId,
        ObjectMapper objectMapper
    ) {
        this.getSeasonPredictionUseCase = getSeasonPredictionUseCase;
        this.createSeasonPredictionUseCase = createSeasonPredictionUseCase;
        this.swapTeamsUseCase = swapTeamsUseCase;
        this.seasonPredictionRepository = seasonPredictionRepository;
        this.viewMapper = viewMapper;
        this.errorMapper = errorMapper;
        this.teamCodeResolver = teamCodeResolver;
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
     */
    @GetMapping
    public String getSeasonPrediction(
        Principal principal,
        Model model,
        HttpServletResponse response,
        @RequestHeader(value = "HX-Request", required = false) String hxRequest
    ) {
        log.info("GET /seasonprediction - user: {}", principal != null ? principal.getName() : "guest");

        UserId userId = principal != null
            ? UserId.of(principal.getName())
            : UserId.generate();

        GetSeasonPredictionCommand command = GetSeasonPredictionCommand.forSeason(userId, activeSeasonId);

        Either<UseCaseError, GetSeasonPredictionUseCase.PredictionViewData> result =
            getSeasonPredictionUseCase.execute(command);

        return result.fold(
            error -> handleError(error, model, response, hxRequest),
            data -> handleGetSuccess(
                new GetSeasonPredictionUseCase.RankingsWithSource(data.source(), data.rankings()),
                model,
                hxRequest
            )
        );
    }

    /**
     * POST /seasonprediction - Create initial season prediction.
     *
     * <p>Accepts ordered list of team codes. Position is implied by array index.
     * Auto-creates MainContestEntry as side-effect.</p>
     *
     * <p>Request: {@code { "teamCodes": ["MCI", "ARS", "LIV", ...] }}</p>
     * <p>Response: {@code { "success": true, "message": "..." }}</p>
     */
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public Map<String, Object> createSeasonPrediction(
        @RequestBody CreateSeasonPredictionRequest request,
        Principal principal,
        HttpServletResponse response
    ) {
        if (principal == null) {
            response.setStatus(401);
            return Map.of("success", false, "message", "Authentication required");
        }

        log.info("POST /seasonprediction - user: {}, teams: {}",
            principal.getName(), request.teamCodes().size());

        UserId userId = UserId.of(principal.getName());
        List<TeamRanking> rankings = viewMapper.toTeamRankings(request);

        CreateSeasonPredictionCommand command = CreateSeasonPredictionCommand.of(
            userId,
            activeSeasonId,
            rankings
        );

        Either<UseCaseError, CreateSeasonPredictionUseCase.CreatedResult> result =
            createSeasonPredictionUseCase.execute(command);

        return result.fold(
            error -> {
                response.setStatus(errorMapper.toHttpStatus(error));
                log.warn("Create prediction failed: {}", error.message());
                return Map.of("success", false, "message", error.message());
            },
            created -> {
                log.info("Created season prediction: {}", created.prediction().getId());
                return Map.of("success", true, "message", "Prediction created successfully");
            }
        );
    }

    /**
     * POST /seasonprediction/swap - Swap exactly two teams by code.
     *
     * <p>Positions are resolved server-side from the user's current prediction.</p>
     *
     * <p>Request: {@code { "teamACode": "MCI", "teamBCode": "ARS" }}</p>
     * <p>Response: {@code { "success": true, "message": "..." }}</p>
     */
    @PostMapping(value = "/swap", consumes = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public Map<String, Object> swapTeams(
        @RequestBody SwapTeamsRequest request,
        Principal principal,
        HttpServletResponse response
    ) {
        if (principal == null) {
            response.setStatus(401);
            return Map.of("success", false, "message", "Authentication required");
        }

        log.info("POST /seasonprediction/swap - user: {}, teamA: {}, teamB: {}",
            principal.getName(), request.teamACode(), request.teamBCode());

        UserId userId = UserId.of(principal.getName());

        // Resolve team codes to TeamIds
        TeamId teamAId = teamCodeResolver.resolve(request.teamACode());
        TeamId teamBId = teamCodeResolver.resolve(request.teamBCode());

        // Load prediction to resolve current positions
        SeasonPrediction prediction = seasonPredictionRepository
            .findByUserIdAndSeasonId(userId, activeSeasonId)
            .orElse(null);

        if (prediction == null) {
            response.setStatus(404);
            return Map.of("success", false, "message", "Season prediction not found");
        }

        // Find current positions from the prediction's rankings
        int teamAPosition = findPosition(prediction, teamAId);
        int teamBPosition = findPosition(prediction, teamBId);

        if (teamAPosition == -1 || teamBPosition == -1) {
            response.setStatus(400);
            return Map.of("success", false, "message", "Team not found in prediction");
        }

        SwapTeamsCommand command = SwapTeamsCommand.of(
            userId,
            activeSeasonId,
            teamAId,
            teamAPosition,
            teamBId,
            teamBPosition
        );

        Either<UseCaseError, SeasonPrediction> result = swapTeamsUseCase.execute(command);

        return result.fold(
            error -> {
                response.setStatus(errorMapper.toHttpStatus(error));
                log.warn("Swap failed: {}", error.message());
                return Map.of("success", false, "message", error.message());
            },
            updated -> {
                log.info("Swapped teams successfully: {}", updated.getId());
                return Map.of("success", true, "message", "Prediction updated successfully");
            }
        );
    }

    /**
     * Find the current position of a team in a prediction.
     *
     * @return the position (1-20) or -1 if not found
     */
    private int findPosition(SeasonPrediction prediction, TeamId teamId) {
        return prediction.getRankings().stream()
            .filter(r -> r.teamId().equals(teamId))
            .findFirst()
            .map(TeamRanking::position)
            .orElse(-1);
    }

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

        return hxRequest != null
            ? "fragments/error-banner :: banner"
            : "predictions/index";
    }

    private String handleGetSuccess(
        GetSeasonPredictionUseCase.RankingsWithSource rankings,
        Model model,
        String hxRequest
    ) {
        SeasonPredictionResponse predictionResponse = viewMapper.toResponse(rankings);
        model.addAttribute("prediction", predictionResponse);
        model.addAttribute("source", rankings.source().name());
        model.addAttribute("pageTitle", "Season Prediction");

        try {
            String rankingsJson = objectMapper.writeValueAsString(predictionResponse.getRankings());
            model.addAttribute("rankingsJson", rankingsJson);
        } catch (Exception e) {
            log.error("Failed to serialize rankings to JSON", e);
            model.addAttribute("rankingsJson", "[]");
        }

        boolean hasSeasonPrediction = rankings.source() == com.ligitabl.domain.model.seasonprediction.RankingSource.USER_PREDICTION;
        model.addAttribute("hasSeasonPrediction", hasSeasonPrediction);

        log.info("Retrieved prediction from source: {}", rankings.source());

        return hxRequest != null
            ? "seasonprediction/index :: rankings-table"
            : "seasonprediction/index";
    }

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
