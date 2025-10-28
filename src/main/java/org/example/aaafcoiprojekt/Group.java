package org.example.aaafcoiprojekt;

import java.util.List;
import java.util.ArrayList;

public class Group {

    public final String name;
    public final List<GroupTeam> teams;

    private final List<Match> round1Fixtures = new ArrayList<>();
    private final List<Match> round2Fixtures = new ArrayList<>();
    private final List<Match> round3Fixtures = new ArrayList<>();

    public Group(String name, List<Team> staticTeams) {
        this.name = name;
        this.teams = new ArrayList<>();

        for (Team t : staticTeams) {
            this.teams.add(new GroupTeam(t));
        }

        generateFixtures();
    }


    private void generateFixtures() {
        GroupTeam t0 = teams.get(0); // A csapat
        GroupTeam t1 = teams.get(1); // B csapat
        GroupTeam t2 = teams.get(2); // C csapat
        GroupTeam t3 = teams.get(3); // D csapat

        // 1. Fordulo: A-B, C-D
        round1Fixtures.add(new Match(t0, t1));
        round1Fixtures.add(new Match(t2, t3));

        // 2. Fordulo: A-C, B-D
        round2Fixtures.add(new Match(t0, t2));
        round2Fixtures.add(new Match(t1, t3));

        // 3. Fordulo: A-D, B-C
        round3Fixtures.add(new Match(t0, t3));
        round3Fixtures.add(new Match(t1, t2));
    }

    public List<Match> getFixturesForRound(int roundNumber) {
        return switch (roundNumber) {
            case 1 -> round1Fixtures;
            case 2 -> round2Fixtures;
            case 3 -> round3Fixtures;
            default -> new ArrayList<>();
        };
    }
}