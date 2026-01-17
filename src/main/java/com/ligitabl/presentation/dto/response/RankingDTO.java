package com.ligitabl.presentation.dto.response;

import java.util.Objects;

/**
 * DTO representing a team's ranking/position.
 *
 * <p>Used in response payloads to show team standings.</p>
 */
public class RankingDTO {

    private final int position;
    private final TeamDTO team;

    public RankingDTO(int position, TeamDTO team) {
        this.position = position;
        this.team = Objects.requireNonNull(team, "team is required");
    }

    public int getPosition() {
        return position;
    }

    public TeamDTO getTeam() {
        return team;
    }

    /**
     * Check if this is a Champions League position (1-4).
     *
     * @return true if position is 1-4
     */
    public boolean isChampionsLeague() {
        return position >= 1 && position <= 4;
    }

    /**
     * Check if this is a relegation zone position (18-20).
     *
     * @return true if position is 18-20
     */
    public boolean isRelegationZone() {
        return position >= 18 && position <= 20;
    }

    /**
     * Nested DTO for team information.
     */
    public static class TeamDTO {
        private final String id;
        private final String code;
        private final String name;
        private final String crestUrl;

        public TeamDTO(String id, String code, String name, String crestUrl) {
            this.id = Objects.requireNonNull(id, "id is required");
            this.code = Objects.requireNonNull(code, "code is required");
            this.name = Objects.requireNonNull(name, "name is required");
            this.crestUrl = crestUrl; // Can be null
        }

        public String getId() {
            return id;
        }

        public String getCode() {
            return code;
        }

        public String getName() {
            return name;
        }

        public String getCrestUrl() {
            return crestUrl;
        }

        @Override
        public String toString() {
            return "TeamDTO{" +
                   "code='" + code + '\'' +
                   ", name='" + name + '\'' +
                   '}';
        }
    }

    @Override
    public String toString() {
        return "RankingDTO{" +
               "position=" + position +
               ", team=" + team +
               '}';
    }
}
