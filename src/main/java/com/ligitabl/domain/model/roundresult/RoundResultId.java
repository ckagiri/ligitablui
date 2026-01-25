package com.ligitabl.domain.model.roundresult;

import java.util.Objects;
import java.util.UUID;

/**
 * Value object representing a round result identifier.
 */
public record RoundResultId(String value) {

    public RoundResultId {
        Objects.requireNonNull(value, "RoundResultId value cannot be null");
        if (value.isBlank()) {
            throw new IllegalArgumentException("RoundResultId value cannot be blank");
        }
    }

    /**
     * Create a RoundResultId from a string value.
     */
    public static RoundResultId of(String value) {
        return new RoundResultId(value);
    }

    /**
     * Generate a new unique RoundResultId.
     */
    public static RoundResultId generate() {
        return new RoundResultId(UUID.randomUUID().toString());
    }

    @Override
    public String toString() {
        return value;
    }
}
