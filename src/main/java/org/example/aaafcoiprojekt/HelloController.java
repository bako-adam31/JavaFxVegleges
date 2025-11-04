package org.example.aaafcoiprojekt;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Ez az osztály kezeli a teljes szimulációs logikát és a grafikus felületet.
 */
public class HelloController {

    // --- A 'Backend' Logika Változói ---
    private DatabaseManager dbManager;
    private MatchSimulator simulator;
    private List<Group> allGroups;
    private int currentRound;

    private List<Match> currentKnockoutMatches = new ArrayList<>();


    // --- Az FXML-ből Behúzott (Injektált) Elemek ---

    // Vezérlők a jobb oldalon
    @FXML private Button forduloButton;
    @FXML private TextArea eredmenyTextArea; // Az eredmény-napló

    // A 8 csoport-panel a bal oldalon (VBox-ok, nem TextArea-k!)
    @FXML private VBox groupAVBox;
    @FXML private VBox groupBVBox;
    @FXML private VBox groupCVBox;
    @FXML private VBox groupDVBox;
    @FXML private VBox groupEVBox;
    @FXML private VBox groupFVBox;
    @FXML private VBox groupGVBox;
    @FXML private VBox groupHVBox;

    @FXML private VBox winnerVBox; // <-- EZ AZ ÚJ SOR

    // Egy lista, ami tárolja a 8 VBox-ot a könnyebb kezelésért
    private List<VBox> groupVBoxes;

    /**
     * Ez a metódus automatikusan lefut, miután az FXML betöltődött.
     * Ez az új "indítási pontunk".
     */
    @FXML
    public void initialize() {
        // 1. Logikai motor inicializálása
        dbManager = new DatabaseManager();
        simulator = new MatchSimulator();
        allGroups = new ArrayList<>();
        currentRound = 1;

        // 2. A 8 VBox összegyűjtése egy listába
        groupVBoxes = List.of(
                groupAVBox, groupBVBox, groupCVBox, groupDVBox,
                groupEVBox, groupFVBox, groupGVBox, groupHVBox
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

            // Eredmények kiírása a JOBB oldali ablakba
            printRoundResults(roundResults, currentRound);

            currentRound++; // Lépés a következő fordulóra

            if (currentRound > 3) {
                // --- 2. ÁTMENETI ÁLLAPOT (A 3. forduló épp lement) ---

                // Gomb átállítása a Nyolcaddöntőre
                forduloButton.setText("Nyolcaddöntő Indítása");

                logMessage("\n--- A CSOPORTKÖR VÉGEREDMÉNYE ---");

                // Végső tabella kiírása a JOBB oldali naplóba
                printFinalStandings(allGroups);
                // BAL oldali panelek frissítése a végeredménnyel
                updateAllGroupDisplays(true);

                // Előkészítjük a kieséses szakaszt (ez az új metódus)
                prepareKnockoutStage();

            } else {
                // Felkészülés a következő csoportkör fordulóra
                forduloButton.setText(String.format("%d. Forduló Indítása", currentRound));
            }

        } else {
            // --- 3. KIESÉSES SZAKASZ ÁLLAPOT (currentRound > 3) ---
            // Ha a currentRound 4 vagy több, már a kieséses szakaszban vagyunk

            // Leszimulálja az aktuális fordulót (pl. Nyolcaddöntő)
            // és előkészíti a következőt (pl. Negyeddöntő)
            simulateKnockoutRound();
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
    // --- GUI FRISSÍTŐ METÓDUSOK (EZEK AZ ÚJAK) ---
    // =========================================================================

    /**
     * Lefuttatja a sorsolást és azonnal frissíti a bal oldali VBox-okat.
     */
    private void runDrawAndDisplayGroups() {
        logMessage("Sorsolás indítása...");

        Map<String, List<Team>> drawnGroupsMap = runDraw(); // A logikát futtatja

        if (drawnGroupsMap.isEmpty()) {
            //logMessage("Hiba: A sorsolás sikertelen! Ellenőrizd az adatbázist (32 csapat?).");
            forduloButton.setText("Hiba!");
            return;
        }

        this.allGroups.clear();
        for (Map.Entry<String, List<Team>> entry : drawnGroupsMap.entrySet()) {
            this.allGroups.add(new Group(entry.getKey(), entry.getValue()));
        }

        logMessage("Sikeres sorsolás! A csoportok kialakítva.");

        // --- CSOPORTOK KIÍRÁSA A BAL OLDALRA ---
        updateAllGroupDisplays(false); // false = még ne mutassuk a pontokat

        // Előkészülünk az 1. fordulóra
        currentRound = 1;
        forduloButton.setDisable(false); // Gomb engedélyezése
        forduloButton.setText("1. Forduló Indítása");
    }

    /**
     * Frissíti a bal oldali 8 szövegdobozt a végső, rendezett állással.
     */
    private void updateGroupPanelsWithFinalStandings() {
       // logMessage("\n--- A CSOPORTKÖR VÉGEREDMÉNYE (bal oldali panelek frissítve) ---");

        // A régi 'for' ciklus (ami a TextArea-ba írt) helyett:
        updateAllGroupDisplays(true); // true = mutasd a pontokat is
    }

    /**
     * Frissíti mind a 8 csoport (VBox) tartalmát zászlókkal és nevekkel.
     * @param showScores Ha true, a pontszámokat is kiírja (a torna végén).
     */
    private void updateAllGroupDisplays(boolean showScores) {

        for (int i = 0; i < allGroups.size(); i++) {
            Group group = allGroups.get(i);
            VBox targetVBox = groupVBoxes.get(i); // A megfelelő (A, B, C...) VBox

            targetVBox.getChildren().clear(); // Előző tartalom törlése!

            // Ha a torna végén járunk, rendezzük a listát
            if (showScores) {
                // Névtelen Comparator osztály
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

            // Most végigmegyünk a (rendezett vagy rendezetlen) csapatokon
            int rank = 1; // Ezt a sorszámozáshoz használjuk
            for (GroupTeam team : group.teams) {

                // 1. Létrehozzuk a (Zászló + Név) sort a segédfüggvénnyel
                HBox teamRow = createTeamRow(team);

                // 2. Hozzáadjuk a pontszámot, HA KELL (showScores == true)
                // (Az általad küldött kódban ez a rész hibás volt, ez a javított verzió)
                if (showScores) {

                    // Létrehozzuk a formázott szöveget (Név, Pont, Gólkülönbség)
                    String scoreText = String.format("%s %dp | %d GD",
                            team.getName(),
                            team.getPoints(),
                            team.getGoalDifference()
                    );

                    // Kicseréljük a HBox-ban lévő sima nevet erre a pontszámosra
                    // (A get(1) a HBox második elemét, a Label-t jelenti)
                    ((Label)teamRow.getChildren().get(1)).setText(scoreText);
                }

                // 3. A HBox-ot (ami a Zászlót és a Szöveget tartalmazza)
                //    hozzáadjuk a VBox-hoz
                targetVBox.getChildren().add(teamRow);
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
     * (LAMBDA NÉLKÜL)
     */

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
    }

    /**
     * Lefuttatja a sorsolást (adatbázis-olvasás, keverés, stb.)
     * (LAMBDA NÉLKÜL)
     */
    private Map<String, List<Team>> runDraw() {
        List<Team> allTeams = dbManager.getTeams();

        if (allTeams.size() != 32) {
            //logMessage("Hiba: A sorsoláshoz pontosan 32 csapat kell! (Találat: " + allTeams.size() + ")");
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
            //logMessage("FIGYELEM: " + unplacedTeams.size() + " csapatot nem sikerült szabályosan elhelyezni, erőltetett elhelyezés...");
            for (Team stuckTeam : unplacedTeams) {
                for (List<Team> group : groups.values()) {
                    if (group.size()< 4) {
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

    /**
     * Előkészíti a nyolcaddöntőt a csoportkör végeredménye alapján.
     * A továbbjutókat a kért logika (A1-C2, stb.) szerint párosítja.
     * Csak a JOBB oldali ablakba (naplóba) ír.
     */
    private void prepareKnockoutStage() {
        logMessage("\n=============================================");
        logMessage("=== EGYENES KIESÉSES SZAKASZ ===");
        logMessage("A nyolcaddöntő párosításai kialakultak:");

        currentKnockoutMatches.clear(); // Töröljük a korábbi meccseket

        // FONTOS: Az 'updateAllGroupDisplays(true)' hívás a gombnyomáskor
        // már sorba rendezte a csapatokat minden csoportban (pont, majd GD szerint).
        // Tehát az allGroups.get(0).teams.get(0) az A1, allGroups.get(0).teams.get(1) az A2.

        try {
            // Csoportok kinyerése (0=A, 1=B, 2=C, 3=D, 4=E, 5=F, 6=G, 7=H)
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

            // Párosítások létrehozása a kérésed alapján
            currentKnockoutMatches.add(new Match(A1, C2)); // A1 - C2
            currentKnockoutMatches.add(new Match(B1, D2)); // B1 - D2
            currentKnockoutMatches.add(new Match(A2, C1)); // A2 - C1
            currentKnockoutMatches.add(new Match(B2, D1)); // B2 - D1
            currentKnockoutMatches.add(new Match(E1, F2)); // E1 - F2
            currentKnockoutMatches.add(new Match(G1, H2)); // G1 - H2
            currentKnockoutMatches.add(new Match(E2, F1)); // E2 - F1
            currentKnockoutMatches.add(new Match(G2, H1)); // G2 - H1

            // Párosítások kiírása a jobb oldali naplóba
            for (Match match : currentKnockoutMatches) {
                logMessage("- " + match.home().getName() + " vs " + match.away().getName());
            }

        } catch (Exception e) {
            logMessage("HIBA a kieséses szakasz előkészítésekor: " + e.getMessage());
            e.printStackTrace();
        }

        displayCurrentKnockoutMatches();
    }

    /**
     * Leszimulál egy teljes kieséses fordulót (Nyolcaddöntő, Negyeddöntő, stb.)
     * Kezeli a döntetleneket (újraszámolással), és előkészíti a következő forduló győzteseit.
     * Csak a JOBB oldali ablakba (naplóba) ír.
     */
    private void simulateKnockoutRound() {
        String roundName = "";
        switch (currentRound) {
            case 4: roundName = "NYOLCADDÖNTŐ"; break;
            case 5: roundName = "NEGYEDDÖNTŐ"; break;
            case 6: roundName = "ELŐDÖNTŐ"; break;
            case 7: roundName = "DÖNTŐ"; break;
            default:
                logMessage("A szimuláció már véget ért!");
                return;
        }

        logMessage("\n=============================================");
        logMessage(String.format("=== %s EREDMÉNYEI ===", roundName));

        List<MatchResult> roundResults = new ArrayList<>();
        List<GroupTeam> winners = new ArrayList<>(); // A következő forduló résztvevői

        // Végigmegyünk az aktuális forduló (pl. nyolcaddöntő) meccsein
        for (Match match : currentKnockoutMatches) {
            MatchResult result;

            // FONTOS: Kieséses szakaszban nincs döntetlen!
            // Addig szimuláljuk újra a meccset, amíg nem lesz győztes.
            do {
                result = simulator.simulate(match);
            } while (result.homeScore() == result.awayScore());

            roundResults.add(result);

            // A győztest hozzáadjuk a következő kör listájához
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
            // Még nincs vége, párosítsuk a győzteseket (pl. 8-ból lesz 4 meccs)

            String nextRoundName = "";
            switch (currentRound + 1) {
                case 5: nextRoundName = "negyeddöntő"; break;
                case 6: nextRoundName = "elődöntő"; break;
                case 7: nextRoundName = "döntő"; break;
            }
            logMessage(String.format("\nKialakult a(z) %s mezőnye:", nextRoundName));

            // Egyszerűen párosítjuk a győzteseket (1. vs 2., 3. vs 4., stb.)
            for (int i = 0; i < winners.size(); i += 2) {
                Match newMatch = new Match(winners.get(i), winners.get(i + 1));
                currentKnockoutMatches.add(newMatch);
                logMessage("- " + newMatch.home().getName() + " vs " + newMatch.away().getName());
            }
            // Beállítjuk a gombot a következő fordulóra
            forduloButton.setText(nextRoundName.substring(0, 1).toUpperCase() + nextRoundName.substring(1) + " Indítása");

        } else if (winners.size() == 1) {
            // MEGVAN A GYŐZTES!
            GroupTeam theWinner = winners.get(0);

            // Kiírjuk a naplóba (jobb oldalra)
            logMessage("\n=============================================");
            logMessage(String.format("🏆🏆🏆 A VILÁGBAJNOK: %s 🏆🏆🏆", theWinner.getName()));
            forduloButton.setText("VÉGE");
            forduloButton.setDisable(true);

            // --- EZ AZ ÚJ RÉSZ A KÖZÉPSŐ KIÍRÁSHOZ ---

            // 1. Töröljük a VBox korábbi tartalmát (ha volt)
            winnerVBox.getChildren().clear();

            // 2. Létrehozzuk a győztes zászlaját (nagy méretben)
            ImageView winnerFlag = new ImageView();
            final double WINNER_FLAG_SIZE = 128.0; // Legyen jó nagy
            try {
                String imagePath = "images/flags/" + theWinner.team.flagFile();
                Image flagImage = new Image(getClass().getResourceAsStream(imagePath));
                winnerFlag.setImage(flagImage);
                winnerFlag.setFitWidth(WINNER_FLAG_SIZE);
                winnerFlag.setFitHeight(WINNER_FLAG_SIZE);

                // Kerekítés (ha a PNG-id nem kerekek, hagyd benne)
                Circle clip = new Circle(WINNER_FLAG_SIZE / 2);
                clip.setCenterX(WINNER_FLAG_SIZE / 2);
                clip.setCenterY(WINNER_FLAG_SIZE / 2);
                winnerFlag.setClip(clip);

            } catch (Exception e) {
                System.out.println("Hiba a győztes zászló betöltésekor: " + theWinner.team.flagFile());
            }


            // 3. Létrehozzuk a győztes nevét (nagy méretben)
            Label winnerLabel = new Label(theWinner.getName());

            // Hozzáadjuk a CSS osztályt ("winner-label")
            winnerLabel.getStyleClass().add("winner-label");

            // 4. Hozzáadjuk a zászlót és a nevet a középső VBox-hoz
            winnerVBox.getChildren().addAll(winnerFlag, winnerLabel);
        }

        currentRound++; // Ezt a sort ne töröld ki, ha az 'else if' után van
        displayCurrentKnockoutMatches();
    }

    /**
     * Segédfüggvény, ami egy csapatból létrehoz egy HBox-ot (Zászló + Név).
     * Ezt használjuk mind a csoportkör, mind a kieséses ág rajzolásához.
     * @param team A csapat (GroupTeam), akit meg akarunk jeleníteni
     * @return Egy HBox, ami a kerek zászlót és a nevet tartalmazza
     */
    private HBox createTeamRow(GroupTeam team) {
        final double FLAG_SIZE = 20.0; // Zászló mérete

        // 1. Zászló (ImageView) létrehozása
        ImageView flagIcon = new ImageView();
        try {
            String imagePath = "images/flags/" + team.team.flagFile();
            Image flagImage = new Image(getClass().getResourceAsStream(imagePath));

            flagIcon.setImage(flagImage);
            flagIcon.setFitWidth(FLAG_SIZE);
            flagIcon.setFitHeight(FLAG_SIZE);
        } catch (Exception e) {
            System.out.println("Hiba a kép betöltésekor: " + team.team.flagFile());
        }



        // 3. Név (Label) létrehozása
        Label teamLabel = new Label(team.getName());
        teamLabel.setTextFill(Color.WHITE);
        // A betűstílust (pl. bold, size) már a 'style.css' kezeli
        // a '.group-box .label' szabályon keresztül

        // 4. Egy HBox-ba (vízszintes dobozba) tesszük őket
        HBox teamRow = new HBox(5); // 5 pixel hely a zászló és a név között
        teamRow.getChildren().addAll(flagIcon, teamLabel);

        return teamRow;
    }


    /**
     * Frissíti a bal oldali 8 VBox-ot, hogy az AKTUÁLIS kieséses meccseket mutassa.
     * (Nyolcaddöntő: 8 meccs, Negyeddöntő: 4 meccs, stb.)
     */
    private void displayCurrentKnockoutMatches() {

        // 1. Először Töröljük az összes VBox tartalmát (a régi csoportkör adatait)
        for (VBox box : groupVBoxes) {
            box.getChildren().clear();
        }

        // 2. Végigmegyünk az aktuális meccseken és kirajzoljuk őket
        // (Ez a lista a nyolcaddöntőben 8 meccset, a negyeddöntőben 4-et, stb. tartalmaz)
        for (int i = 0; i < currentKnockoutMatches.size(); i++) {

            // Megkeressük a VBox-ot, ahova rajzolni kell (pl. groupAVBox)
            VBox targetVBox = groupVBoxes.get(i);
            Match match = currentKnockoutMatches.get(i);

            // 3. Létrehozzuk a "Home" csapat sorát (Zászló + Név)
            HBox homeRow = createTeamRow(match.home());

            // 4. Létrehozunk egy "vs" elválasztót
            Label vsLabel = new Label("vs");
            vsLabel.setTextFill(Color.WHITE);
            vsLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 10px; -fx-opacity: 0.7;");

            // 5. Létrehozzuk az "Away" csapat sorát (Zászló + Név)
            HBox awayRow = createTeamRow(match.away());

            // 6. Hozzáadjuk a 3 elemet a VBox-hoz
            targetVBox.getChildren().addAll(homeRow, vsLabel, awayRow);
        }
    }
}