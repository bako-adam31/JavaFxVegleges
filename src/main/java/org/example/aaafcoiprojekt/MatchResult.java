package org.example.aaafcoiprojekt;

public record MatchResult(
        Match match,
        int homeScore,
        int awayScore
) {
    @Override
    public String toString() {
        return String.format("%s %d - %d %s",
                match.home().getName(),
                homeScore,
                awayScore,
                match.away().getName());
    }
}