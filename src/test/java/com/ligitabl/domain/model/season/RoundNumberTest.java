package com.ligitabl.domain.model.season;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for RoundNumber value object.
 * Target: 100% coverage including boundary conditions and navigation methods.
 */
class RoundNumberTest {

    @Test
    void shouldCreateRoundNumberWithValidValue() {
        // Given
        int validRound = 10;

        // When
        RoundNumber roundNumber = new RoundNumber(validRound);

        // Then
        assertNotNull(roundNumber);
        assertEquals(validRound, roundNumber.value());
    }

    @ParameterizedTest
    @ValueSource(ints = {1, 10, 19, 20, 38})
    void shouldAcceptValidRoundNumbers(int round) {
        // When/Then
        assertDoesNotThrow(() -> new RoundNumber(round));
    }

    @Test
    void shouldCreateRoundNumberUsingFactoryMethod() {
        // Given
        int validRound = 15;

        // When
        RoundNumber roundNumber = RoundNumber.of(validRound);

        // Then
        assertNotNull(roundNumber);
        assertEquals(validRound, roundNumber.value());
    }

    @ParameterizedTest
    @ValueSource(ints = {0, -1, -10, 39, 40, 100})
    void shouldThrowExceptionForInvalidRoundNumbers(int invalidRound) {
        // When/Then
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> new RoundNumber(invalidRound)
        );

        assertTrue(exception.getMessage().contains("Round number must be between 1 and 38"));
        assertTrue(exception.getMessage().contains("Provided: " + invalidRound));
    }

    @Test
    void shouldAcceptMinimumRound() {
        // When/Then
        assertDoesNotThrow(() -> RoundNumber.of(RoundNumber.MIN_ROUND));
    }

    @Test
    void shouldAcceptMaximumRound() {
        // When/Then
        assertDoesNotThrow(() -> RoundNumber.of(RoundNumber.MAX_ROUND));
    }

    @Test
    void shouldIdentifyFirstRound() {
        // Given
        RoundNumber firstRound = RoundNumber.of(1);
        RoundNumber notFirstRound = RoundNumber.of(2);

        // When/Then
        assertTrue(firstRound.isFirstRound());
        assertFalse(notFirstRound.isFirstRound());
    }

    @Test
    void shouldIdentifyLastRound() {
        // Given
        RoundNumber lastRound = RoundNumber.of(38);
        RoundNumber notLastRound = RoundNumber.of(37);

        // When/Then
        assertTrue(lastRound.isLastRound());
        assertFalse(notLastRound.isLastRound());
    }

    @Test
    void shouldGetNextRound() {
        // Given
        RoundNumber round = RoundNumber.of(10);

        // When
        RoundNumber next = round.next();

        // Then
        assertEquals(11, next.value());
    }

    @Test
    void shouldGetPreviousRound() {
        // Given
        RoundNumber round = RoundNumber.of(10);

        // When
        RoundNumber previous = round.previous();

        // Then
        assertEquals(9, previous.value());
    }

    @Test
    void shouldThrowExceptionWhenGettingNextFromLastRound() {
        // Given
        RoundNumber lastRound = RoundNumber.of(38);

        // When/Then
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            lastRound::next
        );

        assertTrue(exception.getMessage().contains("Cannot get next round"));
        assertTrue(exception.getMessage().contains("already at last round"));
    }

    @Test
    void shouldThrowExceptionWhenGettingPreviousFromFirstRound() {
        // Given
        RoundNumber firstRound = RoundNumber.of(1);

        // When/Then
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            firstRound::previous
        );

        assertTrue(exception.getMessage().contains("Cannot get previous round"));
        assertTrue(exception.getMessage().contains("already at first round"));
    }

    @Test
    void shouldNavigateThroughMultipleRounds() {
        // Given
        RoundNumber start = RoundNumber.of(10);

        // When
        RoundNumber next1 = start.next();
        RoundNumber next2 = next1.next();
        RoundNumber back = next2.previous();

        // Then
        assertEquals(11, next1.value());
        assertEquals(12, next2.value());
        assertEquals(11, back.value());
    }

    @Test
    void shouldBeEqualWhenValuesMatch() {
        // Given
        RoundNumber round1 = RoundNumber.of(10);
        RoundNumber round2 = RoundNumber.of(10);

        // When/Then
        assertEquals(round1, round2);
        assertEquals(round1.hashCode(), round2.hashCode());
    }

    @Test
    void shouldNotBeEqualWhenValuesDiffer() {
        // Given
        RoundNumber round1 = RoundNumber.of(10);
        RoundNumber round2 = RoundNumber.of(11);

        // When/Then
        assertNotEquals(round1, round2);
    }

    @Test
    void shouldHaveUsefulToString() {
        // Given
        RoundNumber round = RoundNumber.of(15);

        // When
        String toString = round.toString();

        // Then
        assertTrue(toString.contains("15"));
    }
}
