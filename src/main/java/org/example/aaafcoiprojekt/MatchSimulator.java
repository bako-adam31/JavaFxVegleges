package org.example.aaafcoiprojekt;

import java.util.Random;

public class MatchSimulator {

    private Random random = new Random();


    public MatchResult simulate(Match match) {
        GroupTeam home = match.home();
        GroupTeam away = match.away();

        int homeScore = 0;
        int awayScore = 0;

        int homeAttack = home.getAttack();
        int awayDefense = away.getDefense();
        int awayAttack = away.getAttack();
        int homeDefense = home.getDefense();

        for (int i = 0; i < 5; i++) {
            if (random.nextInt(homeAttack + awayDefense) < homeAttack) {
                homeScore++;
            }
            if (random.nextInt(awayAttack + homeDefense) < awayAttack) {
                awayScore++;
            }
        }

        return new MatchResult(match, homeScore, awayScore);
    }
}