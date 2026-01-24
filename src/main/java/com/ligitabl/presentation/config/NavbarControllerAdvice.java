package com.ligitabl.presentation.config;

import com.ligitabl.domain.model.contest.ContestId;
import com.ligitabl.domain.model.season.SeasonId;
import com.ligitabl.domain.model.user.UserId;
import com.ligitabl.domain.repository.MainContestEntryRepository;
import com.ligitabl.infrastructure.auth.DemoAuthFilter;
import jakarta.servlet.http.HttpSession;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import java.security.Principal;

/**
 * Controller advice to provide navbar context across all views.
 *
 * <p>Provides:
 * <ul>
 *   <li>hasContestEntry - true if logged-in user has joined the main contest</li>
 *   <li>isLoggedIn - true if user is authenticated</li>
 * </ul>
 *
 * <p>Navbar label logic:
 * <ul>
 *   <li>Not logged in → "Predictions" (links to /predictions/user/guest)</li>
 *   <li>Logged in + no entry → "Predictions" (links to /predictions/user/me)</li>
 *   <li>Logged in + has entry → "My Predictions" (links to /predictions/user/me)</li>
 * </ul>
 */
@ControllerAdvice
public class NavbarControllerAdvice {

    private final MainContestEntryRepository mainContestEntryRepository;
    private final SeasonId activeSeasonId;
    private final ContestId mainContestId;

    public NavbarControllerAdvice(
        MainContestEntryRepository mainContestEntryRepository,
        SeasonId activeSeasonId,
        ContestId mainContestId
    ) {
        this.mainContestEntryRepository = mainContestEntryRepository;
        this.activeSeasonId = activeSeasonId;
        this.mainContestId = mainContestId;
    }

    @ModelAttribute("isLoggedIn")
    public boolean isLoggedIn(Principal principal) {
        return principal != null;
    }

    @ModelAttribute("hasContestEntry")
    public boolean hasContestEntry(Principal principal) {
        if (principal == null) {
            return false;
        }

        try {
            UserId userId = UserId.of(principal.getName());
            return mainContestEntryRepository.existsByUserIdAndContestId(userId, mainContestId);
        } catch (IllegalArgumentException e) {
            // Invalid user ID format
            return false;
        }
    }

    @ModelAttribute("predictionsNavLabel")
    public String predictionsNavLabel(Principal principal) {
        if (principal == null) {
            return "Predictions";
        }

        try {
            UserId userId = UserId.of(principal.getName());
            boolean hasEntry = mainContestEntryRepository.existsByUserIdAndContestId(userId, mainContestId);
            return hasEntry ? "My Predictions" : "Predictions";
        } catch (IllegalArgumentException e) {
            return "Predictions";
        }
    }

    @ModelAttribute("predictionsNavLink")
    public String predictionsNavLink(Principal principal) {
        if (principal == null) {
            return "/predictions/user/guest";
        }
        return "/predictions/user/me";
    }

    @ModelAttribute("userDisplayName")
    public String userDisplayName(HttpSession session) {
        if (session == null) {
            return null;
        }
        return (String) session.getAttribute(DemoAuthFilter.SESSION_DISPLAY_NAME_KEY);
    }
}
