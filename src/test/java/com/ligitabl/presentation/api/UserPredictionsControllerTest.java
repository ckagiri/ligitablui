package com.ligitabl.presentation.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ligitabl.api.shared.Either;
import com.ligitabl.application.error.UseCaseError;
import com.ligitabl.application.usecase.prediction.GetUserPredictionUseCase;
import com.ligitabl.domain.model.contest.ContestId;
import com.ligitabl.domain.model.prediction.PredictionAccessMode;
import com.ligitabl.domain.model.season.SeasonId;
import com.ligitabl.domain.model.seasonprediction.RankingSource;
import com.ligitabl.domain.model.seasonprediction.TeamRanking;
import com.ligitabl.domain.model.team.TeamId;
import com.ligitabl.domain.model.user.UserId;
import com.ligitabl.domain.repository.MainContestEntryRepository;
import com.ligitabl.domain.repository.RoundPredictionRepository;
import com.ligitabl.domain.repository.SeasonPredictionRepository;
import com.ligitabl.presentation.mapper.ErrorViewMapper;
import com.ligitabl.presentation.mapper.SeasonPredictionViewMapper;
import com.ligitabl.service.InMemoryDataService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.ui.Model;

import java.security.Principal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for UserPredictionsController.
 * Tests endpoint behavior and model attribute setting.
 */
class UserPredictionsControllerTest {

    private GetUserPredictionUseCase getUserPredictionUseCase;
    private SeasonPredictionRepository seasonPredictionRepository;
    private RoundPredictionRepository roundPredictionRepository;
    private MainContestEntryRepository mainContestEntryRepository;
    private SeasonPredictionViewMapper viewMapper;
    private ErrorViewMapper errorMapper;
    private ObjectMapper objectMapper;
    private InMemoryDataService dataService;
    private SeasonId activeSeasonId;
    private ContestId mainContestId;
    private UserPredictionsController controller;

    @BeforeEach
    void setUp() {
        getUserPredictionUseCase = mock(GetUserPredictionUseCase.class);
        seasonPredictionRepository = mock(SeasonPredictionRepository.class);
        roundPredictionRepository = mock(RoundPredictionRepository.class);
        mainContestEntryRepository = mock(MainContestEntryRepository.class);
        viewMapper = mock(SeasonPredictionViewMapper.class);
        errorMapper = mock(ErrorViewMapper.class);
        objectMapper = new ObjectMapper();
        dataService = mock(InMemoryDataService.class);
        activeSeasonId = SeasonId.generate();
        mainContestId = ContestId.generate();

        controller = new UserPredictionsController(
            getUserPredictionUseCase,
            seasonPredictionRepository,
            roundPredictionRepository,
            mainContestEntryRepository,
            viewMapper,
            errorMapper,
            objectMapper,
            dataService,
            activeSeasonId,
            mainContestId
        );
    }

    @Nested
    @DisplayName("GET /predictions/user/me Tests")
    class MyPredictionsTests {

        @Test
        @DisplayName("Should redirect to guest view when not authenticated")
        void shouldRedirectToGuestWhenNotAuthenticated() {
            // Given - no setup needed, redirect happens before use case call

            Model model = new ExtendedModelMap();
            MockHttpServletResponse response = new MockHttpServletResponse();

            // When
            String viewName = controller.myPredictions(null, null, null, model, response, null);

            // Then - should redirect to /guest endpoint
            assertEquals("redirect:/predictions/user/guest", viewName);
            // Use case should not be called
            verify(getUserPredictionUseCase, never()).execute(any());
        }

        @Test
        @DisplayName("Should redirect to guest with round param when not authenticated")
        void shouldRedirectToGuestWithRoundParamWhenNotAuthenticated() {
            // Given
            Model model = new ExtendedModelMap();
            MockHttpServletResponse response = new MockHttpServletResponse();

            // When
            String viewName = controller.myPredictions(15, null, null, model, response, null);

            // Then - should redirect to /guest endpoint with round param
            assertEquals("redirect:/predictions/user/guest?round=15", viewName);
        }

        @Test
        @DisplayName("Should return editable view when authenticated with prediction")
        void shouldReturnEditableViewWhenAuthenticatedWithPrediction() {
            // Given
            UserId userId = UserId.generate();
            Principal principal = () -> userId.value().toString();

            when(mainContestEntryRepository.existsByUserIdAndContestId(any(), any())).thenReturn(true);
            when(seasonPredictionRepository.existsByUserIdAndSeasonId(any(), any())).thenReturn(true);

            GetUserPredictionUseCase.UserPredictionViewData viewData = createViewData(
                PredictionAccessMode.EDITABLE,
                RankingSource.USER_PREDICTION
            );
            when(getUserPredictionUseCase.execute(any()))
                .thenReturn(Either.right(viewData));
            when(viewMapper.toRankingDTO(any())).thenReturn(mock(com.ligitabl.presentation.dto.response.RankingDTO.class));

            Model model = new ExtendedModelMap();
            MockHttpServletResponse response = new MockHttpServletResponse();

            // When
            String viewName = controller.myPredictions(null, null, principal, model, response, null);

            // Then
            assertEquals("predictions", viewName);
            assertEquals(PredictionAccessMode.EDITABLE.name(), model.getAttribute("accessMode"));
            assertFalse((Boolean) model.getAttribute("isGuest"));
            assertFalse((Boolean) model.getAttribute("isReadonly"));
            assertTrue((Boolean) model.getAttribute("canSwap"));
        }

        @Test
        @DisplayName("Should return CAN_CREATE_ENTRY when authenticated without prediction")
        void shouldReturnCanCreateEntryWhenAuthenticatedWithoutPrediction() {
            // Given
            UserId userId = UserId.generate();
            Principal principal = () -> userId.value().toString();

            when(mainContestEntryRepository.existsByUserIdAndContestId(any(), any())).thenReturn(false);
            when(seasonPredictionRepository.existsByUserIdAndSeasonId(any(), any())).thenReturn(false);

            GetUserPredictionUseCase.UserPredictionViewData viewData = createViewData(
                PredictionAccessMode.CAN_CREATE_ENTRY,
                RankingSource.ROUND_STANDINGS
            );
            when(getUserPredictionUseCase.execute(any()))
                .thenReturn(Either.right(viewData));
            when(viewMapper.toRankingDTO(any())).thenReturn(mock(com.ligitabl.presentation.dto.response.RankingDTO.class));

            Model model = new ExtendedModelMap();
            MockHttpServletResponse response = new MockHttpServletResponse();

            // When
            String viewName = controller.myPredictions(null, null, principal, model, response, null);

            // Then
            assertEquals("predictions", viewName);
            assertEquals(PredictionAccessMode.CAN_CREATE_ENTRY.name(), model.getAttribute("accessMode"));
            assertTrue((Boolean) model.getAttribute("canCreateEntry"));
        }

        @Test
        @DisplayName("Should return HTMX fragment when HX-Request header present for authenticated user")
        void shouldReturnHtmxFragmentWhenHxRequestHeaderPresentForAuthenticatedUser() {
            // Given
            UserId userId = UserId.generate();
            Principal principal = () -> userId.value().toString();

            when(mainContestEntryRepository.existsByUserIdAndContestId(any(), any())).thenReturn(true);
            when(seasonPredictionRepository.existsByUserIdAndSeasonId(any(), any())).thenReturn(true);

            GetUserPredictionUseCase.UserPredictionViewData viewData = createViewData(
                PredictionAccessMode.EDITABLE,
                RankingSource.USER_PREDICTION
            );
            when(getUserPredictionUseCase.execute(any()))
                .thenReturn(Either.right(viewData));
            when(viewMapper.toRankingDTO(any())).thenReturn(mock(com.ligitabl.presentation.dto.response.RankingDTO.class));

            Model model = new ExtendedModelMap();
            MockHttpServletResponse response = new MockHttpServletResponse();

            // When
            String viewName = controller.myPredictions(null, null, principal, model, response, "true");

            // Then
            assertEquals("predictions :: predictionPage", viewName);
        }

        @Test
        @DisplayName("Should still redirect guest even with HTMX header")
        void shouldStillRedirectGuestWithHtmxHeader() {
            // Given - no authentication

            Model model = new ExtendedModelMap();
            MockHttpServletResponse response = new MockHttpServletResponse();

            // When
            String viewName = controller.myPredictions(null, null, null, model, response, "true");

            // Then - should still redirect (HTMX will follow redirect)
            assertEquals("redirect:/predictions/user/guest", viewName);
        }
    }

    @Nested
    @DisplayName("GET /predictions/user/guest Tests")
    class GuestPredictionsTests {

        @Test
        @DisplayName("Should always return guest view regardless of authentication")
        void shouldAlwaysReturnGuestView() {
            // Given
            GetUserPredictionUseCase.UserPredictionViewData viewData = createViewData(
                PredictionAccessMode.READONLY_GUEST,
                RankingSource.ROUND_STANDINGS
            );
            when(getUserPredictionUseCase.execute(any()))
                .thenReturn(Either.right(viewData));
            when(viewMapper.toRankingDTO(any())).thenReturn(mock(com.ligitabl.presentation.dto.response.RankingDTO.class));

            Model model = new ExtendedModelMap();
            MockHttpServletResponse response = new MockHttpServletResponse();

            // When
            String viewName = controller.guestPredictions(null, model, response, null);

            // Then
            assertEquals("predictions", viewName);
            assertEquals(PredictionAccessMode.READONLY_GUEST.name(), model.getAttribute("accessMode"));
            assertTrue((Boolean) model.getAttribute("isGuest"));
        }
    }

    @Nested
    @DisplayName("GET /predictions/user/{userId} Tests")
    class UserPredictionsTests {

        @Test
        @DisplayName("Should redirect to /me when viewing own predictions")
        void shouldRedirectToMeWhenViewingOwnPredictions() {
            // Given
            String userIdStr = "550e8400-e29b-41d4-a716-446655440000";
            Principal principal = () -> userIdStr;

            when(mainContestEntryRepository.existsByUserIdAndContestId(any(), any())).thenReturn(true);
            when(seasonPredictionRepository.existsByUserIdAndSeasonId(any(), any())).thenReturn(true);

            GetUserPredictionUseCase.UserPredictionViewData viewData = createViewData(
                PredictionAccessMode.EDITABLE,
                RankingSource.USER_PREDICTION
            );
            when(getUserPredictionUseCase.execute(any()))
                .thenReturn(Either.right(viewData));
            when(viewMapper.toRankingDTO(any())).thenReturn(mock(com.ligitabl.presentation.dto.response.RankingDTO.class));

            Model model = new ExtendedModelMap();
            MockHttpServletResponse response = new MockHttpServletResponse();

            // When
            String viewName = controller.userPredictions(userIdStr, null, principal, model, response, null);

            // Then
            // Should be treated as /me endpoint
            assertEquals("predictions", viewName);
        }

        @Test
        @DisplayName("Should return READONLY_VIEWING_OTHER when viewing another user")
        void shouldReturnReadonlyViewingOtherWhenViewingAnotherUser() {
            // Given
            String targetUserId = "550e8400-e29b-41d4-a716-446655440000";
            String viewerUserId = "660e8400-e29b-41d4-a716-446655440001";
            Principal principal = () -> viewerUserId;

            when(seasonPredictionRepository.existsByUserIdAndSeasonId(any(), any())).thenReturn(true);

            GetUserPredictionUseCase.UserPredictionViewData viewData = createViewData(
                PredictionAccessMode.READONLY_VIEWING_OTHER,
                RankingSource.USER_PREDICTION,
                "John Doe"
            );
            when(getUserPredictionUseCase.execute(any()))
                .thenReturn(Either.right(viewData));
            when(viewMapper.toRankingDTO(any())).thenReturn(mock(com.ligitabl.presentation.dto.response.RankingDTO.class));

            Model model = new ExtendedModelMap();
            MockHttpServletResponse response = new MockHttpServletResponse();

            // When
            String viewName = controller.userPredictions(targetUserId, null, principal, model, response, null);

            // Then
            assertEquals("predictions", viewName);
            assertEquals(PredictionAccessMode.READONLY_VIEWING_OTHER.name(), model.getAttribute("accessMode"));
            assertTrue((Boolean) model.getAttribute("isViewingOther"));
            assertEquals("John Doe", model.getAttribute("targetDisplayName"));
        }

        @Test
        @DisplayName("Should return READONLY_USER_NOT_FOUND for invalid UUID")
        void shouldReturnReadonlyUserNotFoundForInvalidUuid() {
            // Given
            String invalidUserId = "not-a-valid-uuid";

            GetUserPredictionUseCase.UserPredictionViewData viewData = createViewData(
                PredictionAccessMode.READONLY_USER_NOT_FOUND,
                RankingSource.ROUND_STANDINGS
            );
            when(getUserPredictionUseCase.execute(any()))
                .thenReturn(Either.right(viewData));
            when(viewMapper.toRankingDTO(any())).thenReturn(mock(com.ligitabl.presentation.dto.response.RankingDTO.class));

            Model model = new ExtendedModelMap();
            MockHttpServletResponse response = new MockHttpServletResponse();

            // When
            String viewName = controller.userPredictions(invalidUserId, null, null, model, response, null);

            // Then
            assertEquals("predictions", viewName);
            assertEquals(PredictionAccessMode.READONLY_USER_NOT_FOUND.name(), model.getAttribute("accessMode"));
            assertTrue((Boolean) model.getAttribute("isUserNotFound"));
        }
    }

    @Nested
    @DisplayName("Error Handling Tests")
    class ErrorHandlingTests {

        @Test
        @DisplayName("Should return error view on use case failure for authenticated user")
        void shouldReturnErrorViewOnUseCaseFailure() {
            // Given
            UserId userId = UserId.generate();
            Principal principal = () -> userId.value().toString();

            when(mainContestEntryRepository.existsByUserIdAndContestId(any(), any())).thenReturn(true);
            when(seasonPredictionRepository.existsByUserIdAndSeasonId(any(), any())).thenReturn(true);

            UseCaseError error = new UseCaseError.NotFoundError("Prediction not found");
            when(getUserPredictionUseCase.execute(any()))
                .thenReturn(Either.left(error));
            when(errorMapper.toResponse(error)).thenReturn(
                new com.ligitabl.presentation.dto.response.ErrorResponse("NOT_FOUND", "Prediction not found", List.of())
            );

            Model model = new ExtendedModelMap();
            MockHttpServletResponse response = new MockHttpServletResponse();

            // When
            String viewName = controller.myPredictions(null, null, principal, model, response, null);

            // Then
            assertEquals("error", viewName);
            assertEquals(404, response.getStatus());
            assertNotNull(model.getAttribute("error"));
        }

        @Test
        @DisplayName("Should return error fragment for HTMX request on failure for authenticated user")
        void shouldReturnErrorFragmentForHtmxRequestOnFailure() {
            // Given
            UserId userId = UserId.generate();
            Principal principal = () -> userId.value().toString();

            when(mainContestEntryRepository.existsByUserIdAndContestId(any(), any())).thenReturn(true);
            when(seasonPredictionRepository.existsByUserIdAndSeasonId(any(), any())).thenReturn(true);

            UseCaseError error = new UseCaseError.ValidationError("Invalid request");
            when(getUserPredictionUseCase.execute(any()))
                .thenReturn(Either.left(error));
            when(errorMapper.toResponse(error)).thenReturn(
                new com.ligitabl.presentation.dto.response.ErrorResponse("VALIDATION", "Invalid request", List.of())
            );

            Model model = new ExtendedModelMap();
            MockHttpServletResponse response = new MockHttpServletResponse();

            // When
            String viewName = controller.myPredictions(null, null, principal, model, response, "true");

            // Then
            assertEquals("fragments/error-banner :: banner", viewName);
            assertEquals(400, response.getStatus());
        }
    }

    // Helper methods

    private GetUserPredictionUseCase.UserPredictionViewData createViewData(
        PredictionAccessMode accessMode,
        RankingSource source
    ) {
        return createViewData(accessMode, source, null);
    }

    private GetUserPredictionUseCase.UserPredictionViewData createViewData(
        PredictionAccessMode accessMode,
        RankingSource source,
        String targetDisplayName
    ) {
        List<TeamRanking> rankings = new ArrayList<>();
        for (int i = 1; i <= 20; i++) {
            rankings.add(TeamRanking.create(TeamId.generate(), i));
        }

        return new GetUserPredictionUseCase.UserPredictionViewData(
            rankings,
            source,
            accessMode,
            null, // swapCooldown
            Map.of(), // fixtures
            Map.of(), // standingsMap
            Map.of(), // pointsMap
            19, // currentRound
            19, // viewingRound
            "OPEN", // roundState
            "Test message",
            targetDisplayName,
            null  // roundResult
        );
    }
}
