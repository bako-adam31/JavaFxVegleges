package org.example.aaafcoiprojekt;

// Importok a JavaFX elemekhez
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader; // <-- ÚJ IMPORT a váltáshoz
import javafx.scene.Scene; // <-- ÚJ IMPORT a váltáshoz
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.stage.Stage; // <-- ÚJ IMPORT a váltáshoz
import java.io.IOException; // <-- ÚJ IMPORT a váltáshoz

// Importok a logikához
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Ez az osztály a CSOPORTKÖRT kezeli (a hello-view.fxml-t).
 * A 3. forduló után átvált a KnockoutController-re.
 */
public class HelloController {

    // --- A 'Backend' Logika Változói ---
    private DatabaseManager dbManager;
    private MatchSimulator simulator;
    private List<Group> allGroups;
    private int currentRound;

    // --- Az FXML-ből Behúzott (Injektált) Elemek ---

    // Vezérlők a jobb oldalon
    @FXML private Button forduloButton;
    @FXML private TextArea eredmenyTextArea; // Az eredmény-napló

    // A 8 csoport-panel a bal oldalon
    @FXML private VBox groupAVBox;
    @FXML private VBox groupBVBox;
    @FXML private VBox groupCVBox;
    @FXML private VBox groupDVBox;
    @FXML private VBox groupEVBox;
    @FXML private VBox groupFVBox;
    @FXML private VBox groupGVBox;
    @FXML private VBox groupHVBox;

    @FXML private VBox winnerVBox; // A győztes helye (ha a főoldalon maradna)

    private List<VBox> groupVBoxes; // Lista a 8 VBox-hoz

    /**
     * Ez a metódus automatikusan lefut, miután az FXML betöltődött.
     */
    @FXML
    public void initialize() {
        dbManager = new DatabaseManager();
        simulator = new MatchSimulator();
        allGroups = new ArrayList<>();
        currentRound = 1;

        groupVBoxes = List.of(
                groupAVBox, groupBVBox, groupCVBox, groupDVBox,
                groupEVBox, groupFVBox, groupGVBox, groupHVBox
        );

        forduloButton.setDisable(true);
        forduloButton.setText("Sorsolás...");

        runDrawAndDisplayGroups();
    }
    /**
     * Ez a metódus fut le a "Forduló Indítása" gomb megnyomásakor.
     * JAVÍTOTT VERZIÓ: A 3. forduló után megvárja a következő kattintást.
     */
    @FXML
    private void onForduloButtonClick() {

        if (currentRound <= 3) {
            // --- 1. CSOPORTKÖR ÁLLAPOT (Fordulók 1-3) ---

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

            printRoundResults(roundResults, currentRound);

            // A fordulószámot CSAK a szimuláció VÉGÉN növeljük
            currentRound++; // Lépés a következő fordulóra (2, 3, vagy 4)

            if (currentRound > 3) {
                // --- 2. ÁTMENETI ÁLLAPOT (A 3. forduló épp lement) ---

                // Gomb átállítása a KÖVETKEZŐ LÉPÉSRE
                forduloButton.setText("Kieséses Szakasz Indítása");
                // FONTOS: NEM tiltjuk le a gombot!

                logMessage("\n--- A CSOPORTKÖR VÉGEREDMÉNYE ---");

                // Végső tabella kiírása a JOBB oldali naplóba
                printFinalStandings(allGroups);
                // BAL oldali panelek frissítése a végeredménnyel
                updateAllGroupDisplays(true);

                // A VÁLTÁS KI VÉVE INNEN:
                // A switchToKnockoutScene() hívást töröljük innen.
                // A metódus itt véget ér, és a program várja a 4. kattintást.

            } else {
                // Felkészülés a következő csoportkör fordulóra (2. vagy 3.)
                forduloButton.setText(String.format("%d. Forduló Indítása", currentRound));
            }

        } else {
            // --- 3. VÁLTÁS A KIESÉSES SZAKASZRA (currentRound == 4) ---
            // A felhasználó most kattintott a "Kieséses Szakasz Indítása" gombra

            forduloButton.setText("Betöltés...");
            forduloButton.setDisable(true); // Most már letilthatjuk

            // Most hívjuk meg az oldalváltást
            switchToKnockoutScene();
        }
    }
    /**
     * EZ AZ ÚJ METÓDUS, AMI AZ OLDALVÁLTÁST VÉGZI.
     */
    private void switchToKnockoutScene() {
        try {
            // 1. Betöltjük az új FXML fájlt (knockout-view.fxml)
            FXMLLoader loader = new FXMLLoader(getClass().getResource("knockout-view.fxml"));
            Scene knockoutScene = new Scene(loader.load());

            // 2. Betöltjük a CSS-t az új oldalra is
            String cssPath = getClass().getResource("style.css").toExternalForm();
            knockoutScene.getStylesheets().add(cssPath);

            // 3. Átadjuk az adatokat (A CSOPORTKÖR EREDMÉNYÉT)
            // Lekérjük az új kontroller példányát (KnockoutController)
            KnockoutController knockoutController = loader.getController();

            // Meghívjuk az 'initData' metódusát, és átadjuk a 8 csoportot
            knockoutController.initData(this.allGroups);

            // 4. Jelenet váltása
            // Lekérjük az aktuális ablakot (Stage) a gomb segítségével
            Stage stage = (Stage) forduloButton.getScene().getWindow();

            // Beállítjuk az új jelenetet
            stage.setScene(knockoutScene);
            stage.setFullScreen(true); // Maradjon teljes képernyőn

        } catch (IOException e) {
            logMessage("HIBA az oldalváltáskor: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // --- A CSOPORTKÖR SEGÉDFÜGGVÉNYEI (Változatlanul) ---

    private void logMessage(String message) {
        eredmenyTextArea.appendText(message + "\n");
    }

    private void runDrawAndDisplayGroups() {
        logMessage("Sorsolás indítása...");
        Map<String, List<Team>> drawnGroupsMap = runDraw();

        if (drawnGroupsMap.isEmpty()) {
            logMessage("Hiba: A sorsoláshoz pontosan 32 csapat kell! (Találat: " + allGroups.size() + ")");
            forduloButton.setText("Hiba!");
            return;
        }

        this.allGroups.clear();
        for (Map.Entry<String, List<Team>> entry : drawnGroupsMap.entrySet()) {
            this.allGroups.add(new Group(entry.getKey(), entry.getValue()));
        }

        logMessage("Sikeres sorsolás! A csoportok kialakítva.");
        updateAllGroupDisplays(false); // false = még ne mutassuk a pontokat

        currentRound = 1;
        forduloButton.setDisable(false);
        forduloButton.setText("1. Forduló Indítása");
    }

    private void updateAllGroupDisplays(boolean showScores) {

        for (int i = 0; i < allGroups.size(); i++) {
            Group group = allGroups.get(i);
            VBox targetVBox = groupVBoxes.get(i);
            targetVBox.getChildren().clear();

            if (showScores) {
                Comparator<GroupTeam> teamComparator = new Comparator<GroupTeam>() {
                    @Override
                    public int compare(GroupTeam t1, GroupTeam t2) {
                        int pontKulonbseg = Integer.compare(t2.getPoints(), t1.getPoints());
                        if (pontKulonbseg != 0) return pontKulonbseg;
                        return Integer.compare(t2.getGoalDifference(), t1.getGoalDifference());
                    }
                };
                group.teams.sort(teamComparator);
            }

            int rank = 1;
            for (GroupTeam team : group.teams) {
                HBox teamRow = createTeamRow(team);

                if (showScores) {
                    String scoreText = String.format("%s ",
                            team.getName()
                    );
                    ((Label)teamRow.getChildren().get(1)).setText(scoreText);
                }

                targetVBox.getChildren().add(teamRow);
            }
        }
    }

    private HBox createTeamRow(GroupTeam team) {
        final double FLAG_SIZE = 20.0;
        ImageView flagIcon = new ImageView();
        try {
            String imagePath = "images/flags/" + team.team.flagFile();
            Image flagImage = new Image(getClass().getResourceAsStream(imagePath));
            flagIcon.setImage(flagImage);
            flagIcon.setFitWidth(FLAG_SIZE);
            flagIcon.setFitHeight(FLAG_SIZE);
            // Kerekítés (ha a PNG-id nem kerekek)
            Circle clip = new Circle(FLAG_SIZE / 2);
            clip.setCenterX(FLAG_SIZE / 2);
            clip.setCenterY(FLAG_SIZE / 2);
            flagIcon.setClip(clip);
        } catch (Exception e) {
            System.out.println("Hiba a kép betöltésekor: " + team.team.flagFile());
        }

        Label teamLabel = new Label(team.getName());
        teamLabel.setTextFill(Color.WHITE);
        teamLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");

        HBox teamRow = new HBox(5);
        teamRow.getChildren().addAll(flagIcon, teamLabel);
        return teamRow;
    }

    private void updateStandings(MatchResult result) {
        GroupTeam home = result.match().home();
        GroupTeam away = result.match().away();
        home.goalsScored += result.homeScore();
        home.goalsConceded += result.awayScore();
        away.goalsScored += result.awayScore();
        away.goalsConceded += result.homeScore();
        if (result.homeScore() > result.awayScore()) home.points += 3;
        else if (result.awayScore() > result.homeScore()) away.points += 3;
        else { home.points += 1; away.points += 1; }
    }

    private void printRoundResults(List<MatchResult> results, int round) {
        logMessage(String.format("\n--- %d. FORDULÓ EREDMÉNYEI ---", round));
        for (MatchResult result : results) {
            logMessage(result.toString());
        }
    }

    private void printFinalStandings(List<Group> allGroups) {
        for (Group group : allGroups) {
            logMessage("\n--- " + group.name.toUpperCase() + " ---");
            for (GroupTeam team : group.teams) {
                logMessage(team.toString());
            }
        }
    }

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