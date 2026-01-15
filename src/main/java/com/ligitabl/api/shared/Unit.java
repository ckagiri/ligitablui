package com.ligitabl.api.shared;

/**
 * Unit type for Either monad operations.
 * Used with Either.combine() when the success value doesn't carry meaningful data.
 *
 * Example:
 * <pre>
 * Either<DomainError, Unit> result = Either.combine(
 *     validateTeamRankings(),
 *     validatePositions(),
 *     validateSeasonExists()
 * );
 * </pre>
 */
public enum Unit {
    INSTANCE;

    @Override
    public String toString() {
        return "()";
    }
}
