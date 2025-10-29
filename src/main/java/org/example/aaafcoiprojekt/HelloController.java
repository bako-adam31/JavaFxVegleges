package org.example.aaafcoiprojekt;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.TextArea;

// Importok a régi Main.java-ból és a logikai osztályokból
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

// Ez az osztály a régi Main.java logikáját tartalmazza,
// de már a JavaFX gombokhoz és szövegdobozokhoz van kötve.
public class HelloController {

    // --- A 'Backend' Logika Változói ---
    private DatabaseManager dbManager;
    private MatchSimulator simulator;
    private List<Group> allGroups;
    private int currentRound;

    // Ez a lista tárolja a 8 bal oldali szövegdobozt
    private List<TextArea> groupTextAreas;

    // --- Az FXML-ből Behúzott (Injektált) Elemek ---
    // A @FXML annotáció köti össze a Scene Builderben megadott fx:id-val
    @FXML private Button forduloButton;
    @FXML private TextArea eredmenyTextArea;
    @FXML private TextArea groupATextArea;
    @FXML private TextArea groupBTextArea;
    @FXML private TextArea groupCTextArea;
    @FXML private TextArea groupDTextArea;
    @FXML private TextArea groupETextArea;
    @FXML private TextArea groupFTextArea;
    @FXML private TextArea groupGTextArea;
    @FXML private TextArea groupHTextArea;

    /**
     * Ez a metódus automatikusan lefut, miután az FXML betöltődött.
     * Ez az új "indítási pontunk" (a régi 'main' metódus helyett).
     */
    @FXML
    public void initialize() {
        // 1. Logikai motor inicializálása
        dbManager = new DatabaseManager();
        simulator = new MatchSimulator();
        allGroups = new ArrayList<>();
        currentRound = 1;

        // 2. A 8 szövegdoboz összegyűjtése egy listába
        groupTextAreas = List.of(
                groupATextArea, groupBTextArea, groupCTextArea, groupDTextArea,
                groupETextArea, groupFTextArea, groupGTextArea, groupHTextArea
        );

        // 3. Gomb letiltása, amíg a sorsolás tart
        forduloButton.setDisable(true);
        forduloButton.setText("Sorsolás...");

        // 4. Sorsolás futtatása és eredmények kiírása a GUI-ra
        runDrawAndDisplayGroups();
    }

    /**
     * Ez a metódus fut le, amikor a gombra kattintunk
     * (mert az FXML-ben az 'onAction'-höz ezt adtuk meg).
     */
    @FXML
    private void onForduloButtonClick() {
        // A régi 'Scanner' (ENTER-re vár) helyett ez a metódus fut le

        logMessage("\n=============================================");
        logMessage(String.format("=== %d. FORDULÓ ===", currentRound));

        List<MatchResult> roundResults = new ArrayList<>();

        for (Group group : allGroups) {
            List<Match> fixtures = group.getFixturesForRound(currentRound);
            for (Match match : fixtures) {
                MatchResult result = simulator.simulate(match);
                roundResults.add(result);
                updateStandings(result); // Pontszámok frissítése
            }
        }

        // Eredmények kiírása a jobb oldali dobozba
        printRoundResults(roundResults, currentRound);

        currentRound++; // Lépés a következő fordulóra

        if (currentRound > 3) {
            // VÉGE A CSOPORTKÖRNEK
            forduloButton.setText("Csoportkör Vége");
            forduloButton.setDisable(true);

            logMessage("\n--- A CSOPORTKÖR VÉGEREDMÉNYE ---");
            // 1. Végső tabella kiírása a jobb oldali dobozba
            //printFinalStandings(allGroups);
            // 2. A bal oldali 8 doboz frissítése a pontszámokkal
            updateGroupPanelsWithFinalStandings();

        } else {
            // Felkészülés a következő fordulóra
            forduloButton.setText(String.format("%d. Forduló Indítása", currentRound));
        }
    }

    /**
     * A System.out.println() helyett ez a metódus ír a JOBB oldali
     * 'eredmenyTextArea' szövegdobozba.
     */
    private void logMessage(String message) {
        eredmenyTextArea.appendText(message + "\n");
    }

    // =========================================================================
    // --- ÁTMÁSOLT LOGIKA (a régi Main.java-ból) ---
    // FIGYELEM: A 'static' kulcsszó el lett távolítva mindenhonnan!
    // =========================================================================

    /**
     * Lefuttatja a sorsolást és azonnal kiírja a csapatokat
     * a bal oldali 8 szövegdobozba.
     */
    private void runDrawAndDisplayGroups() {
        logMessage("Sorsolás indítása..."); // Logolás a jobb oldali dobozba

        Map<String, List<Team>> drawnGroupsMap = runDraw();

        if (drawnGroupsMap.isEmpty()) {
            logMessage("Hiba: A sorsolás sikertelen! Ellenőrizd az adatbázist (32 csapat?).");
            forduloButton.setText("Hiba!");
            return;
        }

        this.allGroups.clear();
        for (Map.Entry<String, List<Team>> entry : drawnGroupsMap.entrySet()) {
            this.allGroups.add(new Group(entry.getKey(), entry.getValue()));
        }

        //logMessage("Sikeres sorsolás! A csoportok kialakítva.");

        // --- CSOPORTOK KIÍRÁSA A BAL OLDALRA ---
        for (int i = 0; i < allGroups.size(); i++) {
            Group group = allGroups.get(i);
            TextArea targetArea = groupTextAreas.get(i); // A megfelelő (A, B, C...) doboz

            targetArea.clear(); // Előző tartalom törlése
            for (GroupTeam team : group.teams) {
                targetArea.appendText(team.getName() + "\n");
            }
        }

        // Előkészülünk az 1. fordulóra
        currentRound = 1;
        forduloButton.setDisable(false); // Gomb engedélyezése
        forduloButton.setText("1. Forduló Indítása");
    }

    /**
     * Frissíti a bal oldali 8 szövegdobozt a végső, rendezett állással.
     */
    private void updateGroupPanelsWithFinalStandings() {
        for (int i = 0; i < allGroups.size(); i++) {
            Group group = allGroups.get(i);
            TextArea targetArea = groupTextAreas.get(i);

            targetArea.clear(); // Töröljük a régi (csak nevek) listát

            // Létrehozzuk a rendezőt (LAMBDA NÉLKÜL)
            Comparator<GroupTeam> teamComparator = new Comparator<GroupTeam>() {
                @Override
                public int compare(GroupTeam t1, GroupTeam t2) {
                    int pontKulonbseg = Integer.compare(t2.getPoints(), t1.getPoints());
                    if (pontKulonbseg != 0) return pontKulonbseg;
                    return Integer.compare(t2.getGoalDifference(), t1.getGoalDifference());
                }
            };
            group.teams.sort(teamComparator); // Rendezés

            // Kiírás a bal oldali dobozba
            int rank = 1;
            for (GroupTeam team : group.teams) {
                // Szebb kiíratás a végén
                targetArea.appendText(String.format("%d. %s  P: %d | GD: %d\n",
                        rank++,
                        team.getName(),
                        team.getPoints(),
                        team.getGoalDifference()
                ));
            }
        }
    }

    /**
     * Frissíti a pontszámokat (ez a metódus nem változott).
     */
    private void updateStandings(MatchResult result) {
        GroupTeam home = result.match().home();
        GroupTeam away = result.match().away();

        home.goalsScored += result.homeScore();
        home.goalsConceded += result.awayScore();
        away.goalsScored += result.awayScore();
        away.goalsConceded += result.homeScore();

        if (result.homeScore() > result.awayScore()) {
            home.points += 3;
        } else if (result.awayScore() > result.homeScore()) {
            away.points += 3;
        } else {
            home.points += 1;
            away.points += 1;
        }
    }

    /**
     * Kiírja a forduló eredményeit a JOBB oldali dobozba.
     */
    private void printRoundResults(List<MatchResult> results, int round) {
        logMessage(String.format("\n--- %d. FORDULÓ EREDMÉNYEI ---", round));
        for (MatchResult result : results) {
            logMessage(result.toString());
        }
    }

    /**
     * Kiírja a végső tabellát a JOBB oldali dobozba.

    private void printFinalStandings(List<Group> allGroups) {
        for (Group group : allGroups) {
            logMessage("\n--- " + group.name.toUpperCase() + " ---");

            Comparator<GroupTeam> teamComparator = new Comparator<GroupTeam>() {
                @Override
                public int compare(GroupTeam t1, GroupTeam t2) {
                    int pontKulonbseg = Integer.compare(t2.getPoints(), t1.getPoints());
                    if (pontKulonbseg != 0) return pontKulonbseg;
                    return Integer.compare(t2.getGoalDifference(), t1.getGoalDifference());
                }
            };
            group.teams.sort(teamComparator);

            for (GroupTeam team : group.teams) {
                // A 'toString()' metódus a GroupTeam-ből jön
                logMessage(team.toString());
            }
        }
    }*/

    /**
     * Lefuttatja a sorsolást (adatbázis-olvasás, keverés, stb.)
     */
    private Map<String, List<Team>> runDraw() {
        List<Team> allTeams = dbManager.getTeams();

        if (allTeams.size() != 32) {
            logMessage("Hiba: A sorsoláshoz pontosan 32 csapat kell! (Találat: " + allTeams.size() + ")");
            return Collections.emptyMap();
        }

        Collections.shuffle(allTeams);

        Map<String, List<Team>> groups = new LinkedHashMap<>();
        for (char c = 'A'; c <= 'H'; c++) {
            groups.put("Group " + c, new ArrayList<>());
        }

        List<Team> unplacedTeams = new ArrayList<>();

        for (Team teamToPlace : allTeams) {
            List<List<Team>> validGroups = new ArrayList<>();
            for (List<Team> group : groups.values()) {
                if (group.size() < 4 && isContinentOk(teamToPlace, group)) {
                    validGroups.add(group);
                }
            }

            if (!validGroups.isEmpty()) {
                Comparator<List<Team>> groupSizeComparator = new Comparator<List<Team>>() {
                    @Override
                    public int compare(List<Team> g1, List<Team> g2) {
                        return Integer.compare(g1.size(), g2.size());
                    }
                };
                validGroups.sort(groupSizeComparator);

                List<Team> bestGroup = validGroups.get(0);
                bestGroup.add(teamToPlace);
            } else {
                unplacedTeams.add(teamToPlace);
            }
        }

        if (!unplacedTeams.isEmpty()) {
            logMessage("FIGYELEM: " + unplacedTeams.size() + " csapatot nem sikerült szabályosan elhelyezni, erőltetett elhelyezés...");
            for (Team stuckTeam : unplacedTeams) {
                for (List<Team> group : groups.values()) {
                    if (group.size() < 4) {
                        group.add(stuckTeam);
                        break;
                    }
                }
            }
        }

        return groups;
    }

    /**
     * Kontinens-szabály ellenőrző (nem változott)
     */
    private boolean isContinentOk(Team newTeam, List<Team> group) {
        int count = 0;
        for (Team teamInGroup : group) {
            if (teamInGroup.continent().equals(newTeam.continent())) {
                count++;
            }
        }
        return count < 2;
    }
}