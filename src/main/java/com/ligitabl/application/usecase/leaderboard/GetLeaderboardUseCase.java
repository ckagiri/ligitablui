package com.ligitabl.application.usecase.leaderboard;

import com.ligitabl.api.shared.Either;
import com.ligitabl.application.command.GetLeaderboardCommand;
import com.ligitabl.application.error.ErrorMapper;
import com.ligitabl.application.error.UseCaseError;
import com.ligitabl.domain.model.leaderboard.LeaderboardEntry;
import com.ligitabl.domain.model.leaderboard.Phase;
import com.ligitabl.domain.repository.LeaderboardRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Use case for retrieving leaderboard data.
 *
 * <p>Handles pagination and optionally includes the current user's position
 * if they're not on the current page.</p>
 */
@Service
public class GetLeaderboardUseCase {

    private final LeaderboardRepository leaderboardRepository;

    public GetLeaderboardUseCase(LeaderboardRepository leaderboardRepository) {
        this.leaderboardRepository = Objects.requireNonNull(
            leaderboardRepository,
            "leaderboardRepository is required"
        );
    }

    /**
     * Execute the use case to retrieve leaderboard data.
     *
     * @param command the get leaderboard command
     * @return Either containing UseCaseError (left) or LeaderboardResult (right)
     */
    public Either<UseCaseError, LeaderboardResult> execute(GetLeaderboardCommand command) {
        return Either.catching(
            () -> buildLeaderboardResult(command),
            ErrorMapper::toUseCaseError
        );
    }

    private LeaderboardResult buildLeaderboardResult(GetLeaderboardCommand command) {
        Phase phase = command.phase();
        int page = command.page();
        int pageSize = command.pageSize();

        // Get paginated leaderboard entries
        List<LeaderboardEntry> entries = leaderboardRepository.findByPhase(phase, page, pageSize);

        // Get total count for pagination
        int totalEntries = leaderboardRepository.countByPhase(phase);
        int totalPages = (int) Math.ceil((double) totalEntries / pageSize);

        // Get current user's position if provided
        Optional<LeaderboardEntry> currentUserPosition = command.currentUserId()
            .flatMap(userId -> leaderboardRepository.findUserPosition(userId, phase));

        // Check if user is in current page
        boolean userInCurrentPage = currentUserPosition
            .map(pos -> isPositionInPage(pos.getPosition(), page, pageSize))
            .orElse(false);

        return new LeaderboardResult(
            phase,
            entries,
            currentUserPosition.orElse(null),
            userInCurrentPage,
            page,
            pageSize,
            totalPages,
            totalEntries
        );
    }

    private boolean isPositionInPage(int position, int page, int pageSize) {
        int startPos = (page - 1) * pageSize + 1;
        int endPos = page * pageSize;
        return position >= startPos && position <= endPos;
    }

    /**
     * Result object containing leaderboard data and pagination info.
     */
    public record LeaderboardResult(
        Phase phase,
        List<LeaderboardEntry> entries,
        LeaderboardEntry currentUserPosition,
        boolean userInCurrentPage,
        int currentPage,
        int pageSize,
        int totalPages,
        int totalEntries
    ) {
        public LeaderboardResult {
            Objects.requireNonNull(phase, "phase is required");
            Objects.requireNonNull(entries, "entries are required");
            entries = List.copyOf(entries);
        }

        /**
         * Check if there's a previous page.
         */
        public boolean hasPreviousPage() {
            return currentPage > 1;
        }

        /**
         * Check if there's a next page.
         */
        public boolean hasNextPage() {
            return currentPage < totalPages;
        }

        /**
         * Check if current user should be shown separately (not in current page).
         */
        public boolean shouldShowCurrentUserSeparately() {
            return currentUserPosition != null && !userInCurrentPage;
        }
    }
}
