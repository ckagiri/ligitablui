package com.ligitabl.application.command;

import com.ligitabl.domain.model.prediction.PredictionStatus;
import com.ligitabl.domain.model.season.RoundNumber;
import com.ligitabl.domain.model.user.UserId;

import java.util.Objects;

/**
 * Command object for the GetMyPrediction use case.
 */
public record GetMyPredictionCommand(
    UserId userId,
    RoundNumber round,
    PredictionStatus roundState
) {
    public GetMyPredictionCommand {
        Objects.requireNonNull(userId, "userId is required");
        Objects.requireNonNull(round, "round is required");
        Objects.requireNonNull(roundState, "roundState is required");
    }

    /**
     * Factory method to create from raw values.
     */
    public static GetMyPredictionCommand of(String userId, int round, String state) {
        return new GetMyPredictionCommand(
            UserId.of(userId),
            RoundNumber.of(round),
            PredictionStatus.fromString(state)
        );
    }

    /**
     * Builder for flexible command creation.
     */
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private UserId userId;
        private RoundNumber round;
        private PredictionStatus roundState = PredictionStatus.OPEN;

        public Builder userId(UserId userId) {
            this.userId = userId;
            return this;
        }

        public Builder userId(String userId) {
            this.userId = UserId.of(userId);
            return this;
        }

        public Builder round(RoundNumber round) {
            this.round = round;
            return this;
        }

        public Builder round(int round) {
            this.round = RoundNumber.of(round);
            return this;
        }

        public Builder roundState(PredictionStatus state) {
            this.roundState = state;
            return this;
        }

        public Builder roundState(String state) {
            this.roundState = PredictionStatus.fromString(state);
            return this;
        }

        public GetMyPredictionCommand build() {
            return new GetMyPredictionCommand(userId, round, roundState);
        }
    }
}
