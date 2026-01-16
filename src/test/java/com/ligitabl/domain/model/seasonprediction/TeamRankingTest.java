package com.ligitabl.domain.model.seasonprediction;

import com.ligitabl.domain.model.team.TeamId;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for TeamRanking entity.
 */
class TeamRankingTest {

    private final TeamId teamId = TeamId.generate();

    @Test
    void shouldCreateTeamRankingWithValidInput() {
        // When
        TeamRanking ranking = TeamRanking.create(teamId, 5);

        // Then
        assertNotNull(ranking);
        assertEquals(teamId, ranking.teamId());
        assertEquals(5, ranking.position());
    }

    @Test
    void shouldThrowExceptionForNullTeamId() {
        // When/Then
        assertThrows(NullPointerException.class,
            () -> TeamRanking.create(null, 5));
    }

    @ParameterizedTest
    @ValueSource(ints = {0, -1, 21, 100})
    void shouldThrowExceptionForInvalidPosition(int invalidPosition) {
        // When/Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
            () -> TeamRanking.create(teamId, invalidPosition));

        assertTrue(exception.getMessage().contains("Position must be between 1 and 20"));
    }

    @ParameterizedTest
    @ValueSource(ints = {1, 10, 20})
    void shouldAcceptValidPositions(int validPosition) {
        // When/Then
        assertDoesNotThrow(() -> TeamRanking.create(teamId, validPosition));
    }

    @Test
    void shouldCreateNewInstanceWithUpdatedPosition() {
        // Given
        TeamRanking original = TeamRanking.create(teamId, 5);

        // When
        TeamRanking updated = original.withPosition(10);

        // Then
        assertNotSame(original, updated);
        assertEquals(5, original.position()); // Original unchanged
        assertEquals(10, updated.position());
        assertEquals(teamId, updated.teamId());
    }

    @Test
    void shouldThrowExceptionWhenUpdatingToInvalidPosition() {
        // Given
        TeamRanking ranking = TeamRanking.create(teamId, 5);

        // When/Then
        assertThrows(IllegalArgumentException.class,
            () -> ranking.withPosition(0));
        assertThrows(IllegalArgumentException.class,
            () -> ranking.withPosition(21));
    }

    @Test
    void shouldIdentifyFirstPlace() {
        // Given
        TeamRanking first = TeamRanking.create(teamId, 1);
        TeamRanking notFirst = TeamRanking.create(teamId, 2);

        // When/Then
        assertTrue(first.isFirst());
        assertFalse(notFirst.isFirst());
    }

    @Test
    void shouldIdentifyLastPlace() {
        // Given
        TeamRanking last = TeamRanking.create(teamId, 20);
        TeamRanking notLast = TeamRanking.create(teamId, 19);

        // When/Then
        assertTrue(last.isLast());
        assertFalse(notLast.isLast());
    }

    @ParameterizedTest
    @ValueSource(ints = {1, 2, 3, 4})
    void shouldIdentifyChampionsLeaguePositions(int position) {
        // Given
        TeamRanking ranking = TeamRanking.create(teamId, position);

        // When/Then
        assertTrue(ranking.isChampionsLeague());
    }

    @ParameterizedTest
    @ValueSource(ints = {5, 10, 17})
    void shouldNotIdentifyNonChampionsLeaguePositions(int position) {
        // Given
        TeamRanking ranking = TeamRanking.create(teamId, position);

        // When/Then
        assertFalse(ranking.isChampionsLeague());
    }

    @ParameterizedTest
    @ValueSource(ints = {18, 19, 20})
    void shouldIdentifyRelegationZone(int position) {
        // Given
        TeamRanking ranking = TeamRanking.create(teamId, position);

        // When/Then
        assertTrue(ranking.isRelegationZone());
    }

    @ParameterizedTest
    @ValueSource(ints = {1, 10, 17})
    void shouldNotIdentifyNonRelegationZone(int position) {
        // Given
        TeamRanking ranking = TeamRanking.create(teamId, position);

        // When/Then
        assertFalse(ranking.isRelegationZone());
    }

    @Test
    void shouldBeEqualWhenTeamIdAndPositionMatch() {
        // Given
        TeamRanking ranking1 = TeamRanking.create(teamId, 5);
        TeamRanking ranking2 = TeamRanking.create(teamId, 5);

        // When/Then
        assertEquals(ranking1, ranking2);
        assertEquals(ranking1.hashCode(), ranking2.hashCode());
    }

    @Test
    void shouldNotBeEqualWhenPositionDiffers() {
        // Given
        TeamRanking ranking1 = TeamRanking.create(teamId, 5);
        TeamRanking ranking2 = TeamRanking.create(teamId, 10);

        // When/Then
        assertNotEquals(ranking1, ranking2);
    }

    @Test
    void shouldNotBeEqualWhenTeamIdDiffers() {
        // Given
        TeamRanking ranking1 = TeamRanking.create(teamId, 5);
        TeamRanking ranking2 = TeamRanking.create(TeamId.generate(), 5);

        // When/Then
        assertNotEquals(ranking1, ranking2);
    }

    @Test
    void shouldHaveUsefulToString() {
        // Given
        TeamRanking ranking = TeamRanking.create(teamId, 5);

        // When
        String toString = ranking.toString();

        // Then
        assertTrue(toString.contains("TeamRanking"));
        assertTrue(toString.contains("5"));
    }
}
