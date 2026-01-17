package com.ligitabl.infrastructure.persistence;

import com.ligitabl.domain.model.prediction.Fixture;
import com.ligitabl.domain.model.season.RoundNumber;
import com.ligitabl.domain.repository.FixtureRepository;
import org.springframework.stereotype.Repository;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * In-memory implementation of FixtureRepository.
 *
 * <p>Returns stub fixture data for demonstration purposes.
 * In production, this would be replaced with a database-backed implementation.</p>
 */
@Repository
public class InMemoryFixtureRepository implements FixtureRepository {

    @Override
    public Map<String, List<Fixture>> findByRound(RoundNumber round) {
        // Stub data for demo - GW 19
        Map<String, List<Fixture>> fixtures = new HashMap<>();

        fixtures.put("MCI", List.of(Fixture.home("ARS")));
        fixtures.put("ARS", List.of(Fixture.away("MCI")));
        fixtures.put("LIV", List.of(Fixture.home("TOT"), Fixture.away("CHE"))); // Double gameweek
        fixtures.put("TOT", List.of(Fixture.away("LIV")));
        fixtures.put("CHE", List.of(Fixture.home("NEW"), Fixture.home("LIV"))); // Double gameweek
        fixtures.put("NEW", List.of(Fixture.away("CHE")));
        fixtures.put("AVL", List.of(Fixture.home("MUN")));
        fixtures.put("MUN", List.of(Fixture.away("AVL")));
        fixtures.put("WHU", List.of(Fixture.home("BHA")));
        fixtures.put("BHA", List.of(Fixture.away("WHU")));
        fixtures.put("WOL", List.of(Fixture.away("FUL")));
        fixtures.put("FUL", List.of(Fixture.home("WOL")));
        fixtures.put("BOU", List.of(Fixture.home("CRY")));
        fixtures.put("CRY", List.of(Fixture.away("BOU")));
        fixtures.put("BRE", List.of(Fixture.away("EVE")));
        fixtures.put("EVE", List.of(Fixture.home("BRE")));
        fixtures.put("NFO", List.of(Fixture.home("LEE")));
        fixtures.put("LEE", List.of(Fixture.away("NFO")));
        fixtures.put("BUR", List.of(Fixture.away("SUN")));
        fixtures.put("SUN", List.of(Fixture.home("BUR")));

        return fixtures;
    }

    @Override
    public List<Fixture> findByTeamAndRound(String teamCode, RoundNumber round) {
        Map<String, List<Fixture>> allFixtures = findByRound(round);
        return allFixtures.getOrDefault(teamCode, List.of());
    }
}
