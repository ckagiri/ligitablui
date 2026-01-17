package com.ligitabl.domain.model.prediction;

/**
 * Status of a prediction round.
 */
public enum PredictionStatus {
    /**
     * Round is open for predictions - users can make/modify predictions.
     */
    OPEN,

    /**
     * Round is locked - predictions can no longer be modified.
     */
    LOCKED,

    /**
     * Round is completed - results have been calculated.
     */
    COMPLETED;

    /**
     * Check if modifications are allowed in this status.
     */
    public boolean allowsModifications() {
        return this == OPEN;
    }

    /**
     * Parse status from string.
     */
    public static PredictionStatus fromString(String value) {
        if (value == null || value.isBlank()) {
            return OPEN;
        }
        try {
            return PredictionStatus.valueOf(value.toUpperCase());
        } catch (IllegalArgumentException e) {
            return OPEN;
        }
    }
}
