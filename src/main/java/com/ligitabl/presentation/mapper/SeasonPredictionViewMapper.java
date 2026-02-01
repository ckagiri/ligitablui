package com.ligitabl.presentation.mapper;

import com.ligitabl.application.usecase.seasonprediction.CreateSeasonPredictionUseCase;
import com.ligitabl.application.usecase.seasonprediction.GetSeasonPredictionUseCase;
import com.ligitabl.domain.model.seasonprediction.RankingSource;
import com.ligitabl.domain.model.seasonprediction.SeasonPrediction;
import com.ligitabl.domain.model.seasonprediction.TeamRanking;
import com.ligitabl.domain.model.team.TeamId;
import com.ligitabl.presentation.dto.request.CreateSeasonPredictionRequest;
import com.ligitabl.presentation.dto.response.RankingDTO;
import com.ligitabl.presentation.dto.response.SeasonPredictionResponse;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Maps between domain models and presentation DTOs for season predictions.
 */
@Component
public class SeasonPredictionViewMapper {

    private final TeamCodeResolver teamCodeResolver;

    public SeasonPredictionViewMapper(TeamCodeResolver teamCodeResolver) {
        this.teamCodeResolver = teamCodeResolver;
    }

    /**
     * Map GetSeasonPredictionUseCase result to response DTO.
     */
    public SeasonPredictionResponse toResponse(GetSeasonPredictionUseCase.RankingsWithSource result) {
        List<RankingDTO> rankingDTOs = result.rankings().stream()
            .map(this::toRankingDTO)
            .collect(Collectors.toList());

        return new SeasonPredictionResponse(
            result.source().name(),
            rankingDTOs,
            null
        );
    }

    /**
     * Map SeasonPrediction domain model to response DTO.
     */
    public SeasonPredictionResponse toResponse(SeasonPrediction prediction) {
        List<RankingDTO> rankingDTOs = prediction.getRankings().stream()
            .map(this::toRankingDTO)
            .collect(Collectors.toList());

        return new SeasonPredictionResponse(
            RankingSource.USER_PREDICTION.name(),
            rankingDTOs,
            String.valueOf(prediction.getAtRound().value())
        );
    }

    /**
     * Map CreateSeasonPredictionUseCase result to response DTO.
     */
    public SeasonPredictionResponse toResponse(CreateSeasonPredictionUseCase.CreatedResult result) {
        return toResponse(result.prediction());
    }

    /**
     * Convert a CreateSeasonPredictionRequest (team codes) to TeamRankings.
     */
    public List<TeamRanking> toTeamRankings(CreateSeasonPredictionRequest request) {
        return teamCodeResolver.toRankings(request.teamCodes());
    }

    /**
     * Map TeamRanking domain object to RankingDTO.
     */
    public RankingDTO toRankingDTO(TeamRanking ranking) {
        RankingDTO.TeamDTO teamDTO = createTeamDTO(ranking.teamId());
        return new RankingDTO(ranking.position(), teamDTO);
    }

    private RankingDTO.TeamDTO createTeamDTO(TeamId teamId) {
        String id = teamId.value();
        String code = teamCodeResolver.extractCode(teamId);
        String name = getTeamName(code);

        return new RankingDTO.TeamDTO(
            id,
            code,
            name,
            null
        );
    }

    private String getTeamName(String code) {
        return switch (code) {
            case "MCI" -> "Manchester City";
            case "ARS" -> "Arsenal";
            case "LIV" -> "Liverpool";
            case "AVL" -> "Aston Villa";
            case "TOT" -> "Tottenham";
            case "CHE" -> "Chelsea";
            case "NEW" -> "Newcastle";
            case "MUN" -> "Man United";
            case "WHU" -> "West Ham";
            case "BHA" -> "Brighton";
            case "WOL" -> "Wolves";
            case "FUL" -> "Fulham";
            case "BOU" -> "Bournemouth";
            case "CRY" -> "Crystal Palace";
            case "BRE" -> "Brentford";
            case "EVE" -> "Everton";
            case "NFO" -> "Nottingham Forest";
            case "LEE" -> "Leeds United";
            case "BUR" -> "Burnley";
            case "SUN" -> "Sunderland";
            default -> "Unknown Team";
        };
    }
}
