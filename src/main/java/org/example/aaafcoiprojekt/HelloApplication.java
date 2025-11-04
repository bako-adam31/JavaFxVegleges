package org.example.aaafcoiprojekt;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.scene.text.Font;

import java.io.IOException;

public class HelloApplication extends Application {
    @Override
    public void start(Stage stage) throws IOException {
        Font.loadFont(getClass().getResource("fonts/NotoColorEmoji-Regular.ttf").toExternalForm(), 12);
        FXMLLoader fxmlLoader = new FXMLLoader(HelloApplication.class.getResource("hello-view.fxml"));
        Scene scene = new Scene(fxmlLoader.load(), 320, 240);

        String cssPath = getClass().getResource("style.css").toExternalForm();
        scene.getStylesheets().add(cssPath);

        stage.setTitle("WORLD CUP SIMULATOR");
        stage.setScene(scene);

        stage.setFullScreen(true);

        stage.show();
    }
}
