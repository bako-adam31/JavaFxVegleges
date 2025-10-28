package org.example.aaafcoiprojekt;

public class GroupTeam {

    public final Team team;
    public int points;
    public int goalsScored;
    public int goalsConceded;

    public GroupTeam(Team team) {
        this.team = team;
        this.points = 0;
        this.goalsScored = 0;
        this.goalsConceded = 0;
    }

    public String getName() { return team.name(); }
    public int getAttack() { return team.attack(); }
    public int getDefense() { return team.defense(); }
    public int getGoalDifference() { return goalsScored - goalsConceded; }

    public int getPoints() {
        return this.points;
    }

    @Override
    public String toString() {
        return String.format("%-15s | %d pont | GS: %d | GA: %d | GD: %d",
                getName(), points, goalsScored, goalsConceded, getGoalDifference());
    }
}