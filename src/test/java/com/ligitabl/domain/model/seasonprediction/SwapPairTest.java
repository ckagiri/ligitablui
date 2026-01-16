package com.ligitabl.domain.model.seasonprediction;

import com.ligitabl.domain.exception.MultipleSwapException;
import com.ligitabl.domain.model.team.TeamId;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for SwapPair value object.
 * CRITICAL: These tests validate the single-swap business rule enforcement.
 * Target: 100% coverage on validation logic.
 */
class SwapPairTest {

    private final TeamId teamA = TeamId.generate();
    private final TeamId teamB = TeamId.generate();
    private final TeamId teamC = TeamId.generate();

    @Test
    void shouldCreateSwapPairWithValidInput() {
        // When
        SwapPair swap = SwapPair.create(teamA, 3, teamB, 7);

        // Then
        assertNotNull(swap);
        assertEquals(teamA, swap.teamAId());
        assertEquals(3, swap.teamACurrentPosition());
        assertEquals(teamB, swap.teamBId());
        assertEquals(7, swap.teamBCurrentPosition());
    }

    @Test
    void shouldThrowExceptionForNullTeamAId() {
        // When/Then
        assertThrows(NullPointerException.class,
            () -> SwapPair.create(null, 3, teamB, 7));
    }

    @Test
    void shouldThrowExceptionForNullTeamBId() {
        // When/Then
        assertThrows(NullPointerException.class,
            () -> SwapPair.create(teamA, 3, null, 7));
    }

    @Test
    void shouldThrowExceptionForInvalidTeamAPosition() {
        // When/Then
        assertThrows(IllegalArgumentException.class,
            () -> SwapPair.create(teamA, 0, teamB, 7));
        assertThrows(IllegalArgumentException.class,
            () -> SwapPair.create(teamA, 21, teamB, 7));
    }

    @Test
    void shouldThrowExceptionForInvalidTeamBPosition() {
        // When/Then
        assertThrows(IllegalArgumentException.class,
            () -> SwapPair.create(teamA, 3, teamB, 0));
        assertThrows(IllegalArgumentException.class,
            () -> SwapPair.create(teamA, 3, teamB, 21));
    }

    @Test
    void shouldThrowExceptionForSwappingSameTeam() {
        // When/Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
            () -> SwapPair.create(teamA, 3, teamA, 7));

        assertTrue(exception.getMessage().contains("Cannot swap a team with itself"));
    }

    @Test
    void shouldThrowExceptionForSamePosition() {
        // When/Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
            () -> SwapPair.create(teamA, 5, teamB, 5));

        assertTrue(exception.getMessage().contains("same position"));
    }

    @Test
    void shouldDetectInvolvesTeam() {
        // Given
        SwapPair swap = SwapPair.create(teamA, 3, teamB, 7);

        // When/Then
        assertTrue(swap.involvesTeam(teamA));
        assertTrue(swap.involvesTeam(teamB));
        assertFalse(swap.involvesTeam(teamC));
    }

    @Test
    void shouldGetOtherTeam() {
        // Given
        SwapPair swap = SwapPair.create(teamA, 3, teamB, 7);

        // When/Then
        assertEquals(teamB, swap.getOtherTeam(teamA));
        assertEquals(teamA, swap.getOtherTeam(teamB));
    }

    @Test
    void shouldThrowExceptionWhenGetOtherTeamNotInSwap() {
        // Given
        SwapPair swap = SwapPair.create(teamA, 3, teamB, 7);

        // When/Then
        assertThrows(IllegalArgumentException.class, () -> swap.getOtherTeam(teamC));
    }

    // ===== CRITICAL TESTS: fromRankingsChange validation =====

    @Test
    void shouldCreateSwapPairFromValidSingleSwap() {
        // Given: Create 20 consistent team IDs
        List<TeamId> teamIds = new ArrayList<>();
        for (int i = 0; i < 20; i++) {
            teamIds.add(TeamId.generate());
        }

        // Old rankings: normal order (teamId[i] at position i+1)
        List<TeamRanking> oldRankings = new ArrayList<>();
        for (int i = 0; i < 20; i++) {
            oldRankings.add(TeamRanking.create(teamIds.get(i), i + 1));
        }

        // New rankings: swap team at position 3 with team at position 7
        List<TeamRanking> newRankings = new ArrayList<>();
        for (int i = 0; i < 20; i++) {
            if (i == 2) {
                // Team that was at position 3 is now at position 7
                newRankings.add(TeamRanking.create(teamIds.get(2), 7));
            } else if (i == 6) {
                // Team that was at position 7 is now at position 3
                newRankings.add(TeamRanking.create(teamIds.get(6), 3));
            } else {
                // All other teams stay at their positions
                newRankings.add(TeamRanking.create(teamIds.get(i), i + 1));
            }
        }

        // When
        SwapPair swap = SwapPair.fromRankingsChange(oldRankings, newRankings);

        // Then
        assertNotNull(swap);
        assertTrue(swap.involvesTeam(teamIds.get(2)));
        assertTrue(swap.involvesTeam(teamIds.get(6)));
    }

    @Test
    void shouldThrowMultipleSwapExceptionForNoChanges() {
        // Given: Create consistent team IDs for both lists
        List<TeamId> teamIds = new ArrayList<>();
        for (int i = 0; i < 20; i++) {
            teamIds.add(TeamId.generate());
        }

        // Both old and new have same rankings (no changes)
        List<TeamRanking> oldRankings = new ArrayList<>();
        List<TeamRanking> newRankings = new ArrayList<>();
        for (int i = 0; i < 20; i++) {
            oldRankings.add(TeamRanking.create(teamIds.get(i), i + 1));
            newRankings.add(TeamRanking.create(teamIds.get(i), i + 1));
        }

        // When/Then
        MultipleSwapException exception = assertThrows(MultipleSwapException.class,
            () -> SwapPair.fromRankingsChange(oldRankings, newRankings));

        assertTrue(exception.getMessage().contains("Only one swap allowed"));
        assertTrue(exception.getMessage().contains("Found 0 position changes"));
    }

    @Test
    void shouldThrowMultipleSwapExceptionForMultipleSwaps() {
        // Given: Create consistent team IDs for both lists
        List<TeamId> teamIds = new ArrayList<>();
        for (int i = 0; i < 20; i++) {
            teamIds.add(TeamId.generate());
        }

        // Old rankings: normal order
        List<TeamRanking> oldRankings = new ArrayList<>();
        for (int i = 0; i < 20; i++) {
            oldRankings.add(TeamRanking.create(teamIds.get(i), i + 1));
        }

        // New rankings: multiple swaps
        List<TeamRanking> newRankings = new ArrayList<>();
        for (int i = 0; i < 20; i++) {
            newRankings.add(TeamRanking.create(teamIds.get(i), i + 1));
        }

        // Swap team at pos 1 with pos 2
        swapPositionsInList(newRankings, 1, 2);
        // Swap team at pos 5 with pos 6 (second swap!)
        swapPositionsInList(newRankings, 5, 6);

        // When/Then
        MultipleSwapException exception = assertThrows(MultipleSwapException.class,
            () -> SwapPair.fromRankingsChange(oldRankings, newRankings));

        assertTrue(exception.getMessage().contains("Only one swap allowed"));
        assertTrue(exception.getMessage().contains("Found 4 position changes"));
    }

    @Test
    void shouldThrowExceptionForInvalidRankingsSize() {
        // Given: Less than 20 teams
        List<TeamRanking> oldRankings = List.of(
            TeamRanking.create(teamA, 1),
            TeamRanking.create(teamB, 2)
        );
        List<TeamRanking> newRankings = List.of(
            TeamRanking.create(teamA, 2),
            TeamRanking.create(teamB, 1)
        );

        // When/Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
            () -> SwapPair.fromRankingsChange(oldRankings, newRankings));

        assertTrue(exception.getMessage().contains("exactly 20 teams"));
    }

    @Test
    void shouldThrowExceptionForMissingTeamInNewRankings() {
        // Given: Create consistent team IDs
        List<TeamId> teamIds = new ArrayList<>();
        for (int i = 0; i < 20; i++) {
            teamIds.add(TeamId.generate());
        }

        List<TeamRanking> oldRankings = new ArrayList<>();
        for (int i = 0; i < 20; i++) {
            oldRankings.add(TeamRanking.create(teamIds.get(i), i + 1));
        }

        // New rankings: remove one team and add a different one
        List<TeamRanking> newRankings = new ArrayList<>();
        for (int i = 1; i < 20; i++) { // Skip first team
            newRankings.add(TeamRanking.create(teamIds.get(i), i));
        }
        newRankings.add(TeamRanking.create(TeamId.generate(), 20)); // Add different team

        // When/Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
            () -> SwapPair.fromRankingsChange(oldRankings, newRankings));

        assertTrue(exception.getMessage().contains("exists in old rankings but not in new"));
    }

    @Test
    void shouldThrowExceptionForInvalidSwap() {
        // Given: Create consistent team IDs
        List<TeamId> teamIds = new ArrayList<>();
        for (int i = 0; i < 20; i++) {
            teamIds.add(TeamId.generate());
        }

        List<TeamRanking> oldRankings = new ArrayList<>();
        for (int i = 0; i < 20; i++) {
            oldRankings.add(TeamRanking.create(teamIds.get(i), i + 1));
        }

        List<TeamRanking> newRankings = new ArrayList<>();
        for (int i = 0; i < 20; i++) {
            newRankings.add(TeamRanking.create(teamIds.get(i), i + 1));
        }

        // Change two teams but NOT swapping their positions
        // Team 0: pos 1 -> 3 (not 2)
        // Team 1: pos 2 -> 4 (not 1)
        // This is NOT a swap!
        newRankings.set(0, TeamRanking.create(teamIds.get(0), 3));
        newRankings.set(1, TeamRanking.create(teamIds.get(1), 4));

        // When/Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
            () -> SwapPair.fromRankingsChange(oldRankings, newRankings));

        assertTrue(exception.getMessage().contains("do not represent a valid swap"));
    }

    @Test
    void shouldThrowExceptionForNullOldRankings() {
        // When/Then
        assertThrows(NullPointerException.class,
            () -> SwapPair.fromRankingsChange(null, createFullRankings()));
    }

    @Test
    void shouldThrowExceptionForNullNewRankings() {
        // When/Then
        assertThrows(NullPointerException.class,
            () -> SwapPair.fromRankingsChange(createFullRankings(), null));
    }

    // Helper methods

    private List<TeamRanking> createFullRankings() {
        // Generate 20 consistent team IDs
        List<TeamId> teamIds = new ArrayList<>();
        for (int i = 0; i < 20; i++) {
            teamIds.add(TeamId.generate());
        }

        // Create rankings with these IDs
        List<TeamRanking> rankings = new ArrayList<>();
        for (int i = 0; i < 20; i++) {
            rankings.add(TeamRanking.create(teamIds.get(i), i + 1));
        }
        return rankings;
    }

    private List<TeamRanking> create20Rankings() {
        List<TeamRanking> rankings = new ArrayList<>();
        for (int i = 1; i <= 20; i++) {
            rankings.add(TeamRanking.create(TeamId.generate(), i));
        }
        return rankings;
    }

    private List<TeamRanking> createRankingsWithSwap(TeamId team1, int pos1, TeamId team2, int pos2) {
        List<TeamRanking> rankings = new ArrayList<>();
        rankings.add(TeamRanking.create(team1, pos1));
        rankings.add(TeamRanking.create(team2, pos2));

        // Fill remaining positions
        int nextPos = 1;
        while (rankings.size() < 20) {
            if (nextPos != pos1 && nextPos != pos2) {
                rankings.add(TeamRanking.create(TeamId.generate(), nextPos));
            }
            nextPos++;
        }

        return rankings;
    }

    private void swapPositionsInList(List<TeamRanking> rankings, int pos1, int pos2) {
        TeamRanking team1 = rankings.stream()
            .filter(r -> r.position() == pos1)
            .findFirst()
            .orElseThrow();

        TeamRanking team2 = rankings.stream()
            .filter(r -> r.position() == pos2)
            .findFirst()
            .orElseThrow();

        rankings.remove(team1);
        rankings.remove(team2);
        rankings.add(TeamRanking.create(team1.teamId(), pos2));
        rankings.add(TeamRanking.create(team2.teamId(), pos1));
    }
}
