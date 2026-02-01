package com.ligitabl.presentation.mapper;

import com.ligitabl.domain.model.seasonprediction.TeamRanking;
import com.ligitabl.domain.model.team.TeamCode;
import com.ligitabl.domain.model.team.TeamId;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * Resolves team codes (e.g. "MCI", "ARS") to TeamId values.
 *
 * <p>Uses the established TeamId format from DataInitializer:
 * {@code team-{code}-{zero-padded-number}}</p>
 */
@Component
public class TeamCodeResolver {

    private static final Map<String, String> CODE_TO_TEAM_ID = Map.ofEntries(
        Map.entry("MCI", "team-mci-000000000001"),
        Map.entry("ARS", "team-ars-000000000002"),
        Map.entry("LIV", "team-liv-000000000003"),
        Map.entry("AVL", "team-avl-000000000004"),
        Map.entry("TOT", "team-tot-000000000005"),
        Map.entry("CHE", "team-che-000000000006"),
        Map.entry("NEW", "team-new-000000000007"),
        Map.entry("MUN", "team-mun-000000000008"),
        Map.entry("WHU", "team-whu-000000000009"),
        Map.entry("BHA", "team-bha-000000000010"),
        Map.entry("WOL", "team-wol-000000000011"),
        Map.entry("FUL", "team-ful-000000000012"),
        Map.entry("BOU", "team-bou-000000000013"),
        Map.entry("CRY", "team-cry-000000000014"),
        Map.entry("BRE", "team-bre-000000000015"),
        Map.entry("EVE", "team-eve-000000000016"),
        Map.entry("NFO", "team-nfo-000000000017"),
        Map.entry("LEE", "team-lee-000000000018"),
        Map.entry("BUR", "team-bur-000000000019"),
        Map.entry("SUN", "team-sun-000000000020")
    );

    /**
     * Resolve a team code to its TeamId.
     *
     * @param code the team code (e.g. "MCI")
     * @return the corresponding TeamId
     * @throws IllegalArgumentException if the code is invalid
     */
    public TeamId resolve(String code) {
        String upperCode = code.toUpperCase();
        TeamCode.of(upperCode); // validates the code
        String teamIdValue = CODE_TO_TEAM_ID.get(upperCode);
        return TeamId.of(teamIdValue);
    }

    /**
     * Convert an ordered list of team codes to TeamRankings.
     * Position is derived from list index (index 0 = position 1).
     *
     * @param codes ordered list of team codes
     * @return list of TeamRanking domain objects
     * @throws IllegalArgumentException if any code is invalid
     */
    public List<TeamRanking> toRankings(List<String> codes) {
        return java.util.stream.IntStream.range(0, codes.size())
            .mapToObj(i -> TeamRanking.create(resolve(codes.get(i)), i + 1))
            .toList();
    }

    /**
     * Extract team code from a TeamId value.
     *
     * @param teamId the team ID
     * @return the uppercase team code (e.g. "MCI")
     */
    public String extractCode(TeamId teamId) {
        String id = teamId.value();
        if (id.startsWith("team-")) {
            String[] parts = id.split("-");
            if (parts.length >= 2) {
                return parts[1].toUpperCase();
            }
        }
        return "UNK";
    }
}
