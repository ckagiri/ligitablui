package com.ligitabl.presentation.mapper;

import com.ligitabl.domain.model.standing.Match;
import com.ligitabl.domain.model.standing.TeamStanding;
import com.ligitabl.dto.Responses;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Mapper for converting domain standing/match models to legacy DTOs.
 *
 * <p>This maintains backward compatibility with existing Thymeleaf templates
 * while using the new domain models internally.</p>
 */
@Component
public class StandingViewMapper {

    /**
     * Convert domain TeamStanding to legacy DTO.
     */
    public Responses.StandingsRow toLegacyDTO(TeamStanding standing) {
        return new Responses.StandingsRow(
            standing.getPosition(),
            standing.getTeamCode(),
            standing.getTeamName(),
            standing.getCrestUrl(),
            standing.getPlayed(),
            standing.getWon(),
            standing.getDrawn(),
            standing.getLost(),
            standing.getPoints(),
            standing.getGoalsFor(),
            standing.getGoalsAgainst(),
            standing.getGoalDifference()
        );
    }

    /**
     * Convert list of domain TeamStanding to list of legacy DTOs.
     */
    public List<Responses.StandingsRow> toLegacyDTOList(List<TeamStanding> standings) {
        return standings.stream()
            .map(this::toLegacyDTO)
            .toList();
    }

    /**
     * Convert domain Match to legacy DTO.
     */
    public Responses.Match toLegacyDTO(Match match) {
        return new Responses.Match(
            match.getHomeTeam(),
            match.getAwayTeam(),
            match.getHomeScore(),
            match.getAwayScore(),
            match.getKickOff(),
            match.getStatusString(),
            match.getMatchTime().orElse(null)
        );
    }

    /**
     * Convert list of domain Match to list of legacy DTOs.
     */
    public List<Responses.Match> toMatchDTOList(List<Match> matches) {
        return matches.stream()
            .map(this::toLegacyDTO)
            .toList();
    }
}
