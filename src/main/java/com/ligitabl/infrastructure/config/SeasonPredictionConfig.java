package com.ligitabl.infrastructure.config;

import com.ligitabl.application.usecase.seasonprediction.CreateSeasonPredictionUseCase;
import com.ligitabl.application.usecase.seasonprediction.SwapTeamsUseCase;
import com.ligitabl.domain.model.contest.ContestId;
import com.ligitabl.domain.model.season.RoundNumber;
import com.ligitabl.domain.model.season.SeasonId;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration for season prediction use cases.
 *
 * <p>Provides provider implementations for current round and contest ID.</p>
 */
@Configuration
public class SeasonPredictionConfig {

    // Fixed IDs for game invariants (match DataInitializer)
    private static final SeasonId ACTIVE_SEASON_ID = SeasonId.of("550e8400-e29b-41d4-a716-446655440000");
    private static final ContestId MAIN_CONTEST_ID = ContestId.of("550e8400-e29b-41d4-a716-446655440001");
    private static final RoundNumber CURRENT_ROUND = RoundNumber.of(5); // TODO: Make dynamic

    /**
     * Provide RoundNumberProvider for CreateSeasonPredictionUseCase.
     *
     * @return round number provider
     */
    @Bean
    public CreateSeasonPredictionUseCase.RoundNumberProvider createPredictionRoundNumberProvider() {
        return seasonId -> CURRENT_ROUND;
    }

    /**
     * Provide RoundNumberProvider for SwapTeamsUseCase.
     *
     * @return round number provider
     */
    @Bean
    public SwapTeamsUseCase.RoundNumberProvider swapTeamsRoundNumberProvider() {
        return seasonId -> CURRENT_ROUND;
    }

    /**
     * Provide ContestIdProvider for CreateSeasonPredictionUseCase.
     *
     * @return contest ID provider
     */
    @Bean
    public CreateSeasonPredictionUseCase.ContestIdProvider contestIdProvider() {
        return seasonId -> MAIN_CONTEST_ID;
    }

    /**
     * Get the active season ID.
     *
     * @return active season ID
     */
    @Bean
    public SeasonId activeSeasonId() {
        return ACTIVE_SEASON_ID;
    }

    /**
     * Get the main contest ID.
     *
     * @return main contest ID
     */
    @Bean
    public ContestId mainContestId() {
        return MAIN_CONTEST_ID;
    }
}
