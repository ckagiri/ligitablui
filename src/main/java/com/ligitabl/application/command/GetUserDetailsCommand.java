package com.ligitabl.application.command;

import com.ligitabl.domain.model.season.RoundNumber;
import com.ligitabl.domain.model.user.UserId;

import java.util.Objects;

/**
 * Command object for the GetUserDetails use case.
 *
 * <p>Encapsulates parameters needed to retrieve detailed user information.</p>
 */
public record GetUserDetailsCommand(
    UserId userId,
    RoundNumber round
) {
    public GetUserDetailsCommand {
        Objects.requireNonNull(userId, "userId is required");
        Objects.requireNonNull(round, "round is required");
    }

    /**
     * Factory method to create from raw values.
     */
    public static GetUserDetailsCommand of(String userId, int round) {
        return new GetUserDetailsCommand(
            UserId.of(userId),
            RoundNumber.of(round)
        );
    }
}
