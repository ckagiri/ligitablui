package com.ligitabl.domain.model.contest;

import com.ligitabl.domain.model.seasonprediction.SeasonPredictionId;
import com.ligitabl.domain.model.user.UserId;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for MainContestEntry aggregate.
 */
class MainContestEntryTest {

    private final ContestEntryId entryId = ContestEntryId.generate();
    private final UserId userId = UserId.generate();
    private final ContestId contestId = ContestId.generate();
    private final SeasonPredictionId predictionId = SeasonPredictionId.generate();
    private final Instant now = Instant.now();

    @Test
    void shouldCreateMainContestEntryWithValidInput() {
        // When
        MainContestEntry entry = MainContestEntry.create(
            entryId, userId, contestId, predictionId, now
        );

        // Then
        assertNotNull(entry);
        assertEquals(entryId, entry.getId());
        assertEquals(userId, entry.getUserId());
        assertEquals(contestId, entry.getContestId());
        assertEquals(predictionId, entry.getSeasonPredictionId());
        assertEquals(now, entry.getJoinedAt());
    }

    @Test
    void shouldThrowExceptionForNullEntryId() {
        // When/Then
        assertThrows(NullPointerException.class,
            () -> MainContestEntry.create(null, userId, contestId, predictionId, now));
    }

    @Test
    void shouldThrowExceptionForNullUserId() {
        // When/Then
        assertThrows(NullPointerException.class,
            () -> MainContestEntry.create(entryId, null, contestId, predictionId, now));
    }

    @Test
    void shouldThrowExceptionForNullContestId() {
        // When/Then
        assertThrows(NullPointerException.class,
            () -> MainContestEntry.create(entryId, userId, null, predictionId, now));
    }

    @Test
    void shouldThrowExceptionForNullSeasonPredictionId() {
        // When/Then
        assertThrows(NullPointerException.class,
            () -> MainContestEntry.create(entryId, userId, contestId, null, now));
    }

    @Test
    void shouldThrowExceptionForNullTimestamp() {
        // When/Then
        assertThrows(NullPointerException.class,
            () -> MainContestEntry.create(entryId, userId, contestId, predictionId, null));
    }

    @Test
    void shouldCheckIfBelongsToUser() {
        // Given
        MainContestEntry entry = MainContestEntry.create(
            entryId, userId, contestId, predictionId, now
        );

        // When/Then
        assertTrue(entry.belongsToUser(userId));
        assertFalse(entry.belongsToUser(UserId.generate()));
    }

    @Test
    void shouldCheckIfIsForContest() {
        // Given
        MainContestEntry entry = MainContestEntry.create(
            entryId, userId, contestId, predictionId, now
        );

        // When/Then
        assertTrue(entry.isForContest(contestId));
        assertFalse(entry.isForContest(ContestId.generate()));
    }

    @Test
    void shouldBeEqualWhenIdsMatch() {
        // Given
        MainContestEntry entry1 = MainContestEntry.create(
            entryId, userId, contestId, predictionId, now
        );
        MainContestEntry entry2 = MainContestEntry.create(
            entryId, UserId.generate(), contestId, predictionId, now
        );

        // When/Then: Equal based on ID alone
        assertEquals(entry1, entry2);
        assertEquals(entry1.hashCode(), entry2.hashCode());
    }

    @Test
    void shouldNotBeEqualWhenIdsDiffer() {
        // Given
        MainContestEntry entry1 = MainContestEntry.create(
            ContestEntryId.generate(), userId, contestId, predictionId, now
        );
        MainContestEntry entry2 = MainContestEntry.create(
            ContestEntryId.generate(), userId, contestId, predictionId, now
        );

        // When/Then
        assertNotEquals(entry1, entry2);
    }

    @Test
    void shouldHaveUsefulToString() {
        // Given
        MainContestEntry entry = MainContestEntry.create(
            entryId, userId, contestId, predictionId, now
        );

        // When
        String toString = entry.toString();

        // Then
        assertTrue(toString.contains("MainContestEntry"));
        assertTrue(toString.contains(userId.value()));
    }
}
