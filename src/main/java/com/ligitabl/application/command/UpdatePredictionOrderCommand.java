package com.ligitabl.application.command;

import com.ligitabl.domain.model.user.UserId;

import java.util.List;
import java.util.Objects;

/**
 * Command object for the UpdatePredictionOrder use case.
 */
public record UpdatePredictionOrderCommand(
    UserId userId,
    List<String> teamCodes
) {
    public UpdatePredictionOrderCommand {
        Objects.requireNonNull(userId, "userId is required");
        Objects.requireNonNull(teamCodes, "teamCodes are required");
        if (teamCodes.size() != 20) {
            throw new IllegalArgumentException("Must provide exactly 20 team codes");
        }
        teamCodes = List.copyOf(teamCodes);
    }

    /**
     * Factory method to create from raw values.
     */
    public static UpdatePredictionOrderCommand of(String userId, List<String> teamCodes) {
        return new UpdatePredictionOrderCommand(
            UserId.of(userId),
            teamCodes
        );
    }
}
