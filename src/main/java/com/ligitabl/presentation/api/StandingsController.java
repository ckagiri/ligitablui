package com.ligitabl.presentation.api;

import com.ligitabl.application.command.GetMatchesCommand;
import com.ligitabl.application.command.GetStandingsCommand;
import com.ligitabl.application.error.UseCaseError;
import com.ligitabl.application.usecase.standing.GetMatchesUseCase;
import com.ligitabl.application.usecase.standing.GetStandingsUseCase;
import com.ligitabl.domain.model.season.SeasonId;
import com.ligitabl.presentation.mapper.StandingViewMapper;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Controller for standings and matches endpoints using Clean Architecture.
 *
 * <p>Uses the fold pattern to handle Either results from use cases,
 * maintaining backward compatibility with existing Thymeleaf templates.</p>
 */
@Controller
public class StandingsController {

    private final GetStandingsUseCase getStandingsUseCase;
    private final GetMatchesUseCase getMatchesUseCase;
    private final StandingViewMapper viewMapper;
    private final SeasonId activeSeasonId;

    public StandingsController(
        GetStandingsUseCase getStandingsUseCase,
        GetMatchesUseCase getMatchesUseCase,
        StandingViewMapper viewMapper,
        SeasonId activeSeasonId
    ) {
        this.getStandingsUseCase = getStandingsUseCase;
        this.getMatchesUseCase = getMatchesUseCase;
        this.viewMapper = viewMapper;
        this.activeSeasonId = activeSeasonId;
    }

    /**
     * Display league standings page (default season).
     */
    @GetMapping({"/standings", "/seasons/current/standings"})
    public String standingsDefault(
        @RequestParam(required = false, defaultValue = "19") Integer round,
        Model model,
        HttpServletResponse response
    ) {
        return standings(activeSeasonId.value(), round, model, response);
    }

    /**
     * Display league standings page.
     */
    @GetMapping("/seasons/{id:[0-9a-f\\-]+}/standings")
    public String standings(
        @PathVariable String id,
        @RequestParam(required = false, defaultValue = "19") Integer round,
        Model model,
        HttpServletResponse response
    ) {
        var command = GetStandingsCommand.of(id, round);

        return getStandingsUseCase.execute(command).fold(
            error -> handleStandingsError(error, model, response),
            result -> handleStandingsSuccess(result, model)
        );
    }

    /**
     * Display matches page (default season).
     */
    @GetMapping({"/matches", "/seasons/current/matches"})
    public String matchesDefault(
        @RequestParam(required = false, defaultValue = "19") Integer round,
        Model model,
        HttpServletResponse response
    ) {
        return matches(activeSeasonId.value(), round, model, response);
    }

    /**
     * Display matches page.
     */
    @GetMapping("/seasons/{id:[0-9a-f\\-]+}/matches")
    public String matches(
        @PathVariable String id,
        @RequestParam(required = false, defaultValue = "19") Integer round,
        Model model,
        HttpServletResponse response
    ) {
        var command = GetMatchesCommand.of(id, round);

        return getMatchesUseCase.execute(command).fold(
            error -> handleMatchesError(error, model, response),
            result -> handleMatchesSuccess(result, model)
        );
    }

    // ========== Success Handlers ==========

    private String handleStandingsSuccess(
        GetStandingsUseCase.StandingsResult result,
        Model model
    ) {
        var standingsDTOs = viewMapper.toLegacyDTOList(result.standings());

        model.addAttribute("pageTitle", "Standings");
        model.addAttribute("seasonId", result.seasonId());
        model.addAttribute("currentRound", result.round());
        model.addAttribute("standings", standingsDTOs);

        return "standings";
    }

    private String handleMatchesSuccess(
        GetMatchesUseCase.MatchesResult result,
        Model model
    ) {
        var matchesDTOs = viewMapper.toMatchDTOList(result.matches());

        model.addAttribute("pageTitle", "Matches");
        model.addAttribute("seasonId", result.seasonId());
        model.addAttribute("currentRound", result.round());
        model.addAttribute("matches", matchesDTOs);
        model.addAttribute("hasLiveMatches", result.hasLiveMatches());
        model.addAttribute("liveCount", result.liveCount());
        model.addAttribute("finishedCount", result.finishedCount());
        model.addAttribute("scheduledCount", result.scheduledCount());

        return "matches";
    }

    // ========== Error Handlers ==========

    private String handleStandingsError(
        UseCaseError error,
        Model model,
        HttpServletResponse response
    ) {
        response.setStatus(mapErrorToStatus(error));
        model.addAttribute("error", error.message());
        model.addAttribute("pageTitle", "Error");
        return "error";
    }

    private String handleMatchesError(
        UseCaseError error,
        Model model,
        HttpServletResponse response
    ) {
        response.setStatus(mapErrorToStatus(error));
        model.addAttribute("error", error.message());
        model.addAttribute("pageTitle", "Error");
        return "error";
    }

    private int mapErrorToStatus(UseCaseError error) {
        return switch (error.type()) {
            case NOT_FOUND -> 404;
            case VALIDATION -> 400;
            case CONFLICT -> 409;
            case BUSINESS_RULE -> 422;
        };
    }
}
