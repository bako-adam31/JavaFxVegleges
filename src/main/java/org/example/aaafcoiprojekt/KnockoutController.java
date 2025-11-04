package org.example.aaafcoiprojekt;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Ez az osztály kezeli a teljes kieséses szakaszt (knockout-view.fxml),
 * beleértve az ágrajz kirajzolását is.
 */
public class KnockoutController {

    // --- A 'Backend' Logika Változói ---
    private MatchSimulator simulator;
    private List<Match> currentKnockoutMatches = new ArrayList<>();
    private List<Group> allGroups; // Ezt a csoportkörből kapjuk meg
    private int currentRound = 4; // A kieséses szakasz 4. fordulóként indul

    // --- Az FXML-ből Behúzott Elemek ---
    @FXML private AnchorPane bracketPane; // A bal oldali ágrajz helye
    @FXML private Button nextRoundButton; // A jobb oldali gomb
    @FXML private TextArea logTextArea; // A jobb oldali napló
    @FXML private VBox winnerDisplayBox; // <-- EZT ADD HOZZÁ

    // A kirajzolt meccs-dobozokat (VBox) itt tároljuk
    private List<VBox> matchNodes = new ArrayList<>();

    @FXML
    public void initialize() {
        this.simulator = new MatchSimulator();
        nextRoundButton.setText("Nyolcaddöntő Indítása");

        if (winnerDisplayBox != null) {
            winnerDisplayBox.setVisible(false); // Induláskor rejtett
        }
    }

    /**
     * Ezt a metódust a HelloController fogja meghívni,
     * hogy átadja a csoportkör végeredményét.
     */
    public void initData(List<Group> allGroups) {
        this.allGroups = allGroups;

        // Azonnal előkészítjük és kirajzoljuk a nyolcaddöntőt
        prepareKnockoutStage();
    }

    /**
     * Ez a metódus fut le az FXML-ben lévő 'nextRoundButton' megnyomásakor.
     */
    @FXML
    private void onNextRoundClick() {
        // Leszimulálja az aktuális fordulót és előkészíti (kirajzolja) a következőt
        simulateKnockoutRound();
    }

    private void logMessage(String message) {
        logTextArea.appendText(message + "\n");
    }

    // =========================================================================
    // --- KIESÉSES SZAKASZ LOGIKÁJA ÉS RAJZOLÁSA ---
    // =========================================================================

    /**
     * Előkészíti és KIRAJZOLJA a nyolcaddöntőt az AnchorPane-re.
     */
    private void prepareKnockoutStage() {
        logMessage("=============================================");
        logMessage("=== EGYENES KIESÉSES SZAKASZ ===");
        logMessage("A nyolcaddöntő párosításai kialakultak:");

        currentKnockoutMatches.clear();
        bracketPane.getChildren().clear();
        matchNodes.clear();

        try {
            GroupTeam A1 = allGroups.get(0).teams.get(0);
            GroupTeam A2 = allGroups.get(0).teams.get(1);
            GroupTeam B1 = allGroups.get(1).teams.get(0);
            GroupTeam B2 = allGroups.get(1).teams.get(1);
            GroupTeam C1 = allGroups.get(2).teams.get(0);
            GroupTeam C2 = allGroups.get(2).teams.get(1);
            GroupTeam D1 = allGroups.get(3).teams.get(0);
            GroupTeam D2 = allGroups.get(3).teams.get(1);
            GroupTeam E1 = allGroups.get(4).teams.get(0);
            GroupTeam E2 = allGroups.get(4).teams.get(1);
            GroupTeam F1 = allGroups.get(5).teams.get(0);
            GroupTeam F2 = allGroups.get(5).teams.get(1);
            GroupTeam G1 = allGroups.get(6).teams.get(0);
            GroupTeam G2 = allGroups.get(6).teams.get(1);
            GroupTeam H1 = allGroups.get(7).teams.get(0);
            GroupTeam H2 = allGroups.get(7).teams.get(1);

            // BAL ÁG
            currentKnockoutMatches.add(new Match(A1, C2)); // 0
            currentKnockoutMatches.add(new Match(B1, D2)); // 1
            currentKnockoutMatches.add(new Match(A2, C1)); // 2
            currentKnockoutMatches.add(new Match(B2, D1)); // 3
            // JOBB ÁG
            currentKnockoutMatches.add(new Match(E1, F2)); // 4
            currentKnockoutMatches.add(new Match(G1, H2)); // 5
            currentKnockoutMatches.add(new Match(E2, F1)); // 6
            currentKnockoutMatches.add(new Match(G2, H1)); // 7

            // --- KIRAJZOLÁS az AnchorPane-re ---

            // BAL ÁG (X = 50)
            matchNodes.add(drawMatchNode(currentKnockoutMatches.get(0), 50.0, 50.0));
            matchNodes.add(drawMatchNode(currentKnockoutMatches.get(1), 50.0, 150.0));
            matchNodes.add(drawMatchNode(currentKnockoutMatches.get(2), 50.0, 250.0));
            matchNodes.add(drawMatchNode(currentKnockoutMatches.get(3), 50.0, 350.0));

            // JOBB ÁG (X = 600)
            matchNodes.add(drawMatchNode(currentKnockoutMatches.get(4), 800.0, 50.0));
            matchNodes.add(drawMatchNode(currentKnockoutMatches.get(5), 800.0, 150.0));
            matchNodes.add(drawMatchNode(currentKnockoutMatches.get(6), 800.0, 250.0));
            matchNodes.add(drawMatchNode(currentKnockoutMatches.get(7), 800.0, 350.0));

            for (Match match : currentKnockoutMatches) {
                logMessage("- " + match.home().getName() + " vs " + match.away().getName());
            }

        } catch (Exception e) {
            logMessage("HIBA a kieséses szakasz előkészítésekor: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Leszimulál egy teljes kieséses fordulót, és KIRAJZOLJA a következő fordulót.
     */
    private void simulateKnockoutRound() {
        String roundName = "";
        // Ezek a koordináták határozzák meg, hova rajzoljuk a győzteseket
        double nextRoundX_Left = 0;
        double nextRoundX_Right = 0;

        switch (currentRound) {
            case 4:
                roundName = "NYOLCADDÖNTŐ";
                nextRoundX_Left = 200.0;  // Negyeddöntő X pozíció (bal)
                nextRoundX_Right = 650.0; // Negyeddöntő X pozíció (jobb)
                break;
            case 5:
                roundName = "NEGYEDDÖNTŐ";
                nextRoundX_Left = 350.0;  // Elődöntő X pozíció (bal)
                nextRoundX_Right = 525.0; // Elődöntő X pozíció (jobb)
                break;
            case 6:
                roundName = "ELŐDÖNTŐ";
                nextRoundX_Left = 450.0;  // Döntő X pozíció
                break;
            case 7:
                roundName = "DÖNTŐ";
                break; // A döntő után már nem rajzolunk
            default:
                logMessage("A szimuláció már véget ért!");
                return;
        }

        logMessage("\n=============================================");
        logMessage(String.format("=== %s EREDMÉNYEI ===", roundName));

        List<MatchResult> roundResults = new ArrayList<>();
        List<GroupTeam> winners = new ArrayList<>(); // A következő forduló résztvevői

        List<VBox> newMatchNodes = new ArrayList<>(); // Az új, kirajzolt VBox-okat gyűjtjük

        // Végigmegyünk az aktuális forduló meccsein
        for (Match match : currentKnockoutMatches) {
            MatchResult result;
            do {
                result = simulator.simulate(match);
            } while (result.homeScore() == result.awayScore());

            roundResults.add(result);

            if (result.homeScore() > result.awayScore()) {
                winners.add(match.home());
            } else {
                winners.add(match.away());
            }
        }

        // Kiírjuk az eredményeket a jobb oldali naplóba
        printRoundResults(roundResults, currentRound);

        // --- Felkészülés a KÖVETKEZŐ fordulóra ---
        currentKnockoutMatches.clear(); // Töröljük az épp lejátszott meccseket

        if (winners.size() > 1) {
            // Még nincs vége, párosítsuk a győzteseket
            String nextRoundName = "";
            switch (currentRound + 1) {
                case 5: nextRoundName = "negyeddöntő"; break;
                case 6: nextRoundName = "elődöntő"; break;
                case 7: nextRoundName = "döntő"; break;
            }
            logMessage(String.format("\nKialakult a(z) %s mezőnye:", nextRoundName));

            // A győztesekből új meccseket csinálunk és KIRAJZOLJUK
            for (int i = 0; i < winners.size(); i += 2) {
                Match newMatch = new Match(winners.get(i), winners.get(i + 1));
                currentKnockoutMatches.add(newMatch);
                logMessage("- " + newMatch.home().getName() + " vs " + newMatch.away().getName());

                // KIRAJZOLÁS: Kiszámoljuk, hova kerüljön az új meccs
                double xPos = 0;
                double yPos = 0;

                if (currentRound == 4) { // Nyolcaddöntő -> Negyeddöntő
                    xPos = (i < winners.size() / 2) ? nextRoundX_Left : nextRoundX_Right;

                    // === ITT VOLT A HIBA ===
                    // Az i*2 helyett 'i'-t és 'i+1'-et használunk
                    VBox prevMatch1 = matchNodes.get(i);
                    VBox prevMatch2 = matchNodes.get(i + 1);
                    double y1 = AnchorPane.getTopAnchor(prevMatch1);
                    double y2 = AnchorPane.getTopAnchor(prevMatch2);
                    yPos = (y1 + y2) / 2; // Y pozíció középen

                } else if (currentRound == 5) { // Negyeddöntő -> Elődöntő
                    xPos = (i < winners.size() / 2) ? nextRoundX_Left : nextRoundX_Right;

                    // === ITT VOLT A HIBA ===
                    VBox prevMatch1 = matchNodes.get(i);
                    VBox prevMatch2 = matchNodes.get(i + 1);
                    double y1 = AnchorPane.getTopAnchor(prevMatch1);
                    double y2 = AnchorPane.getTopAnchor(prevMatch2);
                    yPos = (y1 + y2) / 2; // Y pozíció középen

                } else if (currentRound == 6) { // Elődöntő -> DÖNTŐ
                    // A te kérésed szerint a döntőt a dobozba rajzoljuk
                    xPos = 450.0;  // Kb. középen
                    yPos = 400.0;  // Kb. alul
                }

                VBox newMatchNode;
                if (currentRound == 6) {
                    // Ha a 6. forduló (Elődöntő) futott le, akkor a DÖNTŐT rajzoljuk
                    // a NAGYOBB stílusú metódussal (drawFinalMatchNode)
                    newMatchNode = drawFinalMatchNode(newMatch, xPos, yPos);
                } else {
                    // Különben (Nyolcaddöntő/Negyeddöntő) a NORMÁL meccset rajzoljuk
                    newMatchNode = drawMatchNode(newMatch, xPos, yPos);
                }
                // --- EDDIG TART ---

                newMatchNodes.add(newMatchNode); // Elmentjük az új VBox-ot
            }

            matchNodes = newMatchNodes; // Frissítjük a listát a következő fordulóhoz
            nextRoundButton.setText(nextRoundName.substring(0, 1).toUpperCase() + nextRoundName.substring(1) + " Indítása");

        } else if (winners.size() == 1) {
            // --- MEGVAN A GYŐZTES! ---
            GroupTeam theWinner = winners.get(0);
            logMessage("\n=============================================");
            logMessage(String.format("🏆🏆🏆 A VILÁGBAJNOK: %s 🏆🏆🏆", theWinner.getName()));
            nextRoundButton.setText("VÉGE");
            nextRoundButton.setDisable(true);

            // --- JAVÍTOTT, BIZTONSÁGOS KIJELZÉS ---

            // Csak akkor próbáljuk meg, ha a VBox be van kötve
            if (winnerDisplayBox != null) {
                winnerDisplayBox.getChildren().clear();

                // Zászló létrehozása
                final double WINNER_FLAG_SIZE = 128.0;
                ImageView winnerFlag = new ImageView();
                try {
                    String imagePath = "images/flags/" + theWinner.team.flagFile();
                    Image flagImage = new Image(getClass().getResourceAsStream(imagePath));
                    winnerFlag.setImage(flagImage);
                    winnerFlag.setFitWidth(WINNER_FLAG_SIZE);
                    winnerFlag.setFitHeight(WINNER_FLAG_SIZE);

                    Circle clip = new Circle(WINNER_FLAG_SIZE / 2);
                    clip.setCenterX(WINNER_FLAG_SIZE / 2);
                    clip.setCenterY(WINNER_FLAG_SIZE / 2);
                    winnerFlag.setClip(clip);
                } catch (Exception e) {
                    System.out.println("Hiba a győztes zászló betöltésekor: " + theWinner.team.flagFile());
                }

                // Név létrehozása
                Label winnerLabel = new Label(theWinner.getName());
                winnerLabel.getStyleClass().add("winner-label"); // CSS stílus

                // Hozzáadás a VBox-hoz
                winnerDisplayBox.getChildren().addAll(winnerFlag, winnerLabel);

                // És most tesszük láthatóvá
                winnerDisplayBox.setVisible(true);

            } else {
                // Ha a VBox nincs bekötve, kiírjuk a hibát a naplóba ahelyett, hogy összeomlanánk
                logMessage("HIBA: A 'winnerDisplayBox' nincs összekötve az FXML-lel!");
                System.out.println("HIBA: winnerDisplayBox 'null'. Ellenőrizd az fx:id-t a Scene Builderben!");
            }
        }

        currentRound++; // Ez a sor marad a végén
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

    private VBox drawMatchNode(Match match, double x, double y) {
        HBox homeRow = createTeamRow(match.home());
        HBox awayRow = createTeamRow(match.away());

        VBox matchBox = new VBox(5);
        matchBox.getChildren().addAll(homeRow, awayRow);
        matchBox.setStyle("-fx-background-color: rgba(0, 0, 0, 0.4); -fx-background-radius: 5; -fx-padding: 5px;");

        AnchorPane.setLeftAnchor(matchBox, x);
        AnchorPane.setTopAnchor(matchBox, y);

        bracketPane.getChildren().add(matchBox);

        return matchBox;
    }

    private void printRoundResults(List<MatchResult> results, int round) {
        String roundName = "";
        switch (round) {
            case 4: roundName = "Nyolcaddöntő"; break;
            case 5: roundName = "Negyeddöntő"; break;
            case 6: roundName = "Elődöntő"; break;
            case 7: roundName = "Döntő"; break;
        }

        logMessage(String.format("\n--- %s Eredményei ---", roundName));
        for (MatchResult result : results) {
            logMessage(result.toString());
        }
    }

    /**
     * Segédfüggvény: Létrehoz egy NAGYOBB sort (Zászló + Név) a DÖNTŐHÖZ.
     */
    private HBox createFinalTeamRow(GroupTeam team) {
        final double FLAG_SIZE = 40.0; // <-- NAGYOBB ZÁSZLÓ (pl. 40px)
        ImageView flagIcon = new ImageView();
        try {
            String imagePath = "images/flags/" + team.team.flagFile();
            Image flagImage = new Image(getClass().getResourceAsStream(imagePath));
            flagIcon.setImage(flagImage);
            flagIcon.setFitWidth(FLAG_SIZE);
            flagIcon.setFitHeight(FLAG_SIZE);
            // Kerekítés
            Circle clip = new Circle(FLAG_SIZE / 2);
            clip.setCenterX(FLAG_SIZE / 2);
            clip.setCenterY(FLAG_SIZE / 2);
            flagIcon.setClip(clip);
        } catch (Exception e) {
            System.out.println("Hiba a kép betöltésekor: " + team.team.flagFile());
        }

        Label teamLabel = new Label(team.getName());
        teamLabel.setTextFill(Color.WHITE);
        // NAGYOBB BETŰMÉRET
        teamLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 22px;");

        HBox teamRow = new HBox(10); // Nagyobb térköz
        teamRow.getChildren().addAll(flagIcon, teamLabel);
        return teamRow;
    }

    /**
     * Segédfüggvény: Létrehoz egy "meccs" dobozt a DÖNTŐHÖZ (nagyobb méretben).
     */
    private VBox drawFinalMatchNode(Match match, double x, double y) {
        // Létrehozzuk a NAGY csapat sorokat
        HBox homeRow = createFinalTeamRow(match.home());
        HBox awayRow = createFinalTeamRow(match.away());

        // Egy VBox-ba rakjuk őket (egymás alá)
        VBox matchBox = new VBox(10); // Nagyobb belső térköz
        matchBox.getChildren().addAll(homeRow, awayRow);

        // Stílus beállítása (pl. nagyobb belső margó)
        matchBox.setStyle("-fx-background-color: rgba(0, 0, 0, 0.6); -fx-background-radius: 10; -fx-padding: 15px;");

        AnchorPane.setLeftAnchor(matchBox, x);
        AnchorPane.setTopAnchor(matchBox, y);

        bracketPane.getChildren().add(matchBox);

        return matchBox; // Visszaadjuk
    }
}