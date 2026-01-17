package com.ligitabl.domain.model.prediction;

import java.util.Objects;
import java.util.UUID;

/**
 * Value object representing a round prediction's unique identifier.
 */
public record RoundPredictionId(String value) {

    public RoundPredictionId {
        Objects.requireNonNull(value, "RoundPredictionId value cannot be null");
        if (value.isBlank()) {
            throw new IllegalArgumentException("RoundPredictionId value cannot be blank");
        }
    }

    public static RoundPredictionId of(String value) {
        return new RoundPredictionId(value);
    }

    public static RoundPredictionId generate() {
        return new RoundPredictionId(UUID.randomUUID().toString());
    }
}
