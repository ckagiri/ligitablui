package com.ligitabl.application.usecase.leaderboard;

import com.ligitabl.api.shared.Either;
import com.ligitabl.application.command.GetUserDetailsCommand;
import com.ligitabl.application.error.ErrorMapper;
import com.ligitabl.application.error.UseCaseError;
import com.ligitabl.domain.model.leaderboard.UserDetail;
import com.ligitabl.domain.repository.LeaderboardRepository;
import org.springframework.stereotype.Service;

import java.util.Objects;

/**
 * Use case for retrieving detailed user information for leaderboard drill-down.
 *
 * <p>Returns the user's prediction and scoring details for a specific round.</p>
 */
@Service
public class GetUserDetailsUseCase {

    private final LeaderboardRepository leaderboardRepository;

    public GetUserDetailsUseCase(LeaderboardRepository leaderboardRepository) {
        this.leaderboardRepository = Objects.requireNonNull(
            leaderboardRepository,
            "leaderboardRepository is required"
        );
    }

    /**
     * Execute the use case to retrieve user details.
     *
     * @param command the get user details command
     * @return Either containing UseCaseError (left) or UserDetail (right)
     */
    public Either<UseCaseError, UserDetail> execute(GetUserDetailsCommand command) {
        return Either.catching(
            () -> findUserDetails(command),
            ErrorMapper::toUseCaseError
        );
    }

    private UserDetail findUserDetails(GetUserDetailsCommand command) {
        return leaderboardRepository
            .findUserDetails(command.userId(), command.round())
            .orElseThrow(() -> new IllegalArgumentException(
                "User not found: " + command.userId().value()
            ));
    }
}
