package com.ligitabl.infrastructure.persistence;

import com.ligitabl.domain.model.contest.ContestEntryId;
import com.ligitabl.domain.model.contest.ContestId;
import com.ligitabl.domain.model.contest.MainContestEntry;
import com.ligitabl.domain.model.user.UserId;
import com.ligitabl.domain.repository.MainContestEntryRepository;
import org.springframework.stereotype.Repository;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory implementation of MainContestEntryRepository.
 *
 * <p>This implementation uses ConcurrentHashMap for thread-safe storage.
 * Data is stored in memory and will be lost on application restart.</p>
 */
@Repository
public class InMemoryMainContestEntryRepository implements MainContestEntryRepository {

    // Primary storage: ID -> MainContestEntry
    private final Map<String, MainContestEntry> entryById = new ConcurrentHashMap<>();

    // Secondary index: userId+contestId -> entryId (for quick lookup)
    private final Map<String, String> entryIdByUserAndContest = new ConcurrentHashMap<>();

    @Override
    public MainContestEntry save(MainContestEntry entry) {
        Objects.requireNonNull(entry, "MainContestEntry cannot be null");

        String id = entry.getId().value();
        String userContestKey = buildUserContestKey(entry.getUserId(), entry.getContestId());

        // Store in primary storage
        entryById.put(id, entry);

        // Update secondary index
        entryIdByUserAndContest.put(userContestKey, id);

        return entry;
    }

    @Override
    public Optional<MainContestEntry> findById(ContestEntryId id) {
        Objects.requireNonNull(id, "ContestEntryId cannot be null");
        return Optional.ofNullable(entryById.get(id.value()));
    }

    @Override
    public Optional<MainContestEntry> findByUserIdAndContestId(UserId userId, ContestId contestId) {
        Objects.requireNonNull(userId, "UserId cannot be null");
        Objects.requireNonNull(contestId, "ContestId cannot be null");

        String userContestKey = buildUserContestKey(userId, contestId);
        String entryId = entryIdByUserAndContest.get(userContestKey);

        if (entryId == null) {
            return Optional.empty();
        }

        return Optional.ofNullable(entryById.get(entryId));
    }

    @Override
    public boolean existsByUserIdAndContestId(UserId userId, ContestId contestId) {
        Objects.requireNonNull(userId, "UserId cannot be null");
        Objects.requireNonNull(contestId, "ContestId cannot be null");

        String userContestKey = buildUserContestKey(userId, contestId);
        return entryIdByUserAndContest.containsKey(userContestKey);
    }

    @Override
    public void deleteById(ContestEntryId id) {
        Objects.requireNonNull(id, "ContestEntryId cannot be null");

        MainContestEntry entry = entryById.remove(id.value());

        if (entry != null) {
            // Remove from secondary index
            String userContestKey = buildUserContestKey(
                entry.getUserId(),
                entry.getContestId()
            );
            entryIdByUserAndContest.remove(userContestKey);
        }
    }

    /**
     * Build composite key for user+contest index.
     *
     * @param userId the user ID
     * @param contestId the contest ID
     * @return composite key string
     */
    private String buildUserContestKey(UserId userId, ContestId contestId) {
        return userId.value() + ":" + contestId.value();
    }

    /**
     * Clear all data (useful for testing).
     */
    public void clear() {
        entryById.clear();
        entryIdByUserAndContest.clear();
    }

    /**
     * Get total count of entries (useful for testing/monitoring).
     *
     * @return total number of stored entries
     */
    public int count() {
        return entryById.size();
    }
}
