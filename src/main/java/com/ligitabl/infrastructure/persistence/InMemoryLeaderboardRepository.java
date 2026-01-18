package com.ligitabl.infrastructure.persistence;

import com.ligitabl.domain.model.leaderboard.LeaderboardEntry;
import com.ligitabl.domain.model.leaderboard.Phase;
import com.ligitabl.domain.model.leaderboard.UserDetail;
import com.ligitabl.domain.model.season.RoundNumber;
import com.ligitabl.domain.model.user.UserId;
import com.ligitabl.domain.repository.LeaderboardRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * In-memory implementation of LeaderboardRepository.
 *
 * <p>Returns stub data for demonstration purposes.
 * In production, this would be replaced with a database-backed implementation.</p>
 */
@Repository
public class InMemoryLeaderboardRepository implements LeaderboardRepository {

    // Stub leaderboard data (using valid UUIDs)
    private static final List<LeaderboardEntry> STUB_LEADERBOARD = List.of(
        LeaderboardEntry.fromRaw(1, "123e4567-e89b-12d3-a456-426614174001", "Alice Wonder", 1850, 45, 198, 23, 52, 0),
        LeaderboardEntry.fromRaw(2, "123e4567-e89b-12d3-a456-426614174002", "Bob Smith", 1850, 45, 195, 28, 52, -1),
        LeaderboardEntry.fromRaw(3, "123e4567-e89b-12d3-a456-426614174003", "Carol Jones", 1845, 44, 200, 20, 52, 2),
        LeaderboardEntry.fromRaw(4, "123e4567-e89b-12d3-a456-426614174004", "Dave Brown", 1840, 50, 190, 15, 50, -1),
        LeaderboardEntry.fromRaw(5, "123e4567-e89b-12d3-a456-426614174005", "Eve Davis", 1825, 42, 185, 31, 48, 1),
        LeaderboardEntry.fromRaw(6, "123e4567-e89b-12d3-a456-426614174006", "Frank Miller", 1810, 38, 180, 29, 46, -2),
        LeaderboardEntry.fromRaw(7, "123e4567-e89b-12d3-a456-426614174007", "Grace Wilson", 1805, 41, 178, 25, 45, 3),
        LeaderboardEntry.fromRaw(8, "123e4567-e89b-12d3-a456-426614174008", "Henry Moore", 1795, 37, 175, 33, 44, 0),
        LeaderboardEntry.fromRaw(9, "123e4567-e89b-12d3-a456-426614174009", "Ivy Taylor", 1780, 35, 172, 27, 42, -3),
        LeaderboardEntry.fromRaw(10, "123e4567-e89b-12d3-a456-42661417400a", "Jack Anderson", 1775, 40, 170, 22, 42, 1)
    );

    // Current user stub data
    private static final LeaderboardEntry CURRENT_USER_POSITION = LeaderboardEntry.fromRaw(
        45, "00000000-0000-0000-0000-000000000000", "Deejay Wagz", 1702, 156, 175, 15, 168, 3
    );

    @Override
    public List<LeaderboardEntry> findByPhase(Phase phase, int page, int pageSize) {
        // For stub, ignore phase and return paginated results
        int startIndex = (page - 1) * pageSize;
        int endIndex = Math.min(startIndex + pageSize, STUB_LEADERBOARD.size());

        if (startIndex >= STUB_LEADERBOARD.size()) {
            return List.of();
        }

        return STUB_LEADERBOARD.subList(startIndex, endIndex);
    }

    @Override
    public List<LeaderboardEntry> findAllByPhase(Phase phase) {
        // For stub, return all entries regardless of phase
        return STUB_LEADERBOARD;
    }

    @Override
    public Optional<LeaderboardEntry> findUserPosition(UserId userId, Phase phase) {
        // Check if looking for current user (demo user)
        if ("00000000-0000-0000-0000-000000000000".equals(userId.value())) {
            return Optional.of(CURRENT_USER_POSITION);
        }

        // Search in leaderboard
        return STUB_LEADERBOARD.stream()
            .filter(entry -> entry.getUserId().equals(userId))
            .findFirst();
    }

    @Override
    public Optional<UserDetail> findUserDetails(UserId userId, RoundNumber round) {
        // Find the user in leaderboard first
        Optional<LeaderboardEntry> entry = STUB_LEADERBOARD.stream()
            .filter(e -> e.getUserId().equals(userId))
            .findFirst();

        if (entry.isEmpty()) {
            return Optional.empty();
        }

        LeaderboardEntry leaderboardEntry = entry.get();

        // Create stub prediction details
        List<UserDetail.PredictionDetail> predictions = createStubPredictions(round.value());

        return Optional.of(UserDetail.create(
            userId,
            leaderboardEntry.getDisplayName(),
            leaderboardEntry.getPosition(),
            leaderboardEntry.getTotalScore(),
            round.value() == 19 ? 45 : 42, // Different score per round
            leaderboardEntry.getTotalZeroes(),
            leaderboardEntry.getMovement(),
            predictions
        ));
    }

    @Override
    public int countByPhase(Phase phase) {
        return STUB_LEADERBOARD.size();
    }

    /**
     * Create stub prediction details for a user.
     */
    private List<UserDetail.PredictionDetail> createStubPredictions(int round) {
        // Stub predictions based on round
        return List.of(
            new UserDetail.PredictionDetail(1, "MCI", "Manchester City", "/images/crests/mci.png", 0, 1),
            new UserDetail.PredictionDetail(2, "ARS", "Arsenal", "/images/crests/ars.png", 1, 3),
            new UserDetail.PredictionDetail(3, "LIV", "Liverpool", "/images/crests/liv.png", 1, 2),
            new UserDetail.PredictionDetail(4, "AVL", "Aston Villa", "/images/crests/avl.png", 0, 4),
            new UserDetail.PredictionDetail(5, "TOT", "Tottenham", "/images/crests/tot.png", 0, 5),
            new UserDetail.PredictionDetail(6, "CHE", "Chelsea", "/images/crests/che.png", 1, 7),
            new UserDetail.PredictionDetail(7, "NEW", "Newcastle", "/images/crests/new.png", 1, 6),
            new UserDetail.PredictionDetail(8, "MUN", "Man United", "/images/crests/mun.png", 0, 8),
            new UserDetail.PredictionDetail(9, "WHU", "West Ham", "/images/crests/whu.png", 0, 9),
            new UserDetail.PredictionDetail(10, "BHA", "Brighton", "/images/crests/bha.png", 0, 10),
            new UserDetail.PredictionDetail(11, "WOL", "Wolves", "/images/crests/wol.png", 0, 11),
            new UserDetail.PredictionDetail(12, "FUL", "Fulham", "/images/crests/ful.png", 0, 12),
            new UserDetail.PredictionDetail(13, "BOU", "Bournemouth", "/images/crests/bou.png", 0, 13),
            new UserDetail.PredictionDetail(14, "CRY", "Crystal Palace", "/images/crests/cry.png", 0, 14),
            new UserDetail.PredictionDetail(15, "BRE", "Brentford", "/images/crests/bre.png", 0, 15),
            new UserDetail.PredictionDetail(16, "EVE", "Everton", "/images/crests/eve.png", 0, 16),
            new UserDetail.PredictionDetail(17, "NFO", "Nottingham Forest", "/images/crests/nfo.png", 0, 17),
            new UserDetail.PredictionDetail(18, "LEE", "Leeds United", "/images/crests/lee.png", 0, 18),
            new UserDetail.PredictionDetail(19, "BUR", "Burnley", "/images/crests/bur.png", 0, 19),
            new UserDetail.PredictionDetail(20, "SUN", "Sunderland", "/images/crests/sun.png", 0, 20)
        );
    }
}
