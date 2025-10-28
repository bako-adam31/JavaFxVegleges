package org.example.aaafcoiprojekt;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class DatabaseManager {


    private String getConnectionString() {
        return "jdbc:sqlite:DatabaseForWC.db";
    }


    public List<Team> getTeams() {
        String sql = "SELECT * FROM team";

        List<Team> teams = new ArrayList<>();

        try (Connection conn = DriverManager.getConnection(getConnectionString());
             Statement stmt  = conn.createStatement();
             ResultSet rs    = stmt.executeQuery(sql)) {

            while (rs.next()) {
                int id = rs.getInt("id");
                String name = rs.getString("name");
                String continent = rs.getString("continent");
                int attack = rs.getInt("attack");
                int defense = rs.getInt("defense");

                Team currentTeam = new Team(id, name, continent, attack, defense);

                teams.add(currentTeam);
            }

        } catch (SQLException e) {
            System.out.println("Hiba az adatok lekérdezése során: " + e.getMessage());
        }

        return teams;
    }
}