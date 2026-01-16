package com.ligitabl.domain.model.seasonprediction;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for RankingSource enum.
 */
class RankingSourceTest {

    @Test
    void shouldHaveCorrectDisplayNames() {
        // When/Then
        assertEquals("Your Prediction", RankingSource.USER_PREDICTION.getDisplayName());
        assertEquals("Current Round Standings", RankingSource.ROUND_STANDINGS.getDisplayName());
        assertEquals("Season Baseline", RankingSource.SEASON_BASELINE.getDisplayName());
    }

    @Test
    void shouldIdentifyUserPrediction() {
        // When/Then
        assertTrue(RankingSource.USER_PREDICTION.isUserPrediction());
        assertFalse(RankingSource.ROUND_STANDINGS.isUserPrediction());
        assertFalse(RankingSource.SEASON_BASELINE.isUserPrediction());
    }

    @Test
    void shouldIdentifyFallback() {
        // When/Then
        assertFalse(RankingSource.USER_PREDICTION.isFallback());
        assertTrue(RankingSource.ROUND_STANDINGS.isFallback());
        assertTrue(RankingSource.SEASON_BASELINE.isFallback());
    }

    @Test
    void shouldHaveAllThreeValues() {
        // When
        RankingSource[] values = RankingSource.values();

        // Then
        assertEquals(3, values.length);
        assertEquals(RankingSource.USER_PREDICTION, values[0]);
        assertEquals(RankingSource.ROUND_STANDINGS, values[1]);
        assertEquals(RankingSource.SEASON_BASELINE, values[2]);
    }

    @Test
    void shouldConvertFromString() {
        // When/Then
        assertEquals(RankingSource.USER_PREDICTION, RankingSource.valueOf("USER_PREDICTION"));
        assertEquals(RankingSource.ROUND_STANDINGS, RankingSource.valueOf("ROUND_STANDINGS"));
        assertEquals(RankingSource.SEASON_BASELINE, RankingSource.valueOf("SEASON_BASELINE"));
    }
}
