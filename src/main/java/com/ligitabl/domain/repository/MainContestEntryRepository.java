package com.ligitabl.domain.repository;

import com.ligitabl.domain.model.contest.ContestEntryId;
import com.ligitabl.domain.model.contest.ContestId;
import com.ligitabl.domain.model.contest.MainContestEntry;
import com.ligitabl.domain.model.user.UserId;

import java.util.Optional;

/**
 * Repository interface for MainContestEntry aggregate.
 *
 * <p>This is a domain interface (port) that will be implemented by the infrastructure layer (adapter).
 * This follows the Dependency Inversion Principle - the domain defines what it needs,
 * infrastructure provides the implementation.</p>
 *
 * <p>Business Rule: One main contest entry per user per contest.</p>
 */
public interface MainContestEntryRepository {

    /**
     * Save a main contest entry (create or update).
     *
     * @param entry the main contest entry to save
     * @return the saved main contest entry
     */
    MainContestEntry save(MainContestEntry entry);

    /**
     * Find a main contest entry by its ID.
     *
     * @param id the entry ID
     * @return an Optional containing the entry if found, empty otherwise
     */
    Optional<MainContestEntry> findById(ContestEntryId id);

    /**
     * Find a main contest entry by user ID and contest ID.
     *
     * <p>This enforces the business rule: one entry per user per contest.</p>
     *
     * @param userId the user ID
     * @param contestId the contest ID
     * @return an Optional containing the entry if found, empty otherwise
     */
    Optional<MainContestEntry> findByUserIdAndContestId(UserId userId, ContestId contestId);

    /**
     * Check if a main contest entry exists for a user in a contest.
     *
     * @param userId the user ID
     * @param contestId the contest ID
     * @return true if an entry exists, false otherwise
     */
    boolean existsByUserIdAndContestId(UserId userId, ContestId contestId);

    /**
     * Delete a main contest entry.
     *
     * @param id the entry ID to delete
     */
    void deleteById(ContestEntryId id);
}
