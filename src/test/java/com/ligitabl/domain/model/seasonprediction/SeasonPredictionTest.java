package com.ligitabl.domain.model.seasonprediction;

import com.ligitabl.domain.exception.InvalidSeasonPredictionException;
import com.ligitabl.domain.model.season.RoundNumber;
import com.ligitabl.domain.model.season.SeasonId;
import com.ligitabl.domain.model.team.TeamId;
import com.ligitabl.domain.model.user.UserId;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for SeasonPrediction aggregate root.
 * Target: 90%+ coverage on core business logic.
 */
class SeasonPredictionTest {

    private final SeasonPredictionId predictionId = SeasonPredictionId.generate();
    private final UserId userId = UserId.generate();
    private final SeasonId seasonId = SeasonId.generate();
    private final RoundNumber round1 = RoundNumber.of(1);
    private final Instant now = Instant.now();

    @Test
    void shouldCreateSeasonPredictionWithValidInput() {
        // Given
        List<TeamRanking> rankings = create20Rankings();

        // When
        SeasonPrediction prediction = SeasonPrediction.create(
            predictionId, userId, seasonId, round1, rankings, now
        );

        // Then
        assertNotNull(prediction);
        assertEquals(predictionId, prediction.getId());
        assertEquals(userId, prediction.getUserId());
        assertEquals(seasonId, prediction.getSeasonId());
        assertEquals(round1, prediction.getAtRound());
        assertEquals(20, prediction.getRankings().size());
        assertEquals(now, prediction.getCreatedAt());
        assertEquals(now, prediction.getUpdatedAt());
    }

    @Test
    void shouldThrowExceptionForNullParameters() {
        // Given
        List<TeamRanking> rankings = create20Rankings();

        // When/Then
        assertThrows(NullPointerException.class,
            () -> SeasonPrediction.create(null, userId, seasonId, round1, rankings, now));
        assertThrows(NullPointerException.class,
            () -> SeasonPrediction.create(predictionId, null, seasonId, round1, rankings, now));
        assertThrows(NullPointerException.class,
            () -> SeasonPrediction.create(predictionId, userId, null, round1, rankings, now));
        assertThrows(NullPointerException.class,
            () -> SeasonPrediction.create(predictionId, userId, seasonId, null, rankings, now));
        assertThrows(NullPointerException.class,
            () -> SeasonPrediction.create(predictionId, userId, seasonId, round1, null, now));
        assertThrows(NullPointerException.class,
            () -> SeasonPrediction.create(predictionId, userId, seasonId, round1, rankings, null));
    }

    @Test
    void shouldThrowExceptionForIncorrectTeamCount() {
        // Given: Only 19 teams
        List<TeamRanking> rankings = create19Rankings();

        // When/Then
        InvalidSeasonPredictionException exception = assertThrows(
            InvalidSeasonPredictionException.class,
            () -> SeasonPrediction.create(predictionId, userId, seasonId, round1, rankings, now)
        );

        assertTrue(exception.getMessage().contains("exactly 20 teams"));
        assertTrue(exception.getMessage().contains("19"));
    }

    @Test
    void shouldThrowExceptionForDuplicatePositions() {
        // Given: Two teams at position 5
        List<TeamRanking> rankings = create20Rankings();
        rankings.set(5, TeamRanking.create(TeamId.generate(), 1)); // Duplicate position 1

        // When/Then
        InvalidSeasonPredictionException exception = assertThrows(
            InvalidSeasonPredictionException.class,
            () -> SeasonPrediction.create(predictionId, userId, seasonId, round1, rankings, now)
        );

        assertTrue(exception.getMessage().contains("Duplicate positions found"));
    }

    @Test
    void shouldThrowExceptionForMissingPosition() {
        // Given: Positions 1-19, then duplicate position 1 (missing position 20)
        List<TeamRanking> rankings = new ArrayList<>();
        for (int i = 1; i <= 19; i++) {
            rankings.add(TeamRanking.create(TeamId.generate(), i));
        }
        rankings.add(TeamRanking.create(TeamId.generate(), 1)); // Duplicate position 1, missing 20

        // When/Then
        InvalidSeasonPredictionException exception = assertThrows(
            InvalidSeasonPredictionException.class,
            () -> SeasonPrediction.create(predictionId, userId, seasonId, round1, rankings, now)
        );

        // Should catch either duplicate positions or missing position 20
        assertTrue(
            exception.getMessage().contains("Duplicate positions") ||
            exception.getMessage().contains("Missing position 20")
        );
    }

    @Test
    void shouldThrowExceptionForDuplicateTeams() {
        // Given: Same team appears twice
        TeamId duplicateTeam = TeamId.generate();
        List<TeamRanking> rankings = create20Rankings();
        rankings.set(0, TeamRanking.create(duplicateTeam, 1));
        rankings.set(1, TeamRanking.create(duplicateTeam, 2)); // Duplicate team

        // When/Then
        InvalidSeasonPredictionException exception = assertThrows(
            InvalidSeasonPredictionException.class,
            () -> SeasonPrediction.create(predictionId, userId, seasonId, round1, rankings, now)
        );

        assertTrue(exception.getMessage().contains("Duplicate teams found"));
    }

    @Test
    void shouldSwapTeamsSuccessfully() {
        // Given
        TeamId teamA = TeamId.generate();
        TeamId teamB = TeamId.generate();
        List<TeamRanking> rankings = create20Rankings();
        rankings.set(2, TeamRanking.create(teamA, 3));  // Team A at position 3
        rankings.set(6, TeamRanking.create(teamB, 7));  // Team B at position 7

        SeasonPrediction prediction = SeasonPrediction.create(
            predictionId, userId, seasonId, round1, rankings, now
        );

        SwapPair swap = SwapPair.create(teamA, 3, teamB, 7);
        RoundNumber round5 = RoundNumber.of(5);
        Instant later = now.plusSeconds(3600);

        // When
        SeasonPrediction updated = prediction.swapTeams(swap, round5, later);

        // Then
        assertNotSame(prediction, updated); // Immutable
        assertEquals(predictionId, updated.getId()); // Same ID
        assertEquals(round5, updated.getAtRound()); // Updated round
        assertEquals(later, updated.getUpdatedAt()); // Updated timestamp

        // Verify positions swapped
        assertEquals(7, updated.getRankingForTeam(teamA).position());
        assertEquals(3, updated.getRankingForTeam(teamB).position());

        // Original unchanged
        assertEquals(3, prediction.getRankingForTeam(teamA).position());
        assertEquals(7, prediction.getRankingForTeam(teamB).position());
    }

    @Test
    void shouldThrowExceptionForNullSwapPair() {
        // Given
        SeasonPrediction prediction = SeasonPrediction.create(
            predictionId, userId, seasonId, round1, create20Rankings(), now
        );

        // When/Then
        assertThrows(NullPointerException.class,
            () -> prediction.swapTeams(null, RoundNumber.of(2), now));
    }

    @Test
    void shouldThrowExceptionWhenSwappingNonExistentTeam() {
        // Given
        TeamId existingTeam = TeamId.generate();
        TeamId nonExistentTeam = TeamId.generate();
        List<TeamRanking> rankings = create20Rankings();
        rankings.set(2, TeamRanking.create(existingTeam, 3));

        SeasonPrediction prediction = SeasonPrediction.create(
            predictionId, userId, seasonId, round1, rankings, now
        );

        SwapPair swap = SwapPair.create(existingTeam, 3, nonExistentTeam, 7);

        // When/Then
        IllegalStateException exception = assertThrows(IllegalStateException.class,
            () -> prediction.swapTeams(swap, RoundNumber.of(2), now));

        assertTrue(exception.getMessage().contains("Team B"));
        assertTrue(exception.getMessage().contains("not found"));
    }

    @Test
    void shouldThrowExceptionWhenPositionMismatch() {
        // Given: Team A is actually at position 3, but swap claims it's at 5 (stale data)
        TeamId teamA = TeamId.generate();
        TeamId teamB = TeamId.generate();
        List<TeamRanking> rankings = create20Rankings();
        rankings.set(2, TeamRanking.create(teamA, 3));  // Team A at position 3
        rankings.set(6, TeamRanking.create(teamB, 7));  // Team B at position 7

        SeasonPrediction prediction = SeasonPrediction.create(
            predictionId, userId, seasonId, round1, rankings, now
        );

        SwapPair swap = SwapPair.create(teamA, 5, teamB, 7); // Claims team A at 5 (wrong!)

        // When/Then
        IllegalStateException exception = assertThrows(IllegalStateException.class,
            () -> prediction.swapTeams(swap, RoundNumber.of(2), now));

        assertTrue(exception.getMessage().contains("Team A position mismatch"));
        assertTrue(exception.getMessage().contains("Expected: 5"));
        assertTrue(exception.getMessage().contains("Actual: 3"));
    }

    @Test
    void shouldGetRankingForTeam() {
        // Given
        TeamId targetTeam = TeamId.generate();
        List<TeamRanking> rankings = create20Rankings();
        rankings.set(5, TeamRanking.create(targetTeam, 6));

        SeasonPrediction prediction = SeasonPrediction.create(
            predictionId, userId, seasonId, round1, rankings, now
        );

        // When
        TeamRanking ranking = prediction.getRankingForTeam(targetTeam);

        // Then
        assertEquals(targetTeam, ranking.teamId());
        assertEquals(6, ranking.position());
    }

    @Test
    void shouldThrowExceptionWhenTeamNotFound() {
        // Given
        SeasonPrediction prediction = SeasonPrediction.create(
            predictionId, userId, seasonId, round1, create20Rankings(), now
        );

        TeamId nonExistentTeam = TeamId.generate();

        // When/Then
        assertThrows(IllegalArgumentException.class,
            () -> prediction.getRankingForTeam(nonExistentTeam));
    }

    @Test
    void shouldGetRankingAtPosition() {
        // Given
        TeamId targetTeam = TeamId.generate();
        List<TeamRanking> rankings = create20Rankings();
        rankings.set(9, TeamRanking.create(targetTeam, 10));

        SeasonPrediction prediction = SeasonPrediction.create(
            predictionId, userId, seasonId, round1, rankings, now
        );

        // When
        TeamRanking ranking = prediction.getRankingAtPosition(10);

        // Then
        assertEquals(targetTeam, ranking.teamId());
        assertEquals(10, ranking.position());
    }

    @Test
    void shouldGetAllRankingsSortedByPosition() {
        // Given
        SeasonPrediction prediction = SeasonPrediction.create(
            predictionId, userId, seasonId, round1, create20Rankings(), now
        );

        // When
        List<TeamRanking> rankings = prediction.getRankings();

        // Then
        assertEquals(20, rankings.size());
        for (int i = 0; i < 20; i++) {
            assertEquals(i + 1, rankings.get(i).position());
        }
    }

    @Test
    void shouldGetTopN() {
        // Given
        SeasonPrediction prediction = SeasonPrediction.create(
            predictionId, userId, seasonId, round1, create20Rankings(), now
        );

        // When
        List<TeamRanking> top4 = prediction.getTopN(4);

        // Then
        assertEquals(4, top4.size());
        assertEquals(1, top4.get(0).position());
        assertEquals(2, top4.get(1).position());
        assertEquals(3, top4.get(2).position());
        assertEquals(4, top4.get(3).position());
    }

    @Test
    void shouldGetBottomN() {
        // Given
        SeasonPrediction prediction = SeasonPrediction.create(
            predictionId, userId, seasonId, round1, create20Rankings(), now
        );

        // When
        List<TeamRanking> bottom3 = prediction.getBottomN(3);

        // Then
        assertEquals(3, bottom3.size());
        assertEquals(20, bottom3.get(0).position());
        assertEquals(19, bottom3.get(1).position());
        assertEquals(18, bottom3.get(2).position());
    }

    @Test
    void shouldBeEqualWhenIdsMatch() {
        // Given
        SeasonPrediction prediction1 = SeasonPrediction.create(
            predictionId, userId, seasonId, round1, create20Rankings(), now
        );
        SeasonPrediction prediction2 = SeasonPrediction.create(
            predictionId, UserId.generate(), seasonId, round1, create20Rankings(), now
        );

        // When/Then: Equal based on ID alone
        assertEquals(prediction1, prediction2);
        assertEquals(prediction1.hashCode(), prediction2.hashCode());
    }

    @Test
    void shouldNotBeEqualWhenIdsDiffer() {
        // Given
        SeasonPrediction prediction1 = SeasonPrediction.create(
            SeasonPredictionId.generate(), userId, seasonId, round1, create20Rankings(), now
        );
        SeasonPrediction prediction2 = SeasonPrediction.create(
            SeasonPredictionId.generate(), userId, seasonId, round1, create20Rankings(), now
        );

        // When/Then
        assertNotEquals(prediction1, prediction2);
    }

    // Helper methods

    private List<TeamRanking> create20Rankings() {
        List<TeamRanking> rankings = new ArrayList<>();
        for (int i = 1; i <= 20; i++) {
            rankings.add(TeamRanking.create(TeamId.generate(), i));
        }
        return rankings;
    }

    private List<TeamRanking> create19Rankings() {
        List<TeamRanking> rankings = new ArrayList<>();
        for (int i = 1; i <= 19; i++) {
            rankings.add(TeamRanking.create(TeamId.generate(), i));
        }
        return rankings;
    }
}
