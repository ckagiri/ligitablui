package com.ligitabl.domain.model.team;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for TeamCode value object.
 * Target: 100% coverage on validation logic.
 */
class TeamCodeTest {

    @Test
    void shouldCreateTeamCodeWithValidCode() {
        // Given
        String validCode = "MCI";

        // When
        TeamCode teamCode = new TeamCode(validCode);

        // Then
        assertNotNull(teamCode);
        assertEquals(validCode, teamCode.value());
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "MCI", "ARS", "LIV", "AVL", "TOT", "CHE", "NEW", "MUN",
        "WHU", "BHA", "WOL", "FUL", "BOU", "CRY", "BRE", "EVE",
        "NFO", "LEE", "BUR", "SUN"
    })
    void shouldAcceptAllValidPremierLeagueCodes(String code) {
        // When/Then
        assertDoesNotThrow(() -> new TeamCode(code));
    }

    @Test
    void shouldCreateTeamCodeUsingFactoryMethod() {
        // Given
        String validCode = "ARS";

        // When
        TeamCode teamCode = TeamCode.of(validCode);

        // Then
        assertNotNull(teamCode);
        assertEquals(validCode, teamCode.value());
    }

    @Test
    void shouldThrowExceptionForNullCode() {
        // When/Then
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> new TeamCode(null)
        );

        assertEquals("Team code cannot be null", exception.getMessage());
    }

    @Test
    void shouldThrowExceptionForInvalidCode() {
        // Given
        String invalidCode = "INVALID";

        // When/Then
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> new TeamCode(invalidCode)
        );

        assertTrue(exception.getMessage().contains("Invalid team code: 'INVALID'"));
        assertTrue(exception.getMessage().contains("Must be one of:"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "ABC", "mci", "123", "MAN", "UTD"})
    void shouldRejectInvalidCodes(String invalidCode) {
        // When/Then
        assertThrows(IllegalArgumentException.class, () -> new TeamCode(invalidCode));
    }

    @Test
    void shouldProvideValidCodesSet() {
        // When
        var validCodes = TeamCode.validCodes();

        // Then
        assertNotNull(validCodes);
        assertEquals(20, validCodes.size());
        assertTrue(validCodes.contains("MCI"));
        assertTrue(validCodes.contains("ARS"));
        assertTrue(validCodes.contains("LIV"));
    }

    @Test
    void shouldBeEqualWhenCodesMatch() {
        // Given
        TeamCode code1 = TeamCode.of("MCI");
        TeamCode code2 = TeamCode.of("MCI");

        // When/Then
        assertEquals(code1, code2);
        assertEquals(code1.hashCode(), code2.hashCode());
    }

    @Test
    void shouldNotBeEqualWhenCodesDiffer() {
        // Given
        TeamCode code1 = TeamCode.of("MCI");
        TeamCode code2 = TeamCode.of("ARS");

        // When/Then
        assertNotEquals(code1, code2);
    }

    @Test
    void shouldHaveUsefulToString() {
        // Given
        TeamCode code = TeamCode.of("LIV");

        // When
        String toString = code.toString();

        // Then
        assertTrue(toString.contains("LIV"));
    }
}
