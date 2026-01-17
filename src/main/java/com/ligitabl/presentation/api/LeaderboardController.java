package com.ligitabl.presentation.api;

import com.ligitabl.application.command.GetLeaderboardCommand;
import com.ligitabl.application.command.GetUserDetailsCommand;
import com.ligitabl.application.error.UseCaseError;
import com.ligitabl.application.usecase.leaderboard.GetLeaderboardUseCase;
import com.ligitabl.application.usecase.leaderboard.GetUserDetailsUseCase;
import com.ligitabl.domain.model.leaderboard.Phase;
import com.ligitabl.presentation.mapper.LeaderboardViewMapper;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Controller for leaderboard endpoints using Clean Architecture.
 *
 * <p>Uses the fold pattern to handle Either results from use cases,
 * maintaining backward compatibility with existing Thymeleaf templates.</p>
 */
@Controller
public class LeaderboardController {

    private final GetLeaderboardUseCase getLeaderboardUseCase;
    private final GetUserDetailsUseCase getUserDetailsUseCase;
    private final LeaderboardViewMapper viewMapper;

    // Stub values - in production these would come from auth/session
    private static final String CURRENT_USER_ID = "current-user-id";
    private static final String CURRENT_USER_NAME = "Deejay Wagz";
    private static final int CURRENT_ROUND = 19;

    public LeaderboardController(
        GetLeaderboardUseCase getLeaderboardUseCase,
        GetUserDetailsUseCase getUserDetailsUseCase,
        LeaderboardViewMapper viewMapper
    ) {
        this.getLeaderboardUseCase = getLeaderboardUseCase;
        this.getUserDetailsUseCase = getUserDetailsUseCase;
        this.viewMapper = viewMapper;
    }

    /**
     * Display the leaderboard page.
     */
    @GetMapping("/leaderboard")
    public String leaderboard(
        @RequestParam(required = false, defaultValue = "FS") String phase,
        @RequestParam(required = false, defaultValue = "1") int page,
        @RequestHeader(value = "HX-Request", required = false) String hxRequest,
        Model model,
        HttpServletResponse response
    ) {
        var command = GetLeaderboardCommand.builder()
            .phase(phase)
            .page(page)
            .pageSize(10)
            .currentUserId(CURRENT_USER_ID)
            .build();

        return getLeaderboardUseCase.execute(command).fold(
            error -> handleLeaderboardError(error, model, response),
            result -> handleLeaderboardSuccess(result, model, hxRequest)
        );
    }

    /**
     * Display user details for leaderboard drill-down.
     */
    @GetMapping("/leaderboard/user/{userId}/details")
    public String userDetails(
        @PathVariable String userId,
        Model model,
        HttpServletResponse response
    ) {
        // Determine which round to show
        boolean isCurrentRoundOpen = true; // Stub - in production, check round status
        int roundToShow = isCurrentRoundOpen ? CURRENT_ROUND - 1 : CURRENT_ROUND;
        String roundStatus = isCurrentRoundOpen ? "Finalised" : "Locked";

        var command = GetUserDetailsCommand.of(userId, roundToShow);

        return getUserDetailsUseCase.execute(command).fold(
            error -> handleUserDetailsError(error, model, response),
            userDetail -> handleUserDetailsSuccess(userDetail, roundToShow, roundStatus, model)
        );
    }

    // ========== Success Handlers ==========

    private String handleLeaderboardSuccess(
        GetLeaderboardUseCase.LeaderboardResult result,
        Model model,
        String hxRequest
    ) {
        // Convert domain models to legacy DTOs for template compatibility
        var leaderboardDTOs = viewMapper.toLegacyDTOList(result.entries());
        var userPositionDTO = result.currentUserPosition() != null
            ? viewMapper.toLegacyDTO(result.currentUserPosition())
            : null;

        model.addAttribute("pageTitle", "Leaderboard");
        model.addAttribute("currentPhase", result.phase().name());
        model.addAttribute("leaderboard", leaderboardDTOs);
        model.addAttribute("userPosition", userPositionDTO);
        model.addAttribute("userInCurrentPage", result.userInCurrentPage());
        model.addAttribute("currentUserName", CURRENT_USER_NAME);
        model.addAttribute("phases", Phase.values());

        // Pagination info
        model.addAttribute("currentPage", result.currentPage());
        model.addAttribute("totalPages", result.totalPages());
        model.addAttribute("hasPreviousPage", result.hasPreviousPage());
        model.addAttribute("hasNextPage", result.hasNextPage());

        if (hxRequest != null && !hxRequest.isBlank()) {
            return "leaderboard :: leaderboardContent";
        }

        return "leaderboard";
    }

    private String handleUserDetailsSuccess(
        com.ligitabl.domain.model.leaderboard.UserDetail userDetail,
        int roundToShow,
        String roundStatus,
        Model model
    ) {
        var userDTO = viewMapper.toLegacyDTO(userDetail);

        model.addAttribute("user", userDTO);
        model.addAttribute("roundToShow", roundToShow);
        model.addAttribute("roundStatus", roundStatus);

        return "fragments/user-detail :: user-details(user=${user}, round=${roundToShow}, status=${roundStatus})";
    }

    // ========== Error Handlers ==========

    private String handleLeaderboardError(
        UseCaseError error,
        Model model,
        HttpServletResponse response
    ) {
        response.setStatus(mapErrorToStatus(error));
        model.addAttribute("error", error.message());
        model.addAttribute("pageTitle", "Error");
        return "error";
    }

    private String handleUserDetailsError(
        UseCaseError error,
        Model model,
        HttpServletResponse response
    ) {
        response.setStatus(mapErrorToStatus(error));
        model.addAttribute("error", error.message());
        return "fragments/error-banner :: banner";
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
