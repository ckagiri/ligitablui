package com.ligitabl.application.command;

import com.ligitabl.domain.model.leaderboard.Phase;
import com.ligitabl.domain.model.user.UserId;

import java.util.Objects;
import java.util.Optional;

/**
 * Command object for the GetLeaderboard use case.
 *
 * <p>Encapsulates all parameters needed to retrieve leaderboard data.</p>
 */
public record GetLeaderboardCommand(
    Phase phase,
    int page,
    int pageSize,
    Optional<UserId> currentUserId
) {
    public GetLeaderboardCommand {
        Objects.requireNonNull(phase, "phase is required");
        Objects.requireNonNull(currentUserId, "currentUserId optional is required");
        if (page < 1) {
            throw new IllegalArgumentException("Page must be at least 1");
        }
        if (pageSize < 1 || pageSize > 100) {
            throw new IllegalArgumentException("Page size must be between 1 and 100");
        }
    }

    /**
     * Builder for creating GetLeaderboardCommand with defaults.
     */
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private Phase phase = Phase.FS;
        private int page = 1;
        private int pageSize = 10;
        private UserId currentUserId = null;

        public Builder phase(Phase phase) {
            this.phase = phase;
            return this;
        }

        public Builder phase(String phaseStr) {
            this.phase = Phase.fromString(phaseStr);
            return this;
        }

        public Builder page(int page) {
            this.page = page;
            return this;
        }

        public Builder pageSize(int pageSize) {
            this.pageSize = pageSize;
            return this;
        }

        public Builder currentUserId(UserId userId) {
            this.currentUserId = userId;
            return this;
        }

        public Builder currentUserId(String userId) {
            this.currentUserId = userId != null ? UserId.of(userId) : null;
            return this;
        }

        public GetLeaderboardCommand build() {
            return new GetLeaderboardCommand(
                phase,
                page,
                pageSize,
                Optional.ofNullable(currentUserId)
            );
        }
    }
}
